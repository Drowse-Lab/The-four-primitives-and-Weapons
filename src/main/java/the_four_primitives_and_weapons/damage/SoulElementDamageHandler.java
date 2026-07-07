package the_four_primitives_and_weapons.damage;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 魂属性ダメージハンドラー。
 *
 * 対象の魂を削る属性として、相手の残り体力が低いほど少し威力が伸びる。
 * MobEffectには依存せず、専用 DamageType と魂系パーティクルだけで表現する。
 */
public class SoulElementDamageHandler {

    private static final float BASE_MULTIPLIER = 1.05f;
    private static final float LEVEL_MULTIPLIER = 0.04f;
    private static final float MISSING_HEALTH_MULTIPLIER = 0.35f;
    private static final float MAX_LEVEL_BONUS = 0.40f;

    public static float handleSoulDamage(LivingEntity attacker,
                                         LivingEntity target,
                                         ItemStack weapon,
                                         float baseDmg) {
        int level = ElementalDamageUtils.getElementLevel(weapon);
        float damage = calculateDamage(target, baseDmg, level);
        applySoulDamage(target, Math.max(0.0f, damage - baseDmg), attacker, level);
        return damage;
    }

    public static float calculateDamage(LivingEntity target, float originalDamage, int elementLevel) {
        int level = Math.max(1, elementLevel);
        spawnSoulParticles(target, level);

        float maxHealth = Math.max(1.0f, target.getMaxHealth());
        float missingHealthRate = Math.max(0.0f, Math.min(1.0f, 1.0f - target.getHealth() / maxHealth));
        float levelBonus = Math.min(MAX_LEVEL_BONUS, level * LEVEL_MULTIPLIER);
        float missingHealthBonus = missingHealthRate * MISSING_HEALTH_MULTIPLIER;

        return originalDamage * (BASE_MULTIPLIER + levelBonus + missingHealthBonus);
    }

    public static void applySoulDamage(LivingEntity target, float damage, LivingEntity source, int level) {
        if (damage <= 0.0f) return;

        DamageSource ds = ModDamageSources.ofElement(target.level(), ElementType.SOUL, source);
        if (ds instanceof IElementalDamageSource elementalSource) {
            elementalSource.setElementType(ElementType.SOUL);
            elementalSource.setElementLevel(level);
        }
        target.hurt(ds, damage);
    }

    private static void spawnSoulParticles(LivingEntity target, int level) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        double mid = target.getY() + target.getBbHeight() * 0.55;
        int count = Math.min(18, 5 + Math.max(1, level));
        serverLevel.sendParticles(ParticleTypes.SOUL,
                target.getX(), mid, target.getZ(),
                count, 0.3, 0.45, 0.3, 0.03);
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                target.getX(), mid, target.getZ(),
                Math.max(2, count / 3), 0.22, 0.35, 0.22, 0.02);
    }
}
