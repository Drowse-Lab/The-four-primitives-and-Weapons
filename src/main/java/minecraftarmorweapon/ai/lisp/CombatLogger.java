package minecraftarmorweapon.ai.lisp;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 戦闘データをログファイルに記録する。
 *
 * ログ出力先: .minecraft/logs/combat_ai/
 *   combat_log_YYYY-MM-DD.txt — 日別の戦闘ログ
 *
 * 記録内容:
 *   - 日時
 *   - プレイヤー名
 *   - Mob種別 / AIレベル / 世代
 *   - 戦闘時間
 *   - 与ダメージ / 被ダメージ
 *   - 勝敗
 *   - プレイヤーの行動パターン分析
 *   - 使用されたゲノム（S式）
 */
public class CombatLogger {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // 戦闘ログの有効/無効フラグ（/test log on|off で切替）
    private static volatile boolean enabled = true;

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean on) { enabled = on; }

    /** 今日のJSONLイベントログファイルへのパス */
    public static Path getTodayEventFile() {
        String date = LocalDateTime.now().format(DATE_FMT);
        return getLogDir().resolve("combat_events_" + date + ".jsonl");
    }

    /** ログディレクトリ（公開） */
    public static Path getLogDirectory() {
        return getLogDir();
    }

    private static Path logDir;

    private static Path getLogDir() {
        if (logDir == null) {
            logDir = FMLPaths.GAMEDIR.get().resolve("logs").resolve("combat_ai");
            try {
                Files.createDirectories(logDir);
            } catch (IOException e) {
                // 無視
            }
        }
        return logDir;
    }

    /**
     * Mobが死亡したときの戦闘ログを記録する。
     */
    public static void logMobDeath(Mob mob, MobAIBrain brain, LivingEntity killer) {
        try {
            String date = LocalDateTime.now().format(DATE_FMT);
            String time = LocalDateTime.now().format(TIME_FMT);
            Path logFile = getLogDir().resolve("combat_log_" + date + ".txt");

            StringBuilder sb = new StringBuilder();
            sb.append("========================================\n");
            sb.append("[").append(time).append("] MOB DEFEATED\n");
            sb.append("----------------------------------------\n");

            // Mob情報
            sb.append("Mob: ").append(mob.getType().getDescriptionId()).append("\n");
            sb.append("AI Level: ").append(brain.getAiLevel()).append("\n");

            String entityTypeId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).toString();
            sb.append("Generation: ").append(AIEvolutionManager.getGeneration(entityTypeId)).append("\n");

            // スコア
            float fitness = brain.calculateFitness();
            sb.append("Fitness: ").append(String.format("%.1f", fitness)).append("\n");

            // キラー情報
            if (killer instanceof Player player) {
                sb.append("Killed by: ").append(player.getName().getString()).append("\n");

                // プレイヤーの行動パターン
                PlayerBehaviorTracker.PlayerProfile profile =
                    PlayerBehaviorTracker.getProfile(player.getUUID());
                sb.append("\n--- Player Pattern ---\n");
                sb.append("  Avg Attack Interval: ").append(String.format("%.1f", profile.getAvgAttackInterval())).append(" ticks\n");
                sb.append("  Shield Rate: ").append(String.format("%.0f%%", profile.getShieldRate() * 100)).append("\n");
                sb.append("  Hit&Run Rate: ").append(String.format("%.0f%%", profile.getHitAndRunRate() * 100)).append("\n");
                sb.append("  Flank Rate: ").append(String.format("%.0f%%", profile.getFlankRate() * 100)).append("\n");
                sb.append("  Sprint Rate: ").append(String.format("%.0f%%", profile.getSprintRate() * 100)).append("\n");
                sb.append("  Preferred Range: ").append(profile.getPreferredRange()).append("\n");
                sb.append("  Style: ").append(profile.isAggressive() ? "Aggressive" : "Defensive").append("\n");
                sb.append("  W/L: ").append(profile.wins).append("/").append(profile.losses).append("\n");
            } else if (killer != null) {
                sb.append("Killed by: ").append(killer.getName().getString()).append("\n");
            } else {
                sb.append("Killed by: unknown\n");
            }

            // ゲノム
            sb.append("\n--- Genome (S-expression) ---\n");
            sb.append(brain.getGenomeString()).append("\n");
            sb.append("========================================\n\n");

            // ファイルに追記
            Files.writeString(logFile, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (Exception e) {
            // ログ書き込み失敗は無視
        }
    }

    /**
     * プレイヤーが死亡したときの戦闘ログを記録する。
     */
    public static void logPlayerDeath(Player player, Mob killer, MobAIBrain brain) {
        try {
            String date = LocalDateTime.now().format(DATE_FMT);
            String time = LocalDateTime.now().format(TIME_FMT);
            Path logFile = getLogDir().resolve("combat_log_" + date + ".txt");

            StringBuilder sb = new StringBuilder();
            sb.append("========================================\n");
            sb.append("[").append(time).append("] PLAYER DEFEATED!\n");
            sb.append("----------------------------------------\n");
            sb.append("Player: ").append(player.getName().getString()).append("\n");
            sb.append("Killed by: ").append(killer.getType().getDescriptionId()).append("\n");
            sb.append("Mob AI Level: ").append(brain.getAiLevel()).append("\n");

            float fitness = brain.calculateFitness();
            sb.append("Mob Fitness: ").append(String.format("%.1f", fitness)).append("\n");

            // プレイヤーの行動パターン
            PlayerBehaviorTracker.PlayerProfile profile =
                PlayerBehaviorTracker.getProfile(player.getUUID());
            sb.append("\n--- Player Pattern ---\n");
            sb.append("  Total Attacks: ").append(profile.totalAttacks).append("\n");
            sb.append("  Style: ").append(profile.isAggressive() ? "Aggressive" : "Defensive").append("\n");
            sb.append("  Preferred Range: ").append(profile.getPreferredRange()).append("\n");

            sb.append("\n--- Winning Genome ---\n");
            sb.append(brain.getGenomeString()).append("\n");
            sb.append("========================================\n\n");

            Files.writeString(logFile, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (Exception e) {
            // 無視
        }
    }

    /**
     * Claude Code向け: 1行1イベントのJSONLログ。
     * ログ出力先: .minecraft/logs/combat_ai/combat_events_YYYY-MM-DD.jsonl
     * parse容易にJSON Lines形式で記録する。
     *
     * @param eventType "attack" / "hurt" / "kill" / "death" / "dodge" / "spawn" など
     * @param extra    追加情報（JSON文字列として埋め込み、null可）
     */
    public static void logEvent(String eventType, LivingEntity source, LivingEntity target,
                                 float amount, String extra) {
        if (!enabled) return;
        try {
            String date = LocalDateTime.now().format(DATE_FMT);
            String iso = LocalDateTime.now().toString();
            Path logFile = getLogDir().resolve("combat_events_" + date + ".jsonl");

            StringBuilder sb = new StringBuilder(256);
            sb.append("{\"ts\":\"").append(iso).append("\"");
            sb.append(",\"event\":\"").append(esc(eventType)).append("\"");
            if (source != null) {
                sb.append(",\"source\":{\"type\":\"").append(esc(source.getType().getDescriptionId())).append("\"")
                  .append(",\"name\":\"").append(esc(source.getName().getString())).append("\"")
                  .append(",\"uuid\":\"").append(source.getUUID()).append("\"")
                  .append(",\"hp\":").append(source.getHealth()).append("/").append(source.getMaxHealth())
                  .append(",\"pos\":[").append(String.format("%.1f,%.1f,%.1f", source.getX(), source.getY(), source.getZ())).append("]");
                if (source instanceof Mob m) {
                    int ai = MobAILevelHandler.getAILevel(m);
                    sb.append(",\"ai_level\":").append(ai);
                }
                sb.append("}");
            }
            if (target != null) {
                sb.append(",\"target\":{\"type\":\"").append(esc(target.getType().getDescriptionId())).append("\"")
                  .append(",\"name\":\"").append(esc(target.getName().getString())).append("\"")
                  .append(",\"uuid\":\"").append(target.getUUID()).append("\"")
                  .append(",\"hp\":").append(target.getHealth()).append("/").append(target.getMaxHealth())
                  .append(",\"pos\":[").append(String.format("%.1f,%.1f,%.1f", target.getX(), target.getY(), target.getZ())).append("]");
                if (target instanceof Mob m) {
                    int ai = MobAILevelHandler.getAILevel(m);
                    sb.append(",\"ai_level\":").append(ai);
                }
                sb.append("}");
            }
            if (amount != 0f) {
                sb.append(",\"amount\":").append(amount);
            }
            if (source != null && target != null) {
                double dist = Math.sqrt(source.distanceToSqr(target));
                sb.append(",\"distance\":").append(String.format("%.2f", dist));
            }
            if (source != null && source.level() != null) {
                sb.append(",\"dimension\":\"").append(esc(source.level().dimension().location().toString())).append("\"");
                sb.append(",\"in_water\":").append(source.isInWater());
                sb.append(",\"on_ground\":").append(source.onGround());
            }
            if (extra != null && !extra.isEmpty()) {
                sb.append(",\"extra\":").append(extra);
            }
            sb.append("}\n");

            Files.writeString(logFile, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            // 無視
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    /**
     * 世代交代のログを記録する。
     */
    public static void logEvolution(String entityTypeId, int generation, int populationSize) {
        try {
            String date = LocalDateTime.now().format(DATE_FMT);
            String time = LocalDateTime.now().format(TIME_FMT);
            Path logFile = getLogDir().resolve("combat_log_" + date + ".txt");

            String log = "[" + time + "] EVOLUTION: " + entityTypeId +
                " -> Gen " + generation +
                " (pool: " + populationSize + ")\n";

            Files.writeString(logFile, log, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (Exception e) {
            // 無視
        }
    }
}
