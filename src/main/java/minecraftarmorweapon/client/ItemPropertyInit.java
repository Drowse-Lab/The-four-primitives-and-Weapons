package minecraftarmorweapon.client;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import minecraftarmorweapon.item.base.Base3DModelItem;
import minecraftarmorweapon.item.base.Base3DModelSwordItem;
import minecraftarmorweapon.MinecraftArmorWeaponMod;

@OnlyIn(Dist.CLIENT)
public class ItemPropertyInit {
    
    public static void init() {
        // Register texture variant property for all items
        ResourceLocation textureProperty = new ResourceLocation(MinecraftArmorWeaponMod.MODID, "texture_variant");
        
        // This method will be called for each registered item
        // You'll need to call this for each item that extends Base3DModelItem or Base3DModelSwordItem
    }
    
    public static void registerItemProperties(Item item) {
        if (item instanceof Base3DModelItem || item instanceof Base3DModelSwordItem) {
            ItemProperties.register(item, new ResourceLocation(MinecraftArmorWeaponMod.MODID, "texture_variant"),
                (stack, world, entity, seed) -> {
                    String variant = getTextureVariant(stack);
                    // Return a float value based on the variant
                    // This will be used in the item model JSON to select the correct texture
                    return getVariantIndex(variant);
                });
        }
    }
    
    private static String getTextureVariant(ItemStack stack) {
        if (stack.getItem() instanceof Base3DModelItem) {
            return ((Base3DModelItem) stack.getItem()).getTextureVariant(stack);
        } else if (stack.getItem() instanceof Base3DModelSwordItem) {
            return ((Base3DModelSwordItem) stack.getItem()).getTextureVariant(stack);
        }
        return "";
    }
    
    private static float getVariantIndex(String variant) {
        // Map texture variants to float values
        switch (variant) {
            case "fire": return 1.0f;
            case "ice": return 2.0f;
            case "thunder": return 3.0f;
            case "dark": return 4.0f;
            case "light": return 5.0f;
            case "blood": return 6.0f;
            case "magic": return 7.0f;
            case "gold": return 8.0f;
            case "emerald": return 9.0f;
            case "diamond": return 10.0f;
            default: return 0.0f;
        }
    }
}
