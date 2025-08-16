package minecraftarmorweapon.ai;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import minecraftarmorweapon.command.CustomDifficultyCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 武器切り替えシステム
 * AIゴールを使わずにLivingUpdateEventで処理
 */
// 無効化（クラッシュ防止）
//@Mod.EventBusSubscriber
public class WeaponSwitchHandler {
    
    // エンティティごとの武器切り替えクールダウン
    private static final Map<UUID, Integer> switchCooldowns = new HashMap<>();
    private static final Map<UUID, WeaponSet> entityWeapons = new HashMap<>();
    
    private static class WeaponSet {
        ItemStack melee;
        ItemStack ranged;
        
        WeaponSet(ItemStack melee, ItemStack ranged) {
            this.melee = melee;
            this.ranged = ranged;
        }
    }
    
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        // True Crafterモードが無効なら何もしない
        if (!CustomDifficultyCommand.isTrueCrafterEnabled()) {
            return;
        }
        
        // nullチェック
        if (event == null || event.getEntity() == null) {
            return;
        }
        
        LivingEntity entity = event.getEntity();
        
        // クライアントサイドでは処理しない
        if (entity.level.isClientSide) {
            return;
        }
        
        // スケルトンかゾンビのみ処理
        if (!(entity instanceof Skeleton || entity instanceof Zombie)) {
            return;
        }
        
        // ターゲットがいない場合は処理しない
        LivingEntity target = null;
        if (entity instanceof Skeleton skeleton) {
            target = skeleton.getTarget();
        } else if (entity instanceof Zombie zombie) {
            target = zombie.getTarget();
        }
        
        if (target == null || !target.isAlive()) {
            return;
        }
        
        UUID entityId = entity.getUUID();
        
        // 初回の場合、武器セットを初期化
        if (!entityWeapons.containsKey(entityId)) {
            initializeWeaponSet(entity);
        }
        
        // クールダウン処理
        int cooldown = switchCooldowns.getOrDefault(entityId, 0);
        if (cooldown > 0) {
            switchCooldowns.put(entityId, cooldown - 1);
            return;
        }
        
        // 武器切り替えロジック
        WeaponSet weapons = entityWeapons.get(entityId);
        if (weapons == null) {
            return;
        }
        
        double distance = entity.distanceTo(target);
        ItemStack currentWeapon = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        
        try {
            // 距離に応じて武器を切り替え
            if (distance > 6.0) {
                // 遠距離なら弓
                if (!ItemStack.isSame(currentWeapon, weapons.ranged)) {
                    entity.setItemSlot(EquipmentSlot.MAINHAND, weapons.ranged.copy());
                    switchCooldowns.put(entityId, 40); // 2秒のクールダウン
                }
            } else if (distance < 4.0) {
                // 近距離なら剣
                if (!ItemStack.isSame(currentWeapon, weapons.melee)) {
                    entity.setItemSlot(EquipmentSlot.MAINHAND, weapons.melee.copy());
                    switchCooldowns.put(entityId, 40); // 2秒のクールダウン
                }
            }
        } catch (Exception e) {
            // エラーが発生してもクラッシュしない
            System.err.println("Error switching weapon: " + e.getMessage());
        }
    }
    
    private static void initializeWeaponSet(LivingEntity entity) {
        UUID entityId = entity.getUUID();
        
        if (entity instanceof Skeleton) {
            // スケルトンの武器セット
            ItemStack bow = new ItemStack(Items.BOW);
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            entityWeapons.put(entityId, new WeaponSet(sword, bow));
            
            // 初期装備として弓を持たせる
            if (entity.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
                entity.setItemSlot(EquipmentSlot.MAINHAND, bow.copy());
            }
        } else if (entity instanceof Zombie) {
            // ゾンビの武器セット
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            ItemStack bow = new ItemStack(Items.BOW);
            entityWeapons.put(entityId, new WeaponSet(sword, bow));
            
            // 初期装備として剣を持たせる
            if (entity.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
                entity.setItemSlot(EquipmentSlot.MAINHAND, sword.copy());
            }
        }
    }
    
    // メモリリークを防ぐため、定期的に古いエントリをクリーンアップ
    private static int cleanupTicker = 0;
    
    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }
        
        cleanupTicker++;
        if (cleanupTicker >= 6000) { // 5分ごと
            cleanupTicker = 0;
            
            // 存在しないエンティティのエントリを削除
            switchCooldowns.entrySet().removeIf(entry -> {
                // クールダウンが0になったエントリを削除
                return entry.getValue() <= 0;
            });
            
            // 武器セットも同様にクリーンアップ（サイズが大きくなりすぎた場合）
            if (entityWeapons.size() > 100) {
                entityWeapons.clear();
            }
        }
    }
}