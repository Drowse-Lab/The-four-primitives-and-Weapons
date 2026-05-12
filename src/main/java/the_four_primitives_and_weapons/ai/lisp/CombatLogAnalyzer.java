package the_four_primitives_and_weapons.ai.lisp;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CombatLogger が出力する JSONL 戦闘ログを解析し、
 * 集計統計を Lisp 変数として MobAIBrain に注入する。
 *
 * MobAIBrain.updateSensors() から呼ばれ、S式から以下の変数を参照可能:
 *   - log-events-total               : 解析したイベント総数
 *   - log-mob-type-hit-count         : 同タイプMobが与えた攻撃総数
 *   - log-mob-type-avg-damage        : 同タイプMobの平均与ダメ
 *   - log-mob-type-death-count       : 同タイプMobの死亡数（fitness参考）
 *   - log-player-hit-count           : 当該プレイヤーから受けた攻撃総数
 *   - log-player-avg-damage          : 当該プレイヤーの平均ダメ
 *   - log-player-avg-distance        : 当該プレイヤーの攻撃時平均距離
 *   - log-player-melee-rate          : 近接(<4)攻撃の比率
 *   - log-player-ranged-rate         : 遠距離(>8)攻撃の比率
 *   - log-player-kill-count          : 当該プレイヤーのMobキル数
 *
 * 30秒に1回、最新 MAX_LINES 行までを再解析してキャッシュ。
 */
public class CombatLogAnalyzer {

    private static final int MAX_LINES = 2000;
    private static final long REFRESH_INTERVAL_MS = 30_000L;

    private static volatile long lastRefreshMs = 0L;
    private static final Object refreshLock = new Object();

    // 集計キャッシュ
    private static final Map<String, Stats> mobStatsByType = new ConcurrentHashMap<>();
    private static final Map<UUID, Stats> playerStatsByUuid = new ConcurrentHashMap<>();
    private static volatile int totalEvents = 0;

    // JSON抽出用正規表現（自前出力形式に特化）
    private static final Pattern P_EVENT      = Pattern.compile("\"event\":\"([^\"]+)\"");
    private static final Pattern P_SRC_TYPE   = Pattern.compile("\"source\":\\{\"type\":\"([^\"]+)\"");
    private static final Pattern P_SRC_UUID   = Pattern.compile("\"source\":\\{[^}]*\"uuid\":\"([0-9a-f-]+)\"");
    private static final Pattern P_TGT_TYPE   = Pattern.compile("\"target\":\\{\"type\":\"([^\"]+)\"");
    private static final Pattern P_TGT_UUID   = Pattern.compile("\"target\":\\{[^}]*\"uuid\":\"([0-9a-f-]+)\"");
    private static final Pattern P_AMOUNT     = Pattern.compile("\"amount\":([0-9.eE+-]+)");
    private static final Pattern P_DISTANCE   = Pattern.compile("\"distance\":([0-9.eE+-]+)");

    private static final String PLAYER_TYPE_ID = "entity.minecraft.player";

    public static class Stats {
        public int hitCount = 0;           // このエンティティがsourceとして関与した hurt数
        public int deathCount = 0;         // このエンティティがtargetとして死亡した数
        public int killCount = 0;          // このエンティティのsourceで相手が死亡した数
        public double totalDamage = 0.0;
        public double totalDistance = 0.0;
        public int distanceSamples = 0;
        public int meleeHits = 0;          // distance < 4
        public int rangedHits = 0;         // distance > 8

        public double avgDamage()   { return hitCount == 0 ? 0.0 : totalDamage / hitCount; }
        public double avgDistance() { return distanceSamples == 0 ? 0.0 : totalDistance / distanceSamples; }
        public double meleeRate()   { return hitCount == 0 ? 0.0 : (double) meleeHits / hitCount; }
        public double rangedRate()  { return hitCount == 0 ? 0.0 : (double) rangedHits / hitCount; }
    }

