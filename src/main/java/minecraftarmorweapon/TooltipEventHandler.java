package minecraftarmorweapon.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class TooltipEventHandler {
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        System.out.println("[DEBUG] Tooltip event triggered!");
    
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            System.out.println("[DEBUG] Player detected: " + player.getName().getString());
            System.out.println("[DEBUG] isCreative: " + player.isCreative());
            System.out.println("[DEBUG] isShiftKeyDown: " + player.isShiftKeyDown());
    
            if (player.isCreative() && player.isShiftKeyDown()) {
                System.out.println("[DEBUG] Player is in creative mode and pressing Shift.");
    
                if (event.getItemStack().hasTag()) {
                    System.out.println("[DEBUG] Item has NBT data: " + event.getItemStack().getTag());
    
                    event.getToolTip().add(Component.literal("NBT Data: {"));
                    for (String key : event.getItemStack().getTag().getAllKeys()) {
                        Object value = event.getItemStack().getTag().get(key);
                        System.out.println("[DEBUG] Key: " + key + ", Value: " + value);
                        event.getToolTip().add(Component.literal("  \"" + key + "\": " + value));
                    }
                    event.getToolTip().add(Component.literal("}"));
                } else {
                    System.out.println("[DEBUG] Item has no NBT data.");
                }
            } else {
                System.out.println("[DEBUG] Player is not in creative mode or not pressing Shift.");
            }
        } else {
            System.out.println("[DEBUG] Entity is not a player.");
        }
    }
    
}
