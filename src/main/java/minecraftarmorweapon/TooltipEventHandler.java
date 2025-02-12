package minecraftarmorweapon.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.item.Items;
import net.minecraft.ChatFormatting; // ここを追加！

// @Mod.EventBusSubscriber
// public class TooltipEventHandler {

// @SubscribeEvent
// public static void onItemTooltip(ItemTooltipEvent event) {
//     ItemStack stack = event.getItemStack();
//     if (stack.getItem() == Items.WRITTEN_BOOK && stack.hasTag()) {
//         CompoundTag tag = stack.getTag();
//         StringBuilder command = new StringBuilder("/give @s written_book{");

//         if (tag.contains("title")) {
//             command.append("title:\"").append(tag.getString("title")).append("\",");
//         }
//         if (tag.contains("author")) {
//             command.append("author:\"").append(tag.getString("author")).append("\",");
//         }
//         if (tag.contains("pages")) {
//             command.append("pages:").append(tag.getList("pages", 8).toString()).append(",");
//         }
//         if (tag.contains("resolved")) {
//             command.append("resolved:").append(tag.getByte("resolved")).append("b,");
//         }

//         command.append("}");
//         event.getToolTip().add(Component.literal(command.toString()));
//     }
// }

    
// }    


@Mod.EventBusSubscriber
public class TooltipEventHandler {
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // クリエイティブモードまたは Shift を押しているときのみ表示
        if (Minecraft.getInstance().player != null && 
            (Minecraft.getInstance().player.isCreative() || Screen.hasShiftDown())) {
                if (Minecraft.getInstance().player != null && Screen.hasShiftDown()) {
            // F3 + H（詳細ツールチップ表示）が有効な場合のみ
            if (stack.hasTag() && Minecraft.getInstance().options.advancedItemTooltips) {
                event.getToolTip().add(Component.literal(stack.getTag().toString()).withStyle(ChatFormatting.GRAY));
            }
        }}
    }
}
