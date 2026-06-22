package the_four_primitives_and_weapons.damage;

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
    WATER("water"),        // 水属性
    MIASMA("miasma"),      // 瘴気属性
    BLOOD("blood"),        // 血属性 (Rivers of Blood 系)
    ERROR("error");        // エラー属性

    private final String name;

    ElementType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ElementType fromString(String name) {
        if (name == null) return NONE;
        for (ElementType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
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
            case ICE:       return FIRE;     
            case FIRE:      return WATER;  
            case ELECTRIC:  return WIND;      
            case THUNDER:   return WIND;   
            case CORROSION: return WATER;     
            case HOLY:      return DARK;     
            case DARK:      return HOLY;   
            case WATER:     return THUNDER; 
            case WIND:      return ICE;     
            case MIASMA:    return HOLY;
            case BLOOD:     return HOLY;   // 血 ↔ 聖
            case ERROR:     return NONE;
            default:        return NONE;
        }
    }
}
