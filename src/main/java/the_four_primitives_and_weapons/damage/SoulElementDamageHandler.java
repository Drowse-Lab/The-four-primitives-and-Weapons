package the_four_primitives_and_weapons.damage;

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
        float damage = calculateDamage(attacker, target, baseDmg, level);
        applySoulDamage(target, Math.max(0.0f, damage - baseDmg), attacker, level);
        return damage;
    }

    public static float calculateDamage(LivingEntity target, float originalDamage, int elementLevel) {
        return calculateDamage(null, target, originalDamage, elementLevel);
    }

    public static float calculateDamage(LivingEntity attacker, LivingEntity target,
                                        float originalDamage, int elementLevel) {
        int level = Math.max(1, elementLevel);
        SoulEdgeEffect.play(target, attacker, level);

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
}
