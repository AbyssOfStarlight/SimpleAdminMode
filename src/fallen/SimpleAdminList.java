package fallen;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.KeyCode;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.net.*;
import mindustry.net.Packets.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;


import static fallen.SimpleAdminMode.playerHistory;
import static mindustry.Vars.*;

public class SimpleAdminList{
    public Table content = new Table().marginRight(13f).marginLeft(13f);
    private Table mainTable;
    private Table infoPanel;
    private boolean visible = false;
    private Interval timer = new Interval();
    private TextField search;
    private String manualUuid = "";
    private float button_size = 40f;
    private float panelWidth = 400f;

    public void build(Group parent){
        content.name = "players";
        parent.fill(cont -> {
            cont.name = "playerlist";
            cont.visible(() -> visible);
            cont.touchable = Touchable.enabled;
            cont.clicked(()->{
                if(Core.settings.getBool("sam-close-listoutside", false)) {
                    this.toggle();
                }
            });
            cont.update(() -> {
                if(!(net.active() && state.isGame())){
                    visible = false;
                    return;
                }

                if(visible && timer.get(180)){
//                    if(Core.app.isDesktop()){
//                        if(Core.input.keyDown(KeyCode.mouseLeft) ||
//                                Core.input.keyDown(KeyCode.mouseRight) ||
//                                isMouseOverUI()){
//                            return;
//                        }
//                    }
                    rebuild();
                    content.pack();
                    content.act(Core.graphics.getDeltaTime());
                    Core.app.post(() -> Core.scene.act(Math.min(Core.graphics.getDeltaTime(), 0.033f)));
                }
            });

            mainTable = cont.table(Tex.buttonTrans, pane -> {
                pane.touchable = Touchable.enabled;
                pane.addListener(new InputListener() {
                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                        event.stop();
                        return true;
                    }
                });

                pane.label(() -> Core.bundle.format(playerHistory.size == 1 ? "players.single" : "players", playerHistory.size));
                pane.row();

                search = pane.field(null, text -> rebuild()).grow().pad(8).name("search").maxTextLength(maxNameLength).get();
                search.setMessageText(Core.bundle.get("players.search"));

                pane.row();
                pane.pane(content).grow().scrollX(false);
                pane.row();

                pane.table(menu -> {
                    menu.defaults().growX().height(50f).fillY();
                    menu.name = "menu";
                    // 3. ПОЛЕ РУЧНОГО ВВОДА
                    menu.table(manual -> {
                        manual.background(Styles.black3).margin(4);

                        // Поле ввода
                        TextField field = manual.field("", text -> manualUuid = text)
                                .growX()
                                .height(45)
                                .get();
                        field.setMessageText(Core.bundle.get("sam.list.manualUUID"));

                        // Кнопка открытия меню бана для этого UUID
                        manual.button(Icon.waves, Styles.clearNonei, () -> {
                            Call.sendChatMessage("/freeze " + manualUuid);
                        }).size(45).padLeft(8).tooltip(Core.bundle.get("sam.list.freeze"));

                        // Кнопка открытия меню бана для этого UUID
                        manual.button(Icon.hammer, Styles.clearNonei, () -> {
                            if (manualUuid.isEmpty()) {
                                ui.showInfoFade(Core.bundle.get("sam.info.smallUUID"));
                                return;
                            }

                            // Создаем фейкового игрока для заголовка
                            Player fake = Player.create();
                            fake.name = "[gray]Manual Entry[]";

                            new AdvancedBanDialog(fake, manualUuid).show();
                        }).size(45).padLeft(8).tooltip(Core.bundle.get("sam.list.ban"));

                    }).padTop(10).row();
                    menu.table(buttons -> {
                        buttons.defaults().height(50f).fillY();
                        buttons.button("@close", this::toggle).growX();
                        buttons.button(Icon.settings, Styles.defaulti, () -> {
                            new SimpleAdminSettings().show();
                        }).width(50f).padLeft(4f);
                    }).growX().padLeft(4f);

                }).margin(0f).pad(10f).growX();

            }).touchable(Touchable.enabled).margin(14f).minWidth(panelWidth).get();
        });

