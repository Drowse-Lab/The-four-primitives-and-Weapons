package the_four_primitives_and_weapons.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 不死の核 - 不死の特性を持つウィザースケルトンからのレアドロップ。
 * Unbreakable化レシピに必要。
 */
public class ImmortalCoreItem extends Item {
    public ImmortalCoreItem() {
        super(new Properties().rarity(Rarity.EPIC).stacksTo(1));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // エンチャントの光沢
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§5不死の特性を持つ者から得られる核"));
        tooltip.add(Component.literal("§7Unbreakable化に必要"));
    }
}
