package the_four_primitives_and_weapons.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.util.KatanaFittings;

/**
 * 刀アイテムのモデルを {@link KatanaModelWrapper} で包み、 柄巻きデザインの差し替えを可能にする。
 * まずは IRON_KATANA のみ ( 試験 )。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KatanaDynamicModelEvents {

	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
		// 本MOD + addon の拵え対応モデルをラップ。
		// item model json に刀身/鍔/頭/柄の4テクスチャが揃っていれば自動対象にする。
		for (net.minecraft.world.item.Item item : net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues()) {
			if (!KatanaFittings.isPotentialFittingWeaponItem(item)) continue;
			ResourceLocation itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
			if (itemId == null) continue;
			ModelResourceLocation mrl = new ModelResourceLocation(
					new ResourceLocation(itemId.getNamespace(), itemId.getPath()), "inventory");
			BakedModel current = event.getModels().get(mrl);
			if (current == null || current instanceof KatanaModelWrapper) continue;
			event.getModels().put(mrl, new KatanaModelWrapper(current, false, true));
		}
	}
}
