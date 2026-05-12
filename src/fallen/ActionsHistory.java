package fallen;

import arc.struct.Queue;
import arc.util.Log;
import mindustry.Vars;


public class ActionsHistory {
    private static final int MAX_HISTORY_SIZE = 50000;
    public static Queue<BlockPlayerPlan> blocksplayersplans = new LimitedQueue<>(MAX_HISTORY_SIZE, "BlocksBuild");
    public static Queue<BlockConfigPlayerPlan> blockconfplayersplans = new LimitedQueue<>(MAX_HISTORY_SIZE, "BlockConfigs");

    public static void clearactionhistory() {
        blocksplayersplans.clear();
        blockconfplayersplans.clear();

        ((LimitedQueue<?>)blocksplayersplans).resetWarning();
        ((LimitedQueue<?>)blockconfplayersplans).resetWarning();
    }


    public static class LimitedQueue<T> extends Queue<T> {
        private final int limit;
        private final String name;
        private boolean hasWarned = false;

        public LimitedQueue(int limit, String name) {
            super();
            this.limit = limit;
            this.name = name;
        }

        public void resetWarning() {
            this.hasWarned = false;
        }

        @Override
        public void addLast(T object) {
            super.addLast(object);
            checkLimit(true);
        }

        @Override
        public void addFirst(T object) {
            super.addFirst(object);
            checkLimit(false);
        }

        public void checkLimit(boolean isLastAdd) {
            if (this.size > limit) {
                if (isLastAdd) {
                    this.removeFirst();
                } else {
                    this.removeLast();
                }

                if (!hasWarned) {
                    Log.info("[ActionsHistory] Очередь '" + name + "' заполнена (" + limit + ")");
                    if (Vars.ui != null && Vars.ui.hudfrag != null) {
                        Vars.ui.hudfrag.showToast("[orange]История " + name + " заполнена!");
                    }
                    hasWarned = true;
                }
            }
        }
    }

    public static class BlockPlayerPlan {
        public final short x, y, rotation, block;
        public final String lastacs;
        public final Object config;
        public final long timestamp;
        public boolean wasbreaking;

        public BlockPlayerPlan(int x, int y, short rotation, short block, Object config, String lastacs, boolean wasbreaking){
            this.x = (short)x;
            this.y = (short)y;
            this.rotation = rotation;
            this.block = block;
            this.config = config;
            this.lastacs = lastacs;
            this.wasbreaking = wasbreaking;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class BlockConfigPlayerPlan {
        public final short x, y, block;
        public final String lastacs;
        public final long timestamp;

        public BlockConfigPlayerPlan(int x, int y, short block, String lastacs){
            this.x = (short)x;
            this.y = (short)y;
            this.block = block;
            this.lastacs = lastacs;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
