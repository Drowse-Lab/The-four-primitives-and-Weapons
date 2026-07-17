package the_four_primitives_and_weapons.init;

import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.CuriosApi;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

/**
 * Curios に「elytra」スロットを登録する (ElytraSlot mod 相当機能)。
 *
 * <p>エリトラをチェストプレートと併用できる専用スロット。装備可能アイテムは
 * アイテムタグ {@code data/curios/tags/items/elytra.json} で指定する
 * (デフォルト: minecraft:elytra)。</p>
 *
 * <p>飛行ロジックは {@link the_four_primitives_and_weapons.events.CuriosElytraFlightHandler}、
 * 描画は {@code client/renderer/ElytraCurioRenderer} が担当する。</p>
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ElytraCuriosSlot {

	/** Curios スロット ID。アイテムタグ curios:elytra と対応する。 */
	public static final String SLOT_ID = "elytra";

	@SubscribeEvent
	public static void registerCuriosSlots(final InterModEnqueueEvent event) {
		InterModComms.sendTo(CuriosApi.MODID, SlotTypeMessage.REGISTER_TYPE, () -> {
			return new SlotTypeMessage.Builder(SLOT_ID)
					.priority(90)
					.size(1)
					.icon(new ResourceLocation("curios", "slot/elytra_slot"))
					.build();
		});
	}
}
