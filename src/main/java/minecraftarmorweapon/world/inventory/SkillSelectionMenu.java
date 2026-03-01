package minecraftarmorweapon.world.inventory;

import minecraftarmorweapon.init.SkillSelectionRegistration;
import minecraftarmorweapon.skill.PlayerSkillData;
import minecraftarmorweapon.skill.PlayerSkillData.AttackSlot;
import minecraftarmorweapon.skill.PlayerSkillData.WeaponLoadout;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.EnumMap;
import java.util.Map;

/**
 * 技選択メニュー。5つの武器装備スロット + プレイヤーインベントリ。
 * モーション設定はextraDataでサーバーからクライアントに同期される。
 */
public class SkillSelectionMenu extends AbstractContainerMenu {

    public static final int WEAPON_SLOTS = PlayerSkillData.MAX_WEAPON_SLOTS;
    private static final int PLAYER_INV_SLOTS = 36; // 27 + 9

    private final Player player;
    private final ItemStackHandler weaponSlotHandler;

    // GUI内の武器スロットの配置座標（Screenと共有する定数）
    public static final int WEAPON_SLOT_START_X = 62;
    public static final int WEAPON_SLOT_Y = 28;
    public static final int WEAPON_SLOT_GAP = 22;

    // プレイヤーインベントリの配置座標
    public static final int INV_START_X = 89;
    public static final int INV_START_Y = 206;
    public static final int HOTBAR_Y = 264;

    // ローカルモーション状態（クライアント表示用、extraDataで同期）
    private final Map<AttackSlot, String> defaultMotions = new EnumMap<>(AttackSlot.class);
    @SuppressWarnings("unchecked")
    private final Map<AttackSlot, String>[] loadoutMotions = new EnumMap[WEAPON_SLOTS];

    public SkillSelectionMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        super(SkillSelectionRegistration.getMenuType(), containerId);
        this.player = inv.player;

        for (int i = 0; i < WEAPON_SLOTS; i++) {
            loadoutMotions[i] = new EnumMap<>(AttackSlot.class);
        }

        this.weaponSlotHandler = new ItemStackHandler(WEAPON_SLOTS) {
            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };

        if (extraData != null) {
            // クライアント: サーバーから送られたモーションデータを読む
            readMotionData(extraData);
        } else {
            // サーバー: capabilityから読む
            populateMotionsFromCapability();
        }

        // 既存のロードアウトからスロットを復元
        populateFromSkillData();

        // 武器スロット（5つ）
        for (int i = 0; i < WEAPON_SLOTS; i++) {
            int slotX = WEAPON_SLOT_START_X + i * WEAPON_SLOT_GAP;
            this.addSlot(new WeaponSlot(weaponSlotHandler, i, slotX, WEAPON_SLOT_Y));
        }

        // プレイヤーインベントリ (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + (row + 1) * 9,
                        INV_START_X + col * 18, INV_START_Y + row * 18));
            }
        }

        // ホットバー (1x9)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, INV_START_X + col * 18, HOTBAR_Y));
        }
    }

    private void readMotionData(FriendlyByteBuf buf) {
        for (int i = 0; i < WEAPON_SLOTS; i++) {
            if (buf.readBoolean()) {
                for (AttackSlot slot : AttackSlot.values()) {
                    loadoutMotions[i].put(slot, buf.readUtf());
                }
            }
        }
        for (AttackSlot slot : AttackSlot.values()) {
            defaultMotions.put(slot, buf.readUtf());
        }
    }

    private void populateMotionsFromCapability() {
        player.getCapability(PlayerSkillData.SKILL_CAPABILITY).ifPresent(skillData -> {
            for (int i = 0; i < WEAPON_SLOTS; i++) {
                WeaponLoadout loadout = skillData.getLoadoutAt(i);
                if (loadout != null) {
                    for (AttackSlot slot : AttackSlot.values()) {
                        loadoutMotions[i].put(slot, loadout.getMotion(slot));
                    }
                }
            }
            for (AttackSlot slot : AttackSlot.values()) {
                defaultMotions.put(slot, skillData.getMotion(slot));
            }
        });
    }

    private void populateFromSkillData() {
        player.getCapability(PlayerSkillData.SKILL_CAPABILITY).ifPresent(skillData -> {
            for (int i = 0; i < WEAPON_SLOTS; i++) {
                WeaponLoadout loadout = skillData.getLoadoutAt(i);
                if (loadout != null) {
                    weaponSlotHandler.setStackInSlot(i, loadout.getWeapon());
                }
            }
        });
    }

    // === モーションデータのgetter/setter（Screen用） ===

    public String getDefaultMotion(AttackSlot slot) {
        return defaultMotions.getOrDefault(slot, "thrust");
    }

    public void setDefaultMotion(AttackSlot slot, String motionId) {
        defaultMotions.put(slot, motionId);
    }

    public String getLoadoutMotion(int index, AttackSlot slot) {
        if (index < 0 || index >= WEAPON_SLOTS) return getDefaultMotion(slot);
        String motion = loadoutMotions[index].get(slot);
        return motion != null ? motion : getDefaultMotion(slot);
    }

    public void setLoadoutMotion(int index, AttackSlot slot, String motionId) {
        if (index >= 0 && index < WEAPON_SLOTS) {
            loadoutMotions[index].put(slot, motionId);
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
            ItemStack current = slot.getItem();
            itemstack = current.copy();

            if (index < WEAPON_SLOTS) {
                // 武器スロット → プレイヤーインベントリ
                if (!this.moveItemStackTo(current, WEAPON_SLOTS, WEAPON_SLOTS + PLAYER_INV_SLOTS, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(current, itemstack);
            } else {
                // プレイヤーインベントリ → 武器スロット
                if (!this.moveItemStackTo(current, 0, WEAPON_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (current.getCount() == 0) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (current.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, current);
        }
        return itemstack;
    }

    @Override
    public void removed(Player playerIn) {
        // スロットの内容をPlayerSkillDataに保存
        if (playerIn instanceof ServerPlayer) {
            playerIn.getCapability(PlayerSkillData.SKILL_CAPABILITY).ifPresent(skillData -> {
                for (int i = 0; i < WEAPON_SLOTS; i++) {
                    ItemStack slotContent = weaponSlotHandler.getStackInSlot(i);
                    if (slotContent.isEmpty()) {
                        skillData.clearLoadoutAt(i);
                    } else {
                        skillData.setLoadoutAt(i, slotContent);
                    }
                }
            });

            // ハンドラをクリアしてsuper.removed()が不要なドロップをしないようにする
            for (int i = 0; i < WEAPON_SLOTS; i++) {
                weaponSlotHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }

        super.removed(playerIn);
    }
}
