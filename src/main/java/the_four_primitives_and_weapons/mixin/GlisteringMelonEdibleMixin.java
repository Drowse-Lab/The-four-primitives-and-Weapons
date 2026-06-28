package the_four_primitives_and_weapons.mixin;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * バニラの煌めくスイカ ( GLISTERING_MELON_SLICE ) を食料として食べられるようにする。
 * isEdible / getFoodProperties には依存せず、 use / getUseDuration / getUseAnimation /
 * finishUsingItem を直接フックして「食事モーション付き・通常スイカと同じ食事時間 (32t)・
 * クリエイティブ非消費」 を実現する。
 */
@Mixin(Item.class)
public class GlisteringMelonEdibleMixin {

	private static final int TFP_NUTRITION = 4;
	private static final float TFP_SATURATION = 0.6f;

	private boolean tfp_isGlisteringMelon() {
		return (Object) this == Items.GLISTERING_MELON_SLICE;
	}

	/** 右クリックで食事開始（満腹なら不可）。 */
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void tfp_use(Level level, Player player, InteractionHand hand,
	                     CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
		if (!tfp_isGlisteringMelon()) return;
		ItemStack stack = player.getItemInHand(hand);
		if (player.canEat(false)) {
			player.startUsingItem(hand);
			cir.setReturnValue(InteractionResultHolder.consume(stack));
		} else {
			cir.setReturnValue(InteractionResultHolder.fail(stack));
		}
	}

	/** 食事時間 = 普通のスイカと同じ 32 tick。 */
	@Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
	private void tfp_useDuration(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
		if (tfp_isGlisteringMelon()) cir.setReturnValue(32);
	}

	/** 食事モーション。 */
	@Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
	private void tfp_useAnim(ItemStack stack, CallbackInfoReturnable<UseAnim> cir) {
		if (tfp_isGlisteringMelon()) cir.setReturnValue(UseAnim.EAT);
	}

	/** 食べ終わり: 満腹度回復 + 消費（クリエは非消費）。 */
	@Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
	private void tfp_finish(ItemStack stack, Level level, LivingEntity entity,
	                        CallbackInfoReturnable<ItemStack> cir) {
		if (!tfp_isGlisteringMelon()) return;
		if (entity instanceof Player p) {
			p.getFoodData().eat(TFP_NUTRITION, TFP_SATURATION);
			level.playSound(null, p.getX(), p.getY(), p.getZ(),
					SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8f, 1.0f);
			if (!p.getAbilities().instabuild) stack.shrink(1);
		}
		cir.setReturnValue(stack);
	}
}
