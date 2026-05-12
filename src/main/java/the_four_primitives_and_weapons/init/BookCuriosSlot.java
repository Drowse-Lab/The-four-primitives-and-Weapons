package the_four_primitives_and_weapons.init;

import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.CuriosApi;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BookCuriosSlot {
	@SubscribeEvent
	public static void registerCuriosSlots(final InterModEnqueueEvent event) {
		// Register the book slot for magic books
		InterModComms.sendTo(CuriosApi.MODID, SlotTypeMessage.REGISTER_TYPE, () -> {
			return new SlotTypeMessage.Builder("book")
					.priority(100)
					.size(1)
					.icon(new ResourceLocation("curios", "slot/book_slot"))
					.build();
		});
	}
}