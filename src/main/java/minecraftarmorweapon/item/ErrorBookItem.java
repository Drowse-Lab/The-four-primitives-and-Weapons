package minecraftarmorweapon.item;

import minecraftarmorweapon.damage.ElementalDamageUtils;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import javax.annotation.Nullable;

public class ErrorBookItem extends Item implements ICurioItem {

    private static final ResourceLocation NOISE_FONT =
            new ResourceLocation("minecraft_armor_weapon", "static_noise");

    public ErrorBookItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        int lv = ElementalDamageUtils.getElementLevel(stack);
        if (lv >= 1 && lv <= 10) {
            tooltip.add(Component.literal("error属性  Lv." + lv)
                    .withStyle(Style.EMPTY
                            .withColor(ChatFormatting.DARK_RED)
                            .withFont(NOISE_FONT)));
        } else if (lv != 0) {
            tooltip.add(buildErrorComponent());
        }
    }

    /** §4 赤 + ノイズフォントで "error" を表示するコンポーネント */
    public static Component buildErrorComponent() {
        return Component.literal("error")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.DARK_RED)
                        .withFont(new ResourceLocation("minecraft_armor_weapon", "static_noise")));
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
