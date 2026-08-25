package the_four_primitives_and_weapons.skill;

import the_four_primitives_and_weapons.api.ISkillAction;
import the_four_primitives_and_weapons.skill.PlayerSkillData.AttackSlot;

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
        /** 翻訳キー: motion.the_four_primitives_and_weapons.&lt;id&gt; (未翻訳の場合は displayName にフォールバック) */
        public String translationKey() { return "motion.the_four_primitives_and_weapons." + id; }
        /** 説明文の翻訳キー: motion.the_four_primitives_and_weapons.&lt;id&gt;.desc */
        public String descriptionTranslationKey() { return "motion.the_four_primitives_and_weapons." + id + ".desc"; }
    }

    private static final Map<String, MotionInfo> BY_ID = new LinkedHashMap<>();
    private static final Map<String, ISkillAction> HANDLERS = new HashMap<>();

    static {
        Set<AttackSlot> allSlots = EnumSet.allOf(AttackSlot.class);
        // 基本モーションはDASH以外のスロットで使用可能
        Set<AttackSlot> combatSlots = EnumSet.of(
                AttackSlot.FIRST_HIT, AttackSlot.SECOND_HIT,
                AttackSlot.THIRD_HIT, AttackSlot.CHARGED);

        // === 5つの基本モーション（一撃目〜チャージまで） ===
        register("thrust", "突き", "直線的な突き攻撃",
                MotionCategory.UNIVERSAL, combatSlots, null);
        // 連撃はチャージ専用。 一撃目〜三撃目に置くと「連撃の中身」と自己参照になるうえ、
        // チャージ時間で段数が伸びる技 ( SkillComboProcedure ) なのでチャージ以外では意味がない。
        register("thrust_combo", "高速連撃", "チャージ専用。 一撃目・二撃目・三撃目に設定した技を高速で連続発動する ( 溜めるほど段数が増える )",
                MotionCategory.UNIVERSAL, EnumSet.of(AttackSlot.CHARGED), null);
        register("upper_left_slash", "左上斬り", "左上からの斜め斬り",
                MotionCategory.UNIVERSAL, combatSlots, null);
        register("upper_right_slash", "右上斬り", "右上からの斜め斬り",
                MotionCategory.UNIVERSAL, combatSlots, null);
        register("horizontal_slash", "横一文字", "広範囲の横薙ぎ",
                MotionCategory.UNIVERSAL, combatSlots, null);
        register("spin_slash", "回転斬り", "360度の範囲攻撃",
                MotionCategory.UNIVERSAL, combatSlots, null);
        register("slam_down", "叩きつける", "上から振り下ろす強力な一撃。地面を揺らし敵を打ち下ろす",
                MotionCategory.UNIVERSAL, combatSlots, null);

        // === ダッシュ専用モーション（DASHスロットのみ） ===
        Set<AttackSlot> dashOnly = EnumSet.of(AttackSlot.DASH);

        register("dash_rush", "突進斬り", "走り抜けながら通過した場所の敵にダメージ",
                MotionCategory.UNIVERSAL, dashOnly, null);
        register("leap_slash", "跳ね斬り", "跳躍後の移動中に攻撃するとダメージ増加",
                MotionCategory.UNIVERSAL, dashOnly, null);
        register("shadow_step", "影歩き", "5tick無敵の黒い影で高速移動（攻撃/武器変更で解除）",
                MotionCategory.UNIVERSAL, dashOnly, null);

        // === 右クリック専用モーション（RIGHT_CLICKスロットのみ） ===
        Set<AttackSlot> rightClickOnly = EnumSet.of(AttackSlot.RIGHT_CLICK);

        register("dodge", "回避", "右クリックで回避行動を行う（デフォルト）",
                MotionCategory.UNIVERSAL, rightClickOnly, null);
        register("none_right", "なし", "右クリックで何もしない（回避OFF）",
                MotionCategory.UNIVERSAL, rightClickOnly, null);
        // スペル: 右クリックで Iron's Spellbooks の登録スペルを発動（回避しない）。
        // Iron's Spellbooks 導入時のみ選択肢に表示される（getAvailableMotions* でフィルタ）。
        register("spell", "スペル", "右クリックで登録スペルを発動する（Iron's Spellbooks）。回避は行わない",
                MotionCategory.UNIVERSAL, rightClickOnly, null);
        register("trident_throw", "投擲", "右クリックでトライデントを投擲する（vanilla挙動）",
                MotionCategory.UNIVERSAL, rightClickOnly, null);
        register("bow_power_shot", "集中射撃", "矢のダメージ +4 + クリティカル",
                MotionCategory.UNIVERSAL, rightClickOnly, null);
        register("bow_explosive", "爆裂矢", "矢が命中時に爆発（範囲ダメージ）",
                MotionCategory.UNIVERSAL, rightClickOnly, null);
        register("bow_pierce", "貫通矢", "矢が貫通（最大5体）+ ダメージ +1",
                MotionCategory.UNIVERSAL, rightClickOnly, null);
        register("bow_heavy_blow", "強撃", "矢のダメージ +2",
                MotionCategory.UNIVERSAL, rightClickOnly, null);
        register("bow_homing", "追尾矢", "矢が近くの敵に追尾する",
                MotionCategory.UNIVERSAL, rightClickOnly, null);
        register("bow_rapid_fire", "連射", "射撃時に追加で2本同時射出",
                MotionCategory.UNIVERSAL, rightClickOnly, null);
        register("bow_arrow_rain", "矢の雨", "命中地点に8本の矢が降り注ぐ",
                MotionCategory.UNIVERSAL, rightClickOnly, null);
        register("bow_quick_draw", "速射", "矢の速度 ×1.5 (引き絞り時間に依らず高速)",
                MotionCategory.UNIVERSAL, rightClickOnly, null);
        register("bow_wind", "風纏い", "矢が無重力 + 速度 ×1.5 + ノックバック強化",
                MotionCategory.UNIVERSAL, rightClickOnly, null);
        register("gate_special", "Gate・投刃", "右クリック:放射状に直刀を投げる",
                MotionCategory.SPECIAL, rightClickOnly, "GateItem");
        register("rivers_of_blood_special", "Hemorrhagic Eclipse", "右クリック連打:前方扇形に血斬撃 (連撃) / bleed + 命中ごとに +1.5HP 吸収",
                MotionCategory.SPECIAL, rightClickOnly, "RiversOfBloodItem");
        register("giant_bone_arm_special", "巨骨の腕", "右クリック長押し:肩から巨大な骨の腕を生やし 薙ぎ払い→地叩き。着弾点から半径16mを揺らす",
                MotionCategory.SPECIAL, rightClickOnly, "KatanaNiguHumerusItem");

        // === Shift+右クリック専用モーション ===
        Set<AttackSlot> shiftRightClickOnly = EnumSet.of(AttackSlot.SHIFT_RIGHT_CLICK);

        register("guard", "ガード", "Shift+右クリックでガードを行う",
                MotionCategory.UNIVERSAL, shiftRightClickOnly, null);
        register("none_shift", "なし", "Shift+右クリックで何もしない",
                MotionCategory.UNIVERSAL, shiftRightClickOnly, null);

        // === 特殊技（武器をスロットに登録すると使用可能） ===
        register("electric_beam", "電撃ビーム", "前方に電撃のビームを放つ",
                MotionCategory.SPECIAL, allSlots, "KurikarakenItem");
        register("electric_slash", "電撃斬り", "電気を纏った斬撃波",
                MotionCategory.SPECIAL, allSlots, "KurikarakenItem");
        register("electric_discharge", "放電バースト", "周囲に電撃を放出",
                MotionCategory.SPECIAL, allSlots, "KurikarakenItem");
        // 「magic_katana_special」 は MagicalKatanaItem ( 侵食ベース ) と
        // MagischesFeenKatanaItem ( 妖精ベース ) の両方で使えるデフォルト技
        register("magic_katana_special", "魔法刀・特殊攻撃", "魔法を纏った特殊攻撃",
                MotionCategory.SPECIAL, allSlots, "MagicalKatanaItem,MagischesFeenKatanaItem");
    }

    /**
     * requiredWeaponClass の文字列 ( カンマ区切り可 ) と weaponClassName を照合する。
     * 例: required = "MagicalKatanaItem,MagischesFeenKatanaItem"、 weaponClassName = "MagicalKatanaItem" → true
     */
    private static boolean weaponClassMatches(String requiredWeaponClass, String weaponClassName) {
        if (requiredWeaponClass == null || weaponClassName == null) return false;
        if (requiredWeaponClass.equals(weaponClassName)) return true;
        for (String s : requiredWeaponClass.split(",")) {
            if (s.trim().equals(weaponClassName)) return true;
        }
        return false;
    }

    /**
     * モーションを登録する（ハンドラーなし、内部モーション用）。
     */
    public static void register(String id, String displayName, String description,
                                 MotionCategory category, Set<AttackSlot> compatibleSlots,
                                 String requiredWeaponClass) {
        BY_ID.put(id, new MotionInfo(id, displayName, description, category, compatibleSlots, requiredWeaponClass));
    }

    /**
     * モーションを登録する（ISkillActionハンドラー付き、外部mod用）。
     *
     * <pre>
     * SkillRegistry.register("my_mod:fire_slash", "炎斬り", "炎を纏った斬撃",
     *     MotionCategory.SPECIAL, EnumSet.allOf(AttackSlot.class), "MyWeaponItem",
     *     (player, chargePercent) -> {
     *         float damage = ChargeHelper.scaleDamage(10.0f, chargePercent, 2.0f);
     *         ChargeHelper.damageEntitiesInFront(player, 5.0, 3.0, damage);
     *     });
     * </pre>
     */
    public static void register(String id, String displayName, String description,
                                 MotionCategory category, Set<AttackSlot> compatibleSlots,
                                 String requiredWeaponClass, ISkillAction handler) {
        BY_ID.put(id, new MotionInfo(id, displayName, description, category, compatibleSlots, requiredWeaponClass));
        HANDLERS.put(id, handler);
    }

    /**
     * 登録済みハンドラーを取得する（なければnull）。
     */
    public static ISkillAction getHandler(String motionId) {
        return HANDLERS.get(motionId);
    }

    /**
     * 指定スロットで使用可能なモーション一覧を取得（武器クラス指定）
     * UNIVERSAL + その武器クラスに対応したSPECIALを返す。
     * bow_* モーションは bow / crossbow を持っている時のみ含む (他modの BowItem 互換も
     * WeaponTypeRegistry の instanceof フォールバックで bow/crossbow タイプとして
     * 解決されるので、ここに来るケースは「武器なし or 他種類」のみ)。
     */
    /** 登録された全 motion を返す ( /motion コマンド suggest 用 ) */
    public static java.util.Collection<MotionInfo> getAllMotions() {
        return BY_ID.values();
    }

    public static List<MotionInfo> getAvailableMotions(AttackSlot slot, String weaponClass) {
        List<MotionInfo> result = new ArrayList<>();
        for (MotionInfo info : BY_ID.values()) {
            if (!info.compatibleSlots.contains(slot)) continue;
            // spell は Iron's Spellbooks 導入時のみ
            if (info.id.equals("spell")
                    && !the_four_primitives_and_weapons.compat.SpellbooksCompat.isLoaded()) continue;
            if (info.category == MotionCategory.UNIVERSAL) {
                // bow_* は弓/クロスボウ専用 — デフォルトロードアウト等では除外
                if (info.id.startsWith("bow_")) continue;
                result.add(info);
            } else if (info.category == MotionCategory.SPECIAL
                    && weaponClass != null
                    && weaponClassMatches(info.requiredWeaponClass, weaponClass)) {
                result.add(info);
            }
        }
        return result;
    }

    /**
     * JSON武器タイプ定義を使用してモーション一覧を取得（ItemStack版）
     * WeaponTypeRegistryが読み込み済みならJSONの定義を優先する
     */
    public static List<MotionInfo> getAvailableMotionsForWeapon(AttackSlot slot, net.minecraft.world.item.ItemStack weapon) {
        if (weapon != null && !weapon.isEmpty() && WeaponTypeRegistry.isLoaded()) {
            List<String> motionIds = WeaponTypeRegistry.getAvailableMotionIds(weapon, slot);
            if (!motionIds.isEmpty()) {
                List<MotionInfo> result = new ArrayList<>();
                for (String id : motionIds) {
                    // spell は Iron's Spellbooks 導入時のみ
                    if (id.equals("spell")
                            && !the_four_primitives_and_weapons.compat.SpellbooksCompat.isLoaded()) continue;
                    MotionInfo info = getById(id);
                    if (info == null) continue;
                    // そのスロットに置けない技は出さない ( 例: 連撃はチャージ専用 )。
                    // weapon_types の combat リストは 一撃目〜チャージ で共有なので、
                    // ここで絞らないと 連撃が通常技の選択肢に並んでしまう
                    // ( クラス名ベースの getAvailableMotions 側は元々絞っている )。
                    if (!info.compatibleSlots.contains(slot)) continue;
                    result.add(info);
                }
                return result;
            }
        }
        // フォールバック: 従来のクラス名ベース
        String weaponClass = (weapon != null && !weapon.isEmpty())
                ? weapon.getItem().getClass().getSimpleName() : null;
        return getAvailableMotions(slot, weaponClass);
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
                && weaponClassMatches(info.requiredWeaponClass, weaponClassName)) {
                result.add(info.id);
            }
        }
        return result;
    }
}
