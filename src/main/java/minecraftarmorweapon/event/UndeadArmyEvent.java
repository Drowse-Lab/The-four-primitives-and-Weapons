package minecraftarmorweapon.event;

import minecraftarmorweapon.command.CustomDifficultyCommand;
import minecraftarmorweapon.command.CustomDifficultyCommand.CustomDifficulty;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * アンデットアーミー — バニラ襲撃（Raid）風のウェーブ制アンデット侵攻イベント
 *
 * ＜発動条件＞
 *   - difficulty >= NIGHTMARE (aiLevel >= 1)
 *   - 夜間（ゲーム内時刻 13000〜23000）
 *   - 1分に1回チェック、稀な確率で選ばれたプレイヤーに発生
 *   - 同一プレイヤーに発生後10分間はクールダウン
 *
 * ＜進行＞
 *   - ボスバーにウェーブ数と残り敵数を表示
 *   - 1ウェーブ目スポーン → 全滅 → 次ウェーブ → … → 全ウェーブ撃破で勝利
 *   - aiLevelが高いほどウェーブ数・軍団規模・ステータスが増す
 *   - aiLevel 3以上のウェーブにはアンデット将軍（エリート）が混じる
 */
@Mod.EventBusSubscriber
public class UndeadArmyEvent {

    // ============================
    // 進行中の侵攻データ（プレイヤーUUID → 侵攻状態）
    // ============================
    private static class UndeadRaid {
        final ServerPlayer player;
        final BlockPos center;
        final int totalWaves;
        int currentWave = 0;
        final Set<UUID> aliveEntities = ConcurrentHashMap.newKeySet();
        final ServerBossEvent bossBar;
        boolean spawningNextWave = false;
        int nextWaveDelay = 0; // ウェーブ間の猶予tick

        UndeadRaid(ServerPlayer player, BlockPos center, int totalWaves) {
            this.player = player;
            this.center = center;
            this.totalWaves = totalWaves;
            this.bossBar = new ServerBossEvent(
                Component.literal("§5§lアンデットアーミー §7— Wave 1/" + totalWaves),
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.PROGRESS
            );
            this.bossBar.addPlayer(player);
        }

        void updateBossBar(int remaining, int total) {
            bossBar.setName(Component.literal(
                "§5§lアンデットアーミー §7— Wave " + currentWave + "/" + totalWaves
                + " §c(" + remaining + "体残り)"
            ));
            // 進捗: 現ウェーブ内の撃破進捗
            float progress = total > 0 ? 1.0f - (float) remaining / total : 1.0f;
            bossBar.setProgress(Math.max(0f, Math.min(1f, progress)));
        }

        void remove() {
            bossBar.removeAllPlayers();
        }
    }

    private static final Map<UUID, UndeadRaid> activeRaids = new ConcurrentHashMap<>();
    // クールダウン（プレイヤーUUID → 残りtick）
    private static final Map<UUID, Integer> cooldowns = new ConcurrentHashMap<>();

    // ウェーブ間の猶予: 5秒
    private static final int WAVE_DELAY = 100;
    // チェック間隔: 1200tick = 1分
    private static final int CHECK_INTERVAL = 1200;
    // クールダウン: 10分
    private static final int COOLDOWN_TICKS = 12000;

    @SuppressWarnings("unchecked")
    private static final EntityType<? extends Monster>[] UNDEAD_TYPES = new EntityType[]{
        EntityType.ZOMBIE,
        EntityType.SKELETON,
        EntityType.HUSK,
        EntityType.STRAY,
        EntityType.ZOMBIE_VILLAGER,
        EntityType.DROWNED
    };

    // ============================
    // サーバーティック: 発動チェック & ウェーブ進行
    // ============================
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        long tick = event.getServer().getTickCount();

