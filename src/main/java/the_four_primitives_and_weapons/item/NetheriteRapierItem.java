package the_four_primitives_and_weapons.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;

public class NetheriteRapierItem extends AbstractTieredRapierItem {
    public NetheriteRapierItem() {
        super(Tiers.NETHERITE, 2, new Item.Properties().fireResistant());
    }
}
