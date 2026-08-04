package the_four_primitives_and_weapons.item;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class IronDaggerItem extends AbstractDaggerItem {
    public IronDaggerItem() {
        super(new Tier() {
            public int getUses() { return 200; }
            public float getSpeed() { return 4f; }
            public float getAttackDamageBonus() { return 2f; }
            public int getLevel() { return 2; }
            public int getEnchantmentValue() { return 14; }
            public Ingredient getRepairIngredient() { return Ingredient.of(Items.IRON_INGOT); }
        }, 2, -1.8f, new Item.Properties());
    }
}
