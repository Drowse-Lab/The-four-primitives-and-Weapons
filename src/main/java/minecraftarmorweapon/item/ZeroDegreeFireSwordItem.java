package minecraftarmorweapon.item;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import minecraftarmorweapon.procedures.FreezingFireEffectProcedure;
import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;

import java.util.List;

public class ZeroDegreeFireSwordItem extends SwordItem {
    
    public ZeroDegreeFireSwordItem() {
        super(new Tier() {
            @Override
            public int getUses() {
                return 1500;
            }
            
            @Override
            public float getSpeed() {
                return 8f;
            }
            
            @Override
            public float getAttackDamageBonus() {
                return 6f;
            }
            
            @Override
            public int getLevel() {
                return 3;
            }
            
            @Override
            public int getEnchantmentValue() {
                return 15;
            }
            
            @Override
            public Ingredient getRepairIngredient() {
                return Ingredient.of(new ItemStack(net.minecraft.world.item.Items.PACKED_ICE));
            }
        }, 3, -2.4f, new Item.Properties().tab(MinecraftArmorWeaponModTabs.TAB_WEAPON));
    }
    
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level.isClientSide()) {
            // 零度の炎エフェクトを適用（8秒間、レベル1）
            FreezingFireEffectProcedure.execute(VersionHelper.getLevel(target), target, attacker, 160, 0);
        }
        return super.hurtEnemy(stack, target, attacker);
    }
    
    @Override
    public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, world, list, flag);
        list.add(Component.literal("零度の炎").withStyle(ChatFormatting.AQUA));
        list.add(Component.literal("凍結と炎のダメージ").withStyle(ChatFormatting.GRAY));
    }
}