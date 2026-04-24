package minecraftarmorweapon.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@link ItemStack#isSameItemSameTags(ItemStack, ItemStack)} の正規化バイパス。
 *
 * インベントリ GUI 上で同じ見た目のアイテムがスタックされない問題を解消する。
 * 詳細は {@link StackNormalize} を参照。
 *
 * 一致なら {@code true} を返してバニラをバイパス。不一致はフォールスルー
 * (バニラ判定に任せる) → 偽陽性 (別物なのに merge) を作らない。
 */
@Mixin(ItemStack.class)
public abstract class ItemStackStackFixMixin {

    @Inject(method = "isSameItemSameTags", at = @At("HEAD"), cancellable = true)
    private static void maw_stackNormalized(ItemStack a, ItemStack b,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (StackNormalize.sameIgnoringEmptyTags(a, b)) {
            cir.setReturnValue(true);
        }
    }
}
