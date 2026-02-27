package minecraftarmorweapon.skill;

import minecraftarmorweapon.skill.PlayerSkillData.WeaponType;
import minecraftarmorweapon.skill.PlayerSkillData.SkillType;

import java.util.*;

/**
 * 全ての選択可能な技を登録・管理するレジストリ
 */
public class SkillRegistry {

    public static class SkillInfo {
        private final String id;
        private final String displayName;
        private final String description;
        private final WeaponType weaponType;
        private final SkillType skillType;

        public SkillInfo(String id, String displayName, String description, WeaponType weaponType, SkillType skillType) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.weaponType = weaponType;
            this.skillType = skillType;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public WeaponType getWeaponType() { return weaponType; }
        public SkillType getSkillType() { return skillType; }
    }

    private static final Map<WeaponType, Map<SkillType, List<SkillInfo>>> SKILLS = new LinkedHashMap<>();
    private static final Map<String, SkillInfo> BY_ID = new HashMap<>();

    static {
        // === 刀 (KATANA) の技 ===
        register("katana_iai", "居合斬り", "ダッシュ中に素早く抜刀して斬りつける", WeaponType.KATANA, SkillType.DASH_ATTACK);
        register("katana_3combo", "三段斬り", "左上→右上→横の三連撃", WeaponType.KATANA, SkillType.NORMAL_ATTACK);
        register("katana_spin", "回転斬り", "周囲の敵を巻き込む回転攻撃", WeaponType.KATANA, SkillType.CHARGED_ATTACK);

        // === 直刀 (STRAIGHT_SWORD) の技 ===
        register("sword_dash_thrust", "突きながら前進", "突きながら素早く前進する", WeaponType.STRAIGHT_SWORD, SkillType.DASH_ATTACK);
        register("sword_rapid_thrust", "素早い突きの連打", "連続で素早い突きを繰り出す", WeaponType.STRAIGHT_SWORD, SkillType.NORMAL_ATTACK);
        register("sword_heavy_thrust", "強力な突き", "力を込めた強力な一突き", WeaponType.STRAIGHT_SWORD, SkillType.CHARGED_ATTACK);

        // === 特殊 (SPECIAL) の技 ===
        register("special_electric_beam", "電撃ビーム", "前方に電撃のビームを放つ", WeaponType.SPECIAL, SkillType.DASH_ATTACK);
        register("special_electric_slash", "電撃斬り", "電気を纏った斬撃を放つ", WeaponType.SPECIAL, SkillType.NORMAL_ATTACK);
        register("special_discharge", "放電バースト", "周囲に電撃を放出する", WeaponType.SPECIAL, SkillType.CHARGED_ATTACK);
    }

    private static void register(String id, String displayName, String description, WeaponType weaponType, SkillType skillType) {
        SkillInfo info = new SkillInfo(id, displayName, description, weaponType, skillType);
        SKILLS.computeIfAbsent(weaponType, k -> new LinkedHashMap<>())
              .computeIfAbsent(skillType, k -> new ArrayList<>())
              .add(info);
        BY_ID.put(id, info);
    }

    public static List<SkillInfo> getSkills(WeaponType weaponType, SkillType skillType) {
        return SKILLS.getOrDefault(weaponType, Collections.emptyMap())
                     .getOrDefault(skillType, Collections.emptyList());
    }

    public static SkillInfo getById(String id) {
        return BY_ID.get(id);
    }

    public static Collection<SkillInfo> getAllSkills() {
        return BY_ID.values();
    }
}
