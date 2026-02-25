package minecraftarmorweapon.world.inventory;

import minecraftarmorweapon.init.RarityForgeRegistration;
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * レアリティ強化台メニュー
 * スロット0: 素材1, スロット1: 素材2, スロット2: 触媒(レアリティブースト), スロット3: 結果出力
 */
public class RarityForgeMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {

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
        this.internal = new ItemStackHandler(4);
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

        // スロット0: 素材1
        this.customSlots.put(0, this.addSlot(new SlotItemHandler(internal, 0, 7, 41) {
        }));

        // スロット1: 素材2
        this.customSlots.put(1, this.addSlot(new SlotItemHandler(internal, 1, 25, 41) {
        }));

        // スロット2: 触媒（レアリティブースト素材）
        this.customSlots.put(2, this.addSlot(new SlotItemHandler(internal, 2, 43, 41) {
        }));

        // スロット3: 結果出力（取り出し専用）
        this.customSlots.put(3, this.addSlot(new SlotItemHandler(internal, 3, 95, 41) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        }));

        // プレイヤーインベントリ
        for (int si = 0; si < 3; ++si)
            for (int sj = 0; sj < 9; ++sj)
                this.addSlot(new Slot(inv, sj + (si + 1) * 9, -3 + 8 + sj * 18, -3 + 84 + si * 18));
        for (int si = 0; si < 9; ++si)
            this.addSlot(new Slot(inv, si, -3 + 8 + si * 18, -3 + 142));
    }

    /**
     * 強化ボタンが押された時の処理（サーバーサイド）
     * 素材1+素材2でレシピマッチ → 触媒でレアリティ決定 → 結果を出力スロットへ
     */
    public void performForge() {
        ItemStack output = internal.getStackInSlot(3);

        // 出力スロットが埋まっていたら何もしない
        if (!output.isEmpty()) return;

        // レシピマッチ
        RarityForgeRecipe recipe = RarityForgeRecipes.findMatch(internal);
        if (recipe == null) return;

        // 触媒スロットからレアリティ決定
        ItemStack catalyst = internal.getStackInSlot(2);
        WeaponRarity rarity = RarityCraftingLogic.rollRarity(catalyst);

        // 結果アイテム作成
        ItemStack result = new ItemStack(recipe.getResult());
        WeaponRarity.setToStack(result, rarity);

        // 素材を消費
        recipe.consumeIngredients(internal);

        // 触媒を1つ消費
        if (!catalyst.isEmpty()) {
            catalyst.shrink(1);
        }

        // 出力スロットにセット
        if (internal instanceof ItemStackHandler handler) {
            handler.setStackInSlot(3, result);
        }

        // 音を鳴らす
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.level().playSound(null,
                    serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
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
            if (index < 4) {
                if (!this.moveItemStackTo(itemstack1, 4, this.slots.size(), true))
                    return ItemStack.EMPTY;
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (!this.moveItemStackTo(itemstack1, 0, 3, false)) {
                // Shift-click from inventory: try slots 0-2 (not output slot 3)
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
