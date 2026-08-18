package the_four_primitives_and_weapons.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import the_four_primitives_and_weapons.item.rarity.WeaponRarity;

/** ItemStack の独自 WeaponRarity をバニラのレアリティ参照にも反映する。 */
@Mixin(ItemStack.class)
public abstract class ItemStackRarityMixin {

    @Inject(method = "getRarity", at = @At("RETURN"), cancellable = true)
    private void tfpw$applyWeaponRarity(CallbackInfoReturnable<Rarity> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        WeaponRarity rarity = WeaponRarity.getFromStack(stack);
        if (rarity == null) return;

        cir.setReturnValue(switch (rarity) {
            case COMMON -> Rarity.COMMON;
            case UNCOMMON -> Rarity.UNCOMMON;
            case RARE -> Rarity.RARE;
            // バニラには Epic より上がない。独自ツールチップ枠では個別色を維持する。
            case EPIC, LEGENDARY, FORBIDDEN -> Rarity.EPIC;
        });
    }
}
