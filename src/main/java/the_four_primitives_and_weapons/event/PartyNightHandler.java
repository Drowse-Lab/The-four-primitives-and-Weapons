package the_four_primitives_and_weapons.event;

import the_four_primitives_and_weapons.ai.lisp.ProgressionTracker;
import the_four_primitives_and_weapons.command.CustomDifficultyCommand;
import the_four_primitives_and_weapons.command.CustomDifficultyCommand.CustomDifficulty;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * パーティナイト — 難易度MAX (lunatic_extreme / aiLevel 5+) で夜間に発動する狂乱モード。
 *
 *  - 夜間 (gameTime 13000〜23000) の間、100tick(5秒)ごとにプレイヤー周囲に追加の敵をスポーン
 *  - スポーン量は進行度ボーナスでさらに増加
 *  - 夜開始時と朝終了時にプレイヤーへ通知＋効果音
 *  - スポーンするのは通常のホスト系Mob (zombie/skeleton/spider/creeper/witch/phantom...)
 *  - パーティナイト中のMobは永続化＋プレイヤー追尾バフ
 */
@Mod.EventBusSubscriber
public class PartyNightHandler {

    private static final int SPAWN_INTERVAL_TICKS = 100; // 5秒ごと
    private static final int NIGHT_START = 13000;
    private static final int NIGHT_END = 23000;

    // 通知済みプレイヤー（同じ夜で複数回通知するのを防ぐ）
    private static final Set<UUID> notifiedPlayers = new HashSet<>();
    // 現在夜中かどうかの直近状態（朝→夜/夜→朝の遷移検知用、level毎）
    private static final java.util.Map<String, Boolean> wasNight = new java.util.concurrent.ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private static final EntityType<? extends Mob>[] PARTY_MOBS = new EntityType[]{
        EntityType.ZOMBIE,
        EntityType.SKELETON,
        EntityType.SPIDER,
        EntityType.CREEPER,
        EntityType.WITCH,
        EntityType.HUSK,
        EntityType.STRAY,
        EntityType.ZOMBIE_VILLAGER,
        EntityType.DROWNED,
        EntityType.PHANTOM
    };

    /** Randomは毎tick生成するとGC圧なのでキャッシュ */
    private static final Random SHARED_RAND = new Random();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // 20tick毎で十分 (遷移検知もスポーンも1秒分解能で違和感なし)
        long tick = event.getServer().getTickCount();
        if (tick % 20 != 0) return;

        CustomDifficulty diff = CustomDifficultyCommand.getCurrentDifficulty();
        // 難易度MAX (aiLevel 5以上) でのみ発動
        if (diff.getAiLevel() < 5) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.dimensionType().natural()) continue;
            // プレイヤーがいないlevelは完全スキップ
            java.util.List<ServerPlayer> players = level.players();
            if (players.isEmpty()) continue;

            long dayTime = level.getDayTime() % 24000;
            boolean isNight = dayTime >= NIGHT_START && dayTime <= NIGHT_END;
            String key = level.dimension().location().toString();
            boolean wasNightInLevel = wasNight.getOrDefault(key, false);

            // 夜に切替わった瞬間: 通知
            if (isNight && !wasNightInLevel) {
                for (ServerPlayer p : players) {
                    p.sendSystemMessage(Component.literal(
                        "§d§l♪ §5§l狂乱の夜 §d§l♪ §7— パーティタイム開始！"));
                    level.playSound(null, p.blockPosition(),
                        SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6f, 1.5f);
                }
                notifiedPlayers.clear();
            }
            // 朝に切替わった瞬間: 通知
            if (!isNight && wasNightInLevel) {
                for (ServerPlayer p : players) {
                    p.sendSystemMessage(Component.literal(
                        "§e§l☀ §7狂乱の夜が明けた"));
                }
                notifiedPlayers.clear();
            }
            wasNight.put(key, isNight);

            if (!isNight) continue;
            if (tick % SPAWN_INTERVAL_TICKS != 0) continue;

            // プレイヤー毎に追加スポーン
            int progBonus = ProgressionTracker.getBaseLevelBonus();
            int moonBonus = MoonPhaseDifficulty.getDifficultyBonus(level);
            int baseCount = 1 + Math.min(5, progBonus / 40) + (moonBonus / 4);

            for (ServerPlayer player : players) {
                if (player.isCreative() || player.isSpectator()) continue;
                int count = baseCount + SHARED_RAND.nextInt(2);
                spawnPartyMobs(level, player, count, SHARED_RAND);
            }
        }
    }

    private static void spawnPartyMobs(ServerLevel level, ServerPlayer player, int count, Random rand) {
        for (int i = 0; i < count; i++) {
            // プレイヤーから24〜40ブロック離れた位置にスポーン
            double angle = rand.nextDouble() * Math.PI * 2.0;
            double radius = 24.0 + rand.nextDouble() * 16.0;
            int x = (int)(player.getX() + Math.cos(angle) * radius);
            int z = (int)(player.getZ() + Math.sin(angle) * radius);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            // Phantomは空中スポーンが自然
            if (i < 0) y += 30;

            EntityType<? extends Mob> type = PARTY_MOBS[rand.nextInt(PARTY_MOBS.length)];
            Mob mob = type.create(level);
            if (mob == null) continue;

            mob.moveTo(x, y, z, rand.nextFloat() * 360f, 0f);
            mob.getPersistentData().putString(
                the_four_primitives_and_weapons.client.renderer.SpecialLabelRenderLayer.NBT_SPECIAL_LABEL,
                "§d[狂乱の夜]");
            applyPartyBuffs(mob, player);
            level.addFreshEntity(mob);
        }
    }

    /** パーティナイトMobに水中/火耐性・暗視・初期ターゲットを付与（persistenceは付けず自然despawn） */
    private static void applyPartyBuffs(Mob mob, ServerPlayer player) {
        // 永続化しない — 遠距離で自然にdespawnさせて負荷軽減
        int inf = Integer.MAX_VALUE;
        mob.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, inf, 0, false, false));
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, inf, 0, false, false));
        mob.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, inf, 0, false, false));

        // 進行度ボーナスでHP/攻撃力強化
        int progBonus = ProgressionTracker.getBaseLevelBonus();
        double hpMul = 1.0 + progBonus / 100.0;
        double atkMul = 1.0 + progBonus / 150.0;
        if (mob.getAttribute(Attributes.MAX_HEALTH) != null) {
            double hp = mob.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * hpMul;
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
            mob.setHealth((float) hp);
        }
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(
                mob.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() * atkMul);
        }
        if (mob.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            mob.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(64.0);
        }

        mob.setTarget(player);
    }
}
