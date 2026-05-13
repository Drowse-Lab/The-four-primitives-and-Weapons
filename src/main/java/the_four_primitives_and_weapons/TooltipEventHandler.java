package the_four_primitives_and_weapons.event;

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
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class TooltipEventHandler {

    public TooltipEventHandler() {    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // スキル選択画面を開いている時はツールチップに登録IDを表示しない
        // (武器スロットの下に「the_four_primitives_and_weapons_addons_sample:dagger」のような
        // ID文字列が大量に出るのを抑制するため)
        if (Minecraft.getInstance().screen instanceof the_four_primitives_and_weapons.client.gui.SkillSelectionScreen) {
            return;
        }

        // Shiftを押しているときだけ表示
        if (Minecraft.getInstance().player != null && Screen.hasShiftDown()) {
            // F3+Hが有効な場合
            if (Minecraft.getInstance().options.advancedItemTooltips) {
                // 空行を追加
                event.getToolTip().add(Component.literal(""));
                
                // セパレーターライン（上部）
                event.getToolTip().add(Component.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x5555FF))));
                
                // /give コマンドヘッダー
                event.getToolTip().add(Component.literal(" /give Command Format").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF)).withBold(true)));
                
                // 区切り線
                event.getToolTip().add(Component.literal(" ─────────────────────────").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x3333AA))));
                
                // アイテムIDとNBTデータを取得
                String itemId = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                String nbtData = "";
                
                // NBTデータがある場合は、コマンド形式に含める
                if (stack.hasTag()) {
                    CompoundTag tag = stack.getTag();
                    if (tag != null && !tag.isEmpty()) {
                        nbtData = tag.toString();
                    }
                }
                
                // アイテムIDの表示
                event.getToolTip().add(Component.literal(" ▸ Item ID:").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAFF))));
                event.getToolTip().add(Component.literal("   " + itemId).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF))));
                
                // NBTデータがある場合は改行して表示
                if (!nbtData.isEmpty()) {
                    event.getToolTip().add(Component.literal(""));
                    event.getToolTip().add(Component.literal(" ▸ NBT Data:").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAFF))));
                    
                    // NBTデータが長い場合は改行して表示
                    if (nbtData.length() > 50) {
                        // 長いNBTデータを適切な位置で改行
                        String[] nbtParts = nbtData.split("(?<=,)(?=\\w)");
                        for (String part : nbtParts) {
                            if (part.length() > 50) {
                                // さらに長い場合は強制的に改行
                                for (int i = 0; i < part.length(); i += 50) {
                                    int end = Math.min(i + 50, part.length());
                                    event.getToolTip().add(Component.literal("   " + part.substring(i, end)).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFAA00))));
                                }
                            } else {
                                event.getToolTip().add(Component.literal("   " + part).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFAA00))));
                            }
                        }
                    } else {
                        event.getToolTip().add(Component.literal("   " + nbtData).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFAA00))));
                    }
                }
                
                // アイテム数量の表示（スタックサイズが1以外の場合）
                if (stack.getCount() > 1) {
                    event.getToolTip().add(Component.literal(""));
                    event.getToolTip().add(Component.literal(" ▸ Stack Size:").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAFF))));
                    event.getToolTip().add(Component.literal("   " + stack.getCount() + " items").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAFFAA))));
                }
                
                // コマンド全体の表示（コピペ用）
                event.getToolTip().add(Component.literal(""));
                event.getToolTip().add(Component.literal(" ▸ Full Command (Copy):").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAFF))));
                String fullCommand = itemId + (!nbtData.isEmpty() ? nbtData : "");
                event.getToolTip().add(Component.literal("   /give @p " + fullCommand + (stack.getCount() > 1 ? " " + stack.getCount() : ""))
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55)).withItalic(true)));
                
                // セパレーターライン（下部）
                event.getToolTip().add(Component.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x5555FF))));
            }
        }
    }

}
