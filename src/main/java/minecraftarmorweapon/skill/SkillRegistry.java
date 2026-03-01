package minecraftarmorweapon.skill;

import minecraftarmorweapon.skill.PlayerSkillData.AttackSlot;

import java.util.*;

/**
 * 全ての選択可能なモーション・特殊技を登録・管理するレジストリ
 */
public class SkillRegistry {

    public enum MotionCategory {
        UNIVERSAL,  // 最初から全スロットで選択可能
        SPECIAL     // 武器をスロットに登録すると利用可能
    }

    public static class MotionInfo {
        private final String id;
        private final String displayName;
        private final String description;
        private final MotionCategory category;
        private final Set<AttackSlot> compatibleSlots;
        private final String requiredWeaponClass; // null for UNIVERSAL

        public MotionInfo(String id, String displayName, String description,
                         MotionCategory category, Set<AttackSlot> compatibleSlots,
                         String requiredWeaponClass) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.category = category;
            this.compatibleSlots = compatibleSlots;
            this.requiredWeaponClass = requiredWeaponClass;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public MotionCategory getCategory() { return category; }
        public Set<AttackSlot> getCompatibleSlots() { return compatibleSlots; }
        public String getRequiredWeaponClass() { return requiredWeaponClass; }
    }

    private static final Map<String, MotionInfo> BY_ID = new LinkedHashMap<>();

    static {
        Set<AttackSlot> allSlots = EnumSet.allOf(AttackSlot.class);
        // 基本モーションはDASH以外のスロットで使用可能
        Set<AttackSlot> combatSlots = EnumSet.of(
                AttackSlot.FIRST_HIT, AttackSlot.SECOND_HIT,
                AttackSlot.THIRD_HIT, AttackSlot.CHARGED);

        // === 5つの基本モーション（一撃目〜チャージまで） ===
        register("thrust", "突き", "直線的な突き攻撃",
                MotionCategory.UNIVERSAL, combatSlots, null);
        register("upper_left_slash", "左上斬り", "左上からの斜め斬り",
                MotionCategory.UNIVERSAL, combatSlots, null);
        register("upper_right_slash", "右上斬り", "右上からの斜め斬り",
                MotionCategory.UNIVERSAL, combatSlots, null);
        register("horizontal_slash", "横一文字", "広範囲の横薙ぎ",
                MotionCategory.UNIVERSAL, combatSlots, null);
        register("spin_slash", "回転斬り", "360度の範囲攻撃",
                MotionCategory.UNIVERSAL, combatSlots, null);

        // === ダッシュ専用モーション（DASHスロットのみ） ===
        Set<AttackSlot> dashOnly = EnumSet.of(AttackSlot.DASH);

        register("dash_rush", "突進斬り", "走り抜けながら通過した場所の敵にダメージ",
                MotionCategory.UNIVERSAL, dashOnly, null);
        register("leap_slash", "跳ね斬り", "跳躍後の移動中に攻撃するとダメージ増加",
                MotionCategory.UNIVERSAL, dashOnly, null);
        register("shadow_step", "影歩き", "5tick無敵の黒い影で高速移動（攻撃/武器変更で解除）",
                MotionCategory.UNIVERSAL, dashOnly, null);

        // === 特殊技（武器をスロットに登録すると使用可能） ===
        register("electric_beam", "電撃ビーム", "前方に電撃のビームを放つ",
                MotionCategory.SPECIAL, allSlots, "KurikarakenItem");
        register("electric_slash", "電撃斬り", "電気を纏った斬撃波",
                MotionCategory.SPECIAL, allSlots, "KurikarakenutigatanaItem");
        register("electric_discharge", "放電バースト", "周囲に電撃を放出",
                MotionCategory.SPECIAL, allSlots, "KurikarakenswordItem");
        register("sword_of_night_tp", "夜の剣・転移斬", "テレポートして斬りつける",
                MotionCategory.SPECIAL, allSlots, "SwordOfNightItem");
        register("magic_katana_special", "魔法刀・特殊攻撃", "魔法を纏った特殊攻撃",
                MotionCategory.SPECIAL, allSlots, "MagischesFeenKatanaItem");
    }

    private static void register(String id, String displayName, String description,
                                  MotionCategory category, Set<AttackSlot> compatibleSlots,
                                  String requiredWeaponClass) {
        BY_ID.put(id, new MotionInfo(id, displayName, description, category, compatibleSlots, requiredWeaponClass));
    }

    /**
     * 指定スロットで使用可能なモーション一覧を取得（武器クラス指定）
     * UNIVERSAL + その武器クラスに対応したSPECIALを返す
     */
    public static List<MotionInfo> getAvailableMotions(AttackSlot slot, String weaponClass) {
        List<MotionInfo> result = new ArrayList<>();
        for (MotionInfo info : BY_ID.values()) {
            if (!info.compatibleSlots.contains(slot)) continue;
            if (info.category == MotionCategory.UNIVERSAL) {
                result.add(info);
            } else if (info.category == MotionCategory.SPECIAL
                    && weaponClass != null
                    && weaponClass.equals(info.requiredWeaponClass)) {
                result.add(info);
            }
        }
        return result;
    }

    /**
     * 基本モーション一覧を取得
     */
    public static List<MotionInfo> getUniversalMotions() {
        List<MotionInfo> result = new ArrayList<>();
        for (MotionInfo info : BY_ID.values()) {
            if (info.category == MotionCategory.UNIVERSAL) {
                result.add(info);
            }
        }
        return result;
    }

    /**
     * 特殊スキル一覧を取得
     */
    public static List<MotionInfo> getSpecialMotions() {
        List<MotionInfo> result = new ArrayList<>();
        for (MotionInfo info : BY_ID.values()) {
            if (info.category == MotionCategory.SPECIAL) {
                result.add(info);
            }
        }
        return result;
    }

    /**
     * IDからモーション情報を取得
     */
    public static MotionInfo getById(String id) {
        return BY_ID.get(id);
    }

    /**
     * 武器クラス名から対応する特殊スキルIDリストを取得
     */
    public static List<String> getSpecialIdsForWeapon(String weaponClassName) {
        List<String> result = new ArrayList<>();
        for (MotionInfo info : BY_ID.values()) {
            if (info.category == MotionCategory.SPECIAL
                && weaponClassName.equals(info.requiredWeaponClass)) {
                result.add(info.id);
            }
        }
        return result;
    }
}
