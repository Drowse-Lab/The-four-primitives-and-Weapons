package the_four_primitives_and_weapons.item;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class WoodenDaggerItem extends AbstractDaggerItem {
    public WoodenDaggerItem() {
        super(new Tier() {
            public int getUses() { return 50; }
            public float getSpeed() { return 2f; }
            public float getAttackDamageBonus() { return 0f; }
            public int getLevel() { return 0; }
            public int getEnchantmentValue() { return 15; }
            public Ingredient getRepairIngredient() { return Ingredient.of(net.minecraft.tags.ItemTags.PLANKS); }
        }, 2, -1.8f, new Item.Properties());
    }
}
