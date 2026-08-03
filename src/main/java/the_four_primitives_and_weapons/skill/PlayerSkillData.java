package the_four_primitives_and_weapons.skill;

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
        DASH("ダッシュ", "dash"),
        RIGHT_CLICK("右クリック", "right_click"),
        SHIFT_RIGHT_CLICK("Shift+右クリック", "shift_right_click");

        private final String displayName;
        private final String id;

        AttackSlot(String displayName, String id) {
            this.displayName = displayName;
            this.id = id;
        }

        public String getDisplayName() { return displayName; }
        public String getId() { return id; }
        /** 翻訳キー: attack_slot.the_four_primitives_and_weapons.&lt;id&gt; */
        public String translationKey() { return "attack_slot.the_four_primitives_and_weapons." + id; }

        public static AttackSlot fromId(String id) {
            for (AttackSlot slot : values()) {
                if (slot.id.equals(id)) return slot;
            }
            return null;
        }
    }

    // 得意武器タイプ
    public enum WeaponProficiency {
        NONE("なし", "none"),
        KATANA("刀", "katana"),
        STRAIGHT_SWORD("直刀", "straight_sword"),
        SWORD("剣", "sword"),
        SHIELD("盾", "shield");

        private final String displayName;
        private final String id;

        WeaponProficiency(String displayName, String id) {
            this.displayName = displayName;
            this.id = id;
        }

        public String getDisplayName() { return displayName; }
        public String getId() { return id; }
        /** 翻訳キー: weapon_proficiency.the_four_primitives_and_weapons.&lt;id&gt; */
        public String translationKey() { return "weapon_proficiency.the_four_primitives_and_weapons." + id; }

        public static WeaponProficiency fromId(String id) {
            for (WeaponProficiency p : values()) {
                if (p.id.equals(id)) return p;
            }
            return NONE;
        }

        public WeaponProficiency next() {
            WeaponProficiency[] vals = values();
            return vals[(ordinal() + 1) % vals.length];
        }
    }

    // 武器ごとの技設定
    public static class WeaponLoadout implements INBTSerializable<CompoundTag> {
        private ItemStack weapon;
        private final Map<AttackSlot, String> motions = new EnumMap<>(AttackSlot.class);

        /**
         * 武器スロットに武器を置いただけの状態では motions は空。
         * ここで既定技を prefill してしまうと「ユーザーが明示設定した技」と区別が付かず、
         * 武器タイプ別設定 ( タイプタブ ) や JSON の default_motions を常に握り潰してしまうため、
         * 明示設定されたスロットだけを保持する。
         */
        public WeaponLoadout(ItemStack weapon) {
            this.weapon = weapon.copy();
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

        /** このスロットにユーザーが明示設定した技があるか ( 無ければタイプ設定/JSON既定に委ねる )。 */
        public boolean hasMotion(AttackSlot slot) {
            return slot != null && motions.containsKey(slot);
        }

        /** このロードアウトが {@code stack} と同じ武器か ( クラス名ではなくアイテムで判定 )。 */
        public boolean matchesItem(ItemStack stack) {
            return stack != null && !stack.isEmpty() && weapon.getItem() == stack.getItem();
        }

        public void setMotion(AttackSlot slot, String motionId) {
            if (slot != null && motionId != null && !motionId.isEmpty()) {
                motions.put(slot, motionId);
            }
        }

        /** このスロットの明示設定を消す ( タイプ別設定 / JSON既定 に委ねる )。 */
        public void removeMotion(AttackSlot slot) {
            if (slot != null) motions.remove(slot);
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
        // 武器タイプ別モーション設定 (typeId → (AttackSlot → motionId))
        private final Map<String, Map<AttackSlot, String>> typeMotions = new HashMap<>();
        // 得意武器タイプ
        private WeaponProficiency weaponProficiency = WeaponProficiency.NONE;

        public SkillStorage() {
            // デフォルトモーション
            selectedMotions.put(AttackSlot.FIRST_HIT, "upper_left_slash");
            selectedMotions.put(AttackSlot.SECOND_HIT, "upper_right_slash");
            selectedMotions.put(AttackSlot.THIRD_HIT, "horizontal_slash");
            selectedMotions.put(AttackSlot.CHARGED, "spin_slash");
            selectedMotions.put(AttackSlot.DASH, "dash_rush");
            selectedMotions.put(AttackSlot.RIGHT_CLICK, "dodge");
            selectedMotions.put(AttackSlot.SHIFT_RIGHT_CLICK, "guard");
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
         * 持ち物の武器に対応する技IDを返す。
         * 優先順位: 武器スロット > 武器タイプ別設定 > デフォルト
         */
        public String getMotionForWeapon(AttackSlot slot, ItemStack heldItem) {
            if (!heldItem.isEmpty()) {
                // 0. 武器NBTの技設定を最優先（アイテムに直接保存された設定）
                String nbtMotion = WeaponSkillNBT.getMotion(heldItem, slot);
                if (nbtMotion != null) return nbtMotion;

                // 1. 武器スロットの個別設定を確認
                //    「同じ武器」かつ「そのスロットを明示設定済み」のときだけ採用する。
                //    ( 武器を置いただけ / 別スロットだけ設定した状態で、タイプ設定や
                //      JSON の default_motions を握り潰さないため )
                for (WeaponLoadout loadout : weaponSlots) {
                    if (loadout != null && loadout.matchesItem(heldItem) && loadout.hasMotion(slot)) {
                        return loadout.getMotion(slot);
                    }
                }

                // 2. 武器タイプ別のプレイヤー設定を確認
                WeaponTypeRegistry.WeaponTypeData typeData = WeaponTypeRegistry.getTypeForItem(heldItem);
                if (typeData != null) {
                    Map<AttackSlot, String> typeSetting = typeMotions.get(typeData.getId());
                    if (typeSetting != null && typeSetting.containsKey(slot)) {
                        return typeSetting.get(slot);
                    }
                    // 3. 武器タイプのJSON default_motionsを次点として採用
                    String jsonDefault = typeData.getDefaultMotion(slot);
                    if (jsonDefault != null) return jsonDefault;
                }
            }
            return selectedMotions.getOrDefault(slot, "thrust");
        }

        /**
         * このスロットに「ユーザーが明示的に設定した」モーションがあるか。
         * NBT / 武器スロット / 武器タイプ別設定 のいずれかがあれば true。
         * JSON の default_motions やグローバル既定は「明示設定」に含めない
         * ( スペル自動優先が JSON 既定 "dodge" を上書きできるようにするため )。
         */
        public boolean hasExplicitMotion(AttackSlot slot, ItemStack heldItem) {
            if (heldItem == null || heldItem.isEmpty()) return false;
            if (WeaponSkillNBT.getMotion(heldItem, slot) != null) return true;
            for (WeaponLoadout loadout : weaponSlots) {
                if (loadout != null && loadout.matchesItem(heldItem) && loadout.hasMotion(slot)) return true;
            }
            WeaponTypeRegistry.WeaponTypeData typeData = WeaponTypeRegistry.getTypeForItem(heldItem);
            if (typeData != null) {
                Map<AttackSlot, String> typeSetting = typeMotions.get(typeData.getId());
                if (typeSetting != null && typeSetting.containsKey(slot)) return true;
            }
            return false;
        }

        // === 武器タイプ別モーション設定 ===

        public String getTypeMotion(String typeId, AttackSlot slot) {
            Map<AttackSlot, String> typeSetting = typeMotions.get(typeId);
            if (typeSetting != null && typeSetting.containsKey(slot)) {
                return typeSetting.get(slot);
            }
            return selectedMotions.getOrDefault(slot, "thrust");
        }

        public void setTypeMotion(String typeId, AttackSlot slot, String motionId) {
            typeMotions.computeIfAbsent(typeId, k -> new EnumMap<>(AttackSlot.class))
                    .put(slot, motionId);
        }

        /** タイプ別設定が「明示的に入っているか」だけを見る ( 未設定なら null。 既定へのフォールバックはしない )。 */
        public String getRawTypeMotion(String typeId, AttackSlot slot) {
            Map<AttackSlot, String> typeSetting = typeMotions.get(typeId);
            return typeSetting != null ? typeSetting.get(slot) : null;
        }

        /** 指定タイプの、 指定スロットのタイプ別設定を消す ( JSON既定 / グローバル既定 に委ねる )。 */
        public void clearTypeMotion(String typeId, AttackSlot slot) {
            Map<AttackSlot, String> typeSetting = typeMotions.get(typeId);
            if (typeSetting != null) typeSetting.remove(slot);
        }

        /**
         * 指定タイプの武器を登録している武器スロットから、 該当スロットの明示設定を消す。
         *
         * <p>{@link #getMotionForWeapon} は 武器スロット ( 優先度1 ) を タイプ別設定 ( 優先度2 ) より
         * 優先するため、 これを残したままタイプ別タブで選び直しても何も起きない。 スキル画面で
         * 選んだものが必ず反映されるよう、 上位の設定を落としてからタイプ別設定を効かせる。</p>
         */
        public void clearLoadoutMotionsForType(String typeId, AttackSlot slot) {
            if (typeId == null) return;
            for (WeaponLoadout loadout : weaponSlots) {
                if (loadout == null) continue;
                WeaponTypeRegistry.WeaponTypeData type = WeaponTypeRegistry.getTypeForItem(loadout.getWeapon());
                if (type != null && typeId.equals(type.getId())) loadout.removeMotion(slot);
            }
        }

        /** 指定アイテムを登録している武器スロットから、 該当スロットの明示設定を消す。 */
        public void clearLoadoutMotionsForItem(net.minecraft.world.item.Item item, AttackSlot slot) {
            if (item == null) return;
            for (WeaponLoadout loadout : weaponSlots) {
                if (loadout != null && loadout.getWeapon().getItem() == item) loadout.removeMotion(slot);
            }
        }

        /** マルチプレイ同期用: 内部 typeMotions の全体ビューを返す (read-only 想定)。 */
        public Map<String, Map<AttackSlot, String>> getAllTypeMotions() {
            return typeMotions;
        }

        /** マルチプレイ同期用: extraData で受け取った typeMotions をクライアント menu にも展開できるよう setter を追加。 */
        public void replaceAllTypeMotions(Map<String, Map<AttackSlot, String>> source) {
            typeMotions.clear();
            for (Map.Entry<String, Map<AttackSlot, String>> e : source.entrySet()) {
                EnumMap<AttackSlot, String> copy = new EnumMap<>(AttackSlot.class);
                copy.putAll(e.getValue());
                typeMotions.put(e.getKey(), copy);
            }
        }

        // === 固有スキルトグル ===

        public boolean isUniqueSkillEnabled(String itemId) {
            return uniqueSkillToggle.getOrDefault(itemId, true);
        }

        public void setUniqueSkillEnabled(String itemId, boolean enabled) {
            uniqueSkillToggle.put(itemId, enabled);
        }

        // === 得意武器タイプ ===

        public WeaponProficiency getWeaponProficiency() {
            return weaponProficiency;
        }

        public void setWeaponProficiency(WeaponProficiency proficiency) {
            this.weaponProficiency = proficiency != null ? proficiency : WeaponProficiency.NONE;
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

            // 武器タイプ別モーション
            CompoundTag typeMotionsTag = new CompoundTag();
            for (Map.Entry<String, Map<AttackSlot, String>> entry : typeMotions.entrySet()) {
                CompoundTag typeTag = new CompoundTag();
                for (Map.Entry<AttackSlot, String> me : entry.getValue().entrySet()) {
                    typeTag.putString(me.getKey().getId(), me.getValue());
                }
                typeMotionsTag.put(entry.getKey(), typeTag);
            }
            tag.put("typeMotions", typeMotionsTag);

            // 得意武器タイプ
            tag.putString("weaponProficiency", weaponProficiency.getId());

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

            // 武器タイプ別モーション
            typeMotions.clear();
            if (tag.contains("typeMotions")) {
                CompoundTag typeMotionsTag = tag.getCompound("typeMotions");
                for (String typeId : typeMotionsTag.getAllKeys()) {
                    CompoundTag typeTag = typeMotionsTag.getCompound(typeId);
                    Map<AttackSlot, String> slotMap = new EnumMap<>(AttackSlot.class);
                    for (AttackSlot slot : AttackSlot.values()) {
                        String motionId = typeTag.getString(slot.getId());
                        if (!motionId.isEmpty()) {
                            slotMap.put(slot, motionId);
                        }
                    }
                    if (!slotMap.isEmpty()) {
                        typeMotions.put(typeId, slotMap);
                    }
                }
            }

            // 得意武器タイプ
            if (tag.contains("weaponProficiency")) {
                weaponProficiency = WeaponProficiency.fromId(tag.getString("weaponProficiency"));
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

    // ─────────────────────────────────────────────────────────────
    // 技 (motion) の ON/OFF トグル
    // ─────────────────────────────────────────────────────────────
    /**
     * 指定 motion が有効かを返す。 デフォルト ( 一度もトグルしてない ) は true。
     * 各 motion を発動する handler から呼んで、 false なら通常攻撃にフォールバックする想定。
     */
    public static boolean isMotionEnabled(Player player, String motionId) {
        if (player == null || motionId == null || motionId.isEmpty()) return true;
        SkillStorage data = getSkillData(player);
        if (data == null) return true;
        // uniqueSkillToggle を流用 — Boolean.FALSE がセットされてる時だけ disable
        Boolean b = data.uniqueSkillToggle.get(motionId);
        return b == null || b;
    }

    /**
     * 指定 motion の有効/無効をセット。
     */
    public static void setMotionEnabled(Player player, String motionId, boolean enabled) {
        if (player == null || motionId == null || motionId.isEmpty()) return;
        SkillStorage data = getSkillData(player);
        if (data == null) return;
        if (enabled) {
            // デフォルト true なので map から取り除いて clean state にする
            data.uniqueSkillToggle.remove(motionId);
        } else {
            data.uniqueSkillToggle.put(motionId, Boolean.FALSE);
        }
    }
}
