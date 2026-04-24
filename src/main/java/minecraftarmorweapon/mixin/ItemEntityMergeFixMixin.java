package minecraftarmorweapon.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@link ItemEntity#areMergable(ItemStack, ItemStack)} の正規化バイパス。
 *
 * 地面に同じアイテムがドロップしても片方に空 CompoundTag が付いていると merge されない
 * (ユーザー報告の "複数ドロップを拾うとスタックしない" 主犯)。
 * StackNormalize で normalize してから equals 判定し、一致なら true を返す。
 *
 * 最大スタックサイズ超過チェックだけは vanilla に残すため、HEAD インジェクトで
 * 事前判定 → 容量 OK のときだけバイパス。不一致ならフォールスルー。
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMergeFixMixin {

    @Inject(method = "areMergable", at = @At("HEAD"), cancellable = true)
    private static void maw_areMergable(ItemStack dst, ItemStack src,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (!StackNormalize.sameIgnoringEmptyTags(dst, src)) return;  // 従来処理へ
        // サイズ超過は vanilla と同じく不可
        if (dst.getCount() + src.getCount() > dst.getMaxStackSize()) {
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(true);
    }
}
