package the_four_primitives_and_weapons.item;

import the_four_primitives_and_weapons.entity.ThrowingKnifeEntity.KnifeType;

/**
 * オートエームナイフ — 視線先30°コーン内のMobへ初期軌道を補正。満腹度消費 大。
 */
public class HomingKnifeItem extends ThrowingKnifeItem {
    public HomingKnifeItem() { super(16, 3.0); }
    @Override public KnifeType getKnifeType() { return KnifeType.HOMING; }
    @Override public float hungerCost() { return 2.0f; }
    @Override public int cooldown() { return 10; }
}
