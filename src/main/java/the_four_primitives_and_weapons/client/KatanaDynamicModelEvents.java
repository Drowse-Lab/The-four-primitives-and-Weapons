package the_four_primitives_and_weapons.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

/**
 * 刀アイテムのモデルを {@link KatanaModelWrapper} で包み、 柄巻きデザインの差し替えを可能にする。
 * まずは IRON_KATANA のみ ( 試験 )。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KatanaDynamicModelEvents {

	private static final ModelResourceLocation MRL_IRON_KATANA =
			new ModelResourceLocation(new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "iron_katana"), "inventory");

	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
		BakedModel current = event.getModels().get(MRL_IRON_KATANA);
		if (current == null || current instanceof KatanaModelWrapper) return;
		event.getModels().put(MRL_IRON_KATANA, new KatanaModelWrapper(current));
	}
}
