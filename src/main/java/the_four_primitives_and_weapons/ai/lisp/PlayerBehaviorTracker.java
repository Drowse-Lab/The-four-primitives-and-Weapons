package the_four_primitives_and_weapons.ai.lisp;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * プレイヤーの戦闘パターンを記録・分析する。
 *
 * 記録するデータ:
 *   - 攻撃間隔（平均tick）
 *   - 盾使用率
 *   - 回避（スプリント方向転換）頻度
 *   - 好む戦闘距離（近接 / 中距離 / 遠距離）
 *   - ヒット＆ラン率（攻撃後すぐ離れるか）
 *   - 背後から攻撃する率
 *
 * これらをLisp変数 (player-avg-attack-interval 等) として
 * MobAIBrainに注入し、AIがプレイヤーの癖に対策できるようにする。
 *
 * ワールドに永続化（SavedData）。
 */
public class PlayerBehaviorTracker {

    // プレイヤーUUID → 行動プロファイル
    private static final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();

    // === 行動プロファイル ===

    public static class PlayerProfile {
        // 攻撃パターン
        public long lastAttackTime = 0;
        public final List<Integer> attackIntervals = new ArrayList<>(); // 直近20回分の攻撃間隔(tick)
        public int totalAttacks = 0;

        // 盾パターン
        public int shieldBlockCount = 0;   // 盾でブロックした回数
        public int totalHitsReceived = 0;  // 被弾総数

        // 移動パターン
        public int sprintCount = 0;        // スプリント攻撃の回数
        public int retreatAfterHit = 0;    // 攻撃後に離れた回数（ヒット＆ラン）
        public int stayAfterHit = 0;       // 攻撃後にとどまった回数

        // 距離パターン
        public int closeRangeAttacks = 0;  // 3ブロック以内での攻撃
        public int midRangeAttacks = 0;    // 3〜8ブロックでの攻撃
        public int longRangeAttacks = 0;   // 8ブロック以上での攻撃

        // 方向パターン
        public int flankedAttacks = 0;     // 横/背後からの攻撃
        public int frontalAttacks = 0;     // 正面からの攻撃

        // 勝敗
        public int wins = 0;
        public int losses = 0;

        // === 分析結果 ===

        /** 平均攻撃間隔（tick）。短いほど攻撃的。 */
        public double getAvgAttackInterval() {
            if (attackIntervals.isEmpty()) return 20.0;
            return attackIntervals.stream().mapToInt(i -> i).average().orElse(20.0);
        }

        /** 盾使用率 (0.0-1.0) */
        public double getShieldRate() {
            if (totalHitsReceived == 0) return 0.0;
            return (double) shieldBlockCount / totalHitsReceived;
        }

        /** ヒット＆ラン率 (0.0-1.0)。高いほど撃って逃げる。 */
        public double getHitAndRunRate() {
            int total = retreatAfterHit + stayAfterHit;
            if (total == 0) return 0.5;
            return (double) retreatAfterHit / total;
        }

        /** 好む距離カテゴリ: "close", "mid", "long" */
        public String getPreferredRange() {
            if (longRangeAttacks > midRangeAttacks && longRangeAttacks > closeRangeAttacks) return "long";
            if (midRangeAttacks > closeRangeAttacks) return "mid";
            return "close";
        }

        /** 側面攻撃率 (0.0-1.0) */
        public double getFlankRate() {
            int total = flankedAttacks + frontalAttacks;
            if (total == 0) return 0.0;
            return (double) flankedAttacks / total;
        }

        /** 攻撃的かどうか（攻撃間隔が短い＆ヒット＆ランしない） */
        public boolean isAggressive() {
            return getAvgAttackInterval() < 15 && getHitAndRunRate() < 0.3;
        }

        /** スプリント攻撃率 */
        public double getSprintRate() {
            if (totalAttacks == 0) return 0.0;
            return (double) sprintCount / totalAttacks;
        }

        // === 記録メソッド ===

        public void recordAttack(long currentTick, double distance, boolean isSprinting, boolean flanked) {
            totalAttacks++;

            // 攻撃間隔を記録
            if (lastAttackTime > 0) {
                int interval = (int)(currentTick - lastAttackTime);
                attackIntervals.add(interval);
                // 直近20回分のみ保持
                while (attackIntervals.size() > 20) attackIntervals.remove(0);
            }
            lastAttackTime = currentTick;

            // 距離カテゴリ
            if (distance < 3) closeRangeAttacks++;
            else if (distance < 8) midRangeAttacks++;
            else longRangeAttacks++;

            // スプリント
            if (isSprinting) sprintCount++;

            // 方向
            if (flanked) flankedAttacks++;
            else frontalAttacks++;
        }

        public void recordShieldBlock() {
            shieldBlockCount++;
            totalHitsReceived++;
        }

        public void recordHitReceived() {
            totalHitsReceived++;
        }

