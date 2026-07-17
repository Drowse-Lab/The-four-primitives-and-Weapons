package the_four_primitives_and_weapons.events;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.item.ArcherGloveItem;
import the_four_primitives_and_weapons.util.CuriosHandsHelper;

import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 弓懸 ( ArcherGloveItem ) の効果 — 弓の引き絞り加速。
 *
 * <p>矢を放つ瞬間の {@link ArrowLooseEvent} でチャージ tick を
 * {@code CHARGE_MULTIPLIER} 倍に水増しする。 弓の威力はチャージ 20 tick で
 * 上限に達するため、 実質「フルチャージまでの時間が約 2/3 になる」効果になる
 * ( 上限を超えた分は バニラの威力キャップで切り捨てられ、 強化はされない )。</p>
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public class ArcherGloveHandler {

	@SubscribeEvent
	public static void onArrowLoose(ArrowLooseEvent event) {
		if (!CuriosHandsHelper.isWearing(event.getEntity(),
				stack -> stack.getItem() instanceof ArcherGloveItem)) {
			return;
		}
		event.setCharge((int) (event.getCharge() * ArcherGloveItem.CHARGE_MULTIPLIER));
	}
}
