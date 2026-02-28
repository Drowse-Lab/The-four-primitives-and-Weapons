package minecraftarmorweapon.skill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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

    // プレイヤーごとのスキルデータ
    public static class SkillStorage implements INBTSerializable<CompoundTag> {
        // 各スロットに設定されたモーションID
        private final Map<AttackSlot, String> selectedMotions = new EnumMap<>(AttackSlot.class);
        // ロックされた武器（消費済み、取り出し不可）
        private final List<ItemStack> lockedWeapons = new ArrayList<>();
        // 解放された特殊スキルID
        private final Set<String> unlockedSpecials = new HashSet<>();
        // 固有スキルのON/OFF
        private final Map<String, Boolean> uniqueSkillToggle = new HashMap<>();

        public SkillStorage() {
            // デフォルトモーション
            selectedMotions.put(AttackSlot.FIRST_HIT, "upper_left_slash");
            selectedMotions.put(AttackSlot.SECOND_HIT, "upper_right_slash");
            selectedMotions.put(AttackSlot.THIRD_HIT, "horizontal_slash");
            selectedMotions.put(AttackSlot.CHARGED, "spin_slash");
            selectedMotions.put(AttackSlot.DASH, "thrust");
        }

        // === モーション選択 ===

        public String getMotion(AttackSlot slot) {
            return selectedMotions.getOrDefault(slot, "thrust");
        }

        public void setMotion(AttackSlot slot, String motionId) {
            if (slot != null && motionId != null && !motionId.isEmpty()) {
                selectedMotions.put(slot, motionId);
            }
        }

        // === 特殊スキル解放 ===

        public boolean isSpecialUnlocked(String specialId) {
            return unlockedSpecials.contains(specialId);
        }

        public void unlockSpecial(String specialId) {
            unlockedSpecials.add(specialId);
        }

        public Set<String> getUnlockedSpecials() {
            return Collections.unmodifiableSet(unlockedSpecials);
        }

        // === ロック済み武器 ===

        public void lockWeapon(ItemStack weapon) {
            if (!weapon.isEmpty()) {
                lockedWeapons.add(weapon.copy());
            }
        }

        public List<ItemStack> getLockedWeapons() {
            return Collections.unmodifiableList(lockedWeapons);
        }

        // === 固有スキルトグル（互換性維持） ===

        public boolean isUniqueSkillEnabled(String itemId) {
            return uniqueSkillToggle.getOrDefault(itemId, true);
        }

        public void setUniqueSkillEnabled(String itemId, boolean enabled) {
            uniqueSkillToggle.put(itemId, enabled);
        }

        // === NBTシリアライズ ===

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();

            // モーション選択
            CompoundTag motions = new CompoundTag();
            for (Map.Entry<AttackSlot, String> e : selectedMotions.entrySet()) {
                motions.putString(e.getKey().getId(), e.getValue());
            }
            tag.put("motions", motions);

            // ロック済み武器
            ListTag weaponList = new ListTag();
            for (ItemStack w : lockedWeapons) {
                weaponList.add(w.save(new CompoundTag()));
            }
            tag.put("lockedWeapons", weaponList);

            // 解放済み特殊スキル
            ListTag specialList = new ListTag();
            for (String s : unlockedSpecials) {
                specialList.add(StringTag.valueOf(s));
            }
            tag.put("unlockedSpecials", specialList);

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
            // モーション選択
            if (tag.contains("motions")) {
                CompoundTag motions = tag.getCompound("motions");
                for (AttackSlot slot : AttackSlot.values()) {
                    String motionId = motions.getString(slot.getId());
                    if (!motionId.isEmpty()) {
                        selectedMotions.put(slot, motionId);
                    }
                }
            }
            // 旧フォーマット（selectedSkills）からの移行 → デフォルトにリセット
            // 新フォーマットがない場合はコンストラクタのデフォルトが使われる

            // ロック済み武器
            lockedWeapons.clear();
            if (tag.contains("lockedWeapons")) {
                ListTag weaponList = tag.getList("lockedWeapons", Tag.TAG_COMPOUND);
                for (int i = 0; i < weaponList.size(); i++) {
                    ItemStack weapon = ItemStack.of(weaponList.getCompound(i));
                    if (!weapon.isEmpty()) {
                        lockedWeapons.add(weapon);
                    }
                }
            }

            // 解放済み特殊スキル
            unlockedSpecials.clear();
            if (tag.contains("unlockedSpecials")) {
                ListTag specialList = tag.getList("unlockedSpecials", Tag.TAG_STRING);
                for (int i = 0; i < specialList.size(); i++) {
                    unlockedSpecials.add(specialList.getString(i));
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
