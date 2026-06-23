package the_four_primitives_and_weapons.world.inventory;

import the_four_primitives_and_weapons.init.RarityForgeRegistration;
import the_four_primitives_and_weapons.item.rarity.RarityCraftingLogic;
import the_four_primitives_and_weapons.item.rarity.RarityForgeCenterLogic;
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
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Hybrid レアリティ解放テーブル
 *   スロット 0,1   : 触媒 / 媒体 ( 強化モード時は 0=媒体, 1=触媒 )
 *   スロット 2-10 : 3×3 クラフトグリッド ( バニラレシピを引く )
 *   スロット 11   : 結果 ( output 専用 )
 *
 * モード判定:
 *   - グリッドに 1 個でもアイテム有り → クラフトモード
 *     → CraftingRecipe を引いて結果取得、 触媒 2 個があれば追加で rarity/element/unbreakable 付与
 *   - グリッドが空 → 強化モード
 *     → cat0=媒体, cat1=触媒 で既存武器を強化
 */
public class RarityForgeMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {

    public static final int CAT_SLOT_0   = 0;
    public static final int CAT_SLOT_1   = 1;
    public static final int GRID_START   = 2;
    public static final int GRID_SIZE    = 9;
    public static final int RESULT_SLOT  = GRID_START + GRID_SIZE; // 11
    public static final int CONTAINER_SLOTS = RESULT_SLOT + 1;     // 12

    public final Level world;
    public final Player entity;
    public int x, y, z;
    private IItemHandler internal;
    private final Map<Integer, Slot> customSlots = new HashMap<>();
    private boolean bound = false;
    private CraftingRecipe currentRecipe = null;
    /** 旧 RarityForge JSON ( rarity_forge_recipes/ ) でマッチしたレシピ。 */
    private RarityForgeRecipe currentLegacyRecipe = null;
    /** 候補リストから「これを作りたい」 と選ばれたレシピ。 null なら自動 preview。 */
    private CraftingRecipe currentCandidateRecipe = null;
    /** slotsChanged の再帰を防ぐ flag ( TransientCraftingContainer.setItem が menu.slotsChanged を呼ぶ ) */
    private boolean updatingPreview = false;

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

