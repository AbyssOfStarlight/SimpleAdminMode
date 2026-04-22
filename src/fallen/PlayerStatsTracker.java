package fallen;

import arc.Events;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.EventType.*;
import static fallen.SimpleAdminMode.playerHistory;
import arc.Core;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.blocks.ConstructBlock;
import mindustry.ui.Fonts;

public class PlayerStatsTracker {
    public static void init() {

        Events.on(BlockBuildBeginEvent.class, e -> {
            if(Core.settings.getBool("sam-ag-build-warn", true) || e.breaking) {
                antiGrief(e);
            }
            if(!Core.settings.getBool("sam-show-stats", false)) return;
            if(e.unit == null) return;
            if(e.unit.getPlayer() == null) return;
            if(playerHistory == null) return;

            PlayerData data = playerHistory.get(e.unit.getPlayer().id);

            if(data == null) return;

            if(e.breaking) data.breaks++;
            else data.builds++;
        });

        Events.on(ConfigEvent.class, e -> {
            if(!Core.settings.getBool("sam-show-stats", false) || e.player == null) return;
            PlayerData data = playerHistory.get(e.player.id);
            if(data != null) data.configs++;
        });

        Events.on(BuildRotateEvent.class, e -> {
            if(!Core.settings.getBool("sam-show-stats", false) || e.unit == null || e.unit.getPlayer() == null) return;
            PlayerData data = playerHistory.get(e.unit.getPlayer().id);
            if(data != null) data.configs++;
        });
    }

    private static void antiGrief(BlockBuildBeginEvent e) {
        if (e.breaking || e.unit == null || e.unit.getPlayer() == null) return;
        if (!(e.tile.build instanceof ConstructBlock.ConstructBuild cons)) return;

        Block block = cons.current;
        String key = "";

        // 1. Определяем, на какой блок мы "наступили" и какой ключ настроек использовать
        if (block == Blocks.thoriumReactor) key = "thorium";
        else if (block == Blocks.incinerator) key = "incinerator";
        else if (block == Blocks.melter) key = "melter";

        // Если блок не в нашем списке — выходим
        if (key.isEmpty()) return;

        // 2. Проверяем, включен ли детектор именно для этого блока
        if (!Core.settings.getBool("sam-ag-" + key + "-enabled", true)) return;

        PlayerData data = playerHistory.get(e.unit.getPlayer().id);
        if (data == null || data.uuid.equals("Loading...")) return;

        // 3. Условия по кикам/входам (общие)
        int minJ = Core.settings.getInt("sam-ag-min-joins", 5);
        int maxK = Core.settings.getInt("sam-ag-max-kicks", 1);

        if (data.timesJoined < minJ && data.timesKicked >= maxK) {
            var cores = e.unit.team().cores();
            if (cores.isEmpty()) return;

            Building closestCore = cores.min(c -> c.dst(e.tile));

            // 4. Берем ИНДИВИДУАЛЬНЫЙ радиус для этого типа блока
            float radius = Core.settings.getInt("sam-ag-" + key + "-radius", 40);

            if (e.tile.dst(closestCore) < radius * Vars.tilesize) {
                Vars.player.sendMessage(Core.bundle.format("sam.ag.buildAlert",
                        e.unit.getPlayer().name, block.localizedName + " " + mindustry.ui.Fonts.getUnicodeStr(block.name) + "(" + e.tile.getX()/8 + "," + e.tile.getY()/8 + ")"));
            }
        }
    }
}