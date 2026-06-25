package the_four_primitives_and_weapons.util;

import the_four_primitives_and_weapons.events.MagicalKatanaCrystalHandler;
import the_four_primitives_and_weapons.item.MaterializedPouchItem;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * 結晶ローダウトポーチ ( 防具専用 ) の「呼び出し ( 装着 )」と「破壊で戻す」処理。
 *
 *  deploy(player):
 *    Magical Katana の結晶を破壊した瞬間に呼ばれる。 ポーチ ( 未呼び出し ) の中身 ( 戦闘防具 )
 *    を具現化版にして着用し、 <b>元々着ていた防具を一時的にポーチへ退避</b>する。 1 回限り。
 *
 *  returnToPouch(player):
 *    R キーで呼ばれる。 着用中の具現化防具 ( 自分の UUID ) を破壊し、 退避していた元の防具を
 *    着け直す。 破壊した具現化防具の元 ( 戦闘防具 ) はポーチへ戻す。 deploy 解除。
 */
public final class LoadoutPouchHelper {

    private LoadoutPouchHelper() {}

    /** wantDeployed と一致する状態の結晶ポーチを返す ( 無ければ null )。 */
    private static ItemStack findPouch(ServerPlayer player, boolean wantDeployed) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof MaterializedPouchItem
                    && MaterializedPouchItem.isDeployed(s) == wantDeployed) {
                return s;
            }
        }
        return null;
    }

    private static void giveBack(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    // ─────────────────────────────────────────────────────────────
    // 呼び出し ( 装着 )
    // ─────────────────────────────────────────────────────────────

    /**
     * 未呼び出しのポーチの戦闘防具を具現化版で着用し、 元の防具をポーチへ退避する。
     * @return 着用した点数 ( 0 ならポーチ無し / 既に呼び出し済み / 中身空 )
     */
    public static int deploy(ServerPlayer player) {
        if (player == null) return 0;
        ItemStack pouch = findPouch(player, false);
        if (pouch == null) return 0;
        UUID ownerId = player.getUUID();
        ItemStack[] loadout = MaterializedPouchItem.getLoadout(pouch); // 戦闘防具
        int equipped = 0;

        for (int i = 0; i < MaterializedPouchItem.SLOTS; i++) {
            if (loadout[i].isEmpty()) continue;
            EquipmentSlot slot = MaterializedPouchItem.ARMOR_SLOTS[i];
            ItemStack mat = MagicalKatanaCrystalHandler.materializeCopy(loadout[i], ownerId);
            ItemStack worn = player.getItemBySlot(slot).copy(); // 元々着ていた防具 ( 空可 )
            player.setItemSlot(slot, mat);
            loadout[i] = worn; // 元装備をポーチへ一時退避
            equipped++;
        }

        if (equipped <= 0) return 0;

        MaterializedPouchItem.setLoadout(pouch, loadout);
        MaterializedPouchItem.setDeployed(pouch, true);

        if (player.level() instanceof ServerLevel sl) {
            MagicalKatanaCrystalHandler.playShatterAt(sl, player.getX(), player.getY() + 1.0, player.getZ());
        }
        player.displayClientMessage(Component.literal(
                "§d結晶防具を §f" + equipped + " §d点 具現化装着しました"), true);
        return equipped;
    }

    // ─────────────────────────────────────────────────────────────
    // 破壊で戻す
    // ─────────────────────────────────────────────────────────────

    /** player が deploy 中のローダウトを持っているか ( R キーの発動条件判定用 ) */
    public static boolean hasDeployedLoadout(ServerPlayer player) {
        return findPouch(player, true) != null;
    }

    /**
     * 着用中の具現化防具 ( 自分の UUID ) を破壊し、 退避していた元防具を着け直す。
     * 破壊した具現化防具の元 ( 戦闘防具 ) はポーチへ戻す。
     * @return 破壊して戻した点数
     */
    public static int returnToPouch(ServerPlayer player) {
        if (player == null) return 0;
        ItemStack pouch = findPouch(player, true);
        if (pouch == null) pouch = findPouch(player, false); // フラグが消えていても受け皿に使う
        UUID ownerId = player.getUUID();

        ItemStack[] loadout = pouch != null
                ? MaterializedPouchItem.getLoadout(pouch) : new ItemStack[MaterializedPouchItem.SLOTS];
        if (pouch == null) java.util.Arrays.fill(loadout, ItemStack.EMPTY);

        int reverted = 0;

        // 着用中の具現化防具を破壊 → 元防具を着け直し、 戦闘防具をポーチへ戻す
        for (int i = 0; i < MaterializedPouchItem.SLOTS; i++) {
            EquipmentSlot slot = MaterializedPouchItem.ARMOR_SLOTS[i];
            ItemStack worn = player.getItemBySlot(slot);
            if (!MagicalKatanaCrystalHandler.isOwnedMaterialized(worn, ownerId)) continue;
            ItemStack combat = MagicalKatanaCrystalHandler.revertToOriginal(worn); // 戦闘防具 ( 元 )
            ItemStack stashed = loadout[i]; // deploy 時に退避した元装備
            player.setItemSlot(slot, stashed == null ? ItemStack.EMPTY : stashed);
            loadout[i] = combat; // 戦闘防具をポーチへ戻す
            reverted++;
        }

        // インベントリ / 手に残った「自分の具現化版」も破壊回収:
        //   - 具現化防具 → 元に戻してポーチの空き枠へ ( 無ければインベントリ )
        //   - 具現化武器 ( Magical Katana 等 ) → ポーチ対象外なので元に戻してインベントリへ
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!MagicalKatanaCrystalHandler.isOwnedMaterialized(s, ownerId)) continue;
            ItemStack orig = MagicalKatanaCrystalHandler.revertToOriginal(s);
            int free = firstEmptyArmorSlotFor(loadout, orig);
            if (free >= 0) loadout[free] = orig;
            else giveBack(player, orig);
            inv.setItem(i, ItemStack.EMPTY);
            reverted++;
        }

        if (pouch != null) {
            MaterializedPouchItem.setLoadout(pouch, loadout);
            MaterializedPouchItem.setDeployed(pouch, false);
        }

        if (reverted > 0 && player.level() instanceof ServerLevel sl) {
            MagicalKatanaCrystalHandler.playShatterAt(sl, player.getX(), player.getY() + 1.0, player.getZ());
        }
        return reverted;
    }

    /** orig が収納可能な防具なら、 対応する空き枠の index を返す。 不可 / 空き無しは -1。 */
    private static int firstEmptyArmorSlotFor(ItemStack[] loadout, ItemStack orig) {
        if (orig == null || orig.isEmpty()) return -1;
        for (int i = 0; i < MaterializedPouchItem.SLOTS; i++) {
            if (!loadout[i].isEmpty()) continue;
            if (MaterializedPouchItem.canStore(orig, i)) return i;
        }
        return -1;
    }
}
