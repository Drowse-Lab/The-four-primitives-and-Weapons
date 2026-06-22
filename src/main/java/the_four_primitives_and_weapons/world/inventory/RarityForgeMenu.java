package the_four_primitives_and_weapons.world.inventory;

import the_four_primitives_and_weapons.init.RarityForgeRegistration;
import the_four_primitives_and_weapons.item.rarity.RarityForgeCenterLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * シンプル化レアリティ解放テーブル メニュー
 *   スロット 0,1 : 触媒
 *   スロット 2   : 中央 ( 武器 / 魔導書 )
 *   スロット 3   : 結果 ( output 専用 )
 *
 * 中央 + 触媒 の組み合わせで RarityForgeCenterLogic が preview を作る。
 * 取り出し時に finalize ( rarity 抽選 ) + 各スロット 1 個ずつ消費。
 */
public class RarityForgeMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {

    public static final int CAT_SLOT_0 = 0;
    public static final int CAT_SLOT_1 = 1;
    public static final int CENTER_SLOT = 2;
    public static final int RESULT_SLOT = 3;
    public static final int CONTAINER_SLOTS = 4;

    public final Level world;
    public final Player entity;
    public int x, y, z;
    private IItemHandler internal;
    private final Map<Integer, Slot> customSlots = new HashMap<>();
    private boolean bound = false;

    public RarityForgeMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(RarityForgeRegistration.getMenuType(), id);
        this.entity = inv.player;
        this.world = inv.player.level();
        this.internal = new ItemStackHandler(CONTAINER_SLOTS);
        BlockPos pos = null;
        if (extraData != null) {
            pos = extraData.readBlockPos();
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
        }
        if (pos != null) {
            BlockEntity ent = inv.player.level().getBlockEntity(pos);
            if (ent != null) {
                ent.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(capability -> {
                    this.internal = capability;
                    this.bound = true;
                });
            }
        }

        // 触媒 ×2 (左)
        this.customSlots.put(CAT_SLOT_0, this.addSlot(new SlotItemHandler(internal, CAT_SLOT_0, 20, 56)));
        this.customSlots.put(CAT_SLOT_1, this.addSlot(new SlotItemHandler(internal, CAT_SLOT_1, 44, 56)));
        // 中央スロット
        this.customSlots.put(CENTER_SLOT, this.addSlot(new SlotItemHandler(internal, CENTER_SLOT, 96, 56)));
        // 結果スロット
        this.customSlots.put(RESULT_SLOT, this.addSlot(new ResultSlot(internal, RESULT_SLOT, 152, 56)));

        // プレイヤーインベントリ ( 3x9 )
        for (int si = 0; si < 3; ++si)
            for (int sj = 0; sj < 9; ++sj)
                this.addSlot(new Slot(inv, sj + (si + 1) * 9, 8 + sj * 18, 100 + si * 18));
        // ホットバー
        for (int si = 0; si < 9; ++si)
            this.addSlot(new Slot(inv, si, 8 + si * 18, 158));

        updateResultPreview();
    }

    public IItemHandler getInternal() {
        return internal;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        updateResultPreview();
    }

    private void updateResultPreview() {
        ItemStack center = internal.getStackInSlot(CENTER_SLOT);
        ItemStack cat0   = internal.getStackInSlot(CAT_SLOT_0);
        ItemStack cat1   = internal.getStackInSlot(CAT_SLOT_1);
        ItemStack preview = RarityForgeCenterLogic.buildPreview(center, cat0, cat1);
        internal.extractItem(RESULT_SLOT, internal.getStackInSlot(RESULT_SLOT).getCount(), false);
        if (!preview.isEmpty()) {
            internal.insertItem(RESULT_SLOT, preview, false);
        }
    }

    private void consumeAfterTake() {
        // 中央 1 + 触媒 ( 入ってる方 ) 1 ずつ
        if (!internal.getStackInSlot(CENTER_SLOT).isEmpty()) internal.extractItem(CENTER_SLOT, 1, false);
        if (!internal.getStackInSlot(CAT_SLOT_0).isEmpty()) internal.extractItem(CAT_SLOT_0, 1, false);
        if (!internal.getStackInSlot(CAT_SLOT_1).isEmpty()) internal.extractItem(CAT_SLOT_1, 1, false);
    }

    private void playForgeSound() {
        if (entity instanceof ServerPlayer sp) {
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index == RESULT_SLOT) {
                if (!this.moveItemStackTo(itemstack1, CONTAINER_SLOTS, this.slots.size(), true))
                    return ItemStack.EMPTY;
                slot.onQuickCraft(itemstack1, itemstack);
                slot.onTake(playerIn, itemstack1);
            } else if (index < CONTAINER_SLOTS) {
                if (!this.moveItemStackTo(itemstack1, CONTAINER_SLOTS, this.slots.size(), true))
                    return ItemStack.EMPTY;
                slot.onQuickCraft(itemstack1, itemstack);
            } else {
                // プレイヤー → 中央 ( 武器/魔導書 ) を優先、 次に触媒
                if (!this.moveItemStackTo(itemstack1, CENTER_SLOT, CENTER_SLOT + 1, false)
                        && !this.moveItemStackTo(itemstack1, CAT_SLOT_0, CAT_SLOT_1 + 1, false))
                    return ItemStack.EMPTY;
            }
            if (itemstack1.getCount() == 0)
                slot.set(ItemStack.EMPTY);
            else
                slot.setChanged();
            if (itemstack1.getCount() == itemstack.getCount())
                return ItemStack.EMPTY;
            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }

    @Override
    public void removed(Player playerIn) {
        super.removed(playerIn);
        if (!bound && playerIn instanceof ServerPlayer serverPlayer) {
            // 結果は preview なので drop しない
            internal.extractItem(RESULT_SLOT, internal.getStackInSlot(RESULT_SLOT).getCount(), false);
            if (!serverPlayer.isAlive() || serverPlayer.hasDisconnected()) {
                for (int j = 0; j < RESULT_SLOT; ++j) {
                    playerIn.drop(internal.extractItem(j, internal.getStackInSlot(j).getCount(), false), false);
                }
            } else {
                for (int i = 0; i < RESULT_SLOT; ++i) {
                    playerIn.getInventory().placeItemBackInInventory(internal.extractItem(i, internal.getStackInSlot(i).getCount(), false));
                }
            }
        }
    }

    @Override
    public Map<Integer, Slot> get() {
        return customSlots;
    }

    private class ResultSlot extends SlotItemHandler {
        public ResultSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return !getItem().isEmpty();
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            ItemStack center = internal.getStackInSlot(CENTER_SLOT);
            ItemStack cat0   = internal.getStackInSlot(CAT_SLOT_0);
            ItemStack cat1   = internal.getStackInSlot(CAT_SLOT_1);
            // RARITY の場合のみ stack を上書き ( 抽選結果を反映 )
            RarityForgeCenterLogic.Mode mode = RarityForgeCenterLogic.resolveMode(center, cat0, cat1);
            if (mode == RarityForgeCenterLogic.Mode.RARITY) {
                ItemStack finalStack = RarityForgeCenterLogic.finalize(center, cat0, cat1);
                stack.setTag(finalStack.getTag());
            }
            consumeAfterTake();
            playForgeSound();
            super.onTake(player, stack);
            updateResultPreview();
        }
    }
}
