package the_four_primitives_and_weapons.mixin;

import net.minecraft.world.item.enchantment.Enchantment;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 全エンチャントの上限を最低10にする。
 * Integer.MAX_VALUE を返すとクリエイティブタブ/JEIが全レベルの本を列挙し、
 * ワールド参加時に事実上永久停止するため、列挙可能な有限値に制限する。
 */
@Mixin(Enchantment.class)
public class EnchantmentMaxLevelMixin {

    @Inject(method = "getMaxLevel", at = @At("RETURN"), cancellable = true)
    private void msw_overrideMaxLevel(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Math.max(cir.getReturnValue(), 10));
    }
}
