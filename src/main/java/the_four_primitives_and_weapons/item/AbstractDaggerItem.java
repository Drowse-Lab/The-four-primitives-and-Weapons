package the_four_primitives_and_weapons.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * ダガー共通の基底。
 *
 * <p>近接武器としての振る舞いは {@link SwordItem} のまま。 このクラス自体は
 * 「ダガーである」ことを示す目印として使う。</p>
 *
 * <p><b>投擲について</b>: ダガーは手に持って直接投げることはできない。
 * ナイフホルダー ( {@link KnifeLauncherItem} ) に装填した時だけ発射できる
 * ( {@link KnifeLauncherItem#isStorableKnife} がこの型を見て収納を許可する )。
 * 発射された個体は そのまま飛んで回収できるので、 エンチャントや拵えの染色は失われない。</p>
 */
public abstract class AbstractDaggerItem extends SwordItem {

    protected AbstractDaggerItem(Tier tier, int attackDamage, float attackSpeed, Item.Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }
}
