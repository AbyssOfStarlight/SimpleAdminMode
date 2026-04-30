package fallen;

import arc.struct.ObjectSet;
import arc.util.Log;
import arc.util.Time;
import arc.util.Timer;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.net.Packets;

public class PlayerData {
    public int id;
    public String name;
    public String uuid;
    public boolean online;
    public Player player;
    public int builds = 0;
    public int breaks = 0;
    public int configs = 0;
    public String ip = "none";
    public String locale = "nani";
    public boolean modded = false;
    public boolean mobile = false;
    public int timesJoined = 0, timesKicked = 0;
    public String[] ips = {}, names = {};
    public boolean griefWarned = false;
    public boolean autoFrozen = false;

    // Для запросов
    public int traceAttempts = 0;
    public float lastTraceRequest = 0f;
    public arc.util.Timer.Task traceTask;
    private static ObjectSet<Integer> autoTraceRequested;

    public static final int MAX_TRACE_ATTEMPTS = 3;
    public static final float TRACE_INTERVAL = 3f;

    public PlayerData(Player p) {
        this.id = p.id;
        this.name = p.name;
        this.uuid = "Loading...";
        this.online = true;
        this.player = p;
    }


    public static void setAutoTraceRequested(ObjectSet<Integer> set) {
        autoTraceRequested = set;
    }

    // Периодические запросы
    public void startTraceRequests(Player p) {
        if (traceTask != null) return; // Уже запущено

        traceTask = new arc.util.Timer.Task() {
            @Override
            public void run() {
                // Если UUID внезапно пришел между тиками таймера — стопаем
                if (uuid != null && !uuid.equals("Loading...") && !uuid.equals("admin?") && !uuid.equals("none")) {
                    cancel();
                    traceTask = null;
                    return;
                }
                traceAttempts++;
                lastTraceRequest = Time.time;

                Log.info("SimpleAdminMode: Попытка #/@ для @ (uuid='@')",
                        traceAttempts, MAX_TRACE_ATTEMPTS, name, uuid);

                if (p != null && p.unit() != null && autoTraceRequested != null) {
                    autoTraceRequested.add(p.id);
                    Call.adminRequest(p, Packets.AdminAction.trace, null);
                }

                // Если попытки кончились — останавливаем
                if (traceAttempts >= MAX_TRACE_ATTEMPTS) {
                    cancel();
                    traceTask = null;

                    // Ставим заглушку
                    if (player != null && player.admin) {
                        uuid = "admin?";
                        Log.info("SimpleAdminMode: @ помечен как 'admin?'", name);
                    } else {
                        uuid = "none";
                    }
                    return;
                }

                // Если UUID уже есть — останавливаем
                if (uuid != null && !uuid.isEmpty() &&
                        !uuid.equals("Loading...") && !uuid.equals("admin?") &&
                        !uuid.equals("none")) {
                    cancel();
                    traceTask = null;
                    return;
                }
            }
        };

        Timer.schedule(traceTask, 1f, TRACE_INTERVAL);
    }

    public void stopTraceRequests() {
        if (traceTask != null) {
            traceTask.cancel();
            traceTask = null;
            if(autoTraceRequested != null) autoTraceRequested.remove(id);
        }
    }

    public void updateFrom(mindustry.net.Administration.TraceInfo info) {
        stopTraceRequests();
        this.uuid = info.uuid;
        this.ip = info.ip;
        this.locale = info.locale;
        this.modded = info.modded;
        this.mobile = info.mobile;
        this.timesJoined = info.timesJoined;
        this.timesKicked = info.timesKicked;
        this.ips = info.ips;
        this.names = info.names;
    }
}