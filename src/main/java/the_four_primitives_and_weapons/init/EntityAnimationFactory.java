package the_four_primitives_and_weapons.init;

/**
 * 元 GeckoLib の動的アニメーション同期用 (syncable.getSyncedAnimation /
 * setAnimation / animationprocedure) を駆動していたが、GeckoLib 撤去
 * (BlackholeEntity 等を vanilla AnimationState に置換) に伴い不要になった。
 *
 * クラス自体は MCreator が他のエンティティを生やす可能性があるので残してある。
 */
public class EntityAnimationFactory {
	// no-op のまま LivingTickEvent を購読すると全エンティティの毎tickでイベント配送コストだけが
	// 掛かるため、購読ごと削除した。vanilla アニメ駆動が必要になったらここに @SubscribeEvent を復活させる。
}
