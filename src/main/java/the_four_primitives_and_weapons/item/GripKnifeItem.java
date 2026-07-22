package the_four_primitives_and_weapons.item;

import the_four_primitives_and_weapons.entity.ThrowingKnifeEntity.KnifeType;

/**
 * グリップナイフ — 通常版より少し攻撃力↑、満腹度消費なし。
 */
public class GripKnifeItem extends ThrowingKnifeItem {
    public GripKnifeItem() { super(16, 4.0); }
    @Override public KnifeType getKnifeType() { return KnifeType.GRIP; }
    @Override public float hungerCost() { return 0; }
    @Override public int cooldown() { return 6; }
}
