package the_four_primitives_and_weapons.skill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import the_four_primitives_and_weapons.skill.PlayerSkillData.AttackSlot;

/**
 * 武器のNBTに技設定を保存/読み込みするユーティリティ。
 * 武器アイテム自体に技設定が紐づくので、スロットやcapabilityに依存しない。
 *
 * NBT構造:
 * {
 *   SkillMotions: {
 *     first_hit: "thrust",
 *     right_click: "gate_special",
 *     ...
 *   }
 * }
 */
public final class WeaponSkillNBT {

    private static final String TAG_KEY = "SkillMotions";

    private WeaponSkillNBT() {}

    /**
     * 武器のNBTから技IDを取得。未設定ならnull。
     */
    public static String getMotion(ItemStack stack, AttackSlot slot) {
        try {
            if (stack == null || stack.isEmpty() || slot == null) return null;
            if (!stack.hasTag()) return null;
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains(TAG_KEY)) return null;
            CompoundTag motions = tag.getCompound(TAG_KEY);
            String id = motions.getString(slot.getId());
            return id.isEmpty() ? null : id;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 武器のNBTに技IDを保存。
     */
    public static void setMotion(ItemStack stack, AttackSlot slot, String motionId) {
        try {
            if (stack == null || stack.isEmpty() || slot == null || motionId == null) return;
            CompoundTag tag = stack.getOrCreateTag();
            CompoundTag motions = tag.contains(TAG_KEY) ? tag.getCompound(TAG_KEY) : new CompoundTag();
            motions.putString(slot.getId(), motionId);
            tag.put(TAG_KEY, motions);
        } catch (Exception e) {
            // NBT書き込み失敗は無視
        }
    }

    /**
     * 武器のNBTから指定スロットの技設定を消す。
     *
     * <p>この NBT は {@link PlayerSkillData.SkillStorage#getMotionForWeapon} の最優先なので、
     * 残っているとタイプ別設定やデフォルト設定を握り潰してしまう。 スキル画面で
     * より広い範囲の設定をし直したときは、 こちらで消して新しい設定を効かせる。</p>
     */
    public static void removeMotion(ItemStack stack, AttackSlot slot) {
        try {
            if (stack == null || stack.isEmpty() || slot == null || !stack.hasTag()) return;
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains(TAG_KEY)) return;
            CompoundTag motions = tag.getCompound(TAG_KEY);
            motions.remove(slot.getId());
            if (motions.isEmpty()) {
                tag.remove(TAG_KEY);
            } else {
                tag.put(TAG_KEY, motions);
            }
        } catch (Exception e) {
            // NBT書き込み失敗は無視
        }
    }

    /**
     * 武器にSkillMotions NBTがあるか
     */
    public static boolean hasMotions(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_KEY);
    }
}
