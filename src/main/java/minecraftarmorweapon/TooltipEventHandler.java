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

@Mod.EventBusSubscriber
public class TooltipEventHandler {
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        // クリエイティブ + Shift 押してる + 詳細表示がON じゃないと何もしない
        if (!player.getLevel().isClientSide || !player.isCreative() || !Screen.hasShiftDown()) return;
        if (!Minecraft.getInstance().options.advancedItemTooltips) return;

        CompoundTag tag = event.getItemStack().getTag();
        if (tag != null) {
            event.getToolTip().add(Component.literal("§c{"));
            int index = 0;

            for (String key : tag.getAllKeys()) {
                Tag value = tag.get(key);

                // display がない場合はスキップ
                if (key.equals("display")) continue;

                if (key.equals("Enchantments") && value instanceof ListTag) {
                    event.getToolTip().add(Component.literal("  §c\"Enchantments\": ["));

                    ListTag enchantments = (ListTag) value;
                    for (int i = 0; i < enchantments.size(); i++) {
                        CompoundTag enchant = enchantments.getCompound(i);
                        String enchantName = enchant.getString("id");
                        int level = enchant.getInt("lvl");

                        event.getToolTip().add(Component.literal(
                            "    §c{ §c\"id\": §e\"" + enchantName + "\"§r, " +
                            "§c\"lvl\": §9" + level + "§c }"
                        ));

                        if (i < enchantments.size() - 1) {
                            event.getToolTip().add(Component.literal(","));
                        }
                    }
                    event.getToolTip().add(Component.literal("  §c]"));
                } else {
                    event.getToolTip().add(formatNBT(key, value));
                }

                if (++index < tag.getAllKeys().size()) {
                    event.getToolTip().add(Component.literal(","));
                }
            }
            event.getToolTip().add(Component.literal("§c}"));
        }
    }

    private static Component formatNBT(String key, Tag value) {
        String colorKey = "§c\"" + key + "\"§r: ";
        String colorValue;

        if (value instanceof CompoundTag) {
            colorValue = "§c{ ... }";
        } else if (value instanceof ListTag) {
            colorValue = "§c[ ... ]";
        } else {
            String valueString = value.getAsString();
            if (valueString.matches("-?\\d+(\\.\\d+)?")) {
                colorValue = "§9" + valueString + "§r"; // 数値を青
            } else {
                colorValue = "§e\"" + valueString + "\"§r"; // 文字列を黄色
            }
        }

        return Component.literal(colorKey + colorValue);
    }
}
