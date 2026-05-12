package the_four_primitives_and_weapons.mixin;

import the_four_primitives_and_weapons.damage.HolyElementDamageHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 聖属性ダメージで不死のトーテムを貫通するMixin
 * LivingEntity.checkTotemDeathProtection をフックし、
 * 聖属性ダメージを受けたエンティティのトーテム発動を無効化する
 */
@Mixin(LivingEntity.class)
public class TotemBypassMixin {

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void onCheckTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        long currentTick = self.level().getGameTime();

        if (HolyElementDamageHandler.shouldBypassTotem(self.getUUID(), currentTick)) {
            // トーテムの発動をスキップ（falseを返す = トーテムで守られなかった）
            cir.setReturnValue(false);
        }
    }
}
