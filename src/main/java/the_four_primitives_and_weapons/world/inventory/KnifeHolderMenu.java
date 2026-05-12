package the_four_primitives_and_weapons.world.inventory;

import the_four_primitives_and_weapons.init.MawExtraMenus;
import the_four_primitives_and_weapons.item.KnifeLauncherItem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * ナイフホルダーの内蔵在庫を表示・編集するコンテナメニュー。
 *
 *   - 8×2 = 16 スロット: 各スロットに最大 64 本の投げナイフ (同種マージで合計 1024 本)
 *   - プレイヤーインベントリ 3×9 + ホットバー 9
 *
 * スロット変更のたびに NBT へ書き戻すので、GUI を閉じれば即座に保存される。
 */
public class KnifeHolderMenu extends AbstractContainerMenu {

    public static final int HOLDER_COLS = 8;
    public static final int HOLDER_ROWS = 2;
    public static final int HOLDER_SIZE = HOLDER_COLS * HOLDER_ROWS; // 16
    public static final int PER_SLOT_MAX = KnifeLauncherItem.PER_ENTRY_MAX; // 64

    private final Player player;
    private final InteractionHand hand;
    private final Container holderContainer;

    /** Client-side / network コンストラクタ */
    public KnifeHolderMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv,
            (extraData != null && extraData.isReadable() && extraData.readBoolean())
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    /** Server-side コンストラクタ (openMenu から直接呼ばれる) */
    public KnifeHolderMenu(int id, Inventory playerInv, InteractionHand hand) {
        super(MawExtraMenus.KNIFE_HOLDER.get(), id);
        this.player = playerInv.player;
        this.hand = hand;

        // NBT から内容を SimpleContainer に読み込む
        this.holderContainer = new SimpleContainer(HOLDER_SIZE);
        ItemStack holder = player.getItemInHand(hand);
        if (holder.getItem() instanceof KnifeLauncherItem) {
            loadFromHolder(holder);
        }

        // ホルダーのスロット行 (8×2)
        for (int row = 0; row < HOLDER_ROWS; row++) {
            for (int col = 0; col < HOLDER_COLS; col++) {
                int idx = row * HOLDER_COLS + col;
                this.addSlot(new KnifeSlot(holderContainer, idx, 8 + col * 18, 18 + row * 18));
            }
        }

        // プレイヤーインベントリ (3×9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + (row + 1) * 9,
                    8 + col * 18, 68 + row * 18));
            }
        }
        // ホットバー
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 126));
        }
    }

    // --- ホルダー NBT ↔ Container --------------------------------

    private void loadFromHolder(ItemStack holder) {
        CompoundTag tag = holder.getTag();
        if (tag == null || !tag.contains(KnifeLauncherItem.TAG_STORED, Tag.TAG_LIST)) return;
        ListTag list = tag.getList(KnifeLauncherItem.TAG_STORED, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && i < HOLDER_SIZE; i++) {
            ItemStack stack = ItemStack.of(list.getCompound(i));
            if (!stack.isEmpty()) {
                holderContainer.setItem(i, stack);
            }
        }
    }

    private void saveToHolder() {
        ItemStack holder = player.getItemInHand(hand);
        if (!(holder.getItem() instanceof KnifeLauncherItem)) return;
        ListTag list = new ListTag();
        for (int i = 0; i < HOLDER_SIZE; i++) {
            ItemStack s = holderContainer.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                s.save(entry);
                list.add(entry);
            }
        }
        holder.getOrCreateTag().put(KnifeLauncherItem.TAG_STORED, list);
    }

    @Override
    public void slotsChanged(Container c) {
        super.slotsChanged(c);
        if (c == holderContainer) saveToHolder();
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        saveToHolder();
    }

    @Override
    public boolean stillValid(Player p) {
        // プレイヤーがナイフホルダーを手放したら閉じる
        return p.getItemInHand(hand).getItem() instanceof KnifeLauncherItem;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < HOLDER_SIZE) {
                // ホルダー → プレイヤーインベントリ
                if (!this.moveItemStackTo(stack, HOLDER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // プレイヤーインベントリ → ホルダー (投げナイフ / 爆発ナイフのみ)
                if (!KnifeLauncherItem.isStorableKnife(stack)) return ItemStack.EMPTY;
                if (!this.moveItemStackTo(stack, 0, HOLDER_SIZE, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    /** ホルダー専用スロット: 投げナイフのみ、1 スロット 64 まで */
    private static class KnifeSlot extends Slot {
        public KnifeSlot(Container c, int idx, int x, int y) {
            super(c, idx, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.isEmpty() || KnifeLauncherItem.isStorableKnife(stack);
        }

        @Override
        public int getMaxStackSize() {
            return PER_SLOT_MAX;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return PER_SLOT_MAX;
        }
    }
}