        public void recordPostAttackMovement(boolean retreated) {
            if (retreated) retreatAfterHit++;
            else stayAfterHit++;
        }

        // === NBT保存/読み込み ===

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("totalAttacks", totalAttacks);
            tag.putInt("shieldBlockCount", shieldBlockCount);
            tag.putInt("totalHitsReceived", totalHitsReceived);
            tag.putInt("sprintCount", sprintCount);
            tag.putInt("retreatAfterHit", retreatAfterHit);
            tag.putInt("stayAfterHit", stayAfterHit);
            tag.putInt("closeRangeAttacks", closeRangeAttacks);
            tag.putInt("midRangeAttacks", midRangeAttacks);
            tag.putInt("longRangeAttacks", longRangeAttacks);
            tag.putInt("flankedAttacks", flankedAttacks);
            tag.putInt("frontalAttacks", frontalAttacks);
            tag.putInt("wins", wins);
            tag.putInt("losses", losses);
            return tag;
        }

        public static PlayerProfile load(CompoundTag tag) {
            PlayerProfile p = new PlayerProfile();
            p.totalAttacks = tag.getInt("totalAttacks");
            p.shieldBlockCount = tag.getInt("shieldBlockCount");
            p.totalHitsReceived = tag.getInt("totalHitsReceived");
            p.sprintCount = tag.getInt("sprintCount");
            p.retreatAfterHit = tag.getInt("retreatAfterHit");
            p.stayAfterHit = tag.getInt("stayAfterHit");
            p.closeRangeAttacks = tag.getInt("closeRangeAttacks");
            p.midRangeAttacks = tag.getInt("midRangeAttacks");
            p.longRangeAttacks = tag.getInt("longRangeAttacks");
            p.flankedAttacks = tag.getInt("flankedAttacks");
            p.frontalAttacks = tag.getInt("frontalAttacks");
            p.wins = tag.getInt("wins");
            p.losses = tag.getInt("losses");
            return p;
        }
    }

    // === 公開API ===

    public static PlayerProfile getProfile(UUID playerId) {
        return profiles.computeIfAbsent(playerId, k -> new PlayerProfile());
    }

    /**
     * プレイヤーの行動データをLisp変数としてMobAIBrainに注入する。
     */
    public static void injectIntoLisp(LispInterpreter lisp, Player player) {
        PlayerProfile profile = getProfile(player.getUUID());

        lisp.setVariable("player-avg-attack-interval", profile.getAvgAttackInterval());
        lisp.setVariable("player-shield-rate", profile.getShieldRate());
        lisp.setVariable("player-hitrun-rate", profile.getHitAndRunRate());
        lisp.setVariable("player-flank-rate", profile.getFlankRate());
        lisp.setVariable("player-sprint-rate", profile.getSprintRate());
        lisp.setVariable("player-is-aggressive", profile.isAggressive());
        lisp.setVariable("player-prefers-close", "close".equals(profile.getPreferredRange()));
        lisp.setVariable("player-prefers-mid", "mid".equals(profile.getPreferredRange()));
        lisp.setVariable("player-prefers-long", "long".equals(profile.getPreferredRange()));
        lisp.setVariable("player-wins", (double) profile.wins);
        lisp.setVariable("player-losses", (double) profile.losses);
    }

    // === SavedData永続化 ===

    public static void loadFromWorld(ServerLevel level) {
        PlayerBehaviorData data = level.getDataStorage().computeIfAbsent(
            PlayerBehaviorData::load, PlayerBehaviorData::new, "the_four_primitives_and_weapons_player_behavior");
        profiles.clear();
        profiles.putAll(data.profiles);
    }

    public static void saveToWorld(ServerLevel level) {
        PlayerBehaviorData data = level.getDataStorage().computeIfAbsent(
            PlayerBehaviorData::load, PlayerBehaviorData::new, "the_four_primitives_and_weapons_player_behavior");
        data.profiles.clear();
        data.profiles.putAll(profiles);
        data.setDirty();
    }

    public static class PlayerBehaviorData extends SavedData {
        final Map<UUID, PlayerProfile> profiles = new HashMap<>();

        public PlayerBehaviorData() {}

        public static PlayerBehaviorData load(CompoundTag tag) {
            PlayerBehaviorData data = new PlayerBehaviorData();
            CompoundTag playersTag = tag.getCompound("players");
            for (String key : playersTag.getAllKeys()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    data.profiles.put(uuid, PlayerProfile.load(playersTag.getCompound(key)));
                } catch (Exception e) {
                    // 無視
                }
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            CompoundTag playersTag = new CompoundTag();
            for (Map.Entry<UUID, PlayerProfile> entry : profiles.entrySet()) {
                playersTag.put(entry.getKey().toString(), entry.getValue().save());
            }
            tag.put("players", playersTag);
            return tag;
        }
    }
}
