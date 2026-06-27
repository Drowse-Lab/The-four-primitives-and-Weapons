package the_four_primitives_and_weapons.item;

import the_four_primitives_and_weapons.events.MagicalKatanaCrystalHandler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 結晶ローダウトポーチ ( バンドル式 )。
 *
 * 専用 UI は持たず、 バニラのバンドルと同じ操作で出し入れする:
 *   - ポーチをカーソルに持ち、 別スロットを右クリック → そのアイテムを収納 / 空スロットなら 1 個取り出し
 *   - アイテムをカーソルに持ち、 ポーチを右クリック → 収納 / カーソル空なら 1 個取り出し
 *   ( = どのスロットからでも出し入れできる )
 *
 * 中身は NBT の {@link #TAG_LOADOUT} ( スロット番号キーの CompoundTag、 最大 {@link #SLOTS} 個 ) に保持。
 *
 * 結晶化 ( Magical Katana の結晶破壊 ) で中身を具現化版として着用し、 元装備を一時退避。
 * R キー破壊で具現化版を壊して元装備に戻す ( {@code LoadoutPouchHelper} )。
 */
public class MaterializedPouchItem extends Item {

    /** パネルのスロット総数 ( 防具4 + 武器2: メイン/オフ )。 */
    public static final int SLOTS = 6;
    /** 防具スロット ( index 0-3 )。 */
    public static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    /** 武器スロット index ( メインハンド / オフハンド )。 */
    public static final int SLOT_MAINHAND = 4;
    public static final int SLOT_OFFHAND  = 5;
    public static boolean isWeaponSlot(int i) { return i == SLOT_MAINHAND || i == SLOT_OFFHAND; }

    public static final String TAG_LOADOUT  = "Loadout";
    public static final String TAG_DEPLOYED = "Deployed";

    public MaterializedPouchItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    /**
     * 右クリック        : 着用中の防具をポーチへ収納 ( = 装備をセット )
     * スニーク+右クリック: 中身を全部インベントリへ取り出す
     *
     * ( バンドル操作 = ポーチとアイテムを右クリックで重ねる、 も併用可 )
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack pouch = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(pouch, true);
        }

        // スニーク+右クリック → 全部取り出し ( 取り出しは結晶化中でも可 )
        if (player.isShiftKeyDown()) {
            int out = 0;
            ItemStack[] lo = getLoadout(pouch);
            for (int i = 0; i < SLOTS; i++) {
                if (lo[i].isEmpty()) continue;
                // 手動取り出しは刻印しない ( 普通に使うアイテムなので 破壊対象にしない )
                if (!player.getInventory().add(lo[i])) player.drop(lo[i], false);
                lo[i] = ItemStack.EMPTY;
                out++;
            }
            setLoadout(pouch, lo);
            player.displayClientMessage(Component.literal("§d結晶ポーチ §7— 中身を §f" + out + " §7点取り出した"), true);
            return InteractionResultHolder.sidedSuccess(pouch, false);
        }

        // 右クリック → 着用中の防具をポーチへ収納 ( 結晶化中は不可 )
        if (isInsertLocked(pouch, player)) {
            player.displayClientMessage(Component.literal("§c呼び出し中は収納できません"), true);
            return InteractionResultHolder.fail(pouch);
        }
        int stored = 0;
        ItemStack[] lo = getLoadout(pouch);
        for (int idx = 0; idx < ARMOR_SLOTS.length; idx++) {
            EquipmentSlot slot = ARMOR_SLOTS[idx];
            ItemStack worn = player.getItemBySlot(slot);
            if (worn.isEmpty() || !canStore(worn)) continue;
            if (!lo[idx].isEmpty()) continue;     // その部位スロットが既に埋まっている
            lo[idx] = worn.copy();                // 対応部位スロットに収納 ( 装着時に正しく戻る )
            player.setItemSlot(slot, ItemStack.EMPTY);
            stored++;
        }
        setLoadout(pouch, lo);
        if (stored > 0) {
            playInsert(player);
            player.displayClientMessage(Component.literal("§d結晶ポーチ §7— 着用防具を §f" + stored + " §7点セットした"), true);
            return InteractionResultHolder.sidedSuccess(pouch, false);
        }
        player.displayClientMessage(Component.literal(
            "§7着用防具が無い / ポーチが満杯です §8( スニーク+右クリックで取り出し )"), true);
        return InteractionResultHolder.fail(pouch);
    }

    /** 最初の空き枠 index ( 無ければ -1 ) */
    private static int firstEmpty(ItemStack[] lo) {
        for (int i = 0; i < SLOTS; i++) if (lo[i].isEmpty()) return i;
        return -1;
    }

    // --- 収納可否 -------------------------------------------------------

    /** 収納できるアイテムか ( ポーチ自身 / 具現化版 / Magical Katana / 空 は不可 ) */
    public static boolean canStore(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof MaterializedPouchItem) return false;     // 入れ子防止
        if (MagicalKatanaCrystalHandler.isAnyMaterialized(stack)) return false; // 具現化版は不可
        if (MagicalKatanaCrystalHandler.isMagicalKatana(stack)) return false;   // Magical Katana は不可
        return true;
    }

    /**
     * スロット番号別の収納可否。
     *   - slot 0 ( 兜 ) : 基本的に何でも可 ( 防具でなくても可 )
     *   - slot 1..3 ( 胴/脚/靴 ) : その部位の防具のみ
     *   - slot 4/5 ( メイン/オフ武器 ) : 武器 or 納刀した鞘 ( magical katana も可 )
     */
    public static boolean canStore(ItemStack stack, int slotIndex) {
        if (stack.isEmpty()) return true;
        if (slotIndex < 0 || slotIndex >= SLOTS) return false;
        if (stack.getItem() instanceof MaterializedPouchItem) return false;       // 入れ子防止
        if (isWeaponSlot(slotIndex)) {
            // 武器スロット: 武器 / 納刀した鞘 / magical katana ( 具現化版でも可。 収納時に通常版へ戻す )
            return the_four_primitives_and_weapons.events.DodgeAndBattouHandler.isWeapon(stack)
                || the_four_primitives_and_weapons.util.CuriosScabbardHelper.isScabbard(stack)
                || MagicalKatanaCrystalHandler.isMagicalKatana(stack);
        }
        // 防具スロット
        if (MagicalKatanaCrystalHandler.isAnyMaterialized(stack)) return false;    // 防具スロットは具現化版不可
        if (!canStore(stack)) return false;  // ポーチ自身 / 具現化版 / Magical Katana は不可
        if (slotIndex == 0) return true;     // 兜スロットは何でも可
        return net.minecraft.world.entity.LivingEntity.getEquipmentSlotForItem(stack) == ARMOR_SLOTS[slotIndex];
    }

    /** 武器スロット収納用に正規化: 具現化版なら通常版へ戻す ( 結晶ポーチに具現化品を貯めない )。 */
    public static ItemStack normalizeForWeaponSlot(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = MagicalKatanaCrystalHandler.isAnyMaterialized(stack)
                ? MagicalKatanaCrystalHandler.revertToOriginal(stack)
                : stack.copy();
        out.setCount(1);
        return out;
    }

    // --- バンドル操作 ( 右クリック出し入れ ) -------------------------------

    @Override
    public boolean overrideStackedOnOther(ItemStack pouch, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack target = slot.getItem();
        if (target.isEmpty()) {
            // 取り出し — 結晶化中でも常に許可
            ItemStack popped = popOne(pouch);
            if (popped.isEmpty()) return false;
            // 手動取り出しは刻印しない
            ItemStack rem = slot.safeInsert(popped);
            if (!rem.isEmpty()) pushOne(pouch, rem); // 入りきらない分は戻す
            playRemove(player);
            return true;
        }
        // 収納 — 結晶化アイテムがインベントリにある間は不可。
        //   可否は pushOne 内の canStore( stack, slot ) で判定 ( 武器スロットは magical katana も可 )。
        if (isInsertLocked(pouch, player)) return false;
        if (!pushOne(pouch, target)) return false;
        target.shrink(1);
        playInsert(player);
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack pouch, ItemStack cursor, Slot slot,
                                            ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        if (cursor.isEmpty()) {
            // 取り出し — 結晶化中でも常に許可
            ItemStack popped = popOne(pouch);
            if (popped.isEmpty()) return false;
            // 手動取り出しは刻印しない
            access.set(popped);
            playRemove(player);
            return true;
        }
        // 収納 — 可否は pushOne 内の canStore( stack, slot ) で判定 ( 武器スロットは magical katana も可 )
        if (isInsertLocked(pouch, player)) return false;
        if (!pushOne(pouch, cursor)) return false;
        cursor.shrink(1);
        playInsert(player);
        return true;
    }

    /**
     * 収納をロックすべき状態か。
     * 呼び出し中、 または「結晶化 ( 具現化 ) アイテムがプレイヤーのインベントリ / 装備に存在する」
     * 場合は新規収納を禁止する ( 取り出しは常に可 )。 侵食属性の有無は問わない。
     */
    private static boolean isInsertLocked(ItemStack pouch, Player player) {
        return isInsertLocked(player);
    }

    /**
     * GUI / バンドルで「収納」 をロックすべきか。
     * 判定は <b>具現化防具が実際に装備スロットに着いているか</b> ( = 結晶化が有効中 ) 。
     * Deployed フラグが残っても、 具現化防具が無ければ収納できる ( ロック残り防止 )。
     */
    public static boolean isInsertLocked(Player player) {
        if (player == null) return false;
        java.util.UUID id = player.getUUID();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (MagicalKatanaCrystalHandler.isOwnedMaterialized(player.getItemBySlot(slot), id)) return true;
        }
        // 武器装着中はロックしない: 他の刀を持っていても武器スロットを置換できるようにする。
        return false;
    }

    private static void playInsert(Player p) {
        p.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 0.8f + p.getRandom().nextFloat() * 0.4f);
        crystalPuff(p);
    }

    /** 収納時の「結晶化」マゼンタ演出（サーバー側）。 全ての収納経路から呼ぶ。 */
    public static void crystalPuff(Player p) {
        if (p.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(0.75f, 0.1f, 0.55f), 1.2f),
                    p.getX(), p.getY() + 1.0, p.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
        }
    }

    private static void playRemove(Player p) {
        p.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 0.8f + p.getRandom().nextFloat() * 0.4f);
    }

    /** 防具→その部位スロット、 武器/鞘→武器スロット、 それ以外→兜スロット(0)。 */
    private static int targetSlot(ItemStack s) {
        EquipmentSlot es = net.minecraft.world.entity.LivingEntity.getEquipmentSlotForItem(s);
        if (es.getType() == EquipmentSlot.Type.ARMOR) {
            for (int i = 0; i < ARMOR_SLOTS.length; i++) {
                if (ARMOR_SLOTS[i] == es) return i;
            }
        }
        if (the_four_primitives_and_weapons.events.DodgeAndBattouHandler.isWeapon(s)
                || the_four_primitives_and_weapons.util.CuriosScabbardHelper.isScabbard(s)) {
            return SLOT_MAINHAND; // 武器/鞘 → メイン優先 ( pushOne が空きを main→off で探す )
        }
        return 0; // その他 → 兜スロット ( 何でも可 )
    }

    /** 対応するスロットへ 1 個収納 ( 成功で true )。 武器はメイン→オフの順で空きを探す。 */
    private static boolean pushOne(ItemStack pouch, ItemStack incoming) {
        int t = targetSlot(incoming);
        ItemStack[] lo = getLoadout(pouch);
        if (isWeaponSlot(t)) {
            for (int i : new int[]{SLOT_MAINHAND, SLOT_OFFHAND}) {
                if (lo[i].isEmpty() && canStore(incoming, i)) {
                    lo[i] = normalizeForWeaponSlot(incoming); // 具現化版なら通常版へ戻して収納
                    setLoadout(pouch, lo); return true;
                }
            }
            return false;
        }
        if (!canStore(incoming, t) || !lo[t].isEmpty()) return false;
        ItemStack one = incoming.copy();
        one.setCount(1);
        lo[t] = one;
        setLoadout(pouch, lo);
        return true;
    }

    /** 最後の収納物を 1 個取り出す ( LIFO )。 空なら EMPTY。 */
    private static ItemStack popOne(ItemStack pouch) {
        ItemStack[] lo = getLoadout(pouch);
        for (int i = SLOTS - 1; i >= 0; i--) {
            if (!lo[i].isEmpty()) {
                ItemStack out = lo[i];
                lo[i] = ItemStack.EMPTY;
                setLoadout(pouch, lo);
                return out;
            }
        }
        return ItemStack.EMPTY;
    }

    // --- ローダウトアクセサ ----------------------------------------------

    public static ItemStack[] getLoadout(ItemStack pouch) {
        ItemStack[] out = new ItemStack[SLOTS];
        java.util.Arrays.fill(out, ItemStack.EMPTY);
        CompoundTag tag = pouch.getTag();
        if (tag == null || !tag.contains(TAG_LOADOUT, 10)) return out;
        CompoundTag lo = tag.getCompound(TAG_LOADOUT);
        for (int i = 0; i < SLOTS; i++) {
            String key = Integer.toString(i);
            if (lo.contains(key, 10)) {
                ItemStack s = ItemStack.of(lo.getCompound(key));
                if (!s.isEmpty()) out[i] = s;
            }
        }
        return out;
    }

    public static void setLoadout(ItemStack pouch, ItemStack[] stacks) {
        CompoundTag lo = new CompoundTag();
        for (int i = 0; i < SLOTS && i < stacks.length; i++) {
            ItemStack s = stacks[i];
            if (s != null && !s.isEmpty()) {
                lo.put(Integer.toString(i), s.save(new CompoundTag()));
            }
        }
        pouch.getOrCreateTag().put(TAG_LOADOUT, lo);
    }

    public static int getStoredCount(ItemStack pouch) {
        int n = 0;
        for (ItemStack s : getLoadout(pouch)) if (!s.isEmpty()) n++;
        return n;
    }

    public static boolean isDeployed(ItemStack pouch) {
        CompoundTag tag = pouch.getTag();
        return tag != null && tag.getBoolean(TAG_DEPLOYED);
    }

    public static void setDeployed(ItemStack pouch, boolean deployed) {
        pouch.getOrCreateTag().putBoolean(TAG_DEPLOYED, deployed);
    }

    /**
     * スロット i の「空気置換」 フラグ ( bit フラグ )。
     * ON のとき: 結晶化で そのスロットの中身が空でも、 着用中の防具を空気に置換して退避する。
     */
    public static final String TAG_REPLACE_AIR = "ReplaceAir";

    public static boolean getReplaceAir(ItemStack pouch, int i) {
        CompoundTag tag = pouch.getTag();
        if (tag == null) return false;
        return ((tag.getInt(TAG_REPLACE_AIR) >> i) & 1) != 0;
    }

    public static void setReplaceAir(ItemStack pouch, int i, boolean on) {
        CompoundTag tag = pouch.getOrCreateTag();
        int flags = tag.getInt(TAG_REPLACE_AIR);
        if (on) flags |= (1 << i); else flags &= ~(1 << i);
        tag.putInt(TAG_REPLACE_AIR, flags);
    }

    public static void toggleReplaceAir(ItemStack pouch, int i) {
        setReplaceAir(pouch, i, !getReplaceAir(pouch, i));
    }

    // --- 武器スロット ( メイン/オフ ) の deploy 用 ---------------------------

    /** 武器スロットに 1 つでも中身があるか ( = 結晶化で手に装着する内容がある )。 */
    public static boolean hasWeaponLoadout(ItemStack pouch) {
        ItemStack[] lo = getLoadout(pouch);
        return !lo[SLOT_MAINHAND].isEmpty() || !lo[SLOT_OFFHAND].isEmpty();
    }

    private static final String TAG_STASH_MAIN = "StashMain";
    private static final String TAG_STASH_OFF  = "StashOff";

    /** deploy 時に手から退避した元武器を保存 ( main=true: メイン手 )。 */
    public static void setStash(ItemStack pouch, boolean main, ItemStack stack) {
        CompoundTag tag = pouch.getOrCreateTag();
        String key = main ? TAG_STASH_MAIN : TAG_STASH_OFF;
        if (stack == null || stack.isEmpty()) tag.remove(key);
        else tag.put(key, stack.save(new CompoundTag()));
    }

    public static ItemStack getStash(ItemStack pouch, boolean main) {
        CompoundTag tag = pouch.getTag();
        String key = main ? TAG_STASH_MAIN : TAG_STASH_OFF;
        if (tag == null || !tag.contains(key, 10)) return ItemStack.EMPTY;
        return ItemStack.of(tag.getCompound(key));
    }

    public static void clearStash(ItemStack pouch, boolean main) {
        CompoundTag tag = pouch.getTag();
        if (tag != null) tag.remove(main ? TAG_STASH_MAIN : TAG_STASH_OFF);
    }

    /**
     * 「ポーチ由来」 刻印 ( 所有者 UUID )。 ポーチから出した装備に刻んでおくと、
     * Magical Katana の破壊 ( returnToPouch ) で 確実に見つけて回収できる。
     */
    public static final String TAG_FROM_POUCH = "PouchOwner";

    public static void stampFromPouch(ItemStack stack, java.util.UUID owner) {
        if (stack.isEmpty() || owner == null) return;
        stack.getOrCreateTag().putUUID(TAG_FROM_POUCH, owner);
    }

    public static boolean isFromPouch(ItemStack stack, java.util.UUID owner) {
        if (stack.isEmpty()) return false;
        CompoundTag t = stack.getTag();
        return t != null && t.hasUUID(TAG_FROM_POUCH) && t.getUUID(TAG_FROM_POUCH).equals(owner);
    }

    public static void clearFromPouch(ItemStack stack) {
        CompoundTag t = stack.getTag();
        if (t != null) t.remove(TAG_FROM_POUCH);
    }

    public static ItemStack findFirst(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof MaterializedPouchItem) return s;
        }
        return ItemStack.EMPTY;
    }

    // --- 収納バー / ツールチップ ------------------------------------------

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getStoredCount(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.min(1 + 12 * getStoredCount(stack) / SLOTS, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return isDeployed(stack) ? 0x444444 : 0xBF1A8C;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§7▶ 保管中: §f" + getStoredCount(stack) + "§8 / " + SLOTS));
        if (isDeployed(stack)) {
            tooltip.add(Component.literal("§c▶ 呼び出し中 §7( R キー破壊で元装備に戻す )"));
        }
        // 中身を一覧表示
        ItemStack[] lo = getLoadout(stack);
        for (ItemStack s : lo) {
            if (!s.isEmpty()) {
                tooltip.add(Component.literal("§8• ").append(s.getHoverName()));
            }
        }
        tooltip.add(Component.literal("§8バンドル式: ポーチ↔アイテムを右クリックで出し入れ"));
        tooltip.add(Component.literal("§8呼び出し中は取り出しのみ可 ( 収納不可 )"));
        tooltip.add(Component.literal("§8Magical Katana の結晶破壊で具現化装着 ( 1 回 )"));
        tooltip.add(Component.literal("§8R キー: 具現化装備を破壊して元装備に戻す"));
    }
}
