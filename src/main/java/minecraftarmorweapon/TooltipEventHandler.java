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
    private static final boolean DEBUG_MODE = true; 

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (DEBUG_MODE) System.out.println("[DEBUG] Tooltip event triggered!");

        if (event.getEntity() == null || !(event.getEntity() instanceof Player)) {
            if (DEBUG_MODE) System.out.println("[DEBUG] Entity is not a player.");
            return;
        }

        Player player = (Player) event.getEntity();
        if (DEBUG_MODE) System.out.println("[DEBUG] Player detected: " + player.getName().getString());

        if (!player.getLevel().isClientSide || !player.isCreative()) {
            if (DEBUG_MODE) System.out.println("[DEBUG] Player is not in creative mode.");
            return;
        }

        if (!Screen.hasShiftDown()) {
            if (DEBUG_MODE) System.out.println("[DEBUG] Shift key not pressed.");
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.advancedItemTooltips) {
            if (DEBUG_MODE) System.out.println("[DEBUG] Advanced tooltips (F3+H) are disabled.");
            return;
        }

        if (DEBUG_MODE) System.out.println("[DEBUG] Advanced tooltips enabled. Displaying NBT data.");

        CompoundTag tag = event.getItemStack().getTag();
        if (tag != null) {
            if (DEBUG_MODE) System.out.println("[DEBUG] Item has NBT data: " + tag);

            StringBuilder nbtString = new StringBuilder("§7" + event.getItemStack().getItem().toString() + "{");

            for (String key : tag.getAllKeys()) {
                Tag value = tag.get(key);
                nbtString.append(formatNBT(key, value)).append(",");
            }

            if (nbtString.charAt(nbtString.length() - 1) == ',') {
                nbtString.setLength(nbtString.length() - 1);
            }

            nbtString.append("}");
            event.getToolTip().add(Component.literal(nbtString.toString()));
        } else {
            if (DEBUG_MODE) System.out.println("[DEBUG] Item has no NBT data.");
        }
    }

    private static String formatNBT(String key, Tag value) {
        StringBuilder formatted = new StringBuilder("§e" + key + "§r:");
        if (value instanceof CompoundTag) {
            formatted.append("{");
            CompoundTag compound = (CompoundTag) value;
            for (String subKey : compound.getAllKeys()) {
                formatted.append(formatNBT(subKey, compound.get(subKey))).append(",");
            }
            if (formatted.charAt(formatted.length() - 1) == ',') {
                formatted.setLength(formatted.length() - 1);
            }
            formatted.append("}");
        } else if (value instanceof ListTag) {
            formatted.append("[");
            ListTag list = (ListTag) value;
            for (Tag item : list) {
                formatted.append(formatNBT("", item)).append(",");
            }
            if (formatted.charAt(formatted.length() - 1) == ',') {
                formatted.setLength(formatted.length() - 1);
            }
            formatted.append("]");
        } else {
            String valueString = value.getAsString();
            if (valueString.matches("-?\\d+(\\.\\d+)?")) {
                formatted.append("§9").append(valueString).append("§r");
            } else {
                formatted.append("§e").append(valueString).append("§r");
            }
        }
        return formatted.toString();
    }
}
