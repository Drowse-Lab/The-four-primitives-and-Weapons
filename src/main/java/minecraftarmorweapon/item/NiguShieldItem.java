
package minecraftarmorweapon.item;

import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
//import net.minecraft.world.item.CreativeModeTab
import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.fml.loading.moddiscovery.NightConfigWrapper;

public class NiguShieldItem extends ShieldItem {
	public NiguShieldItem() {
		super(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.COMMON));
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemstack) {
		return UseAnim.BLOCK;
	}

	@Override
	public int getEnchantmentValue()
	{return 5;
	}
}
