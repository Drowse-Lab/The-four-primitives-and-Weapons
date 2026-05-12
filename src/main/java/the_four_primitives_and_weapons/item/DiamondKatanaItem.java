package the_four_primitives_and_weapons.item;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class DiamondKatanaItem extends SwordItem {
    public DiamondKatanaItem() {
        super(new Tier() {
            public int getUses() { return 1561; }
            public float getSpeed() { return 6f; }
            public float getAttackDamageBonus() { return 6f; }
            public int getLevel() { return 3; }
            public int getEnchantmentValue() { return 10; }
            public Ingredient getRepairIngredient() { return Ingredient.of(Items.DIAMOND); }
        }, 3, -2.4f, new Item.Properties());
    }
}
