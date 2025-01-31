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
        // プレイヤー以外は処理しない
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        // クリエイティブモードかつ Shift が押されている & F3+H の詳細ツールチップが有効
        if (!player.getLevel().isClientSide || !player.isCreative() || !Screen.hasShiftDown()) return;
        if (!Minecraft.getInstance().options.advancedItemTooltips) return;

        CompoundTag tag = event.getItemStack().getTag();
        if (tag != null) {
            event.getToolTip().add(Component.literal("§c{"));
            int index = 0;
            for (String key : tag.getAllKeys()) {
                Tag value = tag.get(key);
                event.getToolTip().add(Component.literal("  " + formatNBT(key, value) + (index++ < tag.getAllKeys().size() - 1 ? "," : "")));
            }
            event.getToolTip().add(Component.literal("§c}"));
        }
    }

    private static String formatNBT(String key, Tag value) {
        StringBuilder formatted = new StringBuilder("§c\"" + key + "\"§r: ");
        if (value instanceof CompoundTag) {
            formatted.append("§c{");
            CompoundTag compound = (CompoundTag) value;
            int index = 0;
            for (String subKey : compound.getAllKeys()) {
                formatted.append("\n    ").append(formatNBT(subKey, compound.get(subKey))).append(index++ < compound.getAllKeys().size() - 1 ? "," : "");
            }
            formatted.append("\n  §c}");
        } else if (value instanceof ListTag) {
            formatted.append("§c[");
            ListTag list = (ListTag) value;
            int index = 0;
            for (Tag item : list) {
                formatted.append("\n    ").append(formatNBT("", item)).append(index++ < list.size() - 1 ? "," : "");
            }
            formatted.append("\n  §c]");
        } else {
            String valueString = value.getAsString();
            if (valueString.matches("-?\\d+(\\.\\d+)?")) {
                formatted.append("§9").append(valueString).append("§r");
            } else {
                formatted.append("§e\"").append(valueString).append("\"§r");
            }
        }
        return formatted.toString();
    }
}
