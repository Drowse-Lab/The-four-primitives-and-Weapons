package the_four_primitives_and_weapons.mixin;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import the_four_primitives_and_weapons.util.SayaDesign;

/**
 * 機織り機 ( LoomMenu ) のバナースロット ( 匿名クラス LoomMenu$3 ) が
 * BannerItem しか受け付けないのを拡張し、 鞘 ( saya ) も置けるようにする。
 *
 * LoomMenu#setupResultSlot / slotsChanged は BannerItem へキャストせず
 * BlockItem.setBlockEntityData で汎用的に "Patterns" を書き込むため、
 * 鞘が置けさえすれば、 旗とまったく同じ模様付けがそのまま機能する。
 */
@Mixin(targets = "net.minecraft.world.inventory.LoomMenu$3")
public class LoomBannerSlotMixin {

	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void tfp$allowSaya(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (SayaDesign.isSaya(stack)) {
			cir.setReturnValue(true);
		}
	}
}
