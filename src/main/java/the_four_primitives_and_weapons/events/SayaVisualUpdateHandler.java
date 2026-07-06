package the_four_primitives_and_weapons.events;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import top.theillusivec4.curios.api.CuriosApi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final Logger LOGGER = LogManager.getLogger("maw/saya");
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
            detectAndHealDuplicateScabbards(player);
            lastInventoryState.put(playerId, new InventorySnapshot(player));
        }

        // 5秒ごとに念のため全鞘を再検証
        if (player.tickCount % 100 == 0) {
            validateAllSayas(player);
            detectAndHealDuplicateScabbards(player);
        }
    }

    /**
     * 両手 + Curios + インベントリを横断し、複製された鞘 (同一 ItemStack 参照、
     * または同一 Stored* NBT を持つ別 ItemStack) を検出する。
     *
     * <p>検出条件:</p>
     * <ol>
     *   <li><b>参照の重複</b>: メインハンドとオフハンドの {@link ItemStack} が同じ
     *       Java オブジェクト → vanilla swap/プログラムの参照リークによる完全複製。
     *       オフハンド側を {@link ItemStack#EMPTY} にする。</li>
     *   <li><b>NBT の重複</b>: メインハンドとオフハンドが別オブジェクトだが、同じアイテムで
     *       同じ Stored* NBT を持つ → 武器複製の兆候。オフハンド側を EMPTY にして
     *       (どちらか一方を残す形で) 救済。WARN ログを出して再現追跡できるようにする。</li>
     * </ol>
     */
    private static void detectAndHealDuplicateScabbards(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (!isSaya(main) || !isSaya(off)) return;
        if (!hasAnyStoredKey(main) || !hasAnyStoredKey(off)) return;

        // (1) 同一参照 — 確実な複製
        if (main == off) {
            LOGGER.warn("[saya] 同一 ItemStack 参照が両手に存在 — オフハンド側をクリア (player={}, item={})",
                player.getName().getString(), main.getItem());
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            return;
        }

        // (2) NBT 一致 — 別オブジェクトだが内容が同じ
        if (main.getItem() == off.getItem()
                && sameStoredFingerprint(main, off)) {
            LOGGER.warn("[saya] 両手に同一内身入り鞘が出現 — オフハンド側をクリア (player={}, item={}, stored={})",
                player.getName().getString(),
                main.getItem(),
                fingerprintOf(main));
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
    }

    private static boolean hasAnyStoredKey(ItemStack stack) {
        if (!stack.hasTag()) return false;
        CompoundTag tag = stack.getTag();
        for (String key : STORED_KEYS) {
            if (tag.contains(key)) return true;
        }
        return false;
    }

    /** 両鞘の Stored* 内容が一致するか (アイテム種別 + NBT 完全一致)。 */
    private static boolean sameStoredFingerprint(ItemStack a, ItemStack b) {
        String fa = fingerprintOf(a);
        String fb = fingerprintOf(b);
        return fa != null && fa.equals(fb);
    }

    private static String fingerprintOf(ItemStack saya) {
        if (!saya.hasTag()) return null;
        CompoundTag tag = saya.getTag();
        for (String key : STORED_KEYS) {
            if (tag.contains(key)) {
                return key + ":" + tag.getCompound(key).toString();
            }
        }
        return null;
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
        return the_four_primitives_and_weapons.util.CuriosScabbardHelper.isScabbard(stack);
    }

    /**
     * SayaNBTタグを更新（霊刀スタイル判定用）。
     * クライアント側の item property で saya_nbt として使用される。
     */
    private static void updateSayaNBT(CompoundTag tag) {
        // 旧データ互換: 以前の霊刀鞘タグは、今後は「封の鞘」として扱う。
        if ("reitou".equals(tag.getString("SayaStyle"))) {
            tag.putString("Feyn", "sigiled");
            tag.remove("SayaStyle");
        }

        int current = tag.getInt("SayaNBT");
        if (current != 0) {
            tag.putInt("SayaNBT", 0);
        }
    }
}
