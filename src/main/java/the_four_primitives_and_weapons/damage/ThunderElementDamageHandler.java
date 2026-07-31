package the_four_primitives_and_weapons.damage;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;


import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ThunderElementDamageHandler {

    // 基礎倍率（電気属性と同じ）
    private static final float BASE_MULTIPLIER        = 1.2f;
    private static final float WATER_MULTIPLIER       = 1.5f;
    private static final float CONDUCTOR_BONUS        = 0.3f;

    // ── 擬似落雷 ( 雨 / 雷雨の日限定 ) ────────────────────────────────
    /** 雨の日に落雷が起きるまでの累計ダメージ。 */
    private static final float  STRIKE_THRESHOLD_RAIN      = 40.0f;
    /** 雷雨の日は半分の蓄積で落ちる。 */
    private static final float  STRIKE_THRESHOLD_THUNDER   = 20.0f;
    /** 属性Lv 1 につき必要量が減る。 */
    private static final float  STRIKE_THRESHOLD_PER_LEVEL = 1.5f;
    private static final float  STRIKE_THRESHOLD_MIN       = 8.0f;
    private static final float  STRIKE_DAMAGE_BASE         = 5.0f;
    private static final float  STRIKE_DAMAGE_PER_LEVEL    = 1.0f;
    private static final float  STRIKE_DAMAGE_MAX          = 20.0f;
    /** 雷雨の日は落雷そのものも強い。 */
    private static final float  STRIKE_DAMAGE_THUNDER_BONUS = 1.5f;
    private static final double STRIKE_SPLASH_RADIUS       = 3.0;
    private static final float  STRIKE_SPLASH_RATE         = 0.5f;

    /** 命中してから実際に雷が落ちるまでの遅延 ( tick )。 */
    private static final int   STRIKE_DELAY_TICKS         = 5;

    /** 攻撃者UUID → 蓄積した雷属性ダメージ。 */
    private static final Map<UUID, Float> chargedDamage = new ConcurrentHashMap<>();

    // AOE範囲 (ブロック) — 水中 > 雨 > 0
    private static final double AOE_RADIUS_WATER      = 12.0;
    private static final double AOE_RADIUS_RAIN       = 8.0;
    /** レベル毎の半径ボーナス (上限 5) */
    private static final double AOE_RADIUS_PER_LEVEL  = 0.5;
    private static final double AOE_RADIUS_LEVEL_CAP  = 5.0;

    /** 条件に応じた AOE 半径を計算 (水中 / 雨 / それ以外で 0)。 */
    private static double computeAoeRadius(LivingEntity target, int level) {
        boolean inWater = target.isInWaterOrBubble();
        boolean inRainOnly = !inWater && target.isInWaterOrRain();
        double base;
        if (inWater) base = AOE_RADIUS_WATER;
        else if (inRainOnly) base = AOE_RADIUS_RAIN;
        else return 0;
        return base + Math.min(level * AOE_RADIUS_PER_LEVEL, AOE_RADIUS_LEVEL_CAP);
    }

    /**
     * 命中対象に対する Thunder の視覚 + 独自硬直。
     * 水/雨判定無しで毎回発動 — これで地上でも雷属性らしさを感じられる。
     *   - 雷属性色 ( ElementalParticles ) の dust + ELECTRIC_SPARK + FLASH
     *   - MobEffectではなく、attribute modifierによる短時間の鈍化 + 水平方向の減速
     */
    private static void applyThunderImpact(LivingEntity target, int level) {
        if (target == null || target.level().isClientSide()) return;

        int slowLevel = Math.min(Math.max(1, level / 2), 4);
        SpecialDebuffHandler.applySlowness(target, 40, slowLevel);
        target.setDeltaMovement(target.getDeltaMovement().multiply(0.65D, 1.0D, 0.65D));
        target.hurtMarked = true;

        // 視覚エフェクト — 色は攻撃時の雷属性パーティクル ( ElementalParticles ) と統一する。
        if (target.level() instanceof ServerLevel sl) {
            try {
                double mid = target.getY() + target.getBbHeight() / 2.0;
                ElementalParticles.spawnWide(sl, ElementType.THUNDER,
                        target.getX(), mid, target.getZ(),
                        Math.min(16, 8 + level), 0.3, 0.5);
                sl.sendParticles(ParticleTypes.FLASH,
                        target.getX(), mid, target.getZ(),
                        1, 0, 0, 0, 0);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * 雨 / 雷雨の日だけ、 与えた雷属性ダメージを攻撃者ごとに蓄積し、
     * 一定量に達したら命中対象へ擬似的な落雷を落とす。
     *
     * 「擬似的」= バニラの落雷エンティティは {@code setVisualOnly(true)} で見た目と音だけを使い、
     * ダメージ / 着火 / 変身 ( 豚→ゾンビピグリン等 ) は起こさない。 実ダメージは
     * mod の thunder DamageType で与える。
     *
     * @param dealtDamage この一撃で与えた雷属性ダメージ ( 倍率適用後 )
     */
    private static void accumulateAndMaybeStrike(LivingEntity attacker, LivingEntity target,
                                                 float dealtDamage, int level) {
        if (attacker == null || target == null || dealtDamage <= 0.0f) return;
        if (!(target.level() instanceof ServerLevel sl)) return;

        boolean thundering = sl.isThundering();
        boolean raining = sl.isRaining();
        if (!raining && !thundering) return;   // 晴れの日は蓄積しない

        int lv = Math.max(1, level);
        float threshold = Math.max(STRIKE_THRESHOLD_MIN,
                (thundering ? STRIKE_THRESHOLD_THUNDER : STRIKE_THRESHOLD_RAIN)
                        - STRIKE_THRESHOLD_PER_LEVEL * lv);

        UUID id = attacker.getUUID();

        // 既に落雷が予約されている間は蓄積しない。
        // ( 雨天AOEなど 1 スイングで複数回この計算が走っても、 落雷は 1 回だけ )
        if (hasPendingStrike(id)) {
            chargedDamage.put(id, 0.0f);
            return;
        }

        float total = chargedDamage.merge(id, dealtDamage, Float::sum);
        if (total < threshold) return;

        // 落雷が確定したら蓄積をリセット
        chargedDamage.put(id, 0.0f);

        // この場で hurt すると「今処理中の一撃」の中に割り込む形になり、
        // 熟練度ペナルティ等の一撃単位の補正を落雷側が食ってしまう。
        // 少し遅らせて独立した一撃として処理する ( 見た目にも「後から落ちてくる」)。
        pendingStrikes.add(new PendingStrike(attacker.getUUID(), target.getUUID(), lv, STRIKE_DELAY_TICKS));
    }

    /** 擬似落雷を1発落とす。 */
    private static void strikeThunder(ServerLevel sl, LivingEntity attacker, LivingEntity target,
                                      int level, boolean thundering) {
        try {
            // 見た目と音だけの落雷 ( 着火 / 変身なし )
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
            if (bolt != null) {
                bolt.moveTo(target.getX(), target.getY(), target.getZ());
                bolt.setVisualOnly(true);
                if (attacker instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    bolt.setCause(serverPlayer);
                }
                sl.addFreshEntity(bolt);
            } else {
                sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0f, 1.0f);
            }

            float damage = Math.min(STRIKE_DAMAGE_MAX,
                    STRIKE_DAMAGE_BASE + STRIKE_DAMAGE_PER_LEVEL * level);
            if (thundering) damage *= STRIKE_DAMAGE_THUNDER_BONUS;

            hurtWithThunder(sl, attacker, target, damage, level);

            // 落雷点の周囲にも軽い余波
            for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(STRIKE_SPLASH_RADIUS),
                    e -> e != target && e != attacker && e.isAlive())) {
                hurtWithThunder(sl, attacker, nearby, damage * STRIKE_SPLASH_RATE, level);
            }

            ElementalParticles.spawnWide(sl, ElementType.THUNDER,
                    target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(),
                    24, 0.5, 0.8);
            sl.sendParticles(ParticleTypes.FLASH,
                    target.getX(), target.getY() + 0.5, target.getZ(), 1, 0, 0, 0, 0);
        } catch (Throwable ignored) {
        }
    }

    // ── 落雷の遅延実行 ──────────────────────────────────────────────

    private static final class PendingStrike {
        final UUID attackerId;
        final UUID targetId;
        final int level;
        int delay;

        PendingStrike(UUID attackerId, UUID targetId, int level, int delay) {
            this.attackerId = attackerId;
            this.targetId = targetId;
            this.level = level;
            this.delay = delay;
        }
    }

    private static final java.util.List<PendingStrike> pendingStrikes =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    /** その攻撃者の落雷が予約待ちかどうか。 */
    private static boolean hasPendingStrike(UUID attackerId) {
        for (PendingStrike pending : pendingStrikes) {
            if (pending.attackerId.equals(attackerId)) return true;
        }
        return false;
    }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
    public static class ThunderStrikeTickHandler {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
            if (event.getServer() == null || pendingStrikes.isEmpty()) return;

            java.util.List<PendingStrike> done = new java.util.ArrayList<>();
            for (PendingStrike pending : pendingStrikes) {
                if (--pending.delay > 0) continue;
                done.add(pending);
                resolve(event.getServer(), pending);
            }
            if (!done.isEmpty()) pendingStrikes.removeAll(done);
        }

        private static void resolve(net.minecraft.server.MinecraftServer server, PendingStrike pending) {
            for (ServerLevel sl : server.getAllLevels()) {
                net.minecraft.world.entity.Entity targetEntity = sl.getEntity(pending.targetId);
                if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) continue;

                net.minecraft.world.entity.Entity attackerEntity = sl.getEntity(pending.attackerId);
                LivingEntity attacker = attackerEntity instanceof LivingEntity living ? living : null;

                strikeThunder(sl, attacker, target, pending.level, sl.isThundering());
                return;
            }
        }
    }

    /** 落雷ダメージ。 属性を明示した DamageSource で与えるので、 属性処理が再入しない。 */
    private static void hurtWithThunder(ServerLevel sl, LivingEntity attacker, LivingEntity target,
                                        float damage, int level) {
        if (damage <= 0.0f) return;
        DamageSource source = ModDamageSources.ofElement(sl, ElementType.THUNDER, attacker);
        if (source instanceof IElementalDamageSource elemental) {
            elemental.setElementType(ElementType.THUNDER);
            elemental.setElementLevel(level);
        }
        target.invulnerableTime = 0;
        target.hurt(source, damage);
    }

    /**
     * 雷属性ダメージを計算して返す。
     * 電気属性と同仕様：水中でAOE、導体装備時にボーナス。
     *
     * @param attacker 攻撃者
     * @param target   攻撃対象
     * @param weapon   使用武器
     * @param baseDmg  基礎ダメージ
     * @return 属性込みの最終ダメージ
     */
    public static float handleThunderDamage(LivingEntity attacker,
                                            LivingEntity target,
                                            ItemStack weapon,
                                            float baseDmg) {

        float multiplier = BASE_MULTIPLIER;

        // 水中 / 雨ボーナス + AOE
        int weaponLevel = ElementalDamageUtils.getElementLevel(weapon);
        double radius = computeAoeRadius(target, weaponLevel);
        if (radius > 0) {
            multiplier = WATER_MULTIPLIER;
            Level level = target.level();
            List<LivingEntity> nearby = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(
                            target.getX() - radius, target.getY() - radius, target.getZ() - radius,
                            target.getX() + radius, target.getY() + radius, target.getZ() + radius
                    )
            );
            for (LivingEntity nearby_entity : nearby) {
                if (nearby_entity != target && nearby_entity != attacker
                        && (nearby_entity.isInWaterOrRain() || nearby_entity.isInWaterRainOrBubble())) {
                    nearby_entity.hurt(
                            ModDamageSources.ofElement(level, ElementType.THUNDER, attacker),
                            baseDmg * multiplier * 0.5f
                    );
                }
            }
        }

        // 導体装備ボーナス（鉄・金・チェーン装備やタグ指定された防具を導体とみなす）
        int conductorCount = ElectricElementDamageHandler.countConductiveArmorPieces(target);
        multiplier += CONDUCTOR_BONUS * conductorCount;

        // 命中対象に視覚 + 独自硬直を付与 (場所問わず毎回)
        applyThunderImpact(target, weaponLevel);

        float finalDamage = baseDmg * multiplier;
        // 雨 / 雷雨の日は蓄積 → 一定量で擬似落雷
        accumulateAndMaybeStrike(attacker, target, finalDamage, weaponLevel);
        return finalDamage;
    }

    /**
     * レベル指定で雷属性ダメージ計算（魔導書経由用）
     */
    public static float calculateDamage(LivingEntity attacker, LivingEntity target, float baseDmg, int level) {
        float multiplier = BASE_MULTIPLIER;
        double radius = computeAoeRadius(target, level);
        if (radius > 0) {
            multiplier = WATER_MULTIPLIER;
            net.minecraft.world.level.Level world = target.level();
            List<LivingEntity> nearby = world.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(
                            target.getX() - radius, target.getY() - radius, target.getZ() - radius,
                            target.getX() + radius, target.getY() + radius, target.getZ() + radius
                    )
            );
            for (LivingEntity nearby_entity : nearby) {
                if (nearby_entity != target && nearby_entity != attacker
                        && (nearby_entity.isInWaterOrRain() || nearby_entity.isInWaterRainOrBubble())) {
                    nearby_entity.hurt(
                            ModDamageSources.ofElement(world, ElementType.THUNDER, attacker),
                            baseDmg * multiplier * 0.5f);
                }
            }
        }
        multiplier += CONDUCTOR_BONUS * ElectricElementDamageHandler.countConductiveArmorPieces(target);
        applyThunderImpact(target, level);

        float finalDamage = baseDmg * multiplier;
        accumulateAndMaybeStrike(attacker, target, finalDamage, level);
        return finalDamage;
    }
}
