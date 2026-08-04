package the_four_primitives_and_weapons.item;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class DiamondDaggerItem extends AbstractDaggerItem {
    public DiamondDaggerItem() {
        super(new Tier() {
            public int getUses() { return 1250; }
            public float getSpeed() { return 8f; }
            public float getAttackDamageBonus() { return 3f; }
            public int getLevel() { return 3; }
            public int getEnchantmentValue() { return 10; }
            public Ingredient getRepairIngredient() { return Ingredient.of(Items.DIAMOND); }
        }, 2, -1.8f, new Item.Properties());
    }
}
