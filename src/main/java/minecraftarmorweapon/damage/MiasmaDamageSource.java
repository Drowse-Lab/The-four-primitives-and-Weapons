package minecraftarmorweapon.damage;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * 瘴気ダメージソース
 * 
 *
 * In 1.20.1, DamageSource no longer accepts a String constructor.
 * We use a factory method that creates a DamageSource via damageSources().magic()
 * and applies elemental properties via the IElementalDamageSource mixin.
 */
public final class MiasmaDamageSource {

    private MiasmaDamageSource() {}

   //**
    // * Create a miasma damage source using the entity's damage sources.
    // * 
    public static DamageSource corrosion(LivingEntity source) {
        DamageSource ds = source.damageSources().magic();
        IElementalDamageSource elemental = (IElementalDamageSource) ds;
        elemental.setElementType(ElementType.MIASMA);
        return ds;
    }
}
