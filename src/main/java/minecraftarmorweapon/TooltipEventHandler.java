package minecraftarmorweapon.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.item.ItemStack;

import net.minecraft.network.chat.Style;

import net.minecraft.ChatFormatting;

@Mod.EventBusSubscriber
public class TooltipEventHandler {
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // Shift を押しているときだけ表示する
        if (Minecraft.getInstance().player != null && Screen.hasShiftDown()) {
            
            // F3 + H（詳細ツールチップ表示）が有効な場合のみ
            if (stack.hasTag() && Minecraft.getInstance().options.advancedItemTooltips) {
                event.getToolTip().add(Component.literal(stack.getTag().toString()).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}

// @Mod.EventBusSubscriber
// public class TooltipEventHandler {
//     @SubscribeEvent
//     public static void onItemTooltip(ItemTooltipEvent event) {
//         if (!(event.getEntity() instanceof Player)) return;
//         Player player = (Player) event.getEntity();
    
//         // クリエイティブ限定を削除、Shiftなしでも表示するように変更
//         if (!player.getLevel().isClientSide) return;
    
//         CompoundTag tag = event.getItemStack().getTag();
//         if (tag != null) {
//             event.getToolTip().add(Component.literal("§c{"));
//             int keyCount = tag.getAllKeys().size();
//             int currentIndex = 0;
    
//             for (String key : tag.getAllKeys()) {
//                 Tag value = tag.get(key);
    
//                 // "display" キーをスキップ
//                 if (key.equals("display")) continue;
    
//                 if (value instanceof ListTag) {
//                     ListTag list = (ListTag) value;
//                     StringBuilder listText = new StringBuilder("  §c\"" + key + "\": [");
//                     for (int i = 0; i < list.size(); i++) {
//                         listText.append(formatNBTValue(list.get(i)));
//                         if (i < list.size() - 1) {
//                             listText.append(", ");
//                         }
//                     }
//                     listText.append("]");
//                     event.getToolTip().add(Component.literal(listText.toString()));
//                 } else {
//                     event.getToolTip().add(formatNBT(key, value));
//                 }
    
//                 if (++currentIndex < keyCount) {
//                     event.getToolTip().add(Component.literal(",")); // 最後の要素の後にはカンマを追加しない
//                 }
//             }
//             event.getToolTip().add(Component.literal("§c}"));
//         }
//     }
//     private static Component formatNBT(String key, Tag value) {
//         String colorKey = "§c\"" + key + "\"§r: ";
//         String colorValue = formatNBTValue(value);
//         return Component.literal(colorKey + colorValue);
//     }
    
//     private static String formatNBTValue(Tag value) {
//         if (value instanceof CompoundTag) {
//             return "§c{ ... }";
//         } else if (value instanceof ListTag) {
//             return "§c[ ... ]";
//         } else {
//             String valueString = value.getAsString();
//             if (valueString.matches("-?\\d+(\\.\\d+)?")) {
//                 return "§9" + valueString + "§r"; // 数値を青色
//             } else {
//                 return "§e\"" + valueString + "\"§r"; // 文字列を黄色
//             }
//         }
//     }
    
// }    