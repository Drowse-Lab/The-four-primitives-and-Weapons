package the_four_primitives_and_weapons.item;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class NetheriteDaggerItem extends SwordItem {
    public NetheriteDaggerItem() {
        super(new Tier() {
            public int getUses() { return 1625; }
            public float getSpeed() { return 9f; }
            public float getAttackDamageBonus() { return 4f; }
            public int getLevel() { return 4; }
            public int getEnchantmentValue() { return 15; }
            public Ingredient getRepairIngredient() { return Ingredient.of(Items.NETHERITE_INGOT); }
        }, 2, -1.8f, new Item.Properties().fireResistant());
    }
}
