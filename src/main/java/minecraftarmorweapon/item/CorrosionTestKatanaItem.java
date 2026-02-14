package minecraftarmorweapon.item;

import minecraftarmorweapon.damage.ElementType;
import minecraftarmorweapon.damage.ElementalDamageUtils;
import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level().Level;

import java.util.List;

/**
 * 侵食/闇属性テスト用カタナ
 * NBTタグで侵食属性を持つ
 */
public class CorrosionTestKatanaItem extends SwordItem {
    public CorrosionTestKatanaItem() {
        super(new Tier() {
            public int getUses() {
                return 1000;
            }

            public float getSpeed() {
                return 4f;
            }

            public float getAttackDamageBonus() {
                return 5f;
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
    public void onCraftedBy(ItemStack stack, Level world, net.minecraft.world.entity.player.Player player) {
        super.onCraftedBy(stack, world, player);
        ensureElement(stack);
    }

    private void ensureElement(ItemStack stack) {
        if (!ElementalDamageUtils.hasElement(stack)) {
            ElementalDamageUtils.setElement(stack, ElementType.CORROSION, 2);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        // インベントリ内のアイテムに属性を確実に設定
        ensureElement(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        // 属性表示はItemStackTooltipMixinで自動的に追加される
    }
}
