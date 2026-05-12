package fallen;

import arc.*;
import arc.graphics.Color;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.content.Blocks;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import static mindustry.Vars.ui;

public class SimpleAdminSettings extends BaseDialog {

    public SimpleAdminSettings() {
        super(Core.bundle.get("sam.settings.title"));
        addCloseButton();
        setup();
    }

    private void setup() {
        cont.clear();
        cont.table(Styles.black6, main -> {
            main.margin(20);
            main.pane(table -> {
                table.left().defaults().left().pad(4);

                // --- СЕКЦИЯ: ИНТЕРФЕЙС ---
                header(table, "sam.settings.interface");

                addSlider(table, "sam.settings.btnSize", "sam-btn-size", 30, 80, 1, 40);
                addSlider(table, "sam.settings.hudY", "sam-hud-y", -600, 600, 10, 60);
                addSlider(table, "sam.settings.listW", "sam-list-w", 200, 1000, 10, 400);

                table.check(Core.bundle.get("sam.settings.closeOutside"), Core.settings.getBool("sam-close-outside", true), val -> Core.settings.put("sam-close-outside", val)).row();
                table.check(Core.bundle.get("sam.settings.closeListOnInfo"), Core.settings.getBool("sam-close-list", false), val -> Core.settings.put("sam-close-list", val)).row();
                table.check(Core.bundle.get("sam.settings.closeListOutside"), Core.settings.getBool("sam-close-listoutside", false), val -> Core.settings.put("sam-close-listoutside", val)).row();

                // --- СЕКЦИЯ: ФУНКЦИИ ---
                header(table, "sam.settings.functions");

                table.check(Core.bundle.get("sam.settings.stats"), Core.settings.getBool("sam-show-stats", false), val -> Core.settings.put("sam-show-stats", val)).row();
                table.check(Core.bundle.get("sam.settings.save"), Core.settings.getBool("sam-log-save", false), val -> Core.settings.put("sam-log-save", val)).row();
                table.check(Core.bundle.get("sam.settings.vanish"), Core.settings.getBool("sam-vanish", false), val -> Core.settings.put("sam-vanish", val)).row();
                table.check(Core.bundle.get("sam.settings.fastlang"), Core.settings.getBool("sam-fastlang", false), val -> Core.settings.put("sam-fastlang", val)).row();
                table.check(Core.bundle.get("sam.settings.freecam"), Core.settings.getBool("sam-freecam", false), val -> Core.settings.put("sam-freecam", val)).row();

                // --- СЕКЦИЯ: АНТИ-ГРИФ ---
                header(table, "sam.settings.antiGrief");

                table.check(Core.bundle.get("sam.settings.agEnabled"), Core.settings.getBool("sam-ag-enabled", false), val -> Core.settings.put("sam-ag-enabled", val)).row();
                table.check(Core.bundle.get("sam.settings.agAFreeze"), Core.settings.getBool("sam-ag-afr", false), val -> Core.settings.put("sam-ag-afr", val)).row();

                addIntInput(table, "sam.settings.agMinBuild", "sam-ag-min-build", 10);
                addIntInput(table, "sam.settings.agMaxBreak", "sam-ag-max-break", 100);
                addIntInput(table, "sam.settings.agMinJoins", "sam-ag-min-joins", 5);
                addIntInput(table, "sam.settings.agMaxKicks", "sam-ag-max-kicks", 1);

                table.check(Core.bundle.get("sam.settings.agBuildWarn"), Core.settings.getBool("sam-ag-build-warn", true), val -> Core.settings.put("sam-ag-build-warn", val)).row();

                // Настройки по блокам
                table.add(Core.bundle.get("sam.settings.agBlocks")).padTop(10).color(Pal.accent).row();
                addBlockAgSetting(table, Blocks.thoriumReactor, "thorium");
                addBlockAgSetting(table, Blocks.incinerator, "incinerator");
                addBlockAgSetting(table, Blocks.melter, "melter");

                // Кнопка сброса
                table.button(Core.bundle.get("sam.settings.resetSettings"), Icon.refresh, () -> {
                    Core.settings.put("sam-btn-size", 40);
                    Core.settings.put("sam-hud-y", 60);
                    Core.settings.put("sam-list-w", 400);
                    setup();
                    ui.showInfoFade(Core.bundle.get("sam.settings.resetDone"));
                }).width(240f).height(50f).padTop(20);
                table.row();

                // --- СЕКЦИЯ: АНТИ-АТТЕМ ---
                header(table, "sam.settings.iHateAttems");
                table.check(Core.bundle.get("sam.settings.iHateAttems"), Core.settings.getBool("sam-aa", false), val -> Core.settings.put("sam-aa", val)).row();
                table.check(Core.bundle.get("sam.settings.onAttemAlarms"), Core.settings.getBool("sam-oaa", false), val -> Core.settings.put("sam-oaa", val)).row();
                //table.check(Core.bundle.get("sam.settings.aaBan"), Core.settings.getBool("sam-aab", false), val -> Core.settings.put("sam-aab", val)).row();


            }).grow();
        }).width(650f).fillY().center();
    }

    private void header(Table table, String bundleKey) {
        table.add(Core.bundle.get(bundleKey)).left().color(Pal.accent).padTop(20).row();
        table.image().color(Pal.accent).fillX().height(2).padBottom(10).row();
    }

    private void addSlider(Table t, String bundleKey, String key, int min, int max, int step, int def) {
        t.table(s -> {
            s.left().defaults().left();

            // Группируем текст и цифру в одну строку, чтобы они были рядом
            s.table(labels -> {
                labels.left();
                labels.add(Core.bundle.get(bundleKey) + ": ").color(Color.lightGray);
                labels.add(new Label(() -> String.valueOf(Core.settings.getInt(key, def)))).color(Pal.accent);
            }).left().row();

            // Сам слайдер
            s.slider(min, max, step, Core.settings.getInt(key, def), val -> {
                Core.settings.put(key, (int)val);
            }).width(450f).height(40f).padTop(2); // Задаем фиксированную ширину слайдера

        }).padTop(4).padBottom(4).left().row();
    }

    private void addBlockAgSetting(Table t, mindustry.world.Block block, String key) {
        t.table(s -> {
            s.left().defaults().left();

            s.check(block.localizedName + " " + mindustry.ui.Fonts.getUnicodeStr(block.name),
                    Core.settings.getBool("sam-ag-" + key + "-enabled", true),
                    val -> Core.settings.put("sam-ag-" + key + "-enabled", val)).padBottom(2).row();

            Table sub = new Table();
            sub.left();
            addSlider(sub, "sam.settings.agBlockRadius", "sam-ag-" + key + "-radius", 0, 100, 1, 10);
            s.add(sub).padLeft(20f);

        }).left().padBottom(10).row();
    }

    private void addIntInput(Table t, String bundleKey, String key, int def) {
        t.table(i -> {
            i.left();
            i.add(Core.bundle.get(bundleKey) + ": ").color(Color.lightGray).width(230f);

            i.field(String.valueOf(Core.settings.getInt(key, def)), text -> {
                if (Strings.canParseInt(text)) {
                    Core.settings.put(key, Strings.parseInt(text));
                }
            }).width(100f).get().setMessageText(String.valueOf(def));
        }).left().padBottom(4).row();
    }
}