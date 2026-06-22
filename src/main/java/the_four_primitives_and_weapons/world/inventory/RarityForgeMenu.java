package the_four_primitives_and_weapons.world.inventory;

import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;
import the_four_primitives_and_weapons.init.RarityForgeRegistration;
import the_four_primitives_and_weapons.item.CorrosionBookItem;
import the_four_primitives_and_weapons.item.ElectricBookItem;
import the_four_primitives_and_weapons.item.HolyBookItem;
import the_four_primitives_and_weapons.item.IceBookItem;
import the_four_primitives_and_weapons.item.MiasmaBookItem;
import the_four_primitives_and_weapons.item.BubbleshotItem;
import the_four_primitives_and_weapons.item.FireballItem;
import the_four_primitives_and_weapons.item.ThunderboltItem;
import the_four_primitives_and_weapons.item.WindStepItem;
import the_four_primitives_and_weapons.item.StormItem;
import the_four_primitives_and_weapons.item.DarknessItem;
import the_four_primitives_and_weapons.item.rarity.RarityCraftingLogic;
import the_four_primitives_and_weapons.item.rarity.RarityForgeRecipe;
import the_four_primitives_and_weapons.item.rarity.RarityForgeRecipes;
import the_four_primitives_and_weapons.item.rarity.WeaponRarity;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * レアリティ解放テーブルメニュー (バニラ作業台スタイル)
 *   スロット 0-1  : 触媒
 *   スロット 2-10 : 3×3 クラフトグリッド
 *   スロット 11   : 結果 (output, 取り出し時に素材消費)
 *
 * グリッド + 触媒の状態に応じて結果スロットに preview が自動表示される。
 * プレイヤーが結果スロットからアイテムを取り出した瞬間に rarity を抽選し、
 * 素材と触媒を消費する。
 */
public class RarityForgeMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {

    private static final int CATALYST_SLOTS = 2;
    private static final int GRID_SLOTS = 9;
    private static final int RESULT_SLOT_INDEX = CATALYST_SLOTS + GRID_SLOTS; // 11
    private static final int CONTAINER_SLOTS = RESULT_SLOT_INDEX + 1;          // 12

    public final Level world;
    public final Player entity;
    public int x, y, z;
    private IItemHandler internal;
    private final Map<Integer, Slot> customSlots = new HashMap<>();
    private boolean bound = false;
    /** 現在 grid にマッチしている recipe (result スロットに preview 中)。なければ null。 */
    private RarityForgeRecipe currentRecipe = null;

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

        // スロット11: 結果スロット (output専用)
        this.customSlots.put(RESULT_SLOT_INDEX,
                this.addSlot(new ResultSlot(internal, RESULT_SLOT_INDEX, 167, 57)));

        // プレイヤーインベントリ (3×9)
        for (int si = 0; si < 3; ++si)
            for (int sj = 0; sj < 9; ++sj)
                this.addSlot(new Slot(inv, sj + (si + 1) * 9, 59 + sj * 18, 138 + si * 18));

        // ホットバー
        for (int si = 0; si < 9; ++si)
            this.addSlot(new Slot(inv, si, 59 + si * 18, 196));

