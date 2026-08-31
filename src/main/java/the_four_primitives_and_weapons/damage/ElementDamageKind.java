package the_four_primitives_and_weapons.damage;

/**
 * 属性ダメージの「与え方」の種類。
 *
 * <p>{@link ElementType} ( 何属性か ) とは独立した軸で、どの属性でも 3 種類とも取れる。
 * アイテムの NBT {@code ElementDamageKind} で指定し、未設定なら従来どおり
 * {@link #PHYSICAL} として扱う。</p>
 *
 * <ul>
 *   <li>{@link #PHYSICAL} … 一発で与える。防具・防具エンチャで軽減される ( 従来の挙動 )</li>
 *   <li>{@link #MAGIC}    … 一発で与える。防具・防具エンチャを貫通する ( 耐性ポーションは効く )</li>
 *   <li>{@link #BUILDUP}  … 一発では与えず、衰弱のように時間をかけて削る</li>
 * </ul>
 */
public enum ElementDamageKind {
    PHYSICAL("physical"),  // 物理属性ダメージ
    MAGIC("magic"),        // 魔法属性ダメージ (防御貫通)
    BUILDUP("buildup");    // 蓄積属性ダメージ (持続)

    private final String name;

    ElementDamageKind(String name) {
        this.name = name;
    }

    /** NBT / コマンド引数に使う識別子。 */
    public String getName() {
        return name;
    }

    public String getTranslationKey() {
        return "tooltip.the_four_primitives_and_weapons.element_kind." + name;
    }

    /** 未設定・不明な文字列は従来どおり {@link #PHYSICAL} 扱い。 */
    public static ElementDamageKind fromString(String name) {
        if (name == null) return PHYSICAL;
        for (ElementDamageKind kind : values()) {
            if (kind.name.equalsIgnoreCase(name)) {
                return kind;
            }
        }
        return PHYSICAL;
    }
}