        rebuild();
    }

//    private boolean isMouseOverUI(){
//        // Проверяем, не наведена ли мышь на наш контент
//        var hit = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
//        return hit != null && (content.isDescendantOf(hit) || content == hit);
//    }

    public void rebuild(){

        float h = 50f;
        this.button_size = Core.settings.getInt("sam-btn-size", 40);
        this.panelWidth = Core.settings.getInt("sam-list-w", 400);
        if (mainTable != null) {
            mainTable.setWidth(panelWidth);
            mainTable.invalidate();
        }
        float buttonWidth = panelWidth - 60f;

        boolean found = false;


        Seq<PlayerData> filtered = Seq.with(playerHistory.values());
        if(search.getText().length() > 0){
            String query = search.getText().toLowerCase();
            filtered = filtered.copy().retainAll(d ->
                    Strings.stripColors(d.name.toLowerCase()).contains(query)
            );
        }
        filtered.sort((a, b) -> {
            // Онлайн выше оффлайна
            if (a.online != b.online) {
                return a.online ? -1 : 1;
            }
            // Админы выше
            boolean aAdmin = a.player != null && a.player.admin;
            boolean bAdmin = b.player != null && b.player.admin;
            if (aAdmin != bAdmin) {
                return aAdmin ? -1 : 1;
            }
            // По имени
            return Strings.stripColors(a.name).compareToIgnoreCase(Strings.stripColors(b.name));
        });

        content.clear();
        boolean lastWasOnline = true; // Начинаем с true, чтобы не показывать разделитель в начале

        //for(var user : players){
        //for (PlayerData user : SimpleAdminMode.playerHistory.values()) {
        for (PlayerData user : filtered) {
            found = true;

            // Разделитель между онлайн/оффлайн
            if (lastWasOnline && !user.online) {
                content.add().height(8).row();
                content.add(Core.bundle.get("sam.list.offline")).color(Pal.redLight).center().row();
                content.add().height(4).row();
            }
            lastWasOnline = user.online;

            // ОСНОВНАЯ ТАБЛИЦА ИГРОКА
            Table button = new Table();
            button.left();
            button.margin(4).marginBottom(6);
            button.background(Tex.underline);
            button.touchable = Touchable.enabled;
            button.clicked(() -> {});
            // === СТРОКА 1: Имя + Кнопка меню ===
            button.table(nameTable -> {
                nameTable.left().defaults().pad(2);

                // Иконка юнита и переход к нему
                if (user.player != null) {
                    Unit u = user.player.unit();
                    if (u != null && u.type != null) {  // Игрок онлайн и имеет юнит
                        nameTable.button(Styles.cleart.disabled, () -> {
                            control.input.spectate(user.player.unit());
                        }).width(50f).height(35f).get().image(user.player.unit().icon()).size(20f).scaling(Scaling.fit);
                    } else {  // Игрок оффлайн, но объект Player сохранился
                        nameTable.button(Icon.cancelSmall, Styles.cleari, () -> {
                            Core.camera.position.set(user.player.x, user.player.y);
                        }).width(50f).height(35f);
                    }
                } else {  // Объекта Player нет
                    nameTable.button(Icon.cancelSmall, Styles.cleari, () -> {
                            }).width(50f).height(35f);
                }
                // Имя игрока (занимает всё доступное место)
                nameTable.add(user.name).left().growX().wrap();

                nameTable.button(Icon.info, Styles.cleari, () -> {
                    showInfoPanel(user);
                    if(Core.settings.getBool("sam-close-list")){
                        this.toggle();
                    }
                }).size(button_size).margin(2f).tooltip(Core.bundle.get("sam.list.showSaveData"));

                // Кнопка меню (только для онлайн)
                if (user.online) {
                    nameTable.button(Icon.menu, Styles.cleari, () -> {
                        showPlayerMenu(user);
                    }).size(button_size).margin(2f).tooltip(Core.bundle.get("sam.list.admActions"));
                }
            }).growX().row();

            // === СТРОКА 2: Статус + UUID + Кнопки ===
            button.table(infoTable -> {
                infoTable.left().defaults().height(28).pad(1);
                // UUID
                String uuidText = user.uuid.equals("admin?") ? "[green]admin" :
                        user.uuid.equals("Loading...") ? "[gray]waiting..." :
                                user.uuid.equals("none") ? "[gray]none" :
                                        user.uuid;
                infoTable.add("[accent]UUID: [white]" + uuidText).growX().left();
                if(Core.settings.getBool("sam-fastlang", false)){infoTable.add("[accent] L: [white]" + user.locale).right();}

                if(Core.settings.getBool("sam-show-stats", false)) {
                    infoTable.button(st -> {
                        st.defaults().padLeft(2).padRight(2).fontScale(0.8f);
                        st.add(new Label(() -> "[green]" + user.builds + "[]| [red]" + user.breaks + "[]| [sky]" + user.configs)).minWidth(60);
                    }, Styles.flatBordert, () -> {
                        HistoryRender.setTarget(user.name);
                    }).right().height(24).padRight(4);
                }
                // Кнопки действий
                if (user.online) {
                    infoTable.button(Icon.wavesSmall, Styles.cleari, () -> {
                        if (!user.uuid.equals("Loading...") && !user.uuid.equals("none")) {
                            Call.sendChatMessage("/freeze " + user.uuid);
                        } else {
                            ui.showInfoFade("[red]UUID ещё не получен");
                        }
                    }).size(button_size).margin(2f).tooltip(Core.bundle.get("sam.list.freeze"));
                }

                infoTable.button(Icon.hammer, Styles.cleari, () -> {
                    if (!user.uuid.equals("Loading...") && !user.uuid.equals("none")) {
                        Player p = Groups.player.getByID(user.id);
                        if (p == null) p = Player.create();
                        p.name = user.name;
                        new AdvancedBanDialog(p, user.uuid).show();
                    } else {
                        ui.showInfoFade("[red]UUID ещё не получен");
                    }
                }).size(button_size).margin(2f).tooltip(Core.bundle.get("sam.list.ban"));
            }).growX();

            content.add(button).width(buttonWidth).padBottom(4);
            content.row();
        }
        if(!found){
            content.add(Core.bundle.format("players.notfound")).padBottom(6).width(350f).maxHeight(h + 14);
        }

        content.marginBottom(5);

    }

    private void showPlayerMenu(PlayerData user) {
        var dialog = new BaseDialog(user.name);
        dialog.title.setColor(Color.white);
        dialog.titleTable.remove();
        dialog.closeOnBack();

        var bstyle = Styles.defaultt;

        dialog.cont.add(user.name).left().row();
        dialog.cont.image(Tex.whiteui, Pal.accent).fillX().height(3f).pad(4f).row();

        dialog.cont.pane(t -> {
            t.defaults().size(220f, 55f).pad(3f);

            t.button("@player.ban", Icon.hammer, bstyle, () -> {
                ui.showConfirm("@confirm", Core.bundle.format("confirmban", user.name),
                        () -> Call.adminRequest(user.player, AdminAction.ban, null));
                dialog.hide();
            }).row();

            t.button("@player.kick", Icon.cancel, bstyle, () -> {
                ui.showConfirm("@confirm", Core.bundle.format("confirmkick", user.name),
                        () -> Call.adminRequest(user.player, AdminAction.kick, null));
                dialog.hide();
            }).row();

            t.button("@player.trace", Icon.zoom, bstyle, () -> {
                Call.adminRequest(user.player, AdminAction.trace, null);
                dialog.hide();
            }).row();

        }).row();

        dialog.cont.button("@back", Icon.left, dialog::hide).padTop(-1f).size(220f, 55f);
        dialog.show();
    }

    public void toggle(){
        visible = !visible;
        if(visible){
            rebuild();
        }else{
            Core.scene.setKeyboardFocus(null);
            search.clearText();
        }
    }


    private void showInfoPanel(PlayerData data) {
        if(infoPanel != null) infoPanel.remove();

        infoPanel = new Table();
        infoPanel.setFillParent(true);
        infoPanel.touchable = Touchable.enabled;
        infoPanel.clicked(() -> {
            if(Core.settings.getBool("sam-close-outside", true)){
                infoPanel.remove();
                infoPanel = null;
            }
        });

        infoPanel.table(Tex.buttonTrans, t -> {
            t.touchable = Touchable.enabled;
            t.clicked(() -> {});

            t.addListener(new arc.scene.event.InputListener() {
                @Override
                public boolean touchDown(arc.scene.event.InputEvent event, float x, float y, int pointer, arc.input.KeyCode button) {
                    event.stop();
                    return true;
                }
            });

            t.margin(12).defaults().left();

            t.table(h -> {
                h.add(new Image(Icon.infoSmall)).padRight(8);
                h.add(Core.bundle.format("sam.info.title", data.name)).growX();
                h.button(Icon.leftOpen, Styles.cleari, () -> {
                    infoPanel.remove();
                    infoPanel = null;
                    this.toggle();
                }).size(32);
                h.button(Icon.cancel, Styles.cleari, () -> {
                    infoPanel.remove();
                    infoPanel = null;
                }).size(32);
            }).growX().row();

            t.image().color(Pal.accent).fillX().height(2).padTop(4).padBottom(8).row();

            t.pane(p -> {
                p.defaults().left().growX().margin(2);

                addCopyRow(p, Core.bundle.get("sam.info.name"), data.name);
                addCopyRow(p, Core.bundle.get("sam.info.uuid"), data.uuid);
                addCopyRow(p, Core.bundle.get("sam.info.ip"), data.ip);
                addCopyRow(p, Core.bundle.get("sam.info.lang"), data.locale);
                addCopyRow(p, Core.bundle.get("sam.info.joins"), String.valueOf(data.timesJoined));
                addCopyRow(p, Core.bundle.get("sam.info.kicks"), String.valueOf(data.timesKicked));
                addCopyRow(p, Core.bundle.get("sam.info.mobile"), data.mobile ? Core.bundle.get("sam.info.yes") : Core.bundle.get("sam.info.no"));
                addCopyRow(p, Core.bundle.get("sam.info.modded"), data.modded ? Core.bundle.get("sam.info.yes") : Core.bundle.get("sam.info.no"));

                if(data.names.length > 1) {
                    p.add(Core.bundle.get("sam.info.history.name")).padTop(8).row();
                    for(String s : data.names) addCopyRow(p, "", s);
                }

                if(data.ips.length > 1) {
                    p.add(Core.bundle.get("sam.info.history.ip")).padTop(8).row();
                    for(String s : data.ips) addCopyRow(p, "", s);
                }
            }).size(340, 300).row();

            t.button(Core.bundle.get("sam.info.stats"), Icon.refresh, () -> {
                Call.sendChatMessage("/stats " + data.uuid);
                infoPanel.remove();
            }).margin(10).growX().height(45).padTop(10).row();

            if(data.online) {
                t.button(Core.bundle.get("sam.info.update"), Icon.refresh, () -> {
                    Player p = Groups.player.getByID(data.id);
                    if(p != null) Call.adminRequest(p, Packets.AdminAction.trace, null);
                    infoPanel.remove();
                }).margin(10).growX().height(45).padTop(10);
            }

        }).center();

        Core.scene.add(infoPanel);
    }

    private void addCopyRow(Table table, String label, String value) {
        if(value == null || value.isEmpty()) return;

        String displayText = label.isEmpty() ? "[white]" + value : "[lightgray]" + label + ": [accent]" + value;

        table.button(b -> {
            b.left().margin(4);
            b.add(displayText).left().wrap().growX().fontScale(0.9f);
        }, Styles.flatBordert, () -> {
            Core.app.setClipboardText(value);
            ui.showInfoFade("[accent]" + (label.isEmpty() ? value : label) + Core.bundle.get("sam.info.copy"));
        }).growX().height(32).padBottom(2).row();
    }

}