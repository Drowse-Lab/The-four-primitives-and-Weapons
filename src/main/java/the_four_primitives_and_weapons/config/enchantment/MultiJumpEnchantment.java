package the_four_primitives_and_weapons.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * 「ジャンプ回数強化」エンチャント。
 *
 * 装備対象 (どちらかにあれば動作):
 *   - Leggings (LEGS) — 基本付与先 (空中ジャンプ追加回数 = レベル)
 *
 * 効果:
 *   - 空中で keyJump 新規押下 → 残追加ジャンプを 1 消費して上昇 (vanilla jump と同じ初速)
 *   - 着地で追加ジャンプ残量がリセット
 *   - フックショット装着時のみ: tickFloat の地上中に FloatFuel が tick あたり (1 + level) 回復
 */
public class MultiJumpEnchantment extends Enchantment {
    public MultiJumpEnchantment(EquipmentSlot... slots) {
        super(Rarity.RARE, EnchantmentCategory.ARMOR_LEGS, slots);
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return isLeggings(stack);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return isLeggings(stack);
    }

    @Override
    public boolean isTradeable() {
        return true;
    }

    @Override
    public boolean isDiscoverable() {
        return true;
    }

    private static boolean isLeggings(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem ai && ai.getEquipmentSlot() == EquipmentSlot.LEGS;
    }
}
