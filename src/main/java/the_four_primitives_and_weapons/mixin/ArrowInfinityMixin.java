package the_four_primitives_and_weapons.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ArrowItem.isInfinite() の {@code this.getClass() == ArrowItem.class} 制限を無効化する。
 *
 * バニラでは Infinity 付き弓でも {@link net.minecraft.world.item.TippedArrowItem} や
 * {@link net.minecraft.world.item.SpectralArrowItem} は消費されてしまう。
 * 本ミキシンは bow に Infinity が付いていれば全 ArrowItem サブクラスで
 * {@code true} を返すよう head インジェクトし、ポーション矢等も無限化する。
 */
@Mixin(ArrowItem.class)
public abstract class ArrowInfinityMixin {

    // Forge adds this method after Mojang's mappings are produced, so it has no
    // SRG mapping and keeps the same name in production.
    @Inject(method = "isInfinite", at = @At("HEAD"), cancellable = true, remap = false)
    private void maw_allowAnyArrowInfinite(ItemStack stack, ItemStack bow, Player player,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (bow.getEnchantmentLevel(Enchantments.INFINITY_ARROWS) > 0) {
            cir.setReturnValue(true);
        }
    }
}
