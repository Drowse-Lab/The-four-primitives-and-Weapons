package minecraftarmorweapon.mixin;

import minecraftarmorweapon.skill.WeaponStatsRegistry;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemEnchantMixin {

    /**
     * Item.getEnchantmentValue() を上書き: JSONで定義されたエンチャント適性を返す
     */
    @Inject(method = "getEnchantmentValue", at = @At("RETURN"), cancellable = true)
    private void onGetEnchantmentValue(CallbackInfoReturnable<Integer> cir) {
        if (!WeaponStatsRegistry.isLoaded()) return;
        Item self = (Item)(Object)this;
        ResourceLocation regName = ForgeRegistries.ITEMS.getKey(self);
        if (regName == null) return;
        WeaponStatsRegistry.WeaponStats stats = WeaponStatsRegistry.getStats(regName.toString());
        if (stats != null && stats.enchantability >= 0) {
            cir.setReturnValue(stats.enchantability);
        }
    }
}
