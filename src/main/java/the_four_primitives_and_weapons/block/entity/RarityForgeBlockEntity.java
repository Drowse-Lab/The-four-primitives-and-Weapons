package the_four_primitives_and_weapons.block.entity;

import the_four_primitives_and_weapons.init.RarityForgeRegistration;
import the_four_primitives_and_weapons.world.inventory.RarityForgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import javax.annotation.Nullable;
import java.util.stream.IntStream;

import io.netty.buffer.Unpooled;

/**
 * レアリティ解放テーブルのBlockEntity ( hybrid 版 )
 *   スロット 0,1   : 触媒 / 媒体 ( 強化モードでは 0=媒体, 1=触媒 )
 *   スロット 2-10 : 3×3 クラフトグリッド ( バニラレシピを引く )
 *   スロット 11   : 結果 ( output 専用 )
 */
public class RarityForgeBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {

    public static final int TOTAL_SLOTS = 12;
    public static final int GRID_START  = 2;
    public static final int GRID_SIZE   = 9;
    public static final int RESULT_SLOT = 11;

    private NonNullList<ItemStack> stacks = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private final LazyOptional<? extends IItemHandler>[] handlers = SidedInvWrapper.create(this, Direction.values());
    /** facing=null (= Menu からの問い合わせ) 用の sided でないラッパー。 BlockEntity の storage に直結。 */
    private final LazyOptional<InvWrapper> internalHandler = LazyOptional.of(() -> new InvWrapper(this));

    public RarityForgeBlockEntity(BlockPos position, BlockState state) {
        super(RarityForgeRegistration.getBlockEntityType(), position, state);
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        if (!this.tryLoadLootTable(compound))
            this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(compound, this.stacks);
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        if (!this.trySaveLootTable(compound)) {
            ContainerHelper.saveAllItems(compound, this.stacks);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithFullMetadata();
    }

    @Override
    public int getContainerSize() {
        return stacks.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.stacks)
            if (!itemstack.isEmpty())
                return false;
        return true;
    }

    @Override
    public Component getDefaultName() {
        return Component.literal("rarity_forge");
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new RarityForgeMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.the_four_primitives_and_weapons.rarity_forge.title");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.stacks;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        // Menu からの内部操作 ( InvWrapper / facing=null ) は全 slot 許可。
        // 結果スロットへの preview 挿入を Menu 側で行うため。
        // 外部 ( ホッパー等 facing!=null ) は canPlaceItemThroughFace 側で弾く。
        return index < TOTAL_SLOTS;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return IntStream.range(0, this.getContainerSize()).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        // 外部 ( ホッパー等 ) からは結果スロットへ投入不可
        return index < TOTAL_SLOTS && index != RESULT_SLOT;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (!this.remove && capability == ForgeCapabilities.ITEM_HANDLER) {
            // facing=null (Menu からの問い合わせ) は sided でないラッパーを返す。
            // 過去はここで empty を返していて、 Menu が分離した ItemStackHandler を持って
            // BlockEntity の storage と切り離されてた (= preview / craft 失敗のバグ)。
            if (facing == null) return internalHandler.cast();
            return handlers[facing.ordinal()].cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        for (LazyOptional<? extends IItemHandler> handler : handlers)
            handler.invalidate();
        internalHandler.invalidate();
    }
}
