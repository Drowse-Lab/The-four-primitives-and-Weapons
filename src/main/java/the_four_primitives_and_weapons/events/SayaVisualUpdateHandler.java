package the_four_primitives_and_weapons.events;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 鞘 (saya/tyokuto_saya/sword_saya/rapier_saya) の NBT 整備ハンドラ。
 *
 * <p>見た目自体は {@link the_four_primitives_and_weapons.client.SayaModelWrapper}
 * が NBT (StoredKatana / StoredSword / StoredRapier) を読み取って動的に解決するため、
 * このハンドラは <b>CustomModelData の同期は一切行わない</b>。</p>
 *
 * <p>残っている責務:</p>
 * <ul>
 *   <li>霊刀 (reitou) 用の {@code SayaNBT} predicate 値の更新</li>
 *   <li>壊れた / 空の Stored* NBT エントリのクリーンアップ</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class SayaVisualUpdateHandler {

    private static final String[] STORED_KEYS = { "StoredKatana", "StoredSword", "StoredRapier" };

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
            if (!ItemStack.matches(mainHand, player.getMainHandItem())) return true;
            if (!ItemStack.matches(offHand, player.getOffhandItem())) return true;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack currentStack = player.getInventory().getItem(i);
                ItemStack lastStack = inventory.get(i);
                if (lastStack == null && !currentStack.isEmpty()) return true;
                if (lastStack != null && !ItemStack.matches(lastStack, currentStack)) return true;
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
        boolean inventoryChanged = lastSnapshot == null || lastSnapshot.hasChanged(player);

        if (inventoryChanged) {
            checkAndUpdateAllSayas(player);
            lastInventoryState.put(playerId, new InventorySnapshot(player));
        }

        // 5秒ごとに念のため全鞘を再検証
        if (player.tickCount % 100 == 0) {
            validateAllSayas(player);
        }
    }

    private static void checkAndUpdateAllSayas(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (isSaya(mainHand)) validateAndUpdateSaya(mainHand);

        ItemStack offHand = player.getOffhandItem();
        if (isSaya(offHand)) validateAndUpdateSaya(offHand);

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isSaya(stack)) validateAndUpdateSaya(stack);
        }

        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            for (String slotId : new String[]{"belt", "back"}) {
                handler.getStacksHandler(slotId).ifPresent(stacksHandler -> {
                    for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                        ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                        if (isSaya(stack)) validateAndUpdateSaya(stack);
                    }
                });
            }
        });
    }

    private static void validateAllSayas(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isSaya(stack)) validateAndUpdateSaya(stack);
        }
    }

    private static void validateAndUpdateSaya(ItemStack sayaStack) {
        if (!isSaya(sayaStack)) return;

        CompoundTag tag = sayaStack.getTag();
        if (tag == null) {
            sayaStack.setTag(new CompoundTag());
            return;
        }

        // 霊刀 (reitou) 判定用の SayaNBT predicate を更新
        updateSayaNBT(tag);

        // 壊れた Stored* エントリのクリーンアップ
        for (String key : STORED_KEYS) {
            if (tag.contains(key)) {
                ItemStack stored = ItemStack.of(tag.getCompound(key));
                if (stored.isEmpty()) tag.remove(key);
            }
        }

        sayaStack.setTag(tag);
    }

    private static boolean isSaya(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() == TheFourPrimitivesAndWeaponsModItems.SAYA.get()
            || stack.getItem() == TheFourPrimitivesAndWeaponsModItems.TYOKUTO_SAYA.get();
    }

    /**
     * SayaNBTタグを更新（霊刀スタイル判定用）。
     * クライアント側の item property で saya_nbt として使用される。
     */
    private static void updateSayaNBT(CompoundTag tag) {
        boolean isReitouStyle = false;

        if ("reitou".equals(tag.getString("SayaStyle"))) {
            isReitouStyle = true;
        }

        if (!isReitouStyle && tag.contains("StoredKatana")) {
            CompoundTag katanaTag = tag.getCompound("StoredKatana");
            String id = katanaTag.getString("id");
            if (id.contains("reitou")) {
                isReitouStyle = true;
            }
        }

        int current = tag.getInt("SayaNBT");
        int target = isReitouStyle ? 1 : 0;
        if (current != target) {
            tag.putInt("SayaNBT", target);
        }
    }
}
