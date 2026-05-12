package the_four_primitives_and_weapons.trait;

import the_four_primitives_and_weapons.entity.AngelTrioEntity;
import the_four_primitives_and_weapons.entity.CommonSoldierEntity;
import the_four_primitives_and_weapons.entity.EliteSoldierEntity;
import the_four_primitives_and_weapons.entity.HeroicTierEntity;
import the_four_primitives_and_weapons.entity.SingularityEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 学習特性ハンドラー — 繰り返し受けたダメージ種別に対し被ダメージを逓減、
 * かつプレイヤーの動きから移動/追跡/攻撃力を強化する。
 *
 *  対象:
 *   - LEARNER特性を持つMob (MobTraitHandlerから register で登録)
 *   - A-Life系エンティティ (CommonSoldier / EliteSoldier / Singularity / HeroicTier / AngelTrio) は全員自動登録
 *
 *  ダメージ学習:
 *   - 同じダメージ種別(近接/遠距離/魔法/爆発/火など)を受ける度に適応度+1
 *   - 被ダメージ軽減率 = min(0.50, adapt * 0.02)
 *   - 5/10/20回の節目で被害者(プレイヤー)に通知＋青パーティクル演出
 *
 *  動き学習 (A-Life Mob全員が対象):
 *   - 毎秒、視界内のプレイヤーが走る/跳ぶ/急横移動する度に観測回数+1
 *   - 50/100/200/400観測ごとに移動速度+5% 追跡距離+4 攻撃力+5%
 */
@Mod.EventBusSubscriber
public class LearningHandler {

    public enum DamageCategory {
        MELEE, PROJECTILE, MAGIC, EXPLOSION, FIRE, FALL, OTHER;
    }

    private static class LearnState {
        final Map<DamageCategory, Integer> adapt = new EnumMap<>(DamageCategory.class);
        final Map<DamageCategory, Integer> lastAnnouncedTier = new EnumMap<>(DamageCategory.class);
        int moveObs = 0;
        int lastMoveTier = 0;
    }

    private static final Map<UUID, LearnState> states = new ConcurrentHashMap<>();
    private static final float REDUCTION_PER_HIT = 0.02f;
    private static final float MAX_REDUCTION = 0.50f;
    private static final double MOVE_OBSERVATION_RADIUS = 16.0;
    private static final int MOVE_OBSERVATION_INTERVAL = 20; // 1秒ごと

    public static void register(UUID id) {
        states.computeIfAbsent(id, k -> new LearnState());
    }

    public static boolean isLearner(UUID id) {
        return states.containsKey(id);
    }

    /** 現在のカテゴリ別適応回数（デバッグ/表示用） */
    public static int getAdaptCount(UUID id, DamageCategory cat) {
        LearnState s = states.get(id);
        if (s == null) return 0;
        return s.adapt.getOrDefault(cat, 0);
    }

    /** A-Life Mob判定: 兵/特異点/天使系の全登録エンティティを対象とする */
    public static boolean isALifeMob(Entity e) {
        return e instanceof CommonSoldierEntity
            || e instanceof EliteSoldierEntity
            || e instanceof SingularityEntity
            || e instanceof HeroicTierEntity
            || e instanceof AngelTrioEntity;
    }

    // A-Life Mobを自動登録
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (isALifeMob(event.getEntity())) {
            register(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim == null || victim.level().isClientSide) return;
        LearnState state = states.get(victim.getUUID());
        if (state == null) return;

        DamageCategory cat = categorize(event.getSource());
        int prior = state.adapt.getOrDefault(cat, 0);

        // 1. 先に蓄積を適用してダメージを軽減（既に学習済み）
        float reduction = Math.min(MAX_REDUCTION, prior * REDUCTION_PER_HIT);
        if (reduction > 0f) {
            event.setAmount(event.getAmount() * (1.0f - reduction));
        }

        // 2. 今回のヒットを学習として記録
        int next = prior + 1;
        state.adapt.put(cat, next);

        // 3. 節目 (5/10/20) で通知
        int tier = thresholdTier(next);
        int lastTier = state.lastAnnouncedTier.getOrDefault(cat, 0);
        if (tier > lastTier) {
            state.lastAnnouncedTier.put(cat, tier);
            announceAdaptation(victim, event.getSource(), cat, next, tier);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() != null) {
            states.remove(event.getEntity().getUUID());
        }
    }

