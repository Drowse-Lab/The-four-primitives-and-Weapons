package the_four_primitives_and_weapons.item;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class GoldDaggerItem extends SwordItem {
    public GoldDaggerItem() {
        super(new Tier() {
            public int getUses() { return 26; }
            public float getSpeed() { return 12f; }
            public float getAttackDamageBonus() { return 0f; }
            public int getLevel() { return 0; }
            public int getEnchantmentValue() { return 22; }
            public Ingredient getRepairIngredient() { return Ingredient.of(Items.GOLD_INGOT); }
        }, 2, -1.8f, new Item.Properties());
    }
}
