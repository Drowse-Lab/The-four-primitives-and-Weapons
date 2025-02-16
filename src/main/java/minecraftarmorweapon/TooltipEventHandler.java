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
// import net.minecraft.network.chat.ChatFormatting;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.InputEvent;

import java.util.List;
import java.util.ArrayList;

@Mod.EventBusSubscriber
public class TooltipEventHandler {
    private static final int MAX_LINES_BEFORE_SCROLL = 5; // 一度に表示する最大行数
    private static int scrollIndex = 0; // スクロール位置

    public TooltipEventHandler() {
        System.out.println("TooltipEventHandler Registered!");
        MinecraftForge.EVENT_BUS.register(new TooltipScrollHandler());
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // Shiftを押しているときだけ表示
        if (Minecraft.getInstance().player != null && Screen.hasShiftDown()) {
            // F3 + H（詳細ツールチップ表示）が有効な場合のみ
            if (stack.hasTag() && Minecraft.getInstance().options.advancedItemTooltips) {
                CompoundTag tag = stack.getTag();
                if (tag != null) {
                    event.getToolTip().add(Component.literal("NBT Data:").withStyle(ChatFormatting.GRAY));

                    // NBTデータを行単位で取得
                    List<String> formattedNBT = formatNBT(tag, 0);

                    // スクロール可能か判定
                    int maxScroll = Math.max(0, formattedNBT.size() - MAX_LINES_BEFORE_SCROLL);

                    // スクロール位置を制限
                    scrollIndex = Math.min(scrollIndex, maxScroll);
                    scrollIndex = Math.max(scrollIndex, 0);

                    // スクロール時のインジケーター
                    if (formattedNBT.size() > MAX_LINES_BEFORE_SCROLL) {
                        event.getToolTip().add(Component.literal("(Scroll: Mouse Wheel)").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                    }

                    // 現在のスクロール位置に応じて表示
                    for (int i = scrollIndex; i < Math.min(scrollIndex + MAX_LINES_BEFORE_SCROLL, formattedNBT.size()); i++) {
                        event.getToolTip().add(Component.literal(formattedNBT.get(i)).withStyle(ChatFormatting.DARK_GRAY));
                    }
                }
            }
        }
    }

    private static List<String> formatNBT(Tag tag, int indentLevel) {
        List<String> formatted = new ArrayList<>();
        String indent = "  ".repeat(indentLevel);

        if (tag instanceof CompoundTag) {
            CompoundTag compound = (CompoundTag) tag;
            formatted.add(indent + "{");

            for (String key : compound.getAllKeys()) {
                Tag value = compound.get(key);
                formatted.add(indent + "  " + "\"" + key + "\": " + formatNBTValue(value, indentLevel + 1));
            }

            formatted.add(indent + "}");
        } else if (tag instanceof ListTag) {
            ListTag list = (ListTag) tag;
            formatted.add(indent + "[");

            for (Tag value : list) {
                formatted.add(indent + "  " + formatNBTValue(value, indentLevel + 1));
            }

            formatted.add(indent + "]");
        } else {
            formatted.add(indent + tag.getAsString());
        }

        return formatted;
    }

    private static String formatNBTValue(Tag tag, int indentLevel) {
        if (tag instanceof CompoundTag || tag instanceof ListTag) {
            return String.join("\n", formatNBT(tag, indentLevel));
        } else {
            return tag.getAsString();
        }
    }

    // スクロールイベントを処理するクラス
    public static class TooltipScrollHandler {
        public TooltipScrollHandler() {
            System.out.println("TooltipScrollHandler Registered!");
            MinecraftForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public static void onScroll(InputEvent.MouseScrollingEvent event) {
            System.out.println("MouseScrolled event detected! Scroll Delta: " + event.getScrollDelta()); // 確認ログ

            if (Screen.hasShiftDown()) {
                double scrollDelta = event.getScrollDelta();
                adjustScrollIndex(scrollDelta > 0 ? -1 : 1);
                System.out.println("Scroll Index Updated: " + scrollIndex); // 確認ログ

                event.setCanceled(true);
            }
        }
    }

    public static void adjustScrollIndex(int delta) {
        scrollIndex += delta;
        if (scrollIndex < 0) scrollIndex = 0;
    }

    public static int getScrollIndex() {
        return scrollIndex;
    }
}
