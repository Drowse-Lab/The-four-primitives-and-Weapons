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
