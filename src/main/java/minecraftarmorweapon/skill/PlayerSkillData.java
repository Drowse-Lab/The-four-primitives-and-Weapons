package minecraftarmorweapon.skill;

import net.minecraft.nbt.CompoundTag;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerSkillData {
    
    // 武器タイプ
    public enum WeaponType {
        STRAIGHT_SWORD("直刀", "straight_sword"),
        KATANA("刀", "katana"),
        SPECIAL("特殊", "special");
        
        private final String displayName;
        private final String id;
        
        WeaponType(String displayName, String id) {
            this.displayName = displayName;
            this.id = id;
        }
        
        public String getDisplayName() { return displayName; }
        public String getId() { return id; }
    }
    
    // 技タイプ
    public enum SkillType {
        DASH_ATTACK("ダッシュ攻撃", "dash_attack"),
        NORMAL_ATTACK("通常攻撃", "normal_attack"),
        CHARGED_ATTACK("強化攻撃", "charged_attack");
        
        private final String displayName;
        private final String id;
        
        SkillType(String displayName, String id) {
            this.displayName = displayName;
            this.id = id;
        }
        
        public String getDisplayName() { return displayName; }
        public String getId() { return id; }
    }
    
    // プレイヤーごとのスキルデータ
    public static class SkillStorage implements INBTSerializable<CompoundTag> {
        private WeaponType selectedWeaponType = WeaponType.KATANA;
        private Map<String, Boolean> uniqueSkillToggle = new HashMap<>();
        private Map<SkillType, String> selectedSkills = new HashMap<>();
        
        public SkillStorage() {
            // デフォルトスキル設定
            selectedSkills.put(SkillType.DASH_ATTACK, "default_dash");
            selectedSkills.put(SkillType.NORMAL_ATTACK, "default_normal");
            selectedSkills.put(SkillType.CHARGED_ATTACK, "default_charged");
        }
        
        public WeaponType getSelectedWeaponType() {
            return selectedWeaponType;
        }
        
        public void setSelectedWeaponType(WeaponType type) {
            this.selectedWeaponType = type;
        }
        
        public boolean isUniqueSkillEnabled(String itemId) {
            return uniqueSkillToggle.getOrDefault(itemId, true);
        }
        
        public void setUniqueSkillEnabled(String itemId, boolean enabled) {
            uniqueSkillToggle.put(itemId, enabled);
        }
        
        public String getSelectedSkill(SkillType type) {
            return selectedSkills.getOrDefault(type, "default");
        }
        
        public void setSelectedSkill(SkillType type, String skillId) {
            selectedSkills.put(type, skillId);
        }
        
        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("weaponType", selectedWeaponType.getId());
            
            // 固有スキルトグルの保存
            CompoundTag uniqueSkills = new CompoundTag();
            for (Map.Entry<String, Boolean> entry : uniqueSkillToggle.entrySet()) {
                uniqueSkills.putBoolean(entry.getKey(), entry.getValue());
            }
            tag.put("uniqueSkills", uniqueSkills);
            
            // 選択されたスキルの保存
            CompoundTag skills = new CompoundTag();
            for (Map.Entry<SkillType, String> entry : selectedSkills.entrySet()) {
                skills.putString(entry.getKey().getId(), entry.getValue());
            }
            tag.put("selectedSkills", skills);
            
            return tag;
        }
        
        @Override
        public void deserializeNBT(CompoundTag tag) {
            // 武器タイプの読み込み
            String typeId = tag.getString("weaponType");
            for (WeaponType type : WeaponType.values()) {
                if (type.getId().equals(typeId)) {
                    selectedWeaponType = type;
                    break;
                }
            }
            
            // 固有スキルトグルの読み込み
            uniqueSkillToggle.clear();
            if (tag.contains("uniqueSkills")) {
                CompoundTag uniqueSkills = tag.getCompound("uniqueSkills");
                for (String key : uniqueSkills.getAllKeys()) {
                    uniqueSkillToggle.put(key, uniqueSkills.getBoolean(key));
                }
            }
            
            // 選択されたスキルの読み込み
            selectedSkills.clear();
            if (tag.contains("selectedSkills")) {
                CompoundTag skills = tag.getCompound("selectedSkills");
                for (SkillType skillType : SkillType.values()) {
                    String skillId = skills.getString(skillType.getId());
                    if (!skillId.isEmpty()) {
                        selectedSkills.put(skillType, skillId);
                    }
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