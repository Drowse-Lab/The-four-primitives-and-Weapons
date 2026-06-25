package the_four_primitives_and_weapons.item;

import the_four_primitives_and_weapons.events.MagicalKatanaCrystalHandler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
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

    /** 収納枠数 ( 防具一式 = 4 ) */
    public static final int SLOTS = 4;
    /** 各枠を装着する EquipmentSlot ( deploy 時に index 順で着用 ) */
    public static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public static final String TAG_LOADOUT  = "Loadout";
    public static final String TAG_DEPLOYED = "Deployed";

    public MaterializedPouchItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    // --- 収納可否 -------------------------------------------------------

    /** 収納できるアイテムか ( ポーチ自身 / 具現化版 / 空 以外なら何でも可 ) */
    public static boolean canStore(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof MaterializedPouchItem) return false;     // 入れ子防止
        if (MagicalKatanaCrystalHandler.isAnyMaterialized(stack)) return false; // 具現化版は不可
        return true;
    }

    /** ( LoadoutPouchHelper 互換 ) スロット番号指定版。 種別は問わず収納可。 */
    public static boolean canStore(ItemStack stack, int slotIndex) {
        if (stack.isEmpty()) return true;
        if (slotIndex < 0 || slotIndex >= SLOTS) return false;
        return canStore(stack);
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
            ItemStack rem = slot.safeInsert(popped);
            if (!rem.isEmpty()) pushOne(pouch, rem); // 入りきらない分は戻す
            playRemove(player);
            return true;
        }
        // 収納 — 結晶化アイテムがインベントリにある間は不可
        if (isInsertLocked(pouch, player)) return false;
        if (!canStore(target)) return false;
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
            access.set(popped);
            playRemove(player);
            return true;
        }
        // 収納 — 結晶化アイテムがインベントリにある間は不可
        if (isInsertLocked(pouch, player)) return false;
        if (!canStore(cursor)) return false;
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
        if (isDeployed(pouch)) return true;
        if (player != null) {
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (MagicalKatanaCrystalHandler.isAnyMaterialized(inv.getItem(i))) return true;
            }
        }
        return false;
    }

    private static void playInsert(Player p) {
        p.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 0.8f + p.getRandom().nextFloat() * 0.4f);
    }

    private static void playRemove(Player p) {
        p.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 0.8f + p.getRandom().nextFloat() * 0.4f);
    }

    /** 最初の空き枠に 1 個収納 ( 成功で true、 満杯で false )。 */
    private static boolean pushOne(ItemStack pouch, ItemStack incoming) {
        ItemStack[] lo = getLoadout(pouch);
        for (int i = 0; i < SLOTS; i++) {
            if (lo[i].isEmpty()) {
                ItemStack one = incoming.copy();
                one.setCount(1);
                lo[i] = one;
                setLoadout(pouch, lo);
                return true;
            }
        }
        return false;
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
        tooltip.add(Component.literal("§8結晶化アイテム所持中は取り出しのみ可 ( 収納不可 )"));
        tooltip.add(Component.literal("§8Magical Katana の結晶破壊で具現化装着 ( 1 回 )"));
        tooltip.add(Component.literal("§8R キー: 具現化装備を破壊して元装備に戻す"));
    }
}
