package minecraftarmorweapon.item;

import minecraftarmorweapon.entity.ThrowingKnifeEntity.KnifeType;

/**
 * オートエームナイフ — 視線先30°コーン内のMobへ初期軌道を補正。MP消費大。
 */
public class HomingKnifeItem extends ThrowingKnifeItem {
    public HomingKnifeItem() { super(16, 3.0); }
    @Override public KnifeType getKnifeType() { return KnifeType.HOMING; }
    @Override public double manaCost() { return 20; }
    @Override public int cooldown() { return 10; }
}
