package minecraftarmorweapon.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class TooltipEventHandler {
    private static final int MAX_LINES_BEFORE_SCROLL = 5;
    private static int scrollIndex = 0;

    public TooltipEventHandler() {
        System.out.println("TooltipEventHandler Registered!");
        MinecraftForge.EVENT_BUS.register(new TooltipScrollHandler());
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // Shiftを押しているときだけ表示
        if (Minecraft.getInstance().player != null && Screen.hasShiftDown()) {
            // F3+Hが有効な場合
            if (Minecraft.getInstance().options.advancedItemTooltips) {
                // アイテムIDとNBTデータを含む完全なコマンド形式で表示（/giveコマンド用）
                String itemId = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                String fullCommand = itemId;
                
                // NBTデータがある場合は、コマンド形式に含める
                if (stack.hasTag()) {
                    CompoundTag tag = stack.getTag();
                    if (tag != null && !tag.isEmpty()) {
                        fullCommand = itemId + tag.toString();
                    }
                }
                
                event.getToolTip().add(Component.literal("Item ID: " + fullCommand).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55))));
                
                // NBTデータの詳細表示（見やすい形式）
                if (stack.hasTag()) {
                    CompoundTag tag = stack.getTag();
                    if (tag != null && !tag.isEmpty()) {
                        event.getToolTip().add(Component.literal("NBT Data (Formatted):").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA))));

                        List<String> formattedNBT = formatNBT(tag, 0);
                        int maxScroll = Math.max(0, formattedNBT.size() - MAX_LINES_BEFORE_SCROLL);

                        scrollIndex = Math.min(scrollIndex, maxScroll);
                        scrollIndex = Math.max(scrollIndex, 0);

                        if (formattedNBT.size() > MAX_LINES_BEFORE_SCROLL) {
                            event.getToolTip().add(
                                Component.literal("(Scroll: Mouse Wheel)").setStyle(
                                    Style.EMPTY.withColor(TextColor.fromRgb(0x555555)).withItalic(true)
                                )
                            );
                        }

                        for (int i = scrollIndex; i < Math.min(scrollIndex + MAX_LINES_BEFORE_SCROLL, formattedNBT.size()); i++) {
                            event.getToolTip().add(
                                Component.literal(formattedNBT.get(i)).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x555555)))
                            );
                        }
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

    public static class TooltipScrollHandler {
        public TooltipScrollHandler() {
            System.out.println("TooltipScrollHandler Registered!");
            MinecraftForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public static void onScroll(InputEvent.MouseScrollingEvent event) {
            System.out.println("MouseScrolled event detected! Scroll Delta: " + event.getScrollDelta());

            if (Screen.hasShiftDown()) {
                double scrollDelta = event.getScrollDelta();
                adjustScrollIndex(scrollDelta > 0 ? -1 : 1);
                System.out.println("Scroll Index Updated: " + scrollIndex);

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
