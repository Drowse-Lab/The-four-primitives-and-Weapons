package the_four_primitives_and_weapons.damage;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class DarkElementDamageHandler {

    // 基礎倍率
    private static final float BASE_MULTIPLIER          = 1.1f;
    // 暗所ボーナス（光レベル7以下）
    private static final float DARKNESS_MULTIPLIER      = 1.4f;
    private static final int   DARKNESS_LIGHT_THRESHOLD = 7;

    // MobEffectではなく、独自の攻撃力低下 + 黒霧粒子として扱う。
    private static final int BASE_SHROUD_DURATION    = 60;  // 3秒
    private static final int SHROUD_DURATION_PER_LV  = 40;  // +2秒/Lv

    // 持続ダメージ設定
    private static final int   DOT_MIN_LEVEL          = 3;    // 発動最低レベル
    private static final int   DOT_BASE_DURATION      = 60;   // 基礎持続tick (3秒)
    private static final int   DOT_DURATION_PER_LEVEL = 40;   // +2秒/Lv
    private static final float DOT_DAMAGE_BASE        = 0.5f; // 基礎ダメージ/tick
    private static final float DOT_DAMAGE_PER_LEVEL   = 0.5f; // +0.5/tick per Lv

    // ────────────────────────────────────────────────────────────────

    /**
     * 闇属性ダメージを計算して返す。
     */
    public static float handleDarkDamage(LivingEntity attacker,
                                         LivingEntity target,
                                         ItemStack weapon,
                                         float baseDmg) {
        int level = ElementalDamageUtils.getElementLevel(weapon);
        return calculateDamage(attacker, target, baseDmg, level);
    }

    /**
     * レベル指定で闇属性ダメージ計算（魔導書経由用）
     */
    public static float calculateDamage(LivingEntity attacker, LivingEntity target,
                                        float baseDmg, int level) {
        float multiplier = BASE_MULTIPLIER;

        // 暗所ボーナス
        int lightLevel = attacker.level().getBrightness(
                net.minecraft.world.level.LightLayer.BLOCK,
                attacker.blockPosition());
        if (lightLevel <= DARKNESS_LIGHT_THRESHOLD) {
            multiplier = DARKNESS_MULTIPLIER;
        }

        int shroudDuration = BASE_SHROUD_DURATION
                + SHROUD_DURATION_PER_LV * Math.max(level - 1, 0);
        SpecialDebuffHandler.applyWeakness(target, shroudDuration, Math.max(1, level));
        spawnDarkShroudParticles(target, level);

        // 独自持続ダメージ（Wither effect / wither source 不使用）
        if (level >= DOT_MIN_LEVEL) {
            int   duration   = DOT_BASE_DURATION
                    + DOT_DURATION_PER_LEVEL * (level - DOT_MIN_LEVEL);
            float dmgPerTick = DOT_DAMAGE_BASE
                    + DOT_DAMAGE_PER_LEVEL * (level - DOT_MIN_LEVEL);
            ElementalDoTHandler.apply(target, duration, dmgPerTick, ElementType.DARK);
        }

        return baseDmg * multiplier;
    }

    // ────────────────────────────────────────────────────────────────

    private static void spawnDarkShroudParticles(LivingEntity target, int level) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;
        double mid = target.getY() + target.getBbHeight() * 0.55;
        serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                target.getX(), mid, target.getZ(),
                Math.min(16, 4 + level), 0.3, 0.45, 0.3, 0.03);
        serverLevel.sendParticles(ParticleTypes.SMOKE,
                target.getX(), mid, target.getZ(),
                Math.min(12, 3 + level), 0.4, 0.35, 0.4, 0.04);
    }
}
