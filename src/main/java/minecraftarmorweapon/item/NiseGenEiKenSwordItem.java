package minecraftarmorweapon.item;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Rarity;

public class NiseGenEiKenSwordItem extends SwordItem {
    public NiseGenEiKenSwordItem(Tier tier) {
        super(tier, 3, -2.4F, new Item.Properties().tab(CreativeModeTab.TAB_COMBAT).rarity(Rarity.UNCOMMON));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // 常にエンチャントのような光沢
    }
}
