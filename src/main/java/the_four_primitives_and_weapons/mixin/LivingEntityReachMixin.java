package the_four_primitives_and_weapons.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.common.ForgeMod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import the_four_primitives_and_weapons.init.MawExtraAttributes;

/** カスタム entity_reach の差分を Forge の実際のリーチ値へ反映する。 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityReachMixin {
    private static final double DEFAULT_REACH = 3.0;

    @Inject(method = "getAttributeValue", at = @At("RETURN"), cancellable = true, require = 0)
    private void tfpw$applyCustomEntityReach(Attribute attribute, CallbackInfoReturnable<Double> cir) {
        if (attribute != ForgeMod.ENTITY_REACH.get()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        var custom = self.getAttribute(MawExtraAttributes.ENTITY_REACH.get());
        if (custom != null) {
            cir.setReturnValue(cir.getReturnValueD() + custom.getValue() - DEFAULT_REACH);
        }
    }
}
