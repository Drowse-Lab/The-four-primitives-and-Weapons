package minecraftarmorweapon.events;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;

@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class SayaLeftClickHandler {
    
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
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
        
        Player player = event.getEntity();
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
    
    // Mobへの攻撃時でも抜刀できるようにする（優先度を高くして先に処理）
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        // メインハンドに鞘を持っている場合
        if (isSaya(mainHand)) {
            // 抜刀処理を実行
            if (performBattou(player, mainHand, InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND)) {
                // 抜刀に成功したら、元の攻撃をキャンセル
                event.setCanceled(true);
            }
        }
        // オフハンドに鞘を持っている場合
        else if (isSaya(offHand)) {
            if (performBattou(player, offHand, InteractionHand.OFF_HAND, InteractionHand.MAIN_HAND)) {
                event.setCanceled(true);
            }
        }
    }
    
    private static boolean performBattou(Player player, ItemStack sheathStack, InteractionHand sheathHand, InteractionHand otherHand) {
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
                
                // 鞘のアイテムスタックを強制的に更新（見た目の更新を確実にする）
                player.setItemInHand(sheathHand, sheathStack);
                
                // 抜刀音を再生
                player.playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 1.0F);
                
                return true; // 抜刀成功
            }
        }
        return false; // 抜刀失敗（刀が入っていない、または反対の手が空でない）
    }
    
    private static boolean isSaya(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemName = stack.getItem().getClass().getSimpleName();
        return itemName.equals("SayaItem");
    }
}