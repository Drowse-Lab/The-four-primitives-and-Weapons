package minecraftarmorweapon.events;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class SayaVisualUpdateHandler {
    
    // プレイヤーごとの前回のインベントリ状態を記録
    private static final Map<UUID, InventorySnapshot> lastInventoryState = new HashMap<>();
    
    private static class InventorySnapshot {
        ItemStack mainHand;
        ItemStack offHand;
        Map<Integer, ItemStack> inventory = new HashMap<>();
        
        InventorySnapshot(Player player) {
            this.mainHand = player.getMainHandItem().copy();
            this.offHand = player.getOffhandItem().copy();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    this.inventory.put(i, stack.copy());
                }
            }
        }
        
        boolean hasChanged(Player player) {
            // メインハンドの変化をチェック
            if (!ItemStack.matches(mainHand, player.getMainHandItem())) {
                return true;
            }
            // オフハンドの変化をチェック
            if (!ItemStack.matches(offHand, player.getOffhandItem())) {
                return true;
            }
            // インベントリの変化をチェック
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack currentStack = player.getInventory().getItem(i);
                ItemStack lastStack = inventory.get(i);
                
                if (lastStack == null && !currentStack.isEmpty()) {
                    return true;
                }
                if (lastStack != null && !ItemStack.matches(lastStack, currentStack)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Player player = event.player;
        if (player.level.isClientSide) return;
        
        UUID playerId = player.getUUID();
        InventorySnapshot lastSnapshot = lastInventoryState.get(playerId);
        
        // インベントリが変化したかチェック
        boolean inventoryChanged = lastSnapshot == null || lastSnapshot.hasChanged(player);
        
        if (inventoryChanged) {
            // 全ての鞘をチェックして必要に応じて更新
            checkAndUpdateAllSayas(player);
            
            // 現在の状態を保存
            lastInventoryState.put(playerId, new InventorySnapshot(player));
        }
        
        // 定期的に鞘の状態を検証（5秒ごと）
        if (player.tickCount % 100 == 0) {
            validateAllSayas(player);
        }
    }
    
    private static void checkAndUpdateAllSayas(Player player) {
        // メインハンドの鞘をチェック
        ItemStack mainHand = player.getMainHandItem();
        if (isSaya(mainHand)) {
            validateAndUpdateSaya(mainHand);
        }
        
        // オフハンドの鞘をチェック
        ItemStack offHand = player.getOffhandItem();
        if (isSaya(offHand)) {
            validateAndUpdateSaya(offHand);
        }
        
        // インベントリ内の全ての鞘をチェック
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isSaya(stack)) {
                validateAndUpdateSaya(stack);
            }
        }
    }
    
    private static void validateAllSayas(Player player) {
        // 全ての鞘の状態を検証
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isSaya(stack)) {
                CompoundTag tag = stack.getTag();
                if (tag != null) {
                    // StoredKatanaがあるかチェック
                    if (tag.contains("StoredKatana")) {
                        // CustomModelDataが正しいか確認
                        CompoundTag katanaTag = tag.getCompound("StoredKatana");
                        ItemStack katanaStack = ItemStack.of(katanaTag);
                        int expectedModelData = getModelDataForKatana(katanaStack);
                        
                        if (tag.getInt("CustomModelData") != expectedModelData) {
                            tag.putInt("CustomModelData", expectedModelData);
                            stack.setTag(tag);
                        }
                    } else {
                        // StoredKatanaがない場合は空の鞘にする
                        if (tag.getInt("CustomModelData") != 0) {
                            tag.putInt("CustomModelData", 0);
                            stack.setTag(tag);
                        }
                    }
                }
            }
        }
    }
    
    private static void validateAndUpdateSaya(ItemStack sayaStack) {
        if (!isSaya(sayaStack)) return;
        
        CompoundTag tag = sayaStack.getTag();
        if (tag == null) {
            // タグがない場合は空の鞘として初期化
            tag = new CompoundTag();
            tag.putInt("CustomModelData", 0);
            sayaStack.setTag(tag);
            return;
        }
        
        // StoredKatanaの有無でCustomModelDataを更新
        if (tag.contains("StoredKatana")) {
            CompoundTag katanaTag = tag.getCompound("StoredKatana");
            ItemStack katanaStack = ItemStack.of(katanaTag);
            
            // 刀が有効かチェック
            if (katanaStack.isEmpty()) {
                // 無効な刀データの場合、削除
                tag.remove("StoredKatana");
                tag.putInt("CustomModelData", 0);
            } else {
                // 正しいモデルデータを設定
                int modelData = getModelDataForKatana(katanaStack);
                if (tag.getInt("CustomModelData") != modelData) {
                    tag.putInt("CustomModelData", modelData);
                }
            }
        } else {
            // StoredKatanaがない場合は空の鞘
            if (tag.getInt("CustomModelData") != 0) {
                tag.putInt("CustomModelData", 0);
            }
        }
        
        sayaStack.setTag(tag);
    }
    
    private static boolean isSaya(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() == MinecraftArmorWeaponModItems.SAYA.get();
    }
    
    private static int getModelDataForKatana(ItemStack katanaStack) {
        if (katanaStack.isEmpty()) return 0;
        
        String itemName = katanaStack.getItem().getClass().getSimpleName();
        
        // 各刀タイプに対応するカスタムモデルデータ
        if (itemName.equals("IronKatanaItem")) return 1;
        if (itemName.equals("GoldKatanaItem")) return 2;
        if (itemName.equals("StoneKatanaItem")) return 3;
        if (itemName.equals("NetheriteKatanaItem")) return 4;
        if (itemName.equals("WitherKatanaItem")) return 5;
        if (itemName.equals("MotoWitherKatanaItem")) return 6;
        if (itemName.equals("DarknessKatanaItem")) return 7;
        if (itemName.equals("MagicalKatanaItem")) return 8;
        if (itemName.equals("MagischesFeenKatanaItem")) return 9;
        if (itemName.equals("PrototypeKatanaItem")) return 10;
        if (itemName.equals("OldKatanaItem")) return 11;
        if (itemName.equals("MyTestIronKatanaItem")) return 12;
        if (itemName.equals("RiversOfBloodItem")) return 13;
        if (itemName.equals("KatanaNiguHumerusItem")) return 14;
        
        return 0; // デフォルト（空の鞘）
    }
}