        // 初回 preview 計算
        updateResultPreview();
    }

    public IItemHandler getInternal() {
        return internal;
    }

    /** 3×3グリッド部分をスロット0-8として見せるラッパー */
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

    /** 3×3 グリッドのパターンにマッチするレシピ一覧を返す (互換 API、 JEI / Screen が参照) */
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
     * グリッド / 触媒の変化に応じて結果スロットを再計算。バニラ CraftingMenu#slotsChanged 相当。
     */
    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        updateResultPreview();
    }

    private void updateResultPreview() {
        // クライアント側でも preview は表示したいので両側で計算する
        // (RarityForgeRecipes は data pack 経由で両側に load 済み)
        IItemHandler grid = getGridHandler();
        currentRecipe = null;
        for (RarityForgeRecipe r : RarityForgeRecipes.getAll()) {
            if (!r.matches(grid)) continue;
            // catalyst-driven book recipe は触媒スロット 0 に有効な触媒がないとマッチ扱いしない
            if (r.hasCatalystLevels()) {
                ItemStack cat = internal.getStackInSlot(0);
                if (cat.isEmpty() || r.getLevelForCatalyst(cat.getItem()) <= 0) {
                    continue;
                }
            }
            currentRecipe = r;
            break;
        }
        ItemStack preview = (currentRecipe != null)
                ? buildPreviewResult(currentRecipe, grid)
                : ItemStack.EMPTY;
        internal.extractItem(RESULT_SLOT_INDEX, internal.getStackInSlot(RESULT_SLOT_INDEX).getCount(), false);
        if (!preview.isEmpty()) {
            internal.insertItem(RESULT_SLOT_INDEX, preview, false);
        }
    }

    /**
     * preview 用の結果スタックを構築。
     * - book / unbreakable レシピは確定値 (rarity 抽選なし)
     * - 通常武器レシピは base item のみ (rarity / catalyst bonus は onTake で適用)
     */
    private ItemStack buildPreviewResult(RarityForgeRecipe recipe, IItemHandler grid) {
        if (recipe.isUnbreakable()) {
            int inputIdx = recipe.getInputSlotIndex();
            if (inputIdx < 0) return ItemStack.EMPTY;
            int[] offset = recipe.findMatchOffset(grid);
            if (offset == null) return ItemStack.EMPTY;
            int patW = recipe.getPatternWidth();
            int py = inputIdx / patW;
            int px = inputIdx % patW;
            int actualSlot = (py + offset[1]) * 3 + (px + offset[0]);
            ItemStack inputItem = grid.getStackInSlot(actualSlot);
            if (inputItem.isEmpty()) return ItemStack.EMPTY;
            ItemStack result = inputItem.copy();
            result.getOrCreateTag().putBoolean("Unbreakable", true);
            result.setCount(1);
            return result;
        } else if (recipe.isBookRecipe()) {
            ItemStack result = new ItemStack(recipe.getResult());
            ElementType elementType = getElementTypeForBook(recipe.getResult());
            // catalyst-driven の場合は触媒スロット 0 の item でレベルを決める
            int lvl = recipe.getElementLevel();
            if (recipe.hasCatalystLevels()) {
                ItemStack cat = internal.getStackInSlot(0);
                if (!cat.isEmpty()) {
                    int lookup = recipe.getLevelForCatalyst(cat.getItem());
                    if (lookup > 0) lvl = lookup;
                }
            }
            ElementalDamageUtils.setElement(result, elementType, lvl);
            return result;
        } else {
            return new ItemStack(recipe.getResult());
        }
    }

    /**
     * 旧 API: 右パネルから recipeIndex で craft する旧フローの互換。
     * 新フロー (結果スロットから取り出し) では使わないが、互換のため残す。
     */
    public void performForge(int recipeIndex) {
        List<RarityForgeRecipe> all = RarityForgeRecipes.getAll();
        if (recipeIndex < 0 || recipeIndex >= all.size()) return;
        RarityForgeRecipe recipe = all.get(recipeIndex);
        IItemHandler grid = getGridHandler();
        if (!recipe.matches(grid)) return;

        ItemStack result = finalizeResult(recipe, grid);
        if (result.isEmpty()) return;
        consumeIngredientsAndCatalysts(recipe, grid);
        if (!entity.getInventory().add(result)) {
            entity.drop(result, false);
        }
        playForgeSound();
        updateResultPreview();
    }

    /** 取り出し時の最終結果 (rarity 抽選 + bonus 適用) を作る。 */
    private ItemStack finalizeResult(RarityForgeRecipe recipe, IItemHandler grid) {
        if (recipe.isUnbreakable() || recipe.isBookRecipe()) {
            return buildPreviewResult(recipe, grid);
        }
        ItemStack cat0 = internal.getStackInSlot(0);
        ItemStack cat1 = internal.getStackInSlot(1);
        WeaponRarity rarity = RarityCraftingLogic.rollRarity(cat0, cat1);
        double bonus = RarityCraftingLogic.getCatalystBonus(cat0, cat1);
        ItemStack result = new ItemStack(recipe.getResult());
        WeaponRarity.setToStack(result, rarity);
        WeaponRarity.setCatalystBonus(result, bonus);
        return result;
    }

    private void consumeIngredientsAndCatalysts(RarityForgeRecipe recipe, IItemHandler grid) {
        recipe.consumeIngredients(grid);
        if (!internal.getStackInSlot(0).isEmpty()) internal.extractItem(0, 1, false);
        if (!internal.getStackInSlot(1).isEmpty()) internal.extractItem(1, 1, false);
    }

    private void playForgeSound() {
        if (entity instanceof ServerPlayer sp) {
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    private ElementType getElementTypeForBook(net.minecraft.world.item.Item item) {
        if (item instanceof IceBookItem)       return ElementType.ICE;
        if (item instanceof ElectricBookItem)  return ElementType.ELECTRIC;
        if (item instanceof CorrosionBookItem) return ElementType.CORROSION;
        if (item instanceof HolyBookItem)      return ElementType.HOLY;
        if (item instanceof MiasmaBookItem)    return ElementType.MIASMA;
        if (item instanceof BubbleshotItem)    return ElementType.WATER;
        if (item instanceof FireballItem)      return ElementType.FIRE;
        if (item instanceof ThunderboltItem)   return ElementType.THUNDER;
        if (item instanceof WindStepItem)      return ElementType.WIND;
        if (item instanceof StormItem)         return ElementType.THUNDER; // storm = 雷雨系
        if (item instanceof DarknessItem)      return ElementType.DARK;
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
            if (index == RESULT_SLOT_INDEX) {
                // 結果スロット → プレイヤー: onTake が消費処理
                if (!this.moveItemStackTo(itemstack1, CONTAINER_SLOTS, this.slots.size(), true))
                    return ItemStack.EMPTY;
                slot.onQuickCraft(itemstack1, itemstack);
                slot.onTake(playerIn, itemstack1);
            } else if (index < CONTAINER_SLOTS) {
                // 触媒 / グリッド → プレイヤー
                if (!this.moveItemStackTo(itemstack1, CONTAINER_SLOTS, this.slots.size(), true))
                    return ItemStack.EMPTY;
                slot.onQuickCraft(itemstack1, itemstack);
            } else {
                // プレイヤー → グリッド優先、次に触媒
                if (!this.moveItemStackTo(itemstack1, CATALYST_SLOTS, RESULT_SLOT_INDEX, false)
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
            // 結果スロットは drop しない (preview なので)
            internal.extractItem(RESULT_SLOT_INDEX, internal.getStackInSlot(RESULT_SLOT_INDEX).getCount(), false);
            if (!serverPlayer.isAlive() || serverPlayer.hasDisconnected()) {
                for (int j = 0; j < RESULT_SLOT_INDEX; ++j) {
                    playerIn.drop(internal.extractItem(j, internal.getStackInSlot(j).getCount(), false), false);
                }
            } else {
                for (int i = 0; i < RESULT_SLOT_INDEX; ++i) {
                    playerIn.getInventory().placeItemBackInInventory(internal.extractItem(i, internal.getStackInSlot(i).getCount(), false));
                }
            }
        }
    }

    @Override
    public Map<Integer, Slot> get() {
        return customSlots;
    }

    /**
     * 結果スロット: 出力専用 + 取り出し時に rarity 抽選 + 素材消費を行う。
     * バニラ ResultSlot 相当。
     */
    private class ResultSlot extends SlotItemHandler {
        public ResultSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false; // 出力スロットなので置けない
        }

        @Override
        public boolean mayPickup(Player player) {
            return !getItem().isEmpty() && currentRecipe != null;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            if (currentRecipe != null) {
                IItemHandler grid = getGridHandler();
                // 通常武器レシピのみ rarity を上書き (book/unbreakable は preview がそのまま結果)
                if (!currentRecipe.isUnbreakable() && !currentRecipe.isBookRecipe()) {
                    ItemStack cat0 = internal.getStackInSlot(0);
                    ItemStack cat1 = internal.getStackInSlot(1);
                    WeaponRarity rarity = RarityCraftingLogic.rollRarity(cat0, cat1);
                    double bonus = RarityCraftingLogic.getCatalystBonus(cat0, cat1);
                    WeaponRarity.setToStack(stack, rarity);
                    WeaponRarity.setCatalystBonus(stack, bonus);
                }
                consumeIngredientsAndCatalysts(currentRecipe, grid);
                playForgeSound();
            }
            super.onTake(player, stack);
            // 次の preview を更新
            updateResultPreview();
        }
    }
}
