package fallen;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.util.*;
import mindustry.game.EventType;
import mindustry.game.EventType.*;
import mindustry.graphics.*;

import mindustry.world.Block;

import static mindustry.Vars.*;


public class HistoryRender {
    public static String targetNick = null;
    private static float brokenFade = 0f;
    static float alphaMult = Core.settings.getInt("fadedblockallplayers", 5) / 10f;
    private static float nickStartTime = 0f;
    private static float timerAlpha = 1f;

    public static void init() {

        Events.run(EventType.Trigger.draw, () -> {
            if (!state.isGame()) return;
            drawActionHistory();
        });
    }


    public static void setTarget(String nick) {
        if (nick != null && nick.equals(targetNick)) {targetNick = null; return;}
        targetNick = nick;
        nickStartTime = Time.globalTime;
        timerAlpha = 1f;
        ui.hudfrag.showToast("[#ffaa55]Просмотр истории:\n[white]" + targetNick);
    }

    private static void drawActionHistory() {
        brokenFade = Mathf.lerpDelta(brokenFade, 1f, 0.1f);
        if (targetNick == null) return;

        float elapsed = (Time.globalTime - nickStartTime) / 60f;
        if (elapsed > 10f) {
            targetNick = null;
            timerAlpha = 0f;
        } else if (elapsed > 7f) {
            // Плавное затухание: с 7 по 10 сек
            timerAlpha = (10f - elapsed) / 3f;
        } else {
            timerAlpha = 1f;
        }

        String cleanFilter = targetNick != null ? Strings.stripColors(targetNick) : null;

        if (cleanFilter != null) {
            drawBlocks(cleanFilter);
            drawConfigs(cleanFilter);
        }
    }

    private static void drawBlocks(String filter) {

        for (ActionsHistory.BlockPlayerPlan plan : ActionsHistory.blocksplayersplans) {
            if (plan.lastacs == null) continue;

            if (filter != null) {
                if (!Strings.stripColors(plan.lastacs).toLowerCase().contains(filter.toLowerCase())) continue;
            }

            Block b = content.block(plan.block);
            if (b == null) continue;

            float px = plan.x * tilesize + b.offset;
            float py = plan.y * tilesize + b.offset;
            if (!Core.camera.bounds(Tmp.r1).grow(tilesize * 2f).contains(px, py)) continue;

            Draw.z(Layer.overlayUI);
            Draw.alpha(0.5f * brokenFade * alphaMult * timerAlpha);

            Color mix = plan.wasbreaking ? Color.red : Color.green;
            Draw.mixcol(mix, 0.4f + Mathf.absin(Time.globalTime, 6f, 0.2f));

            Draw.rect(b.fullIcon, px, py, b.rotate ? plan.rotation * 90 : 0);
            Draw.reset();
        }
    }

    private static void drawConfigs(String filter) {

        for (ActionsHistory.BlockConfigPlayerPlan plan : ActionsHistory.blockconfplayersplans) {
            if (plan.lastacs == null) continue;

            String cleanPlanName = Strings.stripColors(plan.lastacs);
            if (filter != null) {
                if (!cleanPlanName.toLowerCase().contains(filter.toLowerCase())) continue;
            }

            Block b = content.block(plan.block);
            if (b == null) continue;

            float px = plan.x * tilesize + b.offset;
            float py = plan.y * tilesize + b.offset;

            if (!Core.camera.bounds(Tmp.r1).grow(tilesize * 2f).contains(px, py)) continue;

            Draw.z(Layer.overlayUI);
            Draw.alpha(0.45f * brokenFade * alphaMult * timerAlpha);

            Draw.mixcol(Color.blue, 0.5f + Mathf.absin(Time.globalTime, 6f, 0.2f));

            Draw.rect(b.fullIcon, px, py);
            Draw.reset();
        }
    }
}