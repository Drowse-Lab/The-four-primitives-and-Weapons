package minecraftarmorweapon.item;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class IronTyokutoItem extends SwordItem {
    public IronTyokutoItem() {
        super(new Tier() {
            public int getUses() { return 250; }
            public float getSpeed() { return 4f; }
            public float getAttackDamageBonus() { return 3f; }
            public int getLevel() { return 2; }
            public int getEnchantmentValue() { return 14; }
            public Ingredient getRepairIngredient() { return Ingredient.of(Items.IRON_INGOT); }
        }, 3, -2.4f, new Item.Properties());
    }
}
