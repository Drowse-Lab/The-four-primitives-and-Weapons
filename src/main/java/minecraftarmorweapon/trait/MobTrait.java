package minecraftarmorweapon.trait;

/**
 * Mob特性システム — 難易度に応じてMobにランダムな特性を付与する
 *
 * 特性は10種類あり、レアリティが高いほど出にくく強力:
 *   IRON_WALL    (鉄壁)     … 防御力が非常に高い、ノックバック耐性
 *   BERSERKER    (狂戦士)    … 攻撃力が非常に高い、HP低下で更に強化
 *   SWIFT        (迅速)     … 移動速度が非常に速い、回避能力
 *   REGENERATOR  (再生者)    … HPが自動回復する
 *   WEB_SPINNER  (蜘蛛糸)   … プレイヤーの足元に蜘蛛の巣を設置、移動妨害
 *   REFLECTOR    (反射)     … 近接ダメージの一部を攻撃者に反射する
 *   ARROW_GUARD  (矢盾)     … 飛び道具ダメージを大幅カット〜完全無効化
 *   CURSE_BEARER (呪縛者)    … 攻撃時に衰弱・盲目・毒・鈍化などのデバフを付与
 *   EXPLOSIVE    (爆裂)     … 攻撃時に爆発ダメージ、死亡時に爆発
 *   UNDYING      (不死)     … 最上位特性。不死のトーテムを所持し、
 *                             使われるたびにトーテムが再補充される
 *
 * レアリティ: COMMON → UNCOMMON → RARE → LEGENDARY
 */
public enum MobTrait {
    // name            displayName     colorCode  rarity         weight
    IRON_WALL(     "鉄壁",       "§9",    Rarity.COMMON,    30),
    BERSERKER(     "狂戦士",      "§c",    Rarity.COMMON,    30),
    SWIFT(         "迅速",       "§b",    Rarity.UNCOMMON,  20),
    REGENERATOR(   "再生者",      "§a",    Rarity.UNCOMMON,  20),
    WEB_SPINNER(   "蜘蛛糸",     "§8",    Rarity.UNCOMMON,  18),
    REFLECTOR(     "反射",       "§3",    Rarity.UNCOMMON,  15),
    ARROW_GUARD(   "矢盾",       "§d",    Rarity.RARE,      12),
    POISONER(      "猛毒",       "§2",    Rarity.UNCOMMON,  16),
    BLINDER(       "盲目",       "§8",    Rarity.RARE,      10),
    NAUSEATOR(     "吐き気",     "§e",    Rarity.UNCOMMON,  14),
    WEAKENER(      "弱体",       "§7",    Rarity.RARE,      10),
    EXPLOSIVE(     "爆裂",       "§6",    Rarity.RARE,      10),
    LEARNER(       "学習",       "§b",    Rarity.RARE,      12),
    UNDYING(       "不死",       "§4§l",  Rarity.LEGENDARY,  3);

    public enum Rarity {
        COMMON("§f", 1),
        UNCOMMON("§a", 2),
        RARE("§9", 3),
        LEGENDARY("§6§l", 4);

        private final String color;
        private final int tier;

        Rarity(String color, int tier) {
            this.color = color;
            this.tier = tier;
        }

        public String getColor() { return color; }
        public int getTier() { return tier; }
    }

    private final String displayName;
    private final String colorCode;
    private final Rarity rarity;
    private final int weight;

    MobTrait(String displayName, String colorCode, Rarity rarity, int weight) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.rarity = rarity;
        this.weight = weight;
    }

    public String getDisplayName() { return displayName; }
    public String getColorCode() { return colorCode; }
    public Rarity getRarity() { return rarity; }
    public int getWeight() { return weight; }

    /** 色付き表示名 例: "§9[鉄壁]" */
    public String getFormattedName() {
        return colorCode + "[" + displayName + "]";
    }

    /** 全特性の重みの合計 */
    public static int getTotalWeight() {
        int total = 0;
        for (MobTrait trait : values()) {
            total += trait.weight;
        }
        return total;
    }

    /**
     * 重み付きランダムで特性を選択
     * aiLevelが高いほど高レアリティが出やすくなる
     */
    public static MobTrait rollTrait(float random, int aiLevel) {
        int[] adjustedWeights = new int[values().length];
        int totalAdjusted = 0;

        for (int i = 0; i < values().length; i++) {
            MobTrait trait = values()[i];
            int w = trait.weight;

            if (aiLevel >= 4 && trait.rarity == Rarity.LEGENDARY) {
                w = (int)(w * (1.0 + aiLevel * 0.5));
            } else if (aiLevel >= 3 && trait.rarity == Rarity.RARE) {
                w = (int)(w * 1.5);
            }

            adjustedWeights[i] = w;
            totalAdjusted += w;
        }

        int target = (int)(random * totalAdjusted);
        int cumulative = 0;
        for (int i = 0; i < values().length; i++) {
            cumulative += adjustedWeights[i];
            if (target < cumulative) {
                return values()[i];
            }
        }
        return IRON_WALL;
    }
}
