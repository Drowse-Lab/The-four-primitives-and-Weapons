package minecraftarmorweapon.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.ChatFormatting;

import java.util.List;

@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class ItemTooltipEventHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        CompoundTag nbt = stack.getTag();
        if (nbt != null && nbt.contains("Feyn")) {
            String tagValue = nbt.getString("Feyn");
            List<Component> tooltip = event.getToolTip();
            if ("sigiled".equals(tagValue)) {
                tooltip.add(Component.literal("妖").withStyle(ChatFormatting.DARK_PURPLE));
            } else if ("sealed".equals(tagValue)) {
                tooltip.add(Component.literal("封").withStyle(ChatFormatting.DARK_BLUE));
            }
        }
    }
}