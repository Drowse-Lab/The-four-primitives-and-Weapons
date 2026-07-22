package the_four_primitives_and_weapons.item;

import the_four_primitives_and_weapons.entity.ThrowingKnifeEntity.KnifeType;

/**
 * スクリューナイフ — 木材/木製コンテナを貫通破壊。満腹度消費 中。
 */
public class ScrewKnifeItem extends ThrowingKnifeItem {
    public ScrewKnifeItem() { super(16, 3.0); }
    @Override public KnifeType getKnifeType() { return KnifeType.SCREW; }
    @Override public float hungerCost() { return 1.0f; }
    @Override public int cooldown() { return 10; }
}