        // --- 進行中の侵攻のウェーブ管理 ---
        for (Map.Entry<UUID, UndeadRaid> entry : activeRaids.entrySet()) {
            UndeadRaid raid = entry.getValue();
            ServerPlayer player = raid.player;

            if (!player.isAlive() || player.hasDisconnected()) {
                endRaid(entry.getKey(), false);
                continue;
            }

            // ウェーブ間の待機
            if (raid.spawningNextWave) {
                raid.nextWaveDelay--;
                if (raid.nextWaveDelay <= 0) {
                    raid.spawningNextWave = false;
                    spawnWave(raid);
                }
                continue;
            }

            // 現ウェーブの敵が全滅したか
            if (!raid.aliveEntities.isEmpty()) {
                // 生存確認（すでに削除済みエンティティを除外）
                if (player.level() instanceof ServerLevel level) {
                    raid.aliveEntities.removeIf(uuid -> {
                        var e = level.getEntity(uuid);
                        return e == null || !e.isAlive();
                    });
                }
                // ボスバー更新
                raid.updateBossBar(raid.aliveEntities.size(),
                    waveSize(raid.currentWave,
                        CustomDifficultyCommand.getCurrentDifficulty().getAiLevel()));
                continue;
            }

            // 全滅 — 次ウェーブへ or 勝利
            if (raid.currentWave >= raid.totalWaves) {
                endRaid(entry.getKey(), true);
            } else {
                raid.spawningNextWave = true;
                raid.nextWaveDelay = WAVE_DELAY;
                player.displayClientMessage(
                    Component.literal("§e次のウェーブが来る！備えよ！"),
                    true
                );
            }
        }

        // --- 新規発動チェック（1分ごと） ---
        if (tick % CHECK_INTERVAL != 0) return;

