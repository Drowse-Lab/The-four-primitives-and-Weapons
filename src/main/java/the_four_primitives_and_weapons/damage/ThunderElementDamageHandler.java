package the_four_primitives_and_weapons.damage;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import org.joml.Vector3f;

import java.util.List;

public class ThunderElementDamageHandler {

    // 基礎倍率（電気属性と同じ）
    private static final float BASE_MULTIPLIER        = 1.2f;
    private static final float WATER_MULTIPLIER       = 1.5f;
    private static final float CONDUCTOR_BONUS        = 0.3f;

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
     *   - ELECTRIC_SPARK パーティクル + 黄色 dust
     *   - MobEffectではなく、attribute modifierによる短時間の鈍化 + 水平方向の減速
     */
    private static void applyThunderImpact(LivingEntity target, int level) {
        if (target == null || target.level().isClientSide()) return;

        int slowLevel = Math.min(Math.max(1, level / 2), 4);
        SpecialDebuffHandler.applySlowness(target, 40, slowLevel);
        target.setDeltaMovement(target.getDeltaMovement().multiply(0.65D, 1.0D, 0.65D));
        target.hurtMarked = true;

        // 視覚エフェクト
        if (target.level() instanceof ServerLevel sl) {
            try {
                double mid = target.getY() + target.getBbHeight() / 2.0;
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        target.getX(), mid, target.getZ(),
                        20, 0.3, 0.5, 0.3, 0.15);
                DustParticleOptions yellow = new DustParticleOptions(
                        new Vector3f(1.0f, 1.0f, 0.3f), 1.0f);
                sl.sendParticles(yellow,
                        target.getX(), mid, target.getZ(),
                        15, 0.3, 0.5, 0.3, 0.05);
                sl.sendParticles(ParticleTypes.FLASH,
                        target.getX(), mid, target.getZ(),
                        1, 0, 0, 0, 0);
            } catch (Throwable ignored) {}
        }
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

        return baseDmg * multiplier;
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
        return baseDmg * multiplier;
    }
}
