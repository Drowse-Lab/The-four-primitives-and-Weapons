package minecraftarmorweapon.skill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.core.Direction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class PlayerSkillData {

    public static final int MAX_WEAPON_SLOTS = 5;

    // 攻撃スロット（5種）
    public enum AttackSlot {
        FIRST_HIT("一撃目", "first_hit"),
        SECOND_HIT("二撃目", "second_hit"),
        THIRD_HIT("三撃目", "third_hit"),
        CHARGED("チャージ", "charged"),
        DASH("ダッシュ", "dash");

        private final String displayName;
        private final String id;

        AttackSlot(String displayName, String id) {
            this.displayName = displayName;
            this.id = id;
        }

        public String getDisplayName() { return displayName; }
        public String getId() { return id; }

        public static AttackSlot fromId(String id) {
            for (AttackSlot slot : values()) {
                if (slot.id.equals(id)) return slot;
            }
            return null;
        }
    }

    // 武器ごとの技設定
    public static class WeaponLoadout implements INBTSerializable<CompoundTag> {
        private ItemStack weapon;
        private final Map<AttackSlot, String> motions = new EnumMap<>(AttackSlot.class);

        public WeaponLoadout(ItemStack weapon) {
            this.weapon = weapon.copy();
            // デフォルトモーション
            motions.put(AttackSlot.FIRST_HIT, "upper_left_slash");
            motions.put(AttackSlot.SECOND_HIT, "upper_right_slash");
            motions.put(AttackSlot.THIRD_HIT, "horizontal_slash");
            motions.put(AttackSlot.CHARGED, "spin_slash");
            motions.put(AttackSlot.DASH, "dash_rush");
        }

        // NBT復元用
        private WeaponLoadout(ItemStack weapon, Map<AttackSlot, String> motions) {
            this.weapon = weapon;
            this.motions.putAll(motions);
        }

        public String getWeaponClass() {
            return weapon.getItem().getClass().getSimpleName();
        }

        public ItemStack getWeapon() {
            return weapon.copy();
        }

        public void setWeapon(ItemStack newWeapon) {
            this.weapon = newWeapon.copy();
        }

        public String getMotion(AttackSlot slot) {
            return motions.getOrDefault(slot, "thrust");
        }

        public void setMotion(AttackSlot slot, String motionId) {
            if (slot != null && motionId != null && !motionId.isEmpty()) {
                motions.put(slot, motionId);
            }
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.put("weapon", weapon.save(new CompoundTag()));
            CompoundTag motionTag = new CompoundTag();
            for (Map.Entry<AttackSlot, String> e : motions.entrySet()) {
                motionTag.putString(e.getKey().getId(), e.getValue());
            }
            tag.put("motions", motionTag);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            // 使用しない（staticファクトリを使う）
        }

        public static WeaponLoadout fromNBT(CompoundTag tag) {
            ItemStack weapon = ItemStack.of(tag.getCompound("weapon"));
            Map<AttackSlot, String> motions = new EnumMap<>(AttackSlot.class);
            if (tag.contains("motions")) {
                CompoundTag motionTag = tag.getCompound("motions");
                for (AttackSlot slot : AttackSlot.values()) {
                    String id = motionTag.getString(slot.getId());
                    if (!id.isEmpty()) {
                        motions.put(slot, id);
                    }
                }
            }
            return new WeaponLoadout(weapon, motions);
        }
    }

    // プレイヤーごとのスキルデータ
    public static class SkillStorage implements INBTSerializable<CompoundTag> {
        // デフォルト技設定（スロット未登録武器用）
        private final Map<AttackSlot, String> selectedMotions = new EnumMap<>(AttackSlot.class);
        // 武器スロット（固定5枠、nullは空スロット）
        private final WeaponLoadout[] weaponSlots = new WeaponLoadout[MAX_WEAPON_SLOTS];
        // 固有スキルのON/OFF（内部利用）
        private final Map<String, Boolean> uniqueSkillToggle = new HashMap<>();

        public SkillStorage() {
            // デフォルトモーション
            selectedMotions.put(AttackSlot.FIRST_HIT, "upper_left_slash");
            selectedMotions.put(AttackSlot.SECOND_HIT, "upper_right_slash");
            selectedMotions.put(AttackSlot.THIRD_HIT, "horizontal_slash");
            selectedMotions.put(AttackSlot.CHARGED, "spin_slash");
            selectedMotions.put(AttackSlot.DASH, "dash_rush");
        }

        // === デフォルトモーション設定 ===

        public String getMotion(AttackSlot slot) {
            return selectedMotions.getOrDefault(slot, "thrust");
        }

        public void setMotion(AttackSlot slot, String motionId) {
            if (slot != null && motionId != null && !motionId.isEmpty()) {
                selectedMotions.put(slot, motionId);
            }
        }

        // === 武器スロット（固定配列） ===

        public WeaponLoadout getLoadoutAt(int index) {
            if (index < 0 || index >= MAX_WEAPON_SLOTS) return null;
            return weaponSlots[index];
        }

        /**
         * 指定スロットに武器を設定する。既存のモーション設定が同じ武器クラスなら維持する。
         */
        public void setLoadoutAt(int index, ItemStack weapon) {
            if (index < 0 || index >= MAX_WEAPON_SLOTS) return;
            if (weapon.isEmpty()) {
                weaponSlots[index] = null;
                return;
            }
            String newWeaponClass = weapon.getItem().getClass().getSimpleName();
            WeaponLoadout existing = weaponSlots[index];
            if (existing != null && existing.getWeaponClass().equals(newWeaponClass)) {
                // 同じ武器クラスならモーション設定を維持し、アイテムだけ更新
                existing.setWeapon(weapon);
            } else {
                // 新しい武器クラスなら新規ロードアウトを作成
                weaponSlots[index] = new WeaponLoadout(weapon);
            }
        }

        public void clearLoadoutAt(int index) {
            if (index < 0 || index >= MAX_WEAPON_SLOTS) return;
            weaponSlots[index] = null;
        }

        public void setLoadoutMotion(int slotIndex, AttackSlot slot, String motionId) {
            if (slotIndex < 0 || slotIndex >= MAX_WEAPON_SLOTS) return;
            WeaponLoadout loadout = weaponSlots[slotIndex];
            if (loadout != null) {
                loadout.setMotion(slot, motionId);
            }
        }

        /**
         * 持ち物の武器クラスに対応するロードアウトを探し、その技IDを返す。
         * 見つからない場合はデフォルト設定を返す。
         */
        public String getMotionForWeapon(AttackSlot slot, ItemStack heldItem) {
            if (!heldItem.isEmpty()) {
                String weaponClass = heldItem.getItem().getClass().getSimpleName();
                for (WeaponLoadout loadout : weaponSlots) {
                    if (loadout != null && loadout.getWeaponClass().equals(weaponClass)) {
                        return loadout.getMotion(slot);
                    }
                }
            }
            return selectedMotions.getOrDefault(slot, "thrust");
        }

        // === 固有スキルトグル ===

        public boolean isUniqueSkillEnabled(String itemId) {
            return uniqueSkillToggle.getOrDefault(itemId, true);
        }

        public void setUniqueSkillEnabled(String itemId, boolean enabled) {
            uniqueSkillToggle.put(itemId, enabled);
        }

        /**
         * 指定クラスの武器が既にスロットに登録済みかどうか
         */
        public boolean hasLoadoutForWeapon(String weaponClass) {
            for (WeaponLoadout loadout : weaponSlots) {
                if (loadout != null && loadout.getWeaponClass().equals(weaponClass)) return true;
            }
            return false;
        }

        /**
         * 指定クラスの武器がスロットに登録済みかチェック（特定のスロットを除外可能）
         */
        public boolean hasLoadoutForWeaponExcluding(String weaponClass, int excludeIndex) {
            for (int i = 0; i < MAX_WEAPON_SLOTS; i++) {
                if (i == excludeIndex) continue;
                if (weaponSlots[i] != null && weaponSlots[i].getWeaponClass().equals(weaponClass)) return true;
            }
            return false;
        }

        // === NBTシリアライズ ===

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();

            // デフォルトモーション選択
            CompoundTag motions = new CompoundTag();
            for (Map.Entry<AttackSlot, String> e : selectedMotions.entrySet()) {
                motions.putString(e.getKey().getId(), e.getValue());
            }
            tag.put("motions", motions);

            // 武器スロット（新形式: CompoundTag with indexed keys）
            CompoundTag slotsTag = new CompoundTag();
            for (int i = 0; i < MAX_WEAPON_SLOTS; i++) {
                if (weaponSlots[i] != null) {
                    slotsTag.put("slot_" + i, weaponSlots[i].serializeNBT());
                }
            }
            tag.put("weaponSlots", slotsTag);

            // 固有スキルトグル
            CompoundTag uniqueSkills = new CompoundTag();
            for (Map.Entry<String, Boolean> entry : uniqueSkillToggle.entrySet()) {
                uniqueSkills.putBoolean(entry.getKey(), entry.getValue());
            }
            tag.put("uniqueSkills", uniqueSkills);

            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            // デフォルトモーション選択
            if (tag.contains("motions")) {
                CompoundTag motions = tag.getCompound("motions");
                for (AttackSlot slot : AttackSlot.values()) {
                    String motionId = motions.getString(slot.getId());
                    if (!motionId.isEmpty()) {
                        selectedMotions.put(slot, motionId);
                    }
                }
            }

            // 武器スロット（新形式）
            Arrays.fill(weaponSlots, null);
            if (tag.contains("weaponSlots", Tag.TAG_COMPOUND)) {
                CompoundTag slotsTag = tag.getCompound("weaponSlots");
                for (int i = 0; i < MAX_WEAPON_SLOTS; i++) {
                    String key = "slot_" + i;
                    if (slotsTag.contains(key)) {
                        WeaponLoadout loadout = WeaponLoadout.fromNBT(slotsTag.getCompound(key));
                        if (!loadout.getWeapon().isEmpty()) {
                            weaponSlots[i] = loadout;
                        }
                    }
                }
            } else if (tag.contains("weaponLoadouts", Tag.TAG_LIST)) {
                // 旧形式（ListTag）からの移行
                ListTag loadoutList = tag.getList("weaponLoadouts", Tag.TAG_COMPOUND);
                for (int i = 0; i < Math.min(loadoutList.size(), MAX_WEAPON_SLOTS); i++) {
                    WeaponLoadout loadout = WeaponLoadout.fromNBT(loadoutList.getCompound(i));
                    if (!loadout.getWeapon().isEmpty()) {
                        weaponSlots[i] = loadout;
                    }
                }
            }

            // 固有スキルトグル
            uniqueSkillToggle.clear();
            if (tag.contains("uniqueSkills")) {
                CompoundTag uniqueSkills = tag.getCompound("uniqueSkills");
                for (String key : uniqueSkills.getAllKeys()) {
                    uniqueSkillToggle.put(key, uniqueSkills.getBoolean(key));
                }
            }
        }
    }

    // Capability定義
    public static final Capability<SkillStorage> SKILL_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    // Capabilityプロバイダー
    public static class SkillProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final SkillStorage storage = new SkillStorage();
        private final LazyOptional<SkillStorage> optional = LazyOptional.of(() -> storage);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return cap == SKILL_CAPABILITY ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return storage.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            storage.deserializeNBT(nbt);
        }
    }

    // ヘルパーメソッド
    public static SkillStorage getSkillData(Player player) {
        return player.getCapability(SKILL_CAPABILITY).orElse(new SkillStorage());
    }
}
