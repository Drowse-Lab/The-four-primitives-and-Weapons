package minecraftarmorweapon.skill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 弓ItemStackのNBTで「装備中スキル5枠 + 選択中スロット」を管理。
 *
 *   tag.SkillSlots = int[5]   各スロットに装備しているBowSkillのordinal
 *   tag.SkillIndex = int      現在選択中のスロット(0-4)
 *
 * デフォルトロードアウトは BowSkill.values() の先頭5つ (NONEを除く)。
 */
public final class BowSkillData {
    private static final String KEY_SLOTS = "SkillSlots";
    private static final String KEY_INDEX = "SkillIndex";
    public static final int LOADOUT_SIZE = 5;

    private BowSkillData() {}

    public static int[] getLoadout(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(KEY_SLOTS, 11)) { // 11 = INT_ARRAY
            int[] def = defaultLoadout();
            tag.putIntArray(KEY_SLOTS, def);
            return def;
        }
        int[] arr = tag.getIntArray(KEY_SLOTS);
        if (arr.length != LOADOUT_SIZE) {
            arr = defaultLoadout();
            tag.putIntArray(KEY_SLOTS, arr);
        }
        return arr;
    }

    public static int getIndex(ItemStack stack) {
        int idx = stack.getOrCreateTag().getInt(KEY_INDEX);
        return Math.floorMod(idx, LOADOUT_SIZE);
    }

    public static void setIndex(ItemStack stack, int index) {
        stack.getOrCreateTag().putInt(KEY_INDEX, Math.floorMod(index, LOADOUT_SIZE));
    }

    public static void cycleIndex(ItemStack stack, int delta) {
        setIndex(stack, getIndex(stack) + delta);
    }

    public static BowSkill getSelected(ItemStack stack) {
        int[] slots = getLoadout(stack);
        return BowSkill.byId(slots[getIndex(stack)]);
    }

    public static void setSlot(ItemStack stack, int slot, BowSkill skill) {
        int[] arr = getLoadout(stack);
        if (slot < 0 || slot >= LOADOUT_SIZE) return;
        arr[slot] = skill.ordinal();
        stack.getOrCreateTag().putIntArray(KEY_SLOTS, arr);
    }

    private static int[] defaultLoadout() {
        // NONE(0) を除いた最初の5つ
        return new int[]{
            BowSkill.POWER_SHOT.ordinal(),
            BowSkill.EXPLOSIVE.ordinal(),
            BowSkill.PIERCE.ordinal(),
            BowSkill.RAPID_FIRE.ordinal(),
            BowSkill.HEAVY_BLOW.ordinal()
        };
    }
}
