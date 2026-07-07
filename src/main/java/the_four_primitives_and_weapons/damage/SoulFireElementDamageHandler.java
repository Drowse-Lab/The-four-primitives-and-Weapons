package the_four_primitives_and_weapons.damage;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 燐火属性 (炎 + 魂)。
 *
 * 青白い魂由来の炎で対象を燃やしつつ、魂属性と同じく残り体力が低い相手へ
 * 少しだけ威力が伸びる。MobEffectには依存しない。
 */
public class SoulFireElementDamageHandler {

    private static final float BASE_MULTIPLIER = 1.08f;
    private static final float LEVEL_MULTIPLIER = 0.035f;
    private static final float MISSING_HEALTH_MULTIPLIER = 0.25f;
    private static final float MAX_LEVEL_BONUS = 0.35f;
    private static final int BASE_FIRE_TICKS = 80;
    private static final int FIRE_TICKS_PER_LEVEL = 35;
    private static final int MAX_FIRE_TICKS = 220;

    public static float handleSoulFireDamage(LivingEntity attacker,
                                             LivingEntity target,
                                             ItemStack weapon,
                                             float baseDmg) {
        int level = ElementalDamageUtils.getElementLevel(weapon);
        return calculateDamage(target, baseDmg, level);
    }

    public static float calculateDamage(LivingEntity target, float originalDamage, int elementLevel) {
        int level = Math.max(1, elementLevel);
        int fireTicks = Math.min(MAX_FIRE_TICKS, BASE_FIRE_TICKS + FIRE_TICKS_PER_LEVEL * (level - 1));
        target.setSecondsOnFire(Math.max(1, fireTicks / 20));
        SoulFireHandler.markSoulSource(target, fireTicks + 10);
        spawnSoulFireParticles(target, level);

        float maxHealth = Math.max(1.0f, target.getMaxHealth());
        float missingHealthRate = Math.max(0.0f, Math.min(1.0f, 1.0f - target.getHealth() / maxHealth));
        float levelBonus = Math.min(MAX_LEVEL_BONUS, level * LEVEL_MULTIPLIER);
        float missingHealthBonus = missingHealthRate * MISSING_HEALTH_MULTIPLIER;

        return originalDamage * (BASE_MULTIPLIER + levelBonus + missingHealthBonus);
    }

    private static void spawnSoulFireParticles(LivingEntity target, int level) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        double mid = target.getY() + target.getBbHeight() * 0.55;
        int count = Math.min(24, 8 + Math.max(1, level));
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                target.getX(), mid, target.getZ(),
                count, 0.28, 0.45, 0.28, 0.035);
        serverLevel.sendParticles(ParticleTypes.SOUL,
                target.getX(), mid, target.getZ(),
                Math.max(3, count / 3), 0.24, 0.38, 0.24, 0.02);
    }
}
