package the_four_primitives_and_weapons.init;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * 元 GeckoLib の動的アニメーション同期用 (syncable.getSyncedAnimation /
 * setAnimation / animationprocedure) を駆動していたが、GeckoLib 撤去
 * (BlackholeEntity 等を vanilla AnimationState に置換) に伴い不要になった。
 *
 * イベントクラス自体は MCreator が他のエンティティを生やす可能性があるので残し、
 * メソッドは no-op にしてある。今後別エンティティで vanilla アニメ駆動が必要なら
 * ここに足す。
 */
@Mod.EventBusSubscriber
public class EntityAnimationFactory {
	@SubscribeEvent
	public static void onEntityTick(LivingEvent.LivingTickEvent event) {
		// GeckoLib の syncable animation はもう存在しないので何もしない。
		// vanilla の AnimationState はエンティティ自身の tick で startIfStopped する方式
		// (BlackholeEntity#aiStep 参照) なので、ここでは何もする必要が無い。
	}
}
