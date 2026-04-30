package fallen;

import arc.*;
import arc.graphics.Color;
import arc.struct.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.input.MobileInput;
import mindustry.mod.Mod;
import mindustry.net.Administration;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.TraceDialog;

import static mindustry.Vars.*;

public class SimpleAdminMode extends Mod {
    private SimpleAdminList adminList = new SimpleAdminList();
    public static ObjectMap<Integer, PlayerData> playerHistory = new ObjectMap<>();
    private ObjectSet<Integer> autoTraceRequested = new ObjectSet<>();
    private IntSet knownPlayerIds = new IntSet();
    private static TraceDialog originalTraces;
    private static ObjectMap<Integer, Float> lastAutoTime = new ObjectMap<>();


    public SimpleAdminMode() {
        Events.on(ClientLoadEvent.class, e -> {
            PlayerStatsTracker.init();
            AntiAttemPatcher.load();
            adminList.build(Core.scene.root);
            setupSettings();
            setupTraceOverride(); // Первый запуск

            // === ПОДМЕНА INPUT HANDLER ДЛЯ МОБИЛЬНЫХ ===
            if (Vars.mobile && !(Vars.control.input instanceof FreeCamMobileInput) && Core.settings.getBool("sam-freecam", false)) {
                MobileInput oldInput = (MobileInput) Vars.control.input;
                FreeCamMobileInput newInput = new FreeCamMobileInput();

                // Копируем важные поля состояния:
                newInput.block = oldInput.block;
                newInput.rotation = oldInput.rotation;
                newInput.mode = oldInput.mode;  // ← важно!
                newInput.selectPlans.addAll(oldInput.selectPlans);
                newInput.linePlans.addAll(oldInput.linePlans);
                newInput.down = oldInput.down;  // ← важно для pan()!
                newInput.manualShooting = oldInput.manualShooting;

                // Копируем целевые позиции, чтобы не было скачков:
                newInput.targetPos.set(oldInput.targetPos);
                newInput.movement.set(oldInput.movement);

                Vars.control.input = newInput;
            }

            ui.hudGroup.fill(t -> {
                t.name = "sam-hud-button";
                t.right();
                t.table(bt -> {
                    // Создаем кнопку и сохраняем ссылку на её ячейку (Cell)
                    var adminBtnCell = bt.button(Icon.admin, () -> {
                    }).size(50f);
                    var adminBtn = adminBtnCell.get();
                    final float[] holdTimer = {0f};
                    final boolean[] longPressedTriggered = {false};
                    adminBtn.update(() -> {
                        if (adminBtn.isPressed()) {
                            holdTimer[0] += Core.graphics.getDeltaTime() * 60f;
                            if (holdTimer[0] > 60f && !longPressedTriggered[0]) {
                                Call.sendChatMessage("/history");
                                longPressedTriggered[0] = true;
                            }
                        } else {
                            holdTimer[0] = 0f;
                        }
                    });

                    adminBtn.clicked(() -> {
                        if (!longPressedTriggered[0]) adminList.toggle();
                        longPressedTriggered[0] = false;
                    });


                    // --- КНОПКА 2: СВОБОДНАЯ КАМЕРА ---
                    if (Vars.mobile && Core.settings.getBool("sam-freecam", false)) {
                        bt.row();
                        bt.button(Icon.move, Styles.clearNoneTogglei, () -> {
                            if (Vars.control.input instanceof FreeCamMobileInput fi) {
                                fi.setFreeCam(!fi.isFreeCam());
                            }
                        }).update(b -> {
                            boolean active = false;
                            if (Vars.control.input instanceof FreeCamMobileInput fi) {
                                active = fi.isFreeCam();
                            }
                            b.setChecked(active);
                            b.getImage().setColor(active ? Color.cyan : Color.white);
                        }).size(45f).tooltip("Free Camera");
                    }

                    // --- ЛОГИКА СМЕЩЕНИЯ (Y) ---
                    final int[] lastY = {-1};
                    bt.update(() -> {
                        int currentY = Core.settings.getInt("sam-hud-y", 60);
                        if (lastY[0] != currentY) {
                            bt.margin(currentY, 0, 0, 10f);
                            lastY[0] = currentY;
                            bt.invalidateHierarchy();
                        }
                    });
                });
            });
        });

//        Events.on(PlayerJoin.class, e -> {
//            Time.run(60f, () -> processPlayer(e.player));
//            Vars.player.sendMessage("PlayerJoin");
//
//        });//
//        Events.on(PlayerLeave.class, e -> {
//            PlayerData data = playerHistory.get(e.player.id);
//            if (data != null) data.online = false;
//            Vars.player.sendMessage("PlayerLeave");
//        });

        Events.on(WorldLoadEvent.class, e -> {
            playerHistory.clear();
            autoTraceRequested.clear();
            knownPlayerIds.clear();
            lastAutoTime.clear();

            setupTraceOverride();

            Timer.schedule(() -> {
                if(net.client() && player.unit() != null && Core.settings.getBool("sam-vanish", false)){
                    Call.sendChatMessage("/vanish 1");
                    Log.info("[#00ff]Vanish on");
                }
            }, 5f);

            if(Vars.state.isMenu() || !Vars.net.active()) return;
            Time.run(120f, () -> {
                for (Player p : Groups.player) processPlayer(p);
            });
        });

        Events.run(Trigger.update, () -> {
            if(Vars.state.isMenu() || !Vars.net.active()) return;
            if (net.active() && state.isGame() && Core.graphics.getFrameId() % 60 == 0) {
                try {
                    syncPlayers();
                } catch (Exception ex) {
                    Log.err("Ошибка в syncPlayers", ex);
                }            }
            if (net.active() && state.isGame() && Core.graphics.getFrameId() % 300 == 0) {
                checkGriefers();
            }
        });
    }

