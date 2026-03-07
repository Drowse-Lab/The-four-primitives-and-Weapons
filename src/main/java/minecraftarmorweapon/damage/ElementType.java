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
}
