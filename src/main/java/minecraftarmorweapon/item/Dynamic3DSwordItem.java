package minecraftarmorweapon.item;

import net.minecraft.world.level().Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.NonNullList;

import minecraftarmorweapon.item.base.Base3DModelSwordItem;
import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;

import java.util.List;

public class Dynamic3DSwordItem extends Base3DModelSwordItem {
    
    public Dynamic3DSwordItem() {
        super(new Tier() {
            public int getUses() {
                return 1000;
            }

            public float getSpeed() {
                return 4f;
            }

            public float getAttackDamageBonus() {
                return 6f;
            }

            public int getLevel() {
                return 2;
            }

            public int getEnchantmentValue() {
                return 15;
            }

            public Ingredient getRepairIngredient() {
                return Ingredient.of();
            }
        }, 3, -2.4f, new Item.Properties().tab(MinecraftArmorWeaponModTabs.TAB_WEAPON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
        ItemStack itemstack = entity.getItemInHand(hand);
        
        // Example: Cycle through texture variants when right-clicked
        if (!world.isClientSide()) {
            String currentVariant = getTextureVariant(itemstack);
            String nextVariant = getNextVariant(currentVariant);
            setTextureVariant(itemstack, nextVariant);
            entity.displayClientMessage(Component.literal("Texture variant changed to: " + nextVariant), true);
        }
        
        return InteractionResultHolder.sidedSuccess(itemstack, world.isClientSide());
    }
    
    private String getNextVariant(String current) {
        String[] variants = {"", "fire", "ice", "thunder", "dark", "light", "blood", "magic", "gold", "emerald", "diamond"};
        
        for (int i = 0; i < variants.length; i++) {
            if (variants[i].equals(current)) {
                return variants[(i + 1) % variants.length];
            }
        }
        return "fire";
    }

    @Override
    public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, world, list, flag);
        String variant = getTextureVariant(itemstack);
        if (!variant.isEmpty()) {
            list.add(Component.literal("§7Variant: §e" + variant));
        }
        list.add(Component.literal("§8Right-click to change texture"));
    }
    
    @Override
    public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
        if (this.allowedIn(tab)) {
            // Add default variant
            items.add(new ItemStack(this));
            
            // Add all texture variants
            String[] variants = {"fire", "ice", "thunder", "dark", "light", "blood", "magic", "gold", "emerald", "diamond"};
            for (String variant : variants) {
                ItemStack stack = new ItemStack(this);
                setTextureVariant(stack, variant);
                items.add(stack);
            }
        }
    }
}