    private void checkGriefers() {
        if (!Core.settings.getBool("sam-ag-enabled", false)) return;

        int minB = Core.settings.getInt("sam-ag-min-build", 10);
        int maxBr = Core.settings.getInt("sam-ag-max-break", 100);
        int minJ = Core.settings.getInt("sam-ag-min-joins", 5);
        int maxK = Core.settings.getInt("sam-ag-max-kicks", 1);

        for (PlayerData data : playerHistory.values()) {
            if (!data.online || data.uuid.equals("Loading...")) continue;

            // --- ЛОГИКА 1: ПРЕДУПРЕЖДЕНИЕ ---
            if (!data.griefWarned) {
                boolean suspicious = (data.builds < minB) &&
                        (data.breaks > maxBr) &&
                        (data.timesJoined < minJ) &&
                        (data.timesKicked >= maxK);

                if (suspicious) {
                    data.griefWarned = true;
                    Vars.player.sendMessage(Core.bundle.format("sam.ag.alert", data.name) + "\n" +
                            Core.bundle.format("sam.ag.stats", data.builds, data.breaks, data.timesJoined, data.timesKicked));
                }
            }

            // --- ЛОГИКА 2: АВТО-ФРИЗ (Независимая) ---
            if (!data.player.admin && !data.autoFrozen && Core.settings.getBool("sam-ag-afr", false)) {
                if ((data.builds < minB * 2) && data.breaks > maxBr * 2 ) {
                    data.autoFrozen = true;

                    Call.sendChatMessage("/freeze " + data.uuid);
                    Vars.player.sendMessage(Core.bundle.format("sam.ag.autoFreeze", data.name, maxBr * 2));
                }
            }
        }
    }

    private void setupSettings() {
        ui.settings.addCategory(Core.bundle.get("sam.settings.title"), Icon.admin, table -> {
            table.left().row();
            table.check(Core.bundle.get("sam.settings.stats"), Core.settings.getBool("sam-show-stats", false), val -> {
                Core.settings.put("sam-show-stats", val);
            }).left().row();

            table.check(Core.bundle.get("sam.settings.vanish"), Core.settings.getBool("sam-vanish", false), val -> {
                Core.settings.put("sam-vanish", val);
            }).left().row();

            table.check(Core.bundle.get("sam.settings.freecam"), Core.settings.getBool("sam-freecam", false), val -> {
                Core.settings.put("sam-freecam", val);
            }).left().row();

            addSlider(table, "sam.settings.btnSize", "sam-btn-size", 30, 80, 40);
            addSlider(table, "sam.settings.hudY", "sam-hud-y", 0, 600, 60);

            table.button(Core.bundle.get("sam.settings.resetSettings"), () -> {
                Core.settings.put("sam-show-stats", false);
                Core.settings.put("sam-btn-size", 40);
                Core.settings.put("sam-hud-y", 60);
                ui.showInfoFade(Core.bundle.get("sam.settings.resetDone"));
            }).margin(10).width(240f).padTop(20f);
        });
    }

