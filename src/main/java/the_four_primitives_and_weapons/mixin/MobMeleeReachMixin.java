package the_four_primitives_and_weapons.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import the_four_primitives_and_weapons.init.MawExtraAttributes;

/**
 * カスタム entity_reach を MOB の近接攻撃距離へ反映する。
 *
 * Forge の entity_reach はプレイヤー専用のため、MOB 側は近接攻撃の判定距離
 * (Mob#getMeleeAttackRangeSqr) を直接伸ばす。デフォルト 3.0 からの差分をブロック単位で加算する。
 */
@Mixin(Mob.class)
public abstract class MobMeleeReachMixin {

    @Inject(method = "getMeleeAttackRangeSqr", at = @At("RETURN"), cancellable = true, require = 0)
    private void tfpw$applyCustomMeleeReach(LivingEntity target, CallbackInfoReturnable<Double> cir) {
        Mob self = (Mob) (Object) this;
        AttributeInstance custom = self.getAttribute(MawExtraAttributes.ENTITY_REACH.get());
        if (custom == null) return;

        double bonus = custom.getValue() - MawExtraAttributes.ENTITY_REACH.get().getDefaultValue();
        if (bonus == 0.0) return;

        // 戻り値は距離の2乗。一度ブロック単位へ戻して加算し、再度2乗する
        double range = Math.sqrt(Math.max(cir.getReturnValueD(), 0.0)) + bonus;
        if (range < 0.0) range = 0.0;
        cir.setReturnValue(range * range);
    }
}
