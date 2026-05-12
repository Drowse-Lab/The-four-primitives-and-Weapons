package the_four_primitives_and_weapons.mixin;

import the_four_primitives_and_weapons.skill.WeaponStatsRegistry;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class WeaponStatsMixin {

    /**
     * getMaxDamage() を上書き: JSONで定義された耐久値を返す
     */
    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void onGetMaxDamage(CallbackInfoReturnable<Integer> cir) {
        if (!WeaponStatsRegistry.isLoaded()) return;
        ItemStack self = (ItemStack)(Object)this;
        int durability = WeaponStatsRegistry.getDurability(self);
        if (durability >= 0) {
            cir.setReturnValue(durability);
        }
    }

    /**
     * getEnchantmentValue() の戻り値を上書き: JSONで定義されたエンチャント適性を返す
     * (Item.getEnchantmentValue が ItemStack 経由で呼ばれるパスをカバー)
     */
    @Inject(method = "isEnchantable", at = @At("RETURN"), cancellable = true)
    private void onIsEnchantable(CallbackInfoReturnable<Boolean> cir) {
        if (!WeaponStatsRegistry.isLoaded()) return;
        ItemStack self = (ItemStack)(Object)this;
        int enchant = WeaponStatsRegistry.getEnchantability(self);
        if (enchant > 0) {
            cir.setReturnValue(true);
        }
    }
}
