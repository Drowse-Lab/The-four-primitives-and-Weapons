package the_four_primitives_and_weapons.client;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import the_four_primitives_and_weapons.item.base.Base3DModelItem;
import the_four_primitives_and_weapons.item.base.Base3DModelSwordItem;
import the_four_primitives_and_weapons.item.SayaItem;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

@OnlyIn(Dist.CLIENT)
public class ItemPropertyInit {

    public static void init() {
        // Register texture variant property for all items
        ResourceLocation textureProperty = new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "texture_variant");

        // This method will be called for each registered item
        // You'll need to call this for each item that extends Base3DModelItem or Base3DModelSwordItem
    }

    public static void registerItemProperties(Item item) {
        if (item instanceof Base3DModelItem || item instanceof Base3DModelSwordItem) {
            ItemProperties.register(item, new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "texture_variant"),
                (stack, world, entity, seed) -> {
                    String variant = getTextureVariant(stack);
                    // Return a float value based on the variant
                    // This will be used in the item model JSON to select the correct texture
                    return getVariantIndex(variant);
                });
        }

        // 鞘アイテム用: feyn と saya_nbt プロパティ
        if (item instanceof SayaItem) {
            // feyn: Feyn="sigiled"なら1.0、それ以外0.0
            ItemProperties.register(item, new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "feyn"),
                (stack, world, entity, seed) -> {
                    CompoundTag tag = stack.getTag();
                    if (tag != null && "sigiled".equals(tag.getString("Feyn"))) {
                        return 1.0f;
                    }
                    return 0.0f;
                });

            // saya_nbt: SayaNBT=1なら1.0、それ以外0.0
            ItemProperties.register(item, new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "saya_nbt"),
                (stack, world, entity, seed) -> {
                    CompoundTag tag = stack.getTag();
                    if (tag != null && tag.getInt("SayaNBT") >= 1) {
                        return 1.0f;
                    }
                    return 0.0f;
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
