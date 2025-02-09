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
    
        if (!player.getLevel().isClientSide || !player.isCreative() || !Screen.hasShiftDown()) return;
        if (!Minecraft.getInstance().options.advancedItemTooltips) return;
    
        CompoundTag tag = event.getItemStack().getTag();
        if (tag != null) {
            event.getToolTip().add(Component.literal("§7{"));
            int index = 0;
    
            for (String key : tag.getAllKeys()) {
                Tag value = tag.get(key);
    
                if (key.equals("display")) continue;
    
                // `StoredEnchantments` を `/give` のような形式で表示
                if (key.equals("StoredEnchantments") && value instanceof ListTag) {
                    event.getToolTip().add(Component.literal("  §7StoredEnchantments:["));
    
                    ListTag enchantments = (ListTag) value;
                    for (int i = 0; i < enchantments.size(); i++) {
                        CompoundTag enchant = enchantments.getCompound(i);
                        String enchantName = enchant.getString("id");
                        int level = enchant.getInt("lvl");
    
                        event.getToolTip().add(Component.literal(
                            "    §7{id:\"" + enchantName + "\",lvl:" + level + "}"
                        ));
    
                        if (i < enchantments.size() - 1) {
                            event.getToolTip().add(Component.literal(","));
                        }
                    }
                    event.getToolTip().add(Component.literal("  §7]"));
                }
                // `ChargedProjectiles` を `/give` のような形式で表示
                else if (key.equals("ChargedProjectiles") && value instanceof ListTag) {
                    event.getToolTip().add(Component.literal("  §7ChargedProjectiles:["));
    
                    ListTag projectiles = (ListTag) value;
                    for (int i = 0; i < projectiles.size(); i++) {
                        CompoundTag projectile = projectiles.getCompound(i);
                        String projectileId = projectile.getString("id");
                        int count = projectile.getInt("Count");
    
                        event.getToolTip().add(Component.literal(
                            "    §7{id:\"" + projectileId + "\",Count:" + count + "b}"
                        ));
    
                        if (i < projectiles.size() - 1) {
                            event.getToolTip().add(Component.literal(","));
                        }
                    }
                    event.getToolTip().add(Component.literal("  §7]"));
                }
                // その他の NBT タグを `/give` のような形式で表示
                else {
                    event.getToolTip().add(formatNBT(key, value));
                }
    
                if (++index < tag.getAllKeys().size()) {
                    event.getToolTip().add(Component.literal(","));
                }
            }
            event.getToolTip().add(Component.literal("§7}"));
        }
    }
    
    private static Component formatNBT(String key, Tag value) {
        String formattedKey = "§7" + key + ":";
        String formattedValue;
    
        if (value instanceof CompoundTag) {
            formattedValue = "{...}";
        } else if (value instanceof ListTag) {
            formattedValue = "[...]";
        } else {
            String valueString = value.getAsString();
            if (valueString.matches("-?\\d+(\\.\\d+)?")) {
                formattedValue = valueString; // 数値はそのまま
            } else {
                formattedValue = "\"" + valueString + "\""; // 文字列は `"` で囲む
            }
        }
    
        return Component.literal(formattedKey + formattedValue);
    }

    private static String formatNBTValue(Tag value) {
        if (value instanceof CompoundTag) {
            return "§c{ ... }";
        } else if (value instanceof ListTag) {
            return "§c[ ... ]";
        } else {
            String valueString = value.getAsString();
            if (valueString.matches("-?\\d+(\\.\\d+)?")) {
                return "§9" + valueString + "§r"; // 数値を青
            } else {
                return "§e\"" + valueString + "\"§r"; // 文字列を黄色
            }
        }
    }
}
