package minecraftarmorweapon.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@link Inventory#canMergeItems(ItemStack, ItemStack)} (private) の正規化バイパス。
 *
 * 拾ったアイテムがインベントリ内の既存スタックに合流できるかを判定する内部メソッド。
 * バニラはここでも {@code hasTag() ^ hasTag()} で弾いているので、空タグが付いた
 * ピックアップが同種既存スタックと合流できず別スロットを占有する原因になる。
 *
 * 正規化一致なら且つサイズ上限内なら true 返却でバイパス、不一致はバニラへ。
 *
 * NOTE: canMergeItems は private だが Mixin は AccessTransformer 経由 / @Inject は
 * private 相当でも宣言クラス内なら参照可能。method 名解決は compile 時の bytecode
 * ではなく refmap / mappings によって SRG 名に変換される。
 */
@Mixin(Inventory.class)
public abstract class InventoryMergeFixMixin {

    @Inject(method = "canMergeItems", at = @At("HEAD"), cancellable = true)
    private void maw_canMergeItems(ItemStack a, ItemStack b,
                                   CallbackInfoReturnable<Boolean> cir) {
        if (!StackNormalize.sameIgnoringEmptyTags(a, b)) return;  // vanilla へ
        if (a.getCount() > a.getMaxStackSize()) {
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(true);
    }
}
