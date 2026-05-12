package fallen;

import arc.Events;
import arc.math.Mathf;
import arc.util.Strings;
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

            if(Core.settings.getBool("sam-log-save", false)){
                short blockId;
                if (e.tile.build instanceof ConstructBlock.ConstructBuild cons) {
                    blockId = cons.current.id;
                } else {
                    blockId = e.tile.block().id;
                }

                int rotation = e.tile.build != null ? e.tile.build.rotation : 0;
                Object config = e.tile.build != null ? e.tile.build.config() : null;

                ActionsHistory.blocksplayersplans.addFirst(new ActionsHistory.BlockPlayerPlan(
                        e.tile.x, e.tile.y, (short) rotation,
                        blockId, config,
                        Strings.stripColors(data.name), e.breaking
                ));
                //Log.info("Block: " + (e.breaking ? "Removed " : "Placed ") + Vars.content.block(blockId).localizedName);
            }

            if(e.breaking) data.breaks++;
            else data.builds++;
        });

        Events.on(ConfigEvent.class, e -> {
            if(!Core.settings.getBool("sam-show-stats", false) || e.player == null) return;
            PlayerData data = playerHistory.get(e.player.id);
            if(data != null) {
                data.configs++;
                if(Core.settings.getBool("sam-log-save", false)){
                    ActionsHistory.blockconfplayersplans.addFirst(new ActionsHistory.BlockConfigPlayerPlan( (int)e.tile.x/8, (int)e.tile.y/8, e.tile.block.id, data.name));
                }
            }
        });

        Events.on(BuildRotateEvent.class, e -> {
            if(!Core.settings.getBool("sam-show-stats", false) || e.unit == null || e.unit.getPlayer() == null) return;
            PlayerData data = playerHistory.get(e.unit.getPlayer().id);
            if(data != null) {
                data.configs++;
                if(Core.settings.getBool("sam-log-save", false)){
                    ActionsHistory.blockconfplayersplans.addFirst(new ActionsHistory.BlockConfigPlayerPlan( (int)e.build.x/8, (int)e.build.y/8, e.build.block.id, data.name));
                }
            }
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
                        e.unit.getPlayer().name, block.localizedName + " " + mindustry.ui.Fonts.getUnicodeStr(block.name) + "(" + Mathf.round(e.tile.getX()/8) + " , " + Mathf.round(e.tile.getY()/8) + ")"));
            }
        }
    }
}