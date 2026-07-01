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

	private static final String[] WEAPONS = { "iron_katana", "iron_tyokuto", "iron_rapier" };

	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
		for (String name : WEAPONS) {
			ModelResourceLocation mrl = new ModelResourceLocation(
					new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, name), "inventory");
			BakedModel current = event.getModels().get(mrl);
			if (current == null || current instanceof KatanaModelWrapper) continue;
			// iron_katana: モデル(katana_a_parent)が拵えテクスチャ(tuka/tuba/kasira)を箱UVで直接使い、
			//   tintindex 1=柄/2=鍔/3=頭/4=縁 を持つ。 色は KatanaColorClient(tint) が担う。
			//   黒だけは乗算tintで潰れるので colorBlackMode で 専用黒テクスチャへ差し替える ( 同レイアウトで綺麗 )。
			boolean colorBlackMode = name.equals("iron_katana");
			event.getModels().put(mrl, new KatanaModelWrapper(current, false, colorBlackMode));
		}
	}
}
