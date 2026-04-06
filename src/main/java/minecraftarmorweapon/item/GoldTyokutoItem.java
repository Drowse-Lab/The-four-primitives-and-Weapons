package minecraftarmorweapon.item;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class GoldTyokutoItem extends SwordItem {
    public GoldTyokutoItem() {
        super(new Tier() {
            public int getUses() { return 32; }
            public float getSpeed() { return 4f; }
            public float getAttackDamageBonus() { return 3f; }
            public int getLevel() { return 0; }
            public int getEnchantmentValue() { return 22; }
            public Ingredient getRepairIngredient() { return Ingredient.of(Items.GOLD_INGOT); }
        }, 3, -2.4f, new Item.Properties());
    }
}
