package the_four_primitives_and_weapons.item;

import the_four_primitives_and_weapons.ElementalTooltipEvent;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import javax.annotation.Nullable;

public class SoulBookItem extends Item implements ICurioItem {
    private static final int SOUL_COLOR = 0x55F7FF;

    public SoulBookItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int lv = ElementalDamageUtils.getElementLevel(stack);
        if (lv >= 1 && lv <= 10) {
            tooltip.add(Component.translatable("tooltip.the_four_primitives_and_weapons.element.soul")
                    .append(Component.literal(" " + ElementalTooltipEvent.toRoman(lv)))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(SOUL_COLOR))));
        } else if (lv != 0) {
            tooltip.add(ErasureBookItem.buildErasureComponent());
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(SOUL_COLOR)));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }
}
