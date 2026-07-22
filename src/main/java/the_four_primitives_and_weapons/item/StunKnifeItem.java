package the_four_primitives_and_weapons.item;

import the_four_primitives_and_weapons.entity.ThrowingKnifeEntity.KnifeType;

/**
 * スタンナイフ — 単発火力↑、命中で感電(移動低下+弱体化)、満腹度消費 大。
 */
public class StunKnifeItem extends ThrowingKnifeItem {
    public StunKnifeItem() { super(16, 5.0); }
    @Override public KnifeType getKnifeType() { return KnifeType.STUN; }
    @Override public float hungerCost() { return 2.5f; }
    @Override public int cooldown() { return 16; }
}
