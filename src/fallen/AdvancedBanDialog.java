package fallen;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.ui.*;
import mindustry.ui.dialogs.BaseDialog;
import static mindustry.Vars.ui;

public class AdvancedBanDialog extends BaseDialog {
    private String currentScope = Core.settings.getBool("sam-default-select-all", false) ? "all" : "here";

    public AdvancedBanDialog(Player player, String uuid) {
        super(Core.bundle.format("sam.ban.title", Strings.stripColors(player.name)));
        addCloseButton();

        cont.table(top -> {
            top.add("[lightgray]UUID: [accent]" + uuid).padRight(20);

            top.table(st -> {
                ButtonGroup<Button> sg = new ButtonGroup<>();
                st.button("HERE", Styles.togglet, () -> currentScope = "here").size(80, 40).group(sg).checked(currentScope.equals("here"));
                st.button("ALL", Styles.togglet, () -> currentScope = "all").size(80, 40).group(sg).checked(currentScope.equals("all"));
                st.button("Attack", Styles.togglet, () -> currentScope = "attack").size(80, 40).group(sg);
                st.button("Survival", Styles.togglet, () -> currentScope = "survival").size(80, 40).group(sg);
                st.button("PvP", Styles.togglet, () -> currentScope = "pvp").size(80, 40).group(sg);

                //st.button("E_ATK", Styles.togglet, () -> currentScope = "eattack").size(80, 40).group(sg);
                //st.button("E_SRV", Styles.togglet, () -> currentScope = "esurvival").size(80, 40).group(sg);
            });
        }).row();

        cont.image().color(Pal.accent).fillX().height(3).pad(10).row();

        cont.pane(table -> {
            table.defaults().pad(4).fillX();

            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.2_1"), Core.bundle.get("sam.ban.rule.2_1"), 1, 30, 14, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.2_2"), Core.bundle.get("sam.ban.rule.2_2"), 1, 14, 1, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.2_3"), Core.bundle.get("sam.ban.rule.2_3"), 1, 7, 7, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.3"), Core.bundle.get("sam.ban.rule.3"), 1, 90, 7, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.4"), Core.bundle.get("sam.ban.rule.4"), 1, 14, 1, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.5"), Core.bundle.get("sam.ban.rule.5_1"), 1, 30, 2, "d");

            table.image().color(Pal.redLight).fillX().height(2).padBottom(10).row();

            // so many many rules...

            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.2_1_1"), Core.bundle.get("sam.ban.rule.2_1_1"), 3, 14, 14, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.2_1_2"), Core.bundle.get("sam.ban.rule.2_1_2"), 14, 90, 30, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.2_1_3"), Core.bundle.get("sam.ban.rule.2_1_3"), 14, 90, 14, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.2_1_4"), Core.bundle.get("sam.ban.rule.2_1_4"), 7, 30, 14, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.2_1_5"), Core.bundle.get("sam.ban.rule.2_1_5"), 30, 90, 30, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.2_1_6"), Core.bundle.get("sam.ban.rule.2_1_6"), 1, 90, 14, "d");

            table.image().color(Pal.accent).fillX().height(2).padBottom(10).row();

            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.2_2_1"), Core.bundle.get("sam.ban.rule.2_2_1"), 1, 14, 3, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.2_2_2"), Core.bundle.get("sam.ban.rule.2_2_2"), 1, 7, 3, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.2_2_3"), Core.bundle.get("sam.ban.rule.2_2_3"), 1, 7, 3, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.2_2_4"), Core.bundle.get("sam.ban.rule.2_2_4"), 1, 14, 3, "d");

            table.image().color(Pal.accent).fillX().height(2).padBottom(10).row();

            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.2_3_1"), Core.bundle.get("sam.ban.rule.2_3_1"), 1, 14, 3, "perm");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.2_3_2"), Core.bundle.get("sam.ban.rule.2_3_2"), 1, 14, 3, "d");

            table.image().color(Pal.accent).fillX().height(2).padBottom(10).row();

            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.3_1"), Core.bundle.get("sam.ban.rule.3_1"), 14, 30, 14, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.3_2"), Core.bundle.get("sam.ban.rule.3_2"), 14, 90, 14, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.3_3"), Core.bundle.get("sam.ban.rule.3_3"), 7, 14, 14, "d");

            table.image().color(Pal.accent).fillX().height(2).padBottom(10).row();

            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.4_1"), Core.bundle.get("sam.ban.rule.4_1"), 1, 14, 14, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.4_2"), Core.bundle.get("sam.ban.rule.4_2"), 1, 14, 2, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.4_3"), Core.bundle.get("sam.ban.rule.4_3"), 3, 7, 3, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.4_4"), Core.bundle.get("sam.ban.rule.4_4"), 0, 0, 0, "perm");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.4_5"), Core.bundle.get("sam.ban.rule.4_5"), 3, 14, 3, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.4_6"), Core.bundle.get("sam.ban.rule.4_6"), 0, 0, 0, "perm");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.4_7"), Core.bundle.get("sam.ban.rule.4_7"), 7, 30, 30, "d");


            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.5_1"), Core.bundle.get("sam.ban.rule.5_1"), 1, 30, 3, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.5_2"), Core.bundle.get("sam.ban.rule.5_2"), 0, 0, 0, "perm");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.5_3"), Core.bundle.get("sam.ban.rule.5_3"), 1, 30, 7, "d");
            addRuleRow(table, uuid, Core.bundle.get("sam.ban.rule.5_4"), Core.bundle.get("sam.ban.rule.5_4"), 3, 14, 14, "d");

        }).grow().row();

        // Ручной ввод причины бана
        cont.row();
        cont.table(Styles.black3, custom -> {
            custom.defaults().pad(4).fillX();
            custom.image().color(Pal.health).fillX().height(2).padBottom(10).row();

            final String[] customReason = {""};
            final int[] customTime = {1}; // Значение по умолчанию (1 день)

            custom.table(inputs -> {
                inputs.add("Своя причина: ").left().color(Pal.accent).row();
                inputs.field("", text -> customReason[0] = text).growX().pad(5).get().setMessageText("Введите причину...");

                inputs.row();

                inputs.table(timeTable -> {
                    timeTable.add("Срок: ").left();
                    Label timeLabel = timeTable.add("1d").width(60).color(Pal.accent).get();
                    timeTable.slider(1, 31, 1, 1, v -> {
                        customTime[0] = (int)v;
                        timeLabel.setText(customTime[0] > 30 ? "perm" : customTime[0] + "d");
                    }).growX();
                }).growX();
            }).growX();

            custom.button(Icon.hammer, () -> {
                if(customReason[0].trim().isEmpty()){
                    ui.showInfoFade("Введите причину!");
                    return;
                }
                String timeStr = customTime[0] > 30 ? "perm" : customTime[0] + "d";
                executeBan(uuid, timeStr, currentScope, customReason[0]);
            }).size(60).padLeft(10).color(Color.scarlet);

        }).growX().pad(10);
    }

    private void addRuleRow(Table table, String uuid, String desc, String ruleId, int min, int max, int def, String unit) {
        table.table(row -> {
            row.background(Tex.underline);

            if (unit.equals("perm")) {
                row.button("[red]" + desc, () -> executeBan(uuid, "perm", currentScope, ruleId)).height(60).growX();
                return;
            }

            final int[] currentVal = {def};
            TextButton btn = row.button("[red]" + desc, () -> {
                String finalTime = (currentVal[0] > max) ? "perm" : (currentVal[0] + unit);
                executeBan(uuid, finalTime, currentScope, ruleId);
            }).height(60).width(350).get();
            btn.getLabel().setWrap(true);

            row.table(s -> {
                Label l = s.add("").width(70).get();
                Runnable updateLabel = () -> l.setText(currentVal[0] > max ? "[accent]perm" : currentVal[0] + unit);
                updateLabel.run();

                s.slider(min, max + 1, 1, currentVal[0], v -> {
                    currentVal[0] = (int)v;
                    updateLabel.run();
                }).width(120);
            }).padLeft(10);
        }).margin(5).row();
    }

    private void executeBan(String uuid, String time, String scope, String reason) {
        String cmd = Strings.format("/ban @ @ @ @", uuid, time, scope, reason);
        Call.sendChatMessage(cmd);
        Vars.player.sendMessage("[gray][Sent]: [white]" + cmd);
        hide();
    }
}