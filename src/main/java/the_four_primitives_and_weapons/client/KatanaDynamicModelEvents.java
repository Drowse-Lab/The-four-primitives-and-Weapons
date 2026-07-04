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

	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
		// 本MODの 刀/直刀/レイピア 全種 ( saya除く ) の inventory モデルをラップ。
		//   モデルが拵えテクスチャ(tuka/tuba/kasira)+tintindex を持つものは 色付け・黒差し替えが効く。
		for (net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> ro
				: the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems.REGISTRY.getEntries()) {
			String name = ro.getId().getPath();
			if (name.contains("saya")
					|| !(name.contains("katana") || name.contains("tyokuto") || name.contains("rapier") || name.contains("dagger"))) continue;
			ModelResourceLocation mrl = new ModelResourceLocation(
					new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, name), "inventory");
			BakedModel current = event.getModels().get(mrl);
			if (current == null || current instanceof KatanaModelWrapper) continue;
			event.getModels().put(mrl, new KatanaModelWrapper(current, false, true));
		}
	}
}
