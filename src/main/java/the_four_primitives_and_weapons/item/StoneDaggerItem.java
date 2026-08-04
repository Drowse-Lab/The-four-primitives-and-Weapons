package the_four_primitives_and_weapons.item;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class StoneDaggerItem extends AbstractDaggerItem {
    public StoneDaggerItem() {
        super(new Tier() {
            public int getUses() { return 105; }
            public float getSpeed() { return 4f; }
            public float getAttackDamageBonus() { return 1f; }
            public int getLevel() { return 1; }
            public int getEnchantmentValue() { return 5; }
            public Ingredient getRepairIngredient() { return Ingredient.of(Items.COBBLESTONE); }
        }, 2, -1.8f, new Item.Properties());
    }
}
