package the_four_primitives_and_weapons.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;

import javax.annotation.Nullable;
import java.util.List;

/**
 * レアリティチャーム - Curiosのcharmスロットに装備可能。
 * 装備またはインベントリ所持で敵のレア特性（不死など）の出現率が上がる。
 */
public class RarityCharmItem extends Item implements ICurioItem {
    public RarityCharmItem() {
        super(new Properties().rarity(Rarity.RARE).stacksTo(1));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§d所持しているとレア特性の出現率UP"));
        tooltip.add(Component.literal("§7不死の特性を持つ敵が出やすくなる"));
    }

    @Override
    public boolean canEquip(SlotContext ctx, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canEquipFromUse(SlotContext ctx, ItemStack stack) {
        return true;
    }
}
