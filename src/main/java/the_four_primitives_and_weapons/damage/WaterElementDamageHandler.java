package the_four_primitives_and_weapons.damage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class WaterElementDamageHandler {

    // 基礎倍率
    private static final float BASE_MULTIPLIER       = 1.0f;
    // Slowness持続時間 (tick)
    private static final int BASE_SLOWNESS_DURATION  = 80;   // 4秒
    private static final int SLOWNESS_DURATION_PER_LV = 40;  // +2秒/Lv
    // Slownessの増幅レベル (0 = Slowness I)
    private static final int SLOWNESS_AMPLIFIER      = 1;    // Slowness II

    // 消火範囲 (ブロック)
    private static final double EXTINGUISH_RADIUS_BASE   = 4.0;
    private static final double EXTINGUISH_RADIUS_PER_LV = 0.5;
    private static final double EXTINGUISH_RADIUS_CAP    = 10.0;

    /**
     * 水属性ダメージを計算して返す。
     * 対象に Slowness を付与して移動を妨害する + 周囲の延焼/火ブロックを消火する。
     */
    public static float handleWaterDamage(LivingEntity attacker,
                                          LivingEntity target,
                                          ItemStack weapon,
                                          float baseDmg) {
        int level    = ElementalDamageUtils.getElementLevel(weapon);
        int duration = BASE_SLOWNESS_DURATION + SLOWNESS_DURATION_PER_LV * (level - 1);

        // Slowness付与
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                duration,
                SLOWNESS_AMPLIFIER,
                false,
                true
        ));

        // 消火 (target + 周囲 entity + 周囲の火ブロック)
        extinguishAround(target, level);

        return baseDmg * BASE_MULTIPLIER;
    }

    /**
     * レベル指定で水属性ダメージ計算（魔導書経由用）
     */
    public static float calculateDamage(LivingEntity target, float baseDmg, int level) {
        int duration = BASE_SLOWNESS_DURATION + SLOWNESS_DURATION_PER_LV * Math.max(level - 1, 0);
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, duration, SLOWNESS_AMPLIFIER, false, true));
        extinguishAround(target, level);
        return baseDmg * BASE_MULTIPLIER;
    }

    /**
     * target を中心に半径 r の範囲で:
     *   1. target 本人の延焼を消す
     *   2. 周囲の全 LivingEntity の延焼を消す
     *   3. 周囲の火 (Blocks.FIRE) / 魂火 (Blocks.SOUL_FIRE) を air に置換
     *   4. 視覚エフェクト (SPLASH パーティクル)
     *
     * 半径 = base(4) + 0.5 / Lv (cap 10)。
     */
    private static void extinguishAround(LivingEntity target, int level) {
        Level world = target.level();
        if (world.isClientSide()) return;

        double radius = EXTINGUISH_RADIUS_BASE
                + EXTINGUISH_RADIUS_PER_LV * Math.max(level - 1, 0);
        radius = Math.min(radius, EXTINGUISH_RADIUS_CAP);

        // 1. target 本人
        if (target.isOnFire()) target.clearFire();

        // 2. 周囲の LivingEntity
        AABB box = target.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity le : nearby) {
            if (le.isOnFire()) le.clearFire();
        }

        // 3. 周囲の火ブロック
        BlockPos center = target.blockPosition();
        int r = (int) Math.ceil(radius);
        double r2 = radius * radius;
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (pos.distSqr(center) > r2) continue;
                    BlockState state = world.getBlockState(pos);
                    if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                        world.removeBlock(pos, false);
                    }
                }
            }
        }

        // 4. 視覚演出 (server level のみ)
        if (world instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SPLASH,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    20, radius * 0.4, radius * 0.3, radius * 0.4, 0.1);
            sl.sendParticles(ParticleTypes.CLOUD,
                    target.getX(), target.getY() + 0.2, target.getZ(),
                    10, radius * 0.5, 0.1, radius * 0.5, 0.05);
        }
    }
}
