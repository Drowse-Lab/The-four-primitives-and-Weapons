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

        for (int i = 0; i < MaterializedPouchItem.ARMOR_SLOTS.length; i++) { // 防具スロット(0-3)のみ。 武器スロット(4/5)は収納専用
            EquipmentSlot slot = MaterializedPouchItem.ARMOR_SLOTS[i];
            ItemStack worn = player.getItemBySlot(slot).copy(); // 元々着ていた防具 ( 空可 )
            if (!loadout[i].isEmpty()) {
                // 戦闘防具あり → 具現化装着、 元装備を退避
                ItemStack mat = MagicalKatanaCrystalHandler.materializeCopy(loadout[i], ownerId);
                MaterializedPouchItem.stampFromPouch(mat, ownerId); // ポーチ由来の刻印
                player.setItemSlot(slot, mat);
                loadout[i] = worn;
                equipped++;
            } else if (MaterializedPouchItem.getReplaceAir(pouch, i) && !worn.isEmpty()) {
                // 空きスロット + 空気置換 ON → 着用防具を空気に置換して退避
                player.setItemSlot(slot, ItemStack.EMPTY);
                loadout[i] = worn;
                equipped++;
            }
            // 空き + 置換 OFF → 何もしない
        }

        // 武器スロット → 手に具現化装着 ( 元の手持ちはポーチへ退避。 原本はスロットに残す )
        equipped += deployHand(player, pouch, loadout, MaterializedPouchItem.SLOT_MAINHAND, net.minecraft.world.InteractionHand.MAIN_HAND, ownerId);
        equipped += deployHand(player, pouch, loadout, MaterializedPouchItem.SLOT_OFFHAND, net.minecraft.world.InteractionHand.OFF_HAND, ownerId);

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

    /**
     * 武器スロット idx の中身を具現化コピーして hand に装着。 元の手持ちはスロットへ退避する
     * ( = 防具と同じ move-out 方式。 原本は具現化コピーに埋め込まれており、 破壊時に
     *   {@link #returnToPouch} で必ずポーチへ戻る → スロットを置換されても原本は消えない )。
     */
    private static int deployHand(ServerPlayer player, ItemStack pouch, ItemStack[] loadout, int idx,
                                  net.minecraft.world.InteractionHand hand, UUID ownerId) {
        if (loadout[idx] == null || loadout[idx].isEmpty()) return 0;
        ItemStack held = player.getItemInHand(hand).copy();
        ItemStack mat = MagicalKatanaCrystalHandler.materializeCopy(loadout[idx], ownerId); // 原本を埋め込んだ具現化版
        MaterializedPouchItem.stampFromPouch(mat, ownerId);
        player.setItemInHand(hand, mat);
        // 元の手持ちをスロットへ退避 ( 具現化品/ポーチ自身は退避不可 )。 破壊時に手へ戻す。
        boolean storable = !held.isEmpty()
                && !MagicalKatanaCrystalHandler.isAnyMaterialized(held)
                && !(held.getItem() instanceof MaterializedPouchItem);
        loadout[idx] = storable ? held : ItemStack.EMPTY;
        return 1;
    }

    /** 結晶化時にポーチの武器スロットを手に装着する内容があるか ( materializeFor のデフォルト刀置換判定 )。 */
    public static boolean hasWeaponLoadout(ServerPlayer player) {
        ItemStack pouch = findPouch(player, false);
        if (pouch == null) pouch = findPouch(player, true);
        return pouch != null && MaterializedPouchItem.hasWeaponLoadout(pouch);
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
        // 増殖対策: 結晶ポーチがインベントリに無ければ破壊できない
        if (MaterializedPouchItem.findFirst(player).isEmpty()) {
            player.displayClientMessage(Component.literal("§c結晶ポーチが無いため破壊できません"), true);
            return 0;
        }
        ItemStack pouch = findPouch(player, true);
        if (pouch == null) pouch = findPouch(player, false); // フラグが消えていても受け皿に使う
        UUID ownerId = player.getUUID();

        ItemStack[] loadout = pouch != null
                ? MaterializedPouchItem.getLoadout(pouch) : new ItemStack[MaterializedPouchItem.SLOTS];
        if (pouch == null) java.util.Arrays.fill(loadout, ItemStack.EMPTY);

        boolean wasDeployed = pouch != null && MaterializedPouchItem.isDeployed(pouch);
        int reverted = 0;

        // 着用中の具現化防具を破壊 → 元防具を着け直し、 戦闘防具をポーチへ戻す
        for (int i = 0; i < MaterializedPouchItem.ARMOR_SLOTS.length; i++) { // 防具スロット(0-3)のみ
            EquipmentSlot slot = MaterializedPouchItem.ARMOR_SLOTS[i];
            ItemStack worn = player.getItemBySlot(slot);
            ItemStack stashed = loadout[i]; // deploy 時に退避した元装備
            if (MagicalKatanaCrystalHandler.isOwnedMaterialized(worn, ownerId)) {
                ItemStack combat = MagicalKatanaCrystalHandler.revertToOriginal(worn); // 戦闘防具 ( 元 )
                player.setItemSlot(slot, stashed == null ? ItemStack.EMPTY : stashed);
                loadout[i] = combat; // 戦闘防具をポーチへ戻す
                reverted++;
            } else if (wasDeployed && stashed != null && !stashed.isEmpty() && worn.isEmpty()) {
                // 空気置換していたスロット: 退避した元装備を着け直す ( 戦闘防具は無いので枠は空に )
                player.setItemSlot(slot, stashed);
                loadout[i] = ItemStack.EMPTY;
                reverted++;
            }
        }

        // 武器スロットの解除: 手の具現化武器を破壊し、 退避していた元の手持ちを戻す。
        //   from-pouch コピーは「原本を埋め込んだ具現化版」なので、 破壊時に原本を取り出して
        //   武器スロットへ戻す ( = 防具と同じ move-out )。 スロットを置換されていても原本は
        //   コピーに在るので <b>絶対にポーチへ戻る ( 虚空に消えない )</b>。
        for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
            ItemStack worn = player.getItemInHand(hand);
            if (!MagicalKatanaCrystalHandler.isOwnedMaterialized(worn, ownerId)) continue;
            boolean main = hand == net.minecraft.world.InteractionHand.MAIN_HAND;
            int idx = main ? MaterializedPouchItem.SLOT_MAINHAND : MaterializedPouchItem.SLOT_OFFHAND;
            if (MaterializedPouchItem.isFromPouch(worn, ownerId)) {
                ItemStack original = MagicalKatanaCrystalHandler.revertToOriginal(worn); // 埋め込まれた原本
                ItemStack stashed = (idx >= 0 && idx < loadout.length && loadout[idx] != null)
                        ? loadout[idx] : ItemStack.EMPTY; // deploy 時に退避した手持ち
                player.setItemInHand(hand, stashed);
                if (idx >= 0 && idx < loadout.length) loadout[idx] = original; // 原本をポーチへ戻す
                else giveBack(player, original);
            } else {
                // 虚空由来 ( 結晶割りのデフォルト刀。 原本は別途プレイヤーが保持済み ) → 完全消去
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
            reverted++;
        }

        // インベントリ / 手に残った「自分の具現化版」も破壊回収:
        //   - 具現化防具 → 元装備をポーチの空き枠へ ( 無ければインベントリ )
        //   - ポーチ由来の具現化武器 ( deploy したコピー ) → 原本はポーチに在るのでコピーのみ消す ( 増殖防止 )
        //   - ポーチ外の具現化武器 → 虚無に飛ばさずポーチの武器スロットへ戻す ( 満杯ならインベントリ )
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!MagicalKatanaCrystalHandler.isOwnedMaterialized(s, ownerId)) continue;
            if (armorIndexOf(s) >= 0) {
                // 具現化防具 → 元装備を回収
                ItemStack orig = MagicalKatanaCrystalHandler.revertToOriginal(s);
                int free = firstEmptyArmorSlotFor(loadout, orig);
                if (free >= 0) loadout[free] = orig;
                else giveBack(player, orig);
            } else if (MaterializedPouchItem.isFromPouch(s, ownerId)) {
                // from-pouch 武器コピー ( 手から在庫に移ったもの ) → 原本をポーチの武器スロットへ
                //   ( 満杯ならインベントリ )。 これで虚空に消えない。
                ItemStack orig = MagicalKatanaCrystalHandler.revertToOriginal(s);
                int w = firstEmptyWeaponSlot(loadout);
                if (w >= 0) loadout[w] = orig; else giveBack(player, orig);
            }
            // 上記で回収済み / 虚空由来 ( 原本はプレイヤーが別途保持 ) はそのまま消去
            inv.setItem(i, ItemStack.EMPTY);
            reverted++;
        }

        // 「ポーチ由来」 刻印 ( PouchOwner ) が付いた装備も確実に回収する:
        //   手動でポーチから取り出した防具など ( 具現化版ではないもの )。 刻印を消してポーチへ戻す。
        // 着用スロット
        for (EquipmentSlot slot : MaterializedPouchItem.ARMOR_SLOTS) {
            ItemStack worn = player.getItemBySlot(slot);
            if (worn.isEmpty() || MagicalKatanaCrystalHandler.isOwnedMaterialized(worn, ownerId)) continue;
            if (!MaterializedPouchItem.isFromPouch(worn, ownerId)) continue;
            ItemStack clean = worn.copy();
            MaterializedPouchItem.clearFromPouch(clean);
            int free = firstEmptyArmorSlotFor(loadout, clean);
            if (free >= 0) loadout[free] = clean; else giveBack(player, clean);
            player.setItemSlot(slot, ItemStack.EMPTY);
            reverted++;
        }
        // インベントリ / 手
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || MagicalKatanaCrystalHandler.isOwnedMaterialized(s, ownerId)) continue;
            if (!MaterializedPouchItem.isFromPouch(s, ownerId)) continue;
            ItemStack clean = s.copy();
            MaterializedPouchItem.clearFromPouch(clean);
            int free = firstEmptyArmorSlotFor(loadout, clean);
            if (free >= 0) loadout[free] = clean; else giveBack(player, clean);
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

    /**
     * 具現化防具を「装備スロットから外した」 ( = main インベントリ / オフハンドへ移動した ) ら、
     * その具現化防具を破壊し、 ポーチに退避していた元装備をその装備スロットへ戻す ( 1 枚ごと )。
     * 戦闘防具 ( 具現化の元 ) はポーチへ戻す。 武器は対象外 ( 防具だけ )。
     */
    public static void sweepLooseArmor(ServerPlayer player) {
        if (player == null) return;
        UUID ownerId = player.getUUID();
        ItemStack pouch = findPouch(player, true);
        if (pouch == null) pouch = findPouch(player, false);
        ItemStack[] lo = (pouch != null) ? MaterializedPouchItem.getLoadout(pouch) : null;

        Inventory inv = player.getInventory();
        boolean changed = false, loChanged = false;

        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (!MagicalKatanaCrystalHandler.isOwnedMaterialized(s, ownerId)) continue;
            int armorIdx = armorIndexOf(s);
            if (armorIdx < 0) continue; // 防具だけ

            ItemStack combat = MagicalKatanaCrystalHandler.revertToOriginal(s); // 戦闘防具 ( 元 )
            inv.items.set(i, ItemStack.EMPTY); // 具現化防具を破壊
            changed = true;

            EquipmentSlot eslot = MaterializedPouchItem.ARMOR_SLOTS[armorIdx];
            if (lo != null) {
                ItemStack stashed = lo[armorIdx]; // 退避していた元装備 ( W )
                if (player.getItemBySlot(eslot).isEmpty()) {
                    player.setItemSlot(eslot, stashed == null ? ItemStack.EMPTY : stashed);
                } else if (stashed != null && !stashed.isEmpty()) {
                    giveBack(player, stashed);
                }
                lo[armorIdx] = combat; // 戦闘防具をポーチへ
                loChanged = true;
            } else {
                giveBack(player, combat);
            }
        }

        if (loChanged && pouch != null) MaterializedPouchItem.setLoadout(pouch, lo);
        if (changed && pouch != null && !anyMaterializedArmorEquipped(player, ownerId)) {
            MaterializedPouchItem.setDeployed(pouch, false);
        }
        if (changed && player.level() instanceof ServerLevel sl) {
            MagicalKatanaCrystalHandler.playShatterAt(sl, player.getX(), player.getY() + 1.0, player.getZ());
        }
    }

    /** スタックが どの防具スロット ( 0..3 ) 用か。 防具でなければ -1。 */
    private static int armorIndexOf(ItemStack s) {
        EquipmentSlot es = net.minecraft.world.entity.LivingEntity.getEquipmentSlotForItem(s);
        for (int i = 0; i < MaterializedPouchItem.ARMOR_SLOTS.length; i++) {
            if (MaterializedPouchItem.ARMOR_SLOTS[i] == es) return i;
        }
        return -1;
    }

    /** 防具スロットのいずれかに 自分の具現化防具が着いているか */
    private static boolean anyMaterializedArmorEquipped(ServerPlayer player, UUID ownerId) {
        for (EquipmentSlot es : MaterializedPouchItem.ARMOR_SLOTS) {
            if (MagicalKatanaCrystalHandler.isOwnedMaterialized(player.getItemBySlot(es), ownerId)) return true;
        }
        return false;
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

    /** 空いている武器スロット ( メイン→オフ ) の index を返す。 空き無しは -1。 */
    private static int firstEmptyWeaponSlot(ItemStack[] loadout) {
        for (int idx : new int[]{MaterializedPouchItem.SLOT_MAINHAND, MaterializedPouchItem.SLOT_OFFHAND}) {
            if (idx >= 0 && idx < loadout.length && (loadout[idx] == null || loadout[idx].isEmpty())) return idx;
        }
        return -1;
    }
}
