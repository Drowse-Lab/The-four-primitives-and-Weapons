package minecraftarmorweapon.events;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.nbt.CompoundTag;

@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class SayaLeftClickHandler {
    
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        // メインハンドに鞘を持っている場合
        if (isSaya(mainHand)) {
            performBattou(player, mainHand, InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND);
        }
        // オフハンドに鞘を持っている場合
        else if (isSaya(offHand)) {
            performBattou(player, offHand, InteractionHand.OFF_HAND, InteractionHand.MAIN_HAND);
        }
    }
    
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.isCanceled()) return;
        
        Player player = event.getPlayer();
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        // メインハンドに鞘を持っている場合
        if (isSaya(mainHand)) {
            performBattou(player, mainHand, InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND);
            event.setCanceled(true); // ブロック破壊をキャンセル
        }
        // オフハンドに鞘を持っている場合
        else if (isSaya(offHand)) {
            performBattou(player, offHand, InteractionHand.OFF_HAND, InteractionHand.MAIN_HAND);
            event.setCanceled(true); // ブロック破壊をキャンセル
        }
    }
    
    private static void performBattou(Player player, ItemStack sheathStack, InteractionHand sheathHand, InteractionHand otherHand) {
        CompoundTag tag = sheathStack.getOrCreateTag();
        
        // 鞘に刀が入っている場合、抜刀する
        if (tag.contains("StoredKatana")) {
            ItemStack otherHandItem = player.getItemInHand(otherHand);
            
            // 反対の手が空の場合のみ抜刀
            if (otherHandItem.isEmpty()) {
                // 保存された刀の情報から刀を生成
                ItemStack katanaStack = ItemStack.of(tag.getCompound("StoredKatana"));
                
                // 反対の手に刀を配置
                player.setItemInHand(otherHand, katanaStack);
                
                // 鞘から刀の情報を削除（空の鞘にする）
                tag.remove("StoredKatana");
                tag.putInt("CustomModelData", 0); // 空の鞘のモデル
                
                // タグをItemStackに適用
                sheathStack.setTag(tag);
                
                // 抜刀音を再生（オプション）
                player.playSound(net.minecraft.sounds.SoundEvents.ITEM_ARMOR_EQUIP_IRON, 1.0F, 1.0F);
            }
        }
    }
    
    private static boolean isSaya(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemName = stack.getItem().getClass().getSimpleName();
        return itemName.equals("SayaItem");
    }
}