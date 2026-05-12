package the_four_primitives_and_weapons.item;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class StoneTyokutoItem extends SwordItem {
    public StoneTyokutoItem() {
        super(new Tier() {
            public int getUses() { return 131; }
            public float getSpeed() { return 4f; }
            public float getAttackDamageBonus() { return 1f; }
            public int getLevel() { return 1; }
            public int getEnchantmentValue() { return 5; }
            public Ingredient getRepairIngredient() { return Ingredient.of(Items.COBBLESTONE); }
        }, 3, -2.4f, new Item.Properties());
    }
}
