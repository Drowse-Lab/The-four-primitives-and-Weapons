package the_four_primitives_and_weapons.item;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class NetheriteTyokutoItem extends SwordItem {
    public NetheriteTyokutoItem() {
        super(new Tier() {
            public int getUses() { return 2031; }
            public float getSpeed() { return 9f; }
            public float getAttackDamageBonus() { return 8f; }
            public int getLevel() { return 4; }
            public int getEnchantmentValue() { return 15; }
            public Ingredient getRepairIngredient() { return Ingredient.of(Items.NETHERITE_INGOT); }
        }, 3, -2.4f, new Item.Properties().fireResistant());
    }
}