    private void addSlider(arc.scene.ui.layout.Table table, String bundleKey, String settingKey, int min, int max, int def) {
        table.table(t -> {
            t.left().defaults().left();
            t.add(Core.bundle.get(bundleKey)).row();

            t.table(s -> {
                s.slider(min, max, 1, Core.settings.getInt(settingKey, def), val -> {
                    Core.settings.put(settingKey, (int)val);
                }).width(350f).height(50f).padRight(10f);

                s.label(() -> String.valueOf(Core.settings.getInt(settingKey, def))).color(mindustry.graphics.Pal.accent).width(40f);
            }).row();
        }).padTop(10f).row();
    }

    private void setupTraceOverride() {
        if (originalTraces == null) {
            originalTraces = ui.traces;
        }

        // Если ui.traces сбросился
        if (!(ui.traces instanceof CustomTraceDialog)) {
            ui.traces = new CustomTraceDialog();
            PlayerData.setAutoTraceRequested(autoTraceRequested);
            Log.info("SimpleAdminMode: TraceDialog перехвачен");
        }
    }

    private void syncPlayers() {
        IntSet currentIds = new IntSet();
        for (Player p : Groups.player) {
            currentIds.add(p.id);
            processPlayer(p);
        }

        // Новые игроки
        currentIds.each(id -> {
            if (!knownPlayerIds.contains(id)) {
                Player p = Groups.player.getByID(id);
                if (p != null) {
                    Log.info("SimpleAdminMode: Новый игрок ID=@ (name=@)", id, p.name);
                    ui.showInfoFade("[green]+ [white]" + p.name);
                }
            }
        });

        // Вышедшие игроки
        knownPlayerIds.each(id -> {
            if (!currentIds.contains(id)) {
                Log.info("SimpleAdminMode: Игрок вышел ID=@ ", id);
                PlayerData data = playerHistory.get(id);
                if (data != null) {
                    data.online = false;
                    data.stopTraceRequests();
                    ui.showInfoFade("[red]- [white]" + data.name);
                }
            }
        });

        knownPlayerIds.clear();
        knownPlayerIds.addAll(currentIds);
    }

    private void processPlayer(Player p) {
        PlayerData data = playerHistory.get(p.id);

        if (data == null) {
            data = new PlayerData(p);
            playerHistory.put(p.id, data);
            // Запускаем периодические запросы
            data.startTraceRequests(p);
        } else {
            data.name = p.name;
            data.player = p;
        }
    }

    public class CustomTraceDialog extends TraceDialog {
        @Override
        public void show(Player player, Administration.TraceInfo info) {
            if (!handleTraceLogic(player, info)) {
                originalTraces.show(player, info);
            }
        }

        public void show(Player player, Administration.TraceInfo info, boolean offline) {//фикс дял клиента
            if (!handleTraceLogic(player, info)) {
                try {
                    var method = originalTraces.getClass().getMethod("show", Player.class, Administration.TraceInfo.class, boolean.class);
                    method.invoke(originalTraces, player, info, offline);
                } catch (Exception e) {
                    originalTraces.show(player, info);
                }
            }
        }

        private boolean handleTraceLogic(Player player, Administration.TraceInfo info) {
            PlayerData data = playerHistory.get(player.id);
            boolean wasAuto = autoTraceRequested.contains(player.id);
            if (data != null && info.uuid != null && !info.uuid.isEmpty()) {
                data.updateFrom(info);
            }
            float lastTime = lastAutoTime.get(player.id, 0f);
            boolean isDuplicate = (Time.time - lastTime < 1.5f);
            if (wasAuto || isDuplicate) {
                lastAutoTime.put(player.id, Time.time);
                Log.info("📋 [accent]" + player.name + "[white]: " + (data != null && data.uuid.equals("admin?") ? "админ (локально)" : "данные получены"));
                return true;
            }
            return false;
        }
    }
}