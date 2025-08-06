package minecraftarmorweapon.item.base;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import minecraftarmorweapon.item.renderer.DynamicTextureItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import java.util.function.Consumer;

public abstract class Base3DModelItem extends Item {
    
    public Base3DModelItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final DynamicTextureItemRenderer renderer = new DynamicTextureItemRenderer();
            
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }
    
    /**
     * Get texture variant for this item
     * Override this method to return different texture variants
     */
    public String getTextureVariant(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getString("TextureVariant");
    }
    
    /**
     * Set texture variant for this item
     */
    public void setTextureVariant(ItemStack stack, String variant) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("TextureVariant", variant);
    }
}