    // ============================
    // プレイヤーの動きを観察して学習する
    // 周囲のA-Life Mobに対して、プレイヤーが走る/跳ぶ/急横移動するたび観測回数+1
    // 閾値(50/100/200/400)で移動速度・追跡距離・攻撃力が段階的に強化される
    // ============================
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % MOVE_OBSERVATION_INTERVAL != 0) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (player.isCreative() || player.isSpectator()) continue;
                Vec3 vel = player.getDeltaMovement();
                double h2 = vel.x * vel.x + vel.z * vel.z;
                boolean sprinting = player.isSprinting() || h2 > 0.04;
                boolean jumping = vel.y > 0.30;
                boolean rapidMove = h2 > 0.15;
                if (!sprinting && !jumping && !rapidMove) continue;

                AABB box = player.getBoundingBox().inflate(MOVE_OBSERVATION_RADIUS);
                for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box)) {
                    LearnState state = states.get(le.getUUID());
                    if (state == null) continue;
                    if (le.distanceToSqr(player) > MOVE_OBSERVATION_RADIUS * MOVE_OBSERVATION_RADIUS) continue;
                    if (!le.hasLineOfSight(player)) continue;

                    state.moveObs++;
                    int tier = moveThresholdTier(state.moveObs);
                    if (tier > state.lastMoveTier) {
                        state.lastMoveTier = tier;
                        applyMovementAdaptation(le, player, tier);
                    }
                }
            }
        }
    }

    private static int moveThresholdTier(int obs) {
        if (obs >= 400) return 4;
        if (obs >= 200) return 3;
        if (obs >= 100) return 2;
        if (obs >= 50) return 1;
        return 0;
    }

    // ============================
    // パターン対策: プレイヤーがアクションAを行った時、予測される次手Bに対し先回り対処
    //   例: 盾構え→攻撃パターン → 盾構え観測時、攻撃対策を発動
    //       弓発射→突進パターン → 弓発射観測時、突進を迎撃
    // 発動条件: Mobが A-Life学習者 で移動学習tier ≥ 2 (100観測以上)
    // ============================
    public static void notifyPlayerAction(Player player, PlayerPatternTracker.PlayerAction action) {
        if (player == null || player.level().isClientSide) return;
        PlayerPatternTracker.PlayerAction predicted = PlayerPatternTracker.predictNext(player.getUUID(), action);
        if (predicted == null) return;

        AABB box = player.getBoundingBox().inflate(MOVE_OBSERVATION_RADIUS);
        for (LivingEntity mob : player.level().getEntitiesOfClass(LivingEntity.class, box)) {
            LearnState state = states.get(mob.getUUID());
            if (state == null) continue;
            if (state.lastMoveTier < 2) continue;
            if (mob.distanceToSqr(player) > MOVE_OBSERVATION_RADIUS * MOVE_OBSERVATION_RADIUS) continue;
            applyCounter(mob, player, action, predicted);
        }
    }

    private static void applyCounter(LivingEntity mob, Player player,
                                     PlayerPatternTracker.PlayerAction current,
                                     PlayerPatternTracker.PlayerAction predicted) {
        String mobName = mob.getName().getString();
        String label = null;

        switch (predicted) {
            case MELEE_ATTACK -> {
                // 予測「近接で斬りかかってくる」 → 盾破壊＋攻撃強化、直後ノックバックで退避
                if (player.isUsingItem() && player.getUseItem().getItem() == Items.SHIELD) {
                    player.getCooldowns().addCooldown(Items.SHIELD, 100);
                    player.stopUsingItem();
                }
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1, false, true));
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, true));
                // 自分から離れる方向に微小ノックバック (カウンター後の回避)
                Vec3 away = mob.position().subtract(player.position()).normalize().scale(0.4);
                mob.setDeltaMovement(mob.getDeltaMovement().add(away.x, 0.2, away.z));
                label = "盾破壊＋回避";
            }
            case SHIELD_BLOCK -> {
                // 予測「盾構え」 → 斧的な攻撃で盾を破壊する構え
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 2, false, true));
                label = "盾破りの構え";
            }
            case BOW_SHOT, CROSSBOW_SHOT -> {
                // 予測「射撃」 → 突進接近＋飛び道具耐性
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2, false, true));
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, false, true));
                Vec3 toward = player.position().subtract(mob.position()).normalize().scale(0.5);
                mob.setDeltaMovement(mob.getDeltaMovement().add(toward.x, 0.0, toward.z));
                label = "接近突進";
            }
            case SPRINT_RUSH -> {
                // 予測「突進してくる」 → 横方向へ回避しつつ迎撃の構え
                Vec3 dir = player.position().subtract(mob.position()).normalize();
                Vec3 strafe = new Vec3(-dir.z, 0, dir.x).scale(0.5); // 垂直ベクトル
                mob.setDeltaMovement(mob.getDeltaMovement().add(strafe.x, 0.0, strafe.z));
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1, false, true));
                label = "横回避＋迎撃";
            }
            case JUMP_DODGE -> {
                // 予測「ジャンプ回避」 → 攻撃範囲&速度強化で空中追撃
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, true));
                mob.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 2, false, true));
                label = "空中追撃";
            }
            case CONSUME -> {
                // 予測「回復」 → 回復させないため攻撃強化＋速度強化で突進
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2, false, true));
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, false, true));
                Vec3 rush = player.position().subtract(mob.position()).normalize().scale(0.6);
                mob.setDeltaMovement(mob.getDeltaMovement().add(rush.x, 0.0, rush.z));
                label = "回復妨害突進";
            }
        }

        if (label != null) {
            player.displayClientMessage(Component.literal(
                "§c" + mobName + "§7が対策を実行: §c" + label), true);
            if (mob.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    mob.getX(), mob.getY() + mob.getBbHeight() * 0.9, mob.getZ(),
                    6, 0.3, 0.2, 0.3, 0.1);
            }
        }
    }

    private static void applyMovementAdaptation(LivingEntity mob, Player observedPlayer, int tier) {
        // 各段階で +5% 移動速度, +4 追跡距離, +5% 攻撃力
        if (mob.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            var attr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            attr.setBaseValue(attr.getBaseValue() * 1.05);
        }
        if (mob.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            var attr = mob.getAttribute(Attributes.FOLLOW_RANGE);
            attr.setBaseValue(attr.getBaseValue() + 4.0);
        }
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            var attr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            attr.setBaseValue(attr.getBaseValue() * 1.05);
        }
        String name = mob.getName().getString();
        observedPlayer.displayClientMessage(Component.literal(
            "§b" + name + "§7はあなたの動きを学習した §b[段階" + tier + "]"), false);
        if (mob.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.WITCH,
                mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(),
                12 + tier * 3, 0.5, 0.6, 0.5, 0.3);
        }
    }

    private static DamageCategory categorize(DamageSource src) {
        if (src == null) return DamageCategory.OTHER;
        if (src.is(DamageTypes.EXPLOSION) || src.is(DamageTypes.PLAYER_EXPLOSION)) return DamageCategory.EXPLOSION;
        if (src.is(DamageTypes.ARROW) || src.is(DamageTypes.TRIDENT)
            || src.is(DamageTypes.THROWN) || src.is(DamageTypes.MOB_PROJECTILE)) return DamageCategory.PROJECTILE;
        if (src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC)
            || src.is(DamageTypes.WITHER) || src.is(DamageTypes.DRAGON_BREATH)) return DamageCategory.MAGIC;
        if (src.is(DamageTypes.ON_FIRE) || src.is(DamageTypes.IN_FIRE)
            || src.is(DamageTypes.LAVA) || src.is(DamageTypes.HOT_FLOOR)) return DamageCategory.FIRE;
        if (src.is(DamageTypes.FALL) || src.is(DamageTypes.STALAGMITE)) return DamageCategory.FALL;
        if (src.is(DamageTypes.MOB_ATTACK) || src.is(DamageTypes.MOB_ATTACK_NO_AGGRO)
            || src.is(DamageTypes.PLAYER_ATTACK)) return DamageCategory.MELEE;
        return DamageCategory.OTHER;
    }

    private static int thresholdTier(int count) {
        if (count >= 20) return 3;
        if (count >= 10) return 2;
        if (count >= 5) return 1;
        return 0;
    }

    private static String categoryName(DamageCategory cat) {
        return switch (cat) {
            case MELEE -> "近接";
            case PROJECTILE -> "飛び道具";
            case MAGIC -> "魔法";
            case EXPLOSION -> "爆発";
            case FIRE -> "炎";
            case FALL -> "落下";
            case OTHER -> "その他";
        };
    }

    private static void announceAdaptation(LivingEntity victim, DamageSource src, DamageCategory cat, int count, int tier) {
        String mobName = victim.getName().getString();
        String msg = "§b" + mobName + "§7は §b" + categoryName(cat) + " §7に適応した (" + count + "回)";
        if (src.getEntity() instanceof Player attacker) {
            attacker.displayClientMessage(Component.literal(msg), false);
        }
        if (victim.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ENCHANT,
                victim.getX(), victim.getY() + victim.getBbHeight(), victim.getZ(),
                8 + tier * 4, 0.5, 0.6, 0.5, 0.6);
        }
    }
}