    /** 必要であれば再解析（30秒キャッシュ） */
    public static void refreshIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastRefreshMs < REFRESH_INTERVAL_MS) return;
        synchronized (refreshLock) {
            if (now - lastRefreshMs < REFRESH_INTERVAL_MS) return;
            refresh();
            lastRefreshMs = now;
        }
    }

    /** 強制再解析 */
    public static void refresh() {
        mobStatsByType.clear();
        playerStatsByUuid.clear();
        totalEvents = 0;

        Path file = CombatLogger.getTodayEventFile();
        if (!Files.exists(file)) return;

        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (Exception e) {
            return;
        }
        int from = Math.max(0, lines.size() - MAX_LINES);
        for (int i = from; i < lines.size(); i++) {
            parseLine(lines.get(i));
        }
    }

    private static void parseLine(String line) {
        if (line == null || line.isEmpty()) return;

        Matcher me = P_EVENT.matcher(line);
        if (!me.find()) return;
        String event = me.group(1);
        totalEvents++;

        String srcType = find(P_SRC_TYPE, line);
        String srcUuid = find(P_SRC_UUID, line);
        String tgtType = find(P_TGT_TYPE, line);
        String tgtUuid = find(P_TGT_UUID, line);
        double amount = findDouble(P_AMOUNT, line, 0.0);
        double distance = findDouble(P_DISTANCE, line, -1.0);

        if ("hurt".equals(event)) {
            if (srcType != null) {
                Stats s = statsFor(srcType, srcUuid);
                s.hitCount++;
                s.totalDamage += amount;
                if (distance >= 0) {
                    s.totalDistance += distance;
                    s.distanceSamples++;
                    if (distance < 4.0) s.meleeHits++;
                    else if (distance > 8.0) s.rangedHits++;
                }
            }
        } else if ("death".equals(event) || "kill".equals(event)) {
            if (tgtType != null) {
                Stats t = statsFor(tgtType, tgtUuid);
                t.deathCount++;
            }
            if (srcType != null) {
                Stats k = statsFor(srcType, srcUuid);
                k.killCount++;
            }
        }
    }

    private static Stats statsFor(String type, String uuid) {
        if (PLAYER_TYPE_ID.equals(type) && uuid != null) {
            try {
                return playerStatsByUuid.computeIfAbsent(UUID.fromString(uuid), k -> new Stats());
            } catch (IllegalArgumentException ignored) {}
        }
        return mobStatsByType.computeIfAbsent(type, k -> new Stats());
    }

    private static String find(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? m.group(1) : null;
    }

    private static double findDouble(Pattern p, String s, double def) {
        Matcher m = p.matcher(s);
        if (!m.find()) return def;
        try { return Double.parseDouble(m.group(1)); } catch (Exception e) { return def; }
    }

    public static Stats getMobTypeStats(String typeId) {
        return mobStatsByType.getOrDefault(typeId, new Stats());
    }

    public static Stats getPlayerStats(UUID uuid) {
        if (uuid == null) return new Stats();
        return playerStatsByUuid.getOrDefault(uuid, new Stats());
    }

    public static int getTotalEvents() { return totalEvents; }

    public static Map<String, Stats> getAllMobStats() { return mobStatsByType; }
    public static Map<UUID, Stats> getAllPlayerStats() { return playerStatsByUuid; }

    /**
     * MobAIBrain から呼ぶ: Lisp 変数に戦闘ログ統計を注入する。
     * target が Player の場合、そのプレイヤーの統計も入る。
     */
    public static void injectIntoLisp(LispInterpreter lisp, Mob mob, LivingEntity target) {
        refreshIfNeeded();

        lisp.setVariable("log-events-total", (double) totalEvents);

        if (mob != null) {
            String typeId = mob.getType().getDescriptionId();
            Stats ms = getMobTypeStats(typeId);
            lisp.setVariable("log-mob-type-hit-count",   (double) ms.hitCount);
            lisp.setVariable("log-mob-type-avg-damage",  ms.avgDamage());
            lisp.setVariable("log-mob-type-death-count", (double) ms.deathCount);
            lisp.setVariable("log-mob-type-kill-count",  (double) ms.killCount);
            // fitness指標: kill/(kill+death)
            double total = ms.killCount + ms.deathCount;
            lisp.setVariable("log-mob-type-winrate", total == 0 ? 0.5 : ms.killCount / total);
        }

        if (target instanceof Player player) {
            Stats ps = getPlayerStats(player.getUUID());
            lisp.setVariable("log-player-hit-count",    (double) ps.hitCount);
            lisp.setVariable("log-player-avg-damage",   ps.avgDamage());
            lisp.setVariable("log-player-avg-distance", ps.avgDistance());
            lisp.setVariable("log-player-melee-rate",   ps.meleeRate());
            lisp.setVariable("log-player-ranged-rate",  ps.rangedRate());
            lisp.setVariable("log-player-kill-count",   (double) ps.killCount);
        } else {
            lisp.setVariable("log-player-hit-count",    0.0);
            lisp.setVariable("log-player-avg-damage",   0.0);
            lisp.setVariable("log-player-avg-distance", 0.0);
            lisp.setVariable("log-player-melee-rate",   0.0);
            lisp.setVariable("log-player-ranged-rate",  0.0);
            lisp.setVariable("log-player-kill-count",   0.0);
        }
    }

    /** /test log analyze 用のサマリ文字列 */
    public static List<String> summary(int topN) {
        refreshIfNeeded();
        List<String> out = new ArrayList<>();
        out.add("total events: " + totalEvents);
        // 上位Nのmob type
        List<Map.Entry<String, Stats>> mobSorted = new ArrayList<>(mobStatsByType.entrySet());
        mobSorted.sort((a, b) -> Integer.compare(b.getValue().hitCount, a.getValue().hitCount));
        out.add("-- top mob types by hit count --");
        for (int i = 0; i < Math.min(topN, mobSorted.size()); i++) {
            Map.Entry<String, Stats> e = mobSorted.get(i);
            Stats s = e.getValue();
            out.add(String.format("  %s: hits=%d avg_dmg=%.2f kills=%d deaths=%d winrate=%.1f%%",
                e.getKey(), s.hitCount, s.avgDamage(), s.killCount, s.deathCount,
                (s.killCount + s.deathCount) == 0 ? 0 :
                    100.0 * s.killCount / (s.killCount + s.deathCount)));
        }
        out.add("-- top players --");
        List<Map.Entry<UUID, Stats>> playerSorted = new ArrayList<>(playerStatsByUuid.entrySet());
        playerSorted.sort((a, b) -> Integer.compare(b.getValue().hitCount, a.getValue().hitCount));
        for (int i = 0; i < Math.min(topN, playerSorted.size()); i++) {
            Map.Entry<UUID, Stats> e = playerSorted.get(i);
            Stats s = e.getValue();
            out.add(String.format("  %s: hits=%d avg_dmg=%.2f avg_dist=%.1f melee=%.0f%% ranged=%.0f%% kills=%d",
                e.getKey(), s.hitCount, s.avgDamage(), s.avgDistance(),
                100.0 * s.meleeRate(), 100.0 * s.rangedRate(), s.killCount));
        }
        return out;
    }
}
