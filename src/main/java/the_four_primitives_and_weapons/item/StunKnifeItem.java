package the_four_primitives_and_weapons.item;

import the_four_primitives_and_weapons.entity.ThrowingKnifeEntity.KnifeType;

/**
 * スタンナイフ — 単発火力↑、命中で感電(移動低下+弱体化)、MP消費大。
 */
public class StunKnifeItem extends ThrowingKnifeItem {
    public StunKnifeItem() { super(16, 5.0); }
    @Override public KnifeType getKnifeType() { return KnifeType.STUN; }
    @Override public double manaCost() { return 25; }
    @Override public int cooldown() { return 16; }
}
