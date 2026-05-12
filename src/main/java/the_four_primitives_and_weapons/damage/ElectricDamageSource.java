package the_four_primitives_and_weapons.damage;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * 電気/雷属性ダメージソース
 * 水中にいるとその周りにもダメージが入る
 *
 * In 1.20.1, DamageSource no longer accepts a String constructor.
 * We use a factory method that creates a DamageSource via damageSources().magic()
 * and applies elemental properties via the IElementalDamageSource mixin.
 */
public final class ElectricDamageSource {

    private ElectricDamageSource() {}

    /**
     * Create an electric damage source using the entity's damage sources.
     * Uses MAGIC as the base type (matching the old .setMagic() behavior).
     */
    public static DamageSource electric(LivingEntity source) {
        DamageSource ds = source.damageSources().magic();
        IElementalDamageSource elemental = (IElementalDamageSource) ds;
        elemental.setElementType(ElementType.ELECTRIC);
        return ds;
    }
}
