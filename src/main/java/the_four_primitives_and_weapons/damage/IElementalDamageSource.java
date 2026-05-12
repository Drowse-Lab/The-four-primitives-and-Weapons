package the_four_primitives_and_weapons.damage;

/**
 * DamageSourceに属性を追加するためのインターフェース
 * Mixinを使用してDamageSourceに実装されます
 */
public interface IElementalDamageSource {
    /**
     * 属性タイプを取得
     */
    ElementType getElementType();

    /**
     * 属性タイプを設定
     */
    void setElementType(ElementType type);

    /**
     * 属性レベルを取得
     */
    int getElementLevel();

    /**
     * 属性レベルを設定
     */
    void setElementLevel(int level);
}