        CustomDifficulty diff = CustomDifficultyCommand.getCurrentDifficulty();
        if (diff.getAiLevel() < 1) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.dimensionType().natural()) continue;

            long dayTime = level.getDayTime() % 24000;
            if (dayTime < 13000 || dayTime > 23000) continue;

            float chance = triggerChance(diff.getAiLevel());
            Random rand = new Random();

            for (ServerPlayer player : level.players()) {
                UUID pid = player.getUUID();
                if (activeRaids.containsKey(pid)) continue;

                // クールダウン
                int cd = cooldowns.getOrDefault(pid, 0);
                if (cd > 0) {
                    cooldowns.put(pid, cd - CHECK_INTERVAL);
                    continue;
                }

                if (rand.nextFloat() >= chance) continue;

                startRaid(level, player, diff);
                cooldowns.put(pid, COOLDOWN_TICKS);
            }
        }
    }

    // ============================
    // 侵攻開始
    // ============================
    private static void startRaid(ServerLevel level, ServerPlayer player, CustomDifficulty diff) {
        int aiLevel = diff.getAiLevel();
        // ウェーブ数: nightmare=3, realistic=5, lunatic=7, lunatic+=10, lunatic_extreme=15
        int totalWaves = switch (aiLevel) {
            case 1  -> 3;
            case 2  -> 5;
            case 3  -> 7;
            case 4  -> 10;
            default -> 15;
        };

        BlockPos center = getRaidCenter(player);

        UndeadRaid raid = new UndeadRaid(player, center, totalWaves);
        activeRaids.put(player.getUUID(), raid);

        // 全プレイヤーへ警告
        level.getServer().getPlayerList().broadcastSystemMessage(
            Component.literal("§4§l⚠ §c" + player.getName().getString()
                + " の拠点にアンデットアーミーが侵攻してくる！"),
            false
        );
        player.displayClientMessage(
            Component.literal("§4§l⚠ §cアンデットの軍団が押し寄せてくる！拠点を守れ！"),
            true
        );

        // 第1ウェーブをスポーン
        spawnWave(raid);
    }

    // ============================
    // ウェーブスポーン
    // ============================
    private static void spawnWave(UndeadRaid raid) {
        ServerPlayer player = raid.player;
        if (!(player.level() instanceof ServerLevel level)) return;

        int aiLevel = CustomDifficultyCommand.getCurrentDifficulty().getAiLevel();
        raid.currentWave++;

        int count = waveSize(raid.currentWave, aiLevel);
        boolean hasElite = aiLevel >= 3 && raid.currentWave >= 2;

        Random rand = new Random();

        // 通常アンデット
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0 * i) / count + rand.nextDouble() * 0.3;
            double radius = 22.0 + rand.nextDouble() * 12.0;
            int x = raid.center.getX() + (int)(Math.cos(angle) * radius);
            int z = raid.center.getZ() + (int)(Math.sin(angle) * radius);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            EntityType<? extends Monster> type = UNDEAD_TYPES[rand.nextInt(UNDEAD_TYPES.length)];
            Monster mob = type.create(level);
            if (mob == null) continue;

            mob.moveTo(x, y, z, rand.nextFloat() * 360f, 0f);
            mob.setCustomName(Component.literal("§5[アンデットアーミー Wave" + raid.currentWave + "]"));
            mob.setCustomNameVisible(true);
            applyStats(mob, aiLevel, raid.currentWave, false);
            mob.setTarget(player);
            level.addFreshEntity(mob);
            raid.aliveEntities.add(mob.getUUID());
        }

        // エリート将軍（ウェーブ2以降かつaiLevel 3以上）
        if (hasElite) {
            int eliteCount = Math.min(1 + (raid.currentWave / 2), aiLevel - 1);
            for (int i = 0; i < eliteCount; i++) {
                double angle = rand.nextDouble() * Math.PI * 2.0;
                double radius = 18.0 + rand.nextDouble() * 6.0;
                int x = raid.center.getX() + (int)(Math.cos(angle) * radius);
                int z = raid.center.getZ() + (int)(Math.sin(angle) * radius);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

                EntityType<? extends Monster> type = rand.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON;
                Monster elite = type.create(level);
                if (elite == null) continue;

                elite.moveTo(x, y, z, rand.nextFloat() * 360f, 0f);
                elite.setCustomName(Component.literal("§4§l[アンデット将軍]"));
                elite.setCustomNameVisible(true);
                applyStats(elite, aiLevel, raid.currentWave, true);
                elite.setTarget(player);
                level.addFreshEntity(elite);
                raid.aliveEntities.add(elite.getUUID());
            }
        }

        // ボスバー初期化
        raid.updateBossBar(raid.aliveEntities.size(), raid.aliveEntities.size());

        player.displayClientMessage(
            Component.literal("§cWave " + raid.currentWave + "/" + raid.totalWaves + " 開始！"),
            true
        );
    }

    // ============================
    // 侵攻終了
    // ============================
    private static void endRaid(UUID playerUUID, boolean victory) {
        UndeadRaid raid = activeRaids.remove(playerUUID);
        if (raid == null) return;
        raid.remove();

        ServerPlayer player = raid.player;
        if (!player.isAlive()) return;

        if (victory) {
            player.level().getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§a§l✔ §e" + player.getName().getString()
                    + " がアンデットアーミーを撃退した！"),
                false
            );
            player.displayClientMessage(
                Component.literal("§a§l侵攻を撃退した！"),
                true
            );
        } else {
            player.displayClientMessage(
                Component.literal("§4侵攻が中断された。"),
                true
            );
        }
    }

    // ============================
    // エンティティ死亡追跡
    // ============================
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        UUID id = event.getEntity().getUUID();
        for (UndeadRaid raid : activeRaids.values()) {
            raid.aliveEntities.remove(id);
        }
    }

    // ============================
    // ヘルパー
    // ============================

    /** ウェーブごとの通常アンデット数 */
    private static int waveSize(int wave, int aiLevel) {
        // wave1=6+aiLevel, wave2=8+aiLevel×2, … と増加
        return 4 + wave * 2 + aiLevel * 2;
    }

    /** aiLevelごとの発動確率（1分あたり） */
    private static float triggerChance(int aiLevel) {
        return switch (aiLevel) {
            case 1 -> 0.01f;
            case 2 -> 0.02f;
            case 3 -> 0.04f;
            case 4 -> 0.07f;
            default -> 0.12f;
        };
    }

    /** 侵攻の中心地点: ベッドスポーン地点 → なければ現在位置 */
    private static BlockPos getRaidCenter(ServerPlayer player) {
        BlockPos bed = player.getRespawnPosition();
        return bed != null ? bed : player.blockPosition();
    }

    /** アンデット軍団のステータス強化 */
    private static void applyStats(Monster mob, int aiLevel, int wave, boolean elite) {
        double hpMul = 1.0 + aiLevel * 0.2 + wave * 0.1 + (elite ? 0.5 : 0.0);
        double atkMul = 1.0 + aiLevel * 0.15 + wave * 0.05 + (elite ? 0.3 : 0.0);

        if (mob.getAttribute(Attributes.MAX_HEALTH) != null) {
            double hp = mob.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * hpMul;
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
            mob.setHealth((float) hp);
        }
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(
                mob.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() * atkMul);
        }
        if (mob.getAttribute(Attributes.MOVEMENT_SPEED) != null && aiLevel >= 2) {
            mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
                mob.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() * (1.0 + aiLevel * 0.05));
        }
        if (mob.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            mob.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(48.0 + aiLevel * 4.0);
        }
        if (elite && mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
            mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.5);
        }
    }
}
