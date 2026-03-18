package minecraftarmorweapon.world.inventory;

import minecraftarmorweapon.damage.ElementType;
import minecraftarmorweapon.damage.ElementalDamageUtils;
import minecraftarmorweapon.init.RarityForgeRegistration;
import minecraftarmorweapon.item.CorrosionBookItem;
import minecraftarmorweapon.item.ElectricBookItem;
import minecraftarmorweapon.item.HolyBookItem;
import minecraftarmorweapon.item.IceBookItem;
import minecraftarmorweapon.item.rarity.RarityCraftingLogic;
import minecraftarmorweapon.item.rarity.RarityForgeRecipe;
import minecraftarmorweapon.item.rarity.RarityForgeRecipes;
import minecraftarmorweapon.item.rarity.WeaponRarity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * レアリティ解放テーブルメニュー
 * スロット0-1: 触媒, スロット2-10: 3×3クラフトグリッド
 * 右パネルのクラフト候補をクリックして作成
 */
public class RarityForgeMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {

    private static final int CATALYST_SLOTS = 2;
    private static final int GRID_SLOTS = 9;
    private static final int CONTAINER_SLOTS = CATALYST_SLOTS + GRID_SLOTS; // 11

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

        // スロット0-1: 触媒
        this.customSlots.put(0, this.addSlot(new SlotItemHandler(internal, 0, 10, 55)));
        this.customSlots.put(1, this.addSlot(new SlotItemHandler(internal, 1, 40, 55)));

        // スロット2-10: 3×3クラフトグリッド
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++) {
                int slotIndex = CATALYST_SLOTS + r * 3 + c;
                int slotX = 77 + c * 18;
                int slotY = 39 + r * 18;
                this.customSlots.put(slotIndex, this.addSlot(new SlotItemHandler(internal, slotIndex, slotX, slotY)));
            }

        // プレイヤーインベントリ (3×9)
        for (int si = 0; si < 3; ++si)
            for (int sj = 0; sj < 9; ++sj)
                this.addSlot(new Slot(inv, sj + (si + 1) * 9, 59 + sj * 18, 138 + si * 18));

        // ホットバー
        for (int si = 0; si < 9; ++si)
            this.addSlot(new Slot(inv, si, 59 + si * 18, 196));
    }

    public IItemHandler getInternal() {
        return internal;
    }

    /**
     * 3×3グリッド部分をスロット0-8として見せるラッパー
     */
    private IItemHandler getGridHandler() {
        return new IItemHandler() {
            @Override public int getSlots() { return GRID_SLOTS; }
            @Override public ItemStack getStackInSlot(int slot) { return internal.getStackInSlot(slot + CATALYST_SLOTS); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return internal.insertItem(slot + CATALYST_SLOTS, stack, simulate); }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return internal.extractItem(slot + CATALYST_SLOTS, amount, simulate); }
            @Override public int getSlotLimit(int slot) { return internal.getSlotLimit(slot + CATALYST_SLOTS); }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return internal.isItemValid(slot + CATALYST_SLOTS, stack); }
        };
    }

    /**
     * 3×3グリッドのパターンに一致するレシピ一覧を返す
     */
    public List<RarityForgeRecipe> getMatchingRecipes() {
        IItemHandler grid = getGridHandler();
        List<RarityForgeRecipe> matches = new ArrayList<>();
        for (RarityForgeRecipe recipe : RarityForgeRecipes.getAll()) {
            if (recipe.matches(grid)) {
                matches.add(recipe);
            }
        }
        return matches;
    }

    /**
     * クラフト実行（全レシピリストのインデックス指定）
     */
    public void performForge(int recipeIndex) {
        List<RarityForgeRecipe> all = RarityForgeRecipes.getAll();
        if (recipeIndex < 0 || recipeIndex >= all.size()) return;

        RarityForgeRecipe recipe = all.get(recipeIndex);
        IItemHandler grid = getGridHandler();

        // 3×3グリッドパターンチェック
        if (!recipe.matches(grid)) return;

        // 触媒取得（両スロット）
        ItemStack catalyst0 = internal.getStackInSlot(0);
        ItemStack catalyst1 = internal.getStackInSlot(1);

        // レアリティ抽選（2スロット合算）
        WeaponRarity rarity = RarityCraftingLogic.rollRarity(catalyst0, catalyst1);

        // 触媒ボーナス計算（各触媒のティアに応じた攻撃力加算）
        double catalystBonus = RarityCraftingLogic.getCatalystBonus(catalyst0, catalyst1);

        // 結果アイテム作成
        ItemStack result = new ItemStack(recipe.getResult());
        if (recipe.isBookRecipe()) {
            // magic bookレシピ: ElementType + ElementLevel NBTを設定
            ElementType elementType = getElementTypeForBook(recipe.getResult());
            ElementalDamageUtils.setElement(result, elementType, recipe.getElementLevel());
        } else {
            WeaponRarity.setToStack(result, rarity);
            WeaponRarity.setCatalystBonus(result, catalystBonus);
        }

        // グリッドから素材消費
        recipe.consumeIngredients(grid);

        // 触媒消費（各スロット1個ずつ）
        if (!catalyst0.isEmpty()) {
            internal.extractItem(0, 1, false);
        }
        if (!catalyst1.isEmpty()) {
            internal.extractItem(1, 1, false);
        }

        // プレイヤーに付与
        if (!entity.getInventory().add(result)) {
            entity.drop(result, false);
        }

        // サウンド
        if (entity instanceof ServerPlayer sp) {
            sp.level().playSound(null,
                    sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    private ElementType getElementTypeForBook(net.minecraft.world.item.Item item) {
        if (item instanceof IceBookItem)       return ElementType.ICE;
        if (item instanceof ElectricBookItem)  return ElementType.ELECTRIC;
        if (item instanceof CorrosionBookItem) return ElementType.CORROSION;
        if (item instanceof HolyBookItem)      return ElementType.HOLY;
        return ElementType.NONE;
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
            if (index < CONTAINER_SLOTS) {
                // コンテナ → プレイヤー
                if (!this.moveItemStackTo(itemstack1, CONTAINER_SLOTS, this.slots.size(), true))
                    return ItemStack.EMPTY;
                slot.onQuickCraft(itemstack1, itemstack);
            } else {
                // プレイヤー → グリッド優先、次に触媒
                if (!this.moveItemStackTo(itemstack1, CATALYST_SLOTS, CONTAINER_SLOTS, false)
                        && !this.moveItemStackTo(itemstack1, 0, CATALYST_SLOTS, false))
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
            if (!serverPlayer.isAlive() || serverPlayer.hasDisconnected()) {
                for (int j = 0; j < internal.getSlots(); ++j) {
                    playerIn.drop(internal.extractItem(j, internal.getStackInSlot(j).getCount(), false), false);
                }
            } else {
                for (int i = 0; i < internal.getSlots(); ++i) {
                    playerIn.getInventory().placeItemBackInInventory(internal.extractItem(i, internal.getStackInSlot(i).getCount(), false));
                }
            }
        }
    }

    @Override
    public Map<Integer, Slot> get() {
        return customSlots;
    }
}