        // 触媒 ×2 ( 左 )
        this.customSlots.put(CAT_SLOT_0, this.addSlot(new SlotItemHandler(internal, CAT_SLOT_0, 16, 37)));
        this.customSlots.put(CAT_SLOT_1, this.addSlot(new SlotItemHandler(internal, CAT_SLOT_1, 16, 65)));
        // 3×3 グリッド ( 中央 )
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int slotIndex = GRID_START + r * 3 + c;
                int sx = 52 + c * 18;
                int sy = 23 + r * 18;
                this.customSlots.put(slotIndex, this.addSlot(new SlotItemHandler(internal, slotIndex, sx, sy)));
            }
        }
        // 結果スロット ( 右 )
        this.customSlots.put(RESULT_SLOT, this.addSlot(new ResultSlot(internal, RESULT_SLOT, 132, 41)));

        // プレイヤーインベントリ ( 3x9 )
        for (int si = 0; si < 3; ++si)
            for (int sj = 0; sj < 9; ++sj)
                this.addSlot(new Slot(inv, sj + (si + 1) * 9, 10 + sj * 18, 102 + si * 18));
        // ホットバー
        for (int si = 0; si < 9; ++si)
            this.addSlot(new Slot(inv, si, 10 + si * 18, 160));

        updateResultPreview();
    }

    public IItemHandler getInternal() {
        return internal;
    }

    /**
     * 旧 RarityForgeRecipe の result stack を構築。 種別ごとに分岐:
     *   - unbreakable : input slot 上の武器 を Unbreakable=true で複製
     *   - book        : 結果 ( 魔導書 ) に catalyst_levels で決まる element Lv を付与
     *                   ( 触媒スロット 0 を見る )
     *   - 通常 shaped : recipe.getResult() の ItemStack
     */
    private ItemStack buildLegacyResult(RarityForgeRecipe lr, IItemHandler grid) {
        if (lr.isUnbreakable()) {
            int inputIdx = lr.getInputSlotIndex();
            if (inputIdx < 0) return ItemStack.EMPTY;
            int[] offset = lr.findMatchOffset(grid);
            if (offset == null) return ItemStack.EMPTY;
            int patW = lr.getPatternWidth();
            int py = inputIdx / patW;
            int px = inputIdx % patW;
            int actualSlot = (py + offset[1]) * 3 + (px + offset[0]);
            ItemStack inputItem = grid.getStackInSlot(actualSlot);
            if (inputItem.isEmpty()) return ItemStack.EMPTY;
            ItemStack out = inputItem.copy();
            out.setCount(1);
            out.getOrCreateTag().putBoolean("Unbreakable", true);
            return out;
        }
        ItemStack base = new ItemStack(lr.getResult());
        if (base.isEmpty()) return ItemStack.EMPTY;
        if (lr.isBookRecipe()) {
            int lvl = lr.getElementLevel();
            if (lr.hasCatalystLevels()) {
                ItemStack cat = internal.getStackInSlot(CAT_SLOT_0);
                if (!cat.isEmpty()) {
                    int lookup = lr.getLevelForCatalyst(cat.getItem());
                    if (lookup > 0) lvl = lookup;
                }
            }
            if (lvl > 0) {
                the_four_primitives_and_weapons.damage.ElementType type =
                        RarityForgeCenterLogic.getBookElement(base);
                if (type != the_four_primitives_and_weapons.damage.ElementType.NONE) {
                    the_four_primitives_and_weapons.damage.ElementalDamageUtils.setElement(base, type, lvl);
                }
            }
        }
        return base;
    }

    /**
     * このテーブルでクラフト可能な武器か判定。
     *   - バニラ SwordItem 系列 ( mod 内 KatanaItem 等の継承も含む )
     *   - weapon_types.json に登録されている任意の武器 type ( katana / rapier / straight_sword / scythe / bow 等 )
     */
    private static boolean isCraftableWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof SwordItem) return true;
        the_four_primitives_and_weapons.skill.WeaponTypeRegistry.WeaponTypeData wt =
                the_four_primitives_and_weapons.skill.WeaponTypeRegistry.getTypeForItem(stack);
        return wt != null;
    }

    /**
     * 現在のグリッドにあるアイテムを 1 個でも ingredient として使う
     * バニラ CraftingRecipe を全部集めて返す ( 部分マッチ候補 )。
     * グリッドが完全に空なら空 list。
     */
    public List<CraftingRecipe> getPartialMatchRecipes() {
        List<CraftingRecipe> results = new ArrayList<>();
        if (world == null) return results;
        boolean any = false;
        ItemStack[] gridStacks = new ItemStack[GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            gridStacks[i] = internal.getStackInSlot(GRID_START + i);
            if (!gridStacks[i].isEmpty()) any = true;
        }
        if (!any) return results;

        for (CraftingRecipe recipe : world.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            // 武器 以外の結果のレシピは候補から除外
            ItemStack out;
            try {
                out = recipe.getResultItem(world.registryAccess());
            } catch (Throwable t) {
                continue;
            }
            if (out.isEmpty() || !isCraftableWeapon(out)) continue;

            boolean match = false;
            for (Ingredient ing : recipe.getIngredients()) {
                if (ing.isEmpty()) continue;
                for (ItemStack s : gridStacks) {
                    if (s.isEmpty()) continue;
                    if (ing.test(s)) { match = true; break; }
                }
                if (match) break;
            }
            if (match) results.add(recipe);
        }
        return results;
    }

    /**
     * グリッドの材料を 1 個でも使うレシピの結果アイテム一覧 ( 重複排除 + 武器のみ )。
     * 旧 rarity_forge_recipes ( legacy ) + バニラ CraftingRecipe を統合表示。
     */
    public List<ItemStack> getPartialMatchResults() {
        List<ItemStack> out = new ArrayList<>();
        if (world == null) return out;
        boolean any = false;
        ItemStack[] gridStacks = new ItemStack[GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            gridStacks[i] = internal.getStackInSlot(GRID_START + i);
            if (!gridStacks[i].isEmpty()) any = true;
        }
        if (!any) return out;

        java.util.Set<String> seen = new java.util.HashSet<>();

        // 旧 RarityForge JSON レシピ ( グリッド完全マッチのみ )
        IItemHandler gridHandler = getGridHandler();
        for (RarityForgeRecipe lr : RarityForgeRecipes.getAll()) {
            if (!lr.matches(gridHandler)) continue;
            ItemStack r = lr.isUnbreakable()
                    ? new ItemStack(net.minecraft.world.item.Items.IRON_SWORD)
                    : new ItemStack(lr.getResult());
            if (r.isEmpty()) continue;
            if (!lr.isBookRecipe() && !lr.isUnbreakable() && !isCraftableWeapon(r)) continue;
            String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(r.getItem()).toString();
            if (seen.add(id)) out.add(r);
        }

        // バニラ CraftingRecipe ( グリッド完全マッチのみ ) — TransientCraftingContainer の
        // setItem が menu.slotsChanged を呼ぶので、 再帰防止 flag を一時的に立てる。
        boolean prevFlag = updatingPreview;
        updatingPreview = true;
        try {
            CraftingContainer cc = new TransientCraftingContainer(this, 3, 3);
            for (int i = 0; i < GRID_SIZE; i++) {
                cc.setItem(i, gridStacks[i].copy());
            }
            for (CraftingRecipe recipe : world.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
                boolean matched;
                try { matched = recipe.matches(cc, world); }
                catch (Throwable t) { continue; }
                if (!matched) continue;
                ItemStack r;
                try { r = recipe.getResultItem(world.registryAccess()); }
                catch (Throwable t) { continue; }
                if (r.isEmpty() || !isCraftableWeapon(r)) continue;
                String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(r.getItem()).toString();
                if (seen.add(id)) out.add(r);
            }
        } finally {
            updatingPreview = prevFlag;
        }
        return out;
    }

    /**
     * 結果アイテム ID で候補レシピを選択。 legacy 優先 → バニラ の順。
     * 該当が無ければ選択解除。
     */
    public void selectCandidateByItemId(String itemId) {
        currentCandidateRecipe = null;
        currentLegacyRecipe = null;
        if (itemId == null || itemId.isEmpty()) {
            updateResultPreview();
            broadcastChanges();
            return;
        }
        net.minecraft.world.item.Item target = net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new net.minecraft.resources.ResourceLocation(itemId));
        if (target == null || target == net.minecraft.world.item.Items.AIR) {
            updateResultPreview();
            broadcastChanges();
            return;
        }
        // 旧 RarityForge JSON 優先
        for (RarityForgeRecipe lr : RarityForgeRecipes.getAll()) {
            if (lr.isUnbreakable()) continue; // unbreakable は input 依存なのでアイテム ID では特定できない
            if (lr.getResult() == target) {
                currentLegacyRecipe = lr;
                updateResultPreview();
                broadcastChanges();
                return;
            }
        }
        // バニラ CraftingRecipe
        if (world != null) {
            for (CraftingRecipe recipe : world.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
                ItemStack r;
                try { r = recipe.getResultItem(world.registryAccess()); }
                catch (Throwable t) { continue; }
                if (r.getItem() == target) {
                    currentCandidateRecipe = recipe;
                    updateResultPreview();
                    broadcastChanges();
                    return;
                }
            }
        }
        updateResultPreview();
        broadcastChanges();
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        // グリッドや触媒が変わったら 候補選択を解除 ( 古い選択が残らないように )
        if (!updatingPreview) currentCandidateRecipe = null;
        updateResultPreview();
    }

    /**
     * 候補リストから index を選択 ( クライアントから呼ばれる )。
     *   - index >= 0 && < 候補数 : 該当レシピを selected として保持
     *   - それ以外               : 選択解除
     */
    public void selectCandidate(int index) {
        if (index < 0) {
            currentCandidateRecipe = null;
        } else {
            List<CraftingRecipe> all = getPartialMatchRecipes();
            if (index >= all.size()) {
                currentCandidateRecipe = null;
            } else {
                currentCandidateRecipe = all.get(index);
            }
        }
        updateResultPreview();
        // 結果スロットの内容を client に同期
        broadcastChanges();
    }

    /** 3×3 グリッド部分 ( 9 slot ) を 0-indexed の IItemHandler として返す。 */
    private IItemHandler getGridHandler() {
        return new IItemHandler() {
            @Override public int getSlots() { return GRID_SIZE; }
            @Override public ItemStack getStackInSlot(int slot) { return internal.getStackInSlot(GRID_START + slot); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return internal.insertItem(GRID_START + slot, stack, simulate); }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return internal.extractItem(GRID_START + slot, amount, simulate); }
            @Override public int getSlotLimit(int slot) { return internal.getSlotLimit(GRID_START + slot); }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return internal.isItemValid(GRID_START + slot, stack); }
        };
    }

    private boolean gridIsEmpty() {
        for (int i = 0; i < GRID_SIZE; i++) {
            if (!internal.getStackInSlot(GRID_START + i).isEmpty()) return false;
        }
        return true;
    }

    /**
     * グリッドの内容を CraftingContainer に詰めてバニラの CraftingRecipe を引く。
     * 取れた結果に対し、 触媒で追加効果 ( element / Unbreakable / rarity ) を被せる。
     */
    private ItemStack buildCraftResult() {
        if (world == null) return ItemStack.EMPTY;

        // === 1) 旧 RarityForge 専用 JSON ( rarity_forge_recipes/ ) を試す ===
        //     book / unbreakable / 通常 shaped すべて対応。
        IItemHandler gridHandler = getGridHandler();
        for (RarityForgeRecipe lr : RarityForgeRecipes.getAll()) {
            if (!lr.matches(gridHandler)) continue;

            ItemStack legacyResult = buildLegacyResult(lr, gridHandler);
            if (legacyResult.isEmpty()) continue;
            // 武器以外フィルタは book / unbreakable は対象外 ( 魔導書も結果として OK )
            if (!lr.isBookRecipe() && !lr.isUnbreakable() && !isCraftableWeapon(legacyResult)) continue;

            currentLegacyRecipe = lr;
            currentRecipe = null;
            // 通常 shaped のみ触媒強化を被せる ( book/unbreakable は legacy 内で完結 )
            if (!lr.isBookRecipe() && !lr.isUnbreakable()) {
                ItemStack lcat0 = internal.getStackInSlot(CAT_SLOT_0);
                ItemStack lcat1 = internal.getStackInSlot(CAT_SLOT_1);
                ItemStack lenhanced = RarityForgeCenterLogic.buildPreview(legacyResult, lcat0, lcat1);
                if (!lenhanced.isEmpty()) return lenhanced;
            }
            return legacyResult;
        }
        currentLegacyRecipe = null;

        // === 2) バニラ CraftingRecipe を試す ===
        CraftingContainer cc = new TransientCraftingContainer(this, 3, 3);
        for (int i = 0; i < GRID_SIZE; i++) {
            cc.setItem(i, internal.getStackInSlot(GRID_START + i).copy());
        }
        Optional<CraftingRecipe> opt = world.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, cc, world);
        if (opt.isEmpty()) {
            currentRecipe = null;
            return ItemStack.EMPTY;
        }
        currentRecipe = opt.get();
        ItemStack craftResult = currentRecipe.assemble(cc, world.registryAccess());
        if (craftResult.isEmpty()) return ItemStack.EMPTY;
        // このテーブルでは武器のみクラフト可能 ( バニラ SwordItem + weapon_types.json 登録 )
        if (!isCraftableWeapon(craftResult)) {
            currentRecipe = null;
            return ItemStack.EMPTY;
        }

        // 触媒で追加効果 ( center logic のクラフトモード分岐を流用 )
        ItemStack cat0 = internal.getStackInSlot(CAT_SLOT_0);
        ItemStack cat1 = internal.getStackInSlot(CAT_SLOT_1);
        ItemStack enhanced = RarityForgeCenterLogic.buildPreview(craftResult, cat0, cat1);
        if (!enhanced.isEmpty()) return enhanced;
        // 触媒なし / 認識外 → ベース結果のみ
        return craftResult;
    }

    private void updateResultPreview() {
        // TransientCraftingContainer.setItem → menu.slotsChanged → updateResultPreview の
        // 再帰を遮断 ( buildCraftResult 内で TransientCraftingContainer.setItem を呼ぶ )
        if (updatingPreview) return;
        updatingPreview = true;
        try {
            currentRecipe = null;
            ItemStack preview;
            // === 0a) 候補リストから選択された旧 RarityForge レシピを優先 ===
            if (currentLegacyRecipe != null && world != null) {
                IItemHandler grid = getGridHandler();
                boolean matched = currentLegacyRecipe.matches(grid);
                internal.extractItem(RESULT_SLOT, internal.getStackInSlot(RESULT_SLOT).getCount(), false);
                if (matched) {
                    ItemStack base = buildLegacyResult(currentLegacyRecipe, grid);
                    if (!base.isEmpty()) {
                        internal.insertItem(RESULT_SLOT, base, false);
                    }
                }
                return;
            }
            // === 0b) 候補リストから選択されたバニラレシピを優先 ( rarity 抜きのベース ) ===
            //   ただし グリッドが レシピと一致する場合のみ preview 表示。
            //   一致しなければ 結果スロットは空 ( = 素材不足を視覚的に伝える )。
            if (currentCandidateRecipe != null && world != null) {
                CraftingContainer cc = new TransientCraftingContainer(this, 3, 3);
                for (int i = 0; i < GRID_SIZE; i++) {
                    cc.setItem(i, internal.getStackInSlot(GRID_START + i).copy());
                }
                boolean matched = false;
                try {
                    matched = currentCandidateRecipe.matches(cc, world);
                } catch (Throwable ignored) {}
                internal.extractItem(RESULT_SLOT, internal.getStackInSlot(RESULT_SLOT).getCount(), false);
                if (matched) {
                    ItemStack base;
                    try {
                        base = currentCandidateRecipe.getResultItem(world.registryAccess()).copy();
                    } catch (Throwable t) {
                        base = ItemStack.EMPTY;
                    }
                    if (!base.isEmpty()) {
                        internal.insertItem(RESULT_SLOT, base, false);
                    }
                }
                return;
            }
            if (!gridIsEmpty()) {
                // クラフトモード
                preview = buildCraftResult();
            } else {
                // 強化モード — cat0=媒体, cat1=触媒
                ItemStack medium = internal.getStackInSlot(CAT_SLOT_0);
                ItemStack cat    = internal.getStackInSlot(CAT_SLOT_1);
                preview = RarityForgeCenterLogic.buildEnhancePreview(medium, cat);
            }
            internal.extractItem(RESULT_SLOT, internal.getStackInSlot(RESULT_SLOT).getCount(), false);
            if (preview != null && !preview.isEmpty()) {
                internal.insertItem(RESULT_SLOT, preview, false);
            }
        } finally {
            updatingPreview = false;
        }
    }

    /** 取り出し時の消費処理。 craft / enhance で分岐。 */
    private void consumeAfterTake(boolean craftMode) {
        if (craftMode) {
            if (currentLegacyRecipe != null) {
                // legacy ( 旧 JSON ) は pattern にマッチした slot のみ消費
                currentLegacyRecipe.consumeIngredients(getGridHandler());
            } else {
                for (int i = 0; i < GRID_SIZE; i++) {
                    ItemStack s = internal.getStackInSlot(GRID_START + i);
                    if (!s.isEmpty()) internal.extractItem(GRID_START + i, 1, false);
                }
            }
            // 触媒は装着されてた場合のみ消費
            if (!internal.getStackInSlot(CAT_SLOT_0).isEmpty()) internal.extractItem(CAT_SLOT_0, 1, false);
            if (!internal.getStackInSlot(CAT_SLOT_1).isEmpty()) internal.extractItem(CAT_SLOT_1, 1, false);
        } else {
            // 強化モード: 媒体 + 触媒 各 1 個
            if (!internal.getStackInSlot(CAT_SLOT_0).isEmpty()) internal.extractItem(CAT_SLOT_0, 1, false);
            if (!internal.getStackInSlot(CAT_SLOT_1).isEmpty()) internal.extractItem(CAT_SLOT_1, 1, false);
        }
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
                // プレイヤー → グリッド優先、 次に触媒
                if (!this.moveItemStackTo(itemstack1, GRID_START, GRID_START + GRID_SIZE, false)
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
            // 候補から legacy ( 旧 RarityForge JSON ) を選んでた場合
            if (currentLegacyRecipe != null) {
                IItemHandler grid = getGridHandler();
                if (!currentLegacyRecipe.matches(grid)) {
                    if (player instanceof ServerPlayer sp) {
                        sp.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                "gui.the_four_primitives_and_weapons.rarity_forge.no_materials")
                                .withStyle(net.minecraft.ChatFormatting.RED), true);
                    }
                    internal.insertItem(RESULT_SLOT, stack.copy(), false);
                    stack.setCount(0);
                    return;
                }
                ItemStack base = buildLegacyResult(currentLegacyRecipe, grid);
                if (!base.isEmpty()) {
                    boolean legacySpecial = currentLegacyRecipe.isBookRecipe() || currentLegacyRecipe.isUnbreakable();
                    if (legacySpecial) {
                        if (base.getTag() != null) stack.setTag(base.getTag());
                    } else {
                        ItemStack lc0 = internal.getStackInSlot(CAT_SLOT_0);
                        ItemStack lc1 = internal.getStackInSlot(CAT_SLOT_1);
                        ItemStack finalStack = RarityForgeCenterLogic.finalize(base, lc0, lc1);
                        if (finalStack.getTag() != null) stack.setTag(finalStack.getTag());
                    }
                }
                consumeAfterTake(true);
                playForgeSound();
                currentLegacyRecipe = null;
                super.onTake(player, stack);
                updateResultPreview();
                return;
            }

            // 候補リストから「これを作る」 を選んでた場合は
            //   1. グリッドが該当レシピと **正確にマッチ** しているなら → rarity 抽選 + 素材消費 + 取得
            //   2. マッチしてない場合は → 取得を拒否 ( stack を返す + メッセージ )
            if (currentCandidateRecipe != null) {
                CraftingContainer cc = new TransientCraftingContainer(RarityForgeMenu.this, 3, 3);
                for (int i = 0; i < GRID_SIZE; i++) {
                    cc.setItem(i, internal.getStackInSlot(GRID_START + i).copy());
                }
                boolean matched = false;
                try {
                    matched = currentCandidateRecipe.matches(cc, world);
                } catch (Throwable ignored) {}
                if (!matched) {
                    // 素材が揃ってない / 配置が違う → 取得拒否
                    if (player instanceof ServerPlayer sp) {
                        sp.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                "gui.the_four_primitives_and_weapons.rarity_forge.no_materials")
                                .withStyle(net.minecraft.ChatFormatting.RED), true);
                    }
                    // 取得した stack を内部に戻す
                    internal.insertItem(RESULT_SLOT, stack.copy(), false);
                    stack.setCount(0);
                    return;
                }

                ItemStack cat0 = internal.getStackInSlot(CAT_SLOT_0);
                ItemStack cat1 = internal.getStackInSlot(CAT_SLOT_1);
                ItemStack baseResult = currentCandidateRecipe.assemble(cc, world.registryAccess());
                if (!baseResult.isEmpty()) {
                    ItemStack finalStack = RarityForgeCenterLogic.finalize(baseResult, cat0, cat1);
                    if (!finalStack.isEmpty() && finalStack.getTag() != null) {
                        stack.setTag(finalStack.getTag());
                    }
                }
                consumeAfterTake(true);
                playForgeSound();
                currentCandidateRecipe = null;
                super.onTake(player, stack);
                updateResultPreview();
                return;
            }

            boolean craftMode = !gridIsEmpty();
            // RARITY 抽選を反映 ( craft / enhance 両方 )
            ItemStack cat0 = internal.getStackInSlot(CAT_SLOT_0);
            ItemStack cat1 = internal.getStackInSlot(CAT_SLOT_1);
            if (craftMode) {
                ItemStack baseResult = ItemStack.EMPTY;
                if (currentLegacyRecipe != null) {
                    baseResult = buildLegacyResult(currentLegacyRecipe, getGridHandler());
                } else if (currentRecipe != null) {
                    CraftingContainer cc = new TransientCraftingContainer(RarityForgeMenu.this, 3, 3);
                    for (int i = 0; i < GRID_SIZE; i++) {
                        cc.setItem(i, internal.getStackInSlot(GRID_START + i).copy());
                    }
                    baseResult = currentRecipe.assemble(cc, world.registryAccess());
                }
                if (!baseResult.isEmpty()) {
                    boolean legacySpecial = currentLegacyRecipe != null
                            && (currentLegacyRecipe.isBookRecipe() || currentLegacyRecipe.isUnbreakable());
                    if (legacySpecial) {
                        // legacy book/unbreakable は buildLegacyResult で完結 → そのまま反映
                        if (baseResult.getTag() != null) stack.setTag(baseResult.getTag());
                    } else {
                        ItemStack finalStack = RarityForgeCenterLogic.finalize(baseResult, cat0, cat1);
                        if (!finalStack.isEmpty() && finalStack.getTag() != null) {
                            stack.setTag(finalStack.getTag());
                        }
                    }
                }
            } else {
                ItemStack medium = cat0;
                ItemStack cat    = cat1;
                ItemStack finalStack = RarityForgeCenterLogic.finalizeEnhance(medium, cat);
                if (!finalStack.isEmpty() && finalStack.getTag() != null) {
                    stack.setTag(finalStack.getTag());
                }
            }
            consumeAfterTake(craftMode);
            playForgeSound();
            super.onTake(player, stack);
            updateResultPreview();
        }
    }
}
