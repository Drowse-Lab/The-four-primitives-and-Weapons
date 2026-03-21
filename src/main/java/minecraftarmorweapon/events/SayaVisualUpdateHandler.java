package minecraftarmorweapon.events;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.util.CuriosScabbardHelper;
import top.theillusivec4.curios.api.CuriosApi;

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
        if (player.level().isClientSide) return;
        
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

        // Curiosスロット内の鞘もチェック
        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            for (String slotId : new String[]{"belt", "back"}) {
                handler.getStacksHandler(slotId).ifPresent(stacksHandler -> {
                    for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                        ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                        if (isSaya(stack)) {
                            validateAndUpdateSaya(stack);
                        }
                    }
                });
            }
        });
    }
    
    private static void validateAllSayas(Player player) {
        // 全ての鞘の状態を検証
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isSaya(stack)) {
                CompoundTag tag = stack.getTag();
                if (tag != null) {
                    if (tag.contains("StoredKatana")) {
                        CompoundTag katanaTag = tag.getCompound("StoredKatana");
                        ItemStack katanaStack = ItemStack.of(katanaTag);
                        int expectedModelData = getModelDataForKatana(katanaStack, tag);
                        if (tag.getInt("CustomModelData") != expectedModelData) {
                            tag.putInt("CustomModelData", expectedModelData);
                            stack.setTag(tag);
                        }
                    } else {
                        int emptyModelData = getEmptyModelData(tag);
                        if (tag.getInt("CustomModelData") != emptyModelData) {
                            tag.putInt("CustomModelData", emptyModelData);
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
        
        // StoredKatanaまたはStoredSwordの有無でCustomModelDataを更新
        String storedKey = tag.contains("StoredKatana") ? "StoredKatana" :
                           tag.contains("StoredSword") ? "StoredSword" : null;
        if (storedKey != null) {
            CompoundTag weaponTag = tag.getCompound(storedKey);
            ItemStack weaponStack = ItemStack.of(weaponTag);

            // 刀が有効かチェック
            if (weaponStack.isEmpty()) {
                // 無効なデータの場合、削除
                tag.remove(storedKey);
                tag.putInt("CustomModelData", getEmptyModelData(tag));
            } else {
                // 正しいモデルデータを設定
                int modelData;
                if (storedKey.equals("StoredSword")) {
                    modelData = minecraftarmorweapon.item.TyokutoSayaItem.getSwordModelData(weaponStack);
                } else {
                    modelData = getModelDataForKatana(weaponStack, tag);
                }
                if (tag.getInt("CustomModelData") != modelData) {
                    tag.putInt("CustomModelData", modelData);
                }
            }
        } else {
            // 武器がない場合、鞘のスタイルに応じたモデルを設定
            int emptyModelData = getEmptyModelData(tag);
            if (tag.getInt("CustomModelData") != emptyModelData) {
                tag.putInt("CustomModelData", emptyModelData);
            }
        }
        
        sayaStack.setTag(tag);
    }
    
    private static boolean isSaya(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() == MinecraftArmorWeaponModItems.SAYA.get()
            || stack.getItem() == MinecraftArmorWeaponModItems.TYOKUTO_SAYA.get();
    }
    
    private static int getEmptyModelData(CompoundTag tag) {
        if ("sigiled".equals(tag.getString("Feyn"))) return 18;
        if ("reitou".equals(tag.getString("SayaStyle"))) return 16;
        return 0;
    }

    private static int getModelDataForKatana(ItemStack katanaStack, CompoundTag sayaTag) {
        if (katanaStack.isEmpty()) return 0;

        // 鞘のスタイルを先にチェック（どの刀が入っていても鞘の見た目を優先）
        if (sayaTag != null) {
            if ("sigiled".equals(sayaTag.getString("Feyn"))) return 17; // 封印鞘（刀入り・柄あり）
            if ("reitou".equals(sayaTag.getString("SayaStyle"))) return 16; // 霊刀スタイル鞘
        }

        // 鞘にスタイルがない場合は刀の種類で判定
        String itemName = katanaStack.getItem().getClass().getSimpleName();

        if (itemName.equals("ReitouItem")) return 16;
        if (itemName.equals("IronKatanaItem")) return 1;
        if (itemName.equals("GoldKatanaItem")) return 2;
        if (itemName.equals("StoneKatanaItem")) return 3;
        if (itemName.equals("NetheriteKatanaItem")) return 4;
        if (itemName.equals("WitherKatanaItem")) return 5;
        if (itemName.equals("DarknessKatanaItem")) return 7;
        if (itemName.equals("MagicalKatanaItem")) return 8;
        if (itemName.equals("MagischesFeenKatanaItem")) return 9;
        if (itemName.equals("PrototypeKatanaItem")) return 10;
        if (itemName.equals("OldKatanaItem")) return 11;
        if (itemName.equals("MyTestIronKatanaItem")) return 12;
        if (itemName.equals("RiversOfBloodItem")) return 13;
        if (itemName.equals("KatanaNiguHumerusItem")) return 14;
        if (itemName.equals("LokiTheTricksterItem")) return 15;

        return 0; // デフォルト（空の鞘）
    }
}