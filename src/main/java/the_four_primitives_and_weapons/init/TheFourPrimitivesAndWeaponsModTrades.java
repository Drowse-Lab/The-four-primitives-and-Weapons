
/*
*	MCreator note: This file will be REGENERATED on each build.
*/
package the_four_primitives_and_weapons.init;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.common.BasicItemListing;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TheFourPrimitivesAndWeaponsModTrades {
	@SubscribeEvent
	public static void registerTrades(VillagerTradesEvent event) {
		if (event.getType() == TheFourPrimitivesAndWeaponsModVillagerProfessions.MINECRAFT_ARMOR_AND_WEAPON.get()) {
			event.getTrades().get(1).add(new BasicItemListing(new ItemStack(Items.SHIELD), new ItemStack(Items.IRON_INGOT, 32), new ItemStack(TheFourPrimitivesAndWeaponsModItems.ACHROMATIC_SHIELD.get()), 72000, 5, 1f));
		}
		if (event.getType() == TheFourPrimitivesAndWeaponsModVillagerProfessions.MINECRAFT_ARMOR_AND_WEAPON.get()) {
			event.getTrades().get(1).add(new BasicItemListing(new ItemStack(Blocks.DROPPER), new ItemStack(Blocks.CRAFTING_TABLE), new ItemStack(TheFourPrimitivesAndWeaponsModBlocks.CUSTOM_CRAFTER_CRAFTING.get()), 72000, 5, 0f));
		}
	}
}
