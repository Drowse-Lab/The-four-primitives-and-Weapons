package minecraftarmorweapon.item;

import minecraftarmorweapon.entity.ThrowingKnifeEntity.KnifeType;

/**
 * スクリューナイフ — 木材/木製コンテナを貫通破壊。MP消費中。
 */
public class ScrewKnifeItem extends ThrowingKnifeItem {
    public ScrewKnifeItem() { super(16, 3.0); }
    @Override public KnifeType getKnifeType() { return KnifeType.SCREW; }
    @Override public double manaCost() { return 10; }
    @Override public int cooldown() { return 10; }
}
