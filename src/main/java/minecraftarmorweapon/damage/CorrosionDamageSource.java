package minecraftarmorweapon.damage;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * 侵食/闇属性ダメージソース
 * 防御力を一時的に落とす
 *
 * In 1.20.1, DamageSource no longer accepts a String constructor.
 * We use a factory method that creates a DamageSource via damageSources().magic()
 * and applies elemental properties via the IElementalDamageSource mixin.
 */
public final class CorrosionDamageSource {

    private CorrosionDamageSource() {}

    /**
     * Create a corrosion damage source using the entity's damage sources.
     * Uses MAGIC as the base type (bypasses armor similar to the old .setMagic().bypassArmor()).
     */
    public static DamageSource corrosion(LivingEntity source) {
        DamageSource ds = source.damageSources().magic();
        IElementalDamageSource elemental = (IElementalDamageSource) ds;
        elemental.setElementType(ElementType.CORROSION);
        return ds;
    }
}
