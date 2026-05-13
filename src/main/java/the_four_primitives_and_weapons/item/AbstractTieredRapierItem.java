package the_four_primitives_and_weapons.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * 階層別レイピアの共通基底。
 *
 * 攻撃速度・ダメージは通常剣と同等。
 * 「突き」が高速になる挙動は data/weapon_types/preferred_motions.jsonc で
 * "rapier" → ["thrust"] が指定されていることに依存（MotionExecutor が
 * 得意技かつ thrust の場合に後硬直をリセットする）。
 */
public abstract class AbstractTieredRapierItem extends SwordItem {
    protected AbstractTieredRapierItem(Tier tier, int damageModifier, Item.Properties properties) {
        super(tier, damageModifier, -2.4f, properties);
    }

    protected AbstractTieredRapierItem(Tier tier, int damageModifier) {
        this(tier, damageModifier, new Item.Properties());
    }
}
