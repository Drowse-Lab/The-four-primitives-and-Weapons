package minecraftarmorweapon.damage;

/**
 * 属性タイプの列挙型
 */
public enum ElementType {
    NONE("none"),           // 無属性
    ICE("ice"),            // 氷属性
    ELECTRIC("electric"),  // 電気/雷属性
    CORROSION("corrosion"), // 侵食/闇属性
    HOLY("holy"),          // 聖属性
    DARK("dark"),          // 闇属性
    FIRE("fire"),          // 火属性
    WIND("wind"),          // 風属性
    THUNDER("thunder"),    // 雷属性
    WATER("water");        // 水属性

    private final String name;

    ElementType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ElementType fromString(String name) {
        for (ElementType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return NONE;
    }

    /**
     * この属性ダメージを無効化できるカウンター属性を返す
     * bookスロットにカウンター属性の魔導書があれば、ダメージ無効化
     */
    public ElementType getCounterElement() {
        switch (this) {
            case ICE:       return FIRE;      // 火は氷を溶かす
            case FIRE:      return WATER;     // 水は火を消す
            case ELECTRIC:  return WIND;      // 風は雷を散らす
            case THUNDER:   return WIND;      // 風は雷を散らす
            case CORROSION: return WATER;     // 水は腐食を洗い流す
            case HOLY:      return DARK;      // 闇は聖を打ち消す
            case DARK:      return HOLY;      // 聖は闇を浄化する
            case WATER:     return THUNDER;   // 雷は水を蒸発させる
            case WIND:      return ICE;       // 氷は風を凍てつかせる
            default:        return NONE;
        }
    }
}
