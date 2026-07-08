package the_four_primitives_and_weapons.damage;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.network.SoulFireSyncPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「魂の炎 (青い炎)」の描画対象を判定し、クライアントへ同期するサーバー側ハンドラー。
 *
 * <p>炎そのものはバニラの {@code setSecondsOnFire} で付与されるが、
 * エンティティに重なる炎オーバーレイの見た目はバニラだと常にオレンジ。
 * ここでは「その炎が魂由来か」を追跡し、該当エンティティを青い炎として描画するよう
 * {@link SoulFireSyncPacket} で通知する。</p>
 *
 * <p>魂由来と判定するのは次の 2 種類:</p>
 * <ul>
 *   <li>SOUL 属性の攻撃 ({@link #markSoulSource} を明示的に呼ぶ)</li>
 *   <li>バニラの Soul Fire ブロック / 火の点いた Soul Campfire の上で燃えているとき</li>
 * </ul>
 */
@Mod.EventBusSubscriber
public class SoulFireHandler {

	/** entityUUID → この gameTime まで炎を魂の炎として扱う。 */
	private static final Map<UUID, Long> SOUL_UNTIL = new ConcurrentHashMap<>();
	/** entityUUID → 直近にクライアントへ同期した soul フラグ。差分検出用。 */
	private static final Map<UUID, Boolean> LAST_SYNCED = new ConcurrentHashMap<>();

	/**
	 * 対象の炎を、これから {@code ticks} tick の間「魂の炎」として扱うようマークする。
	 * SOUL 属性の攻撃側から呼ぶ。 実際に炎が付いていなければ描画には反映されない (無害)。
	 */
	public static void markSoulSource(LivingEntity target, int ticks) {
		if (target == null || target.level().isClientSide) return;
		long until = target.level().getGameTime() + Math.max(1, ticks);
		SOUL_UNTIL.merge(target.getUUID(), until, Math::max);
	}

	/**
	 * Soul Fire ブロックで燃えた時と同じ「青い炎上状態」として扱う。
	 * ダメージや持ち歩きデバフ側はこの入口を使うことで、通常炎ではなく
	 * Soul Fire 由来の炎として描画・同期される。
	 */
	public static void setSoulFire(LivingEntity target, int ticks) {
		if (target == null || target.level().isClientSide) return;
		int safeTicks = Math.max(20, ticks);
		target.setSecondsOnFire(Math.max(1, (safeTicks + 19) / 20));
		markSoulSource(target, safeTicks + 10);
	}

	@SubscribeEvent
	public static void onLivingTick(LivingEvent.LivingTickEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity.level().isClientSide) return;

		UUID id = entity.getUUID();
		long now = entity.level().getGameTime();

		if (!entity.isOnFire()) {
			// 燃えていない: 状態をクリアし、必要なら false を送る。
			SOUL_UNTIL.remove(id);
			Boolean was = LAST_SYNCED.remove(id);
			if (Boolean.TRUE.equals(was)) {
				sync(entity, false);
			}
			return;
		}

		// バニラの Soul Fire ブロックで燃えている間は、残り炎時間ぶん魂扱いを延長する
		// (ブロックから離れても残り火が青いままになるように)。
		if (standingInSoulFire(entity)) {
			long until = now + entity.getRemainingFireTicks() + 5L;
			SOUL_UNTIL.merge(id, until, Math::max);
		}

		Long until = SOUL_UNTIL.get(id);
		boolean isSoul = until != null && now <= until;
		if (until != null && now > until) {
			SOUL_UNTIL.remove(id);
		}

		Boolean prev = LAST_SYNCED.get(id);
		if (prev == null || prev.booleanValue() != isSoul) {
			LAST_SYNCED.put(id, isSoul);
			sync(entity, isSoul);
		}
	}

	private static void sync(LivingEntity entity, boolean soul) {
		TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.send(
				PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
				new SoulFireSyncPacket(entity.getId(), soul));
	}

	/** エンティティの当たり判定内に Soul Fire ブロック / 点火中 Soul Campfire があるか。 */
	private static boolean standingInSoulFire(LivingEntity entity) {
		AABB box = entity.getBoundingBox();
		BlockPos min = BlockPos.containing(box.minX + 0.001, box.minY + 0.001, box.minZ + 0.001);
		BlockPos max = BlockPos.containing(box.maxX - 0.001, box.maxY - 0.001, box.maxZ - 0.001);
		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			BlockState state = entity.level().getBlockState(pos);
			if (state.is(Blocks.SOUL_FIRE)) {
				return true;
			}
			if (state.is(Blocks.SOUL_CAMPFIRE) && state.getValue(CampfireBlock.LIT)) {
				return true;
			}
		}
		return false;
	}
}
