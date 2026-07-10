package the_four_primitives_and_weapons.damage;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.network.SoulFireSyncPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
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

	/**
	 * エンティティ NBT (ForgeData) に保存する「この gameTime まで魂の炎として扱う」キー。
	 * static マップだとワールドに入り直す (＝サーバー再起動) と消えてしまい、燃え続けている
	 * エンティティが再びオレンジ表示になるため、永続化してリロードをまたいで保持する。
	 */
	private static final String TAG_SOUL_UNTIL = "TfpwSoulFireUntil";

	/** entityUUID → 直近にクライアントへ同期した soul フラグ。差分検出用 (揮発でよい)。 */
	private static final Map<UUID, Boolean> LAST_SYNCED = new ConcurrentHashMap<>();

	/**
	 * 対象の炎を、これから {@code ticks} tick の間「魂の炎」として扱うようマークする。
	 * SOUL 属性の攻撃側から呼ぶ。 実際に炎が付いていなければ描画には反映されない (無害)。
	 */
	public static void markSoulSource(LivingEntity target, int ticks) {
		if (target == null || target.level().isClientSide) return;
		long until = target.level().getGameTime() + Math.max(1, ticks);
		CompoundTag data = target.getPersistentData();
		if (until > data.getLong(TAG_SOUL_UNTIL)) {
			data.putLong(TAG_SOUL_UNTIL, until);
		}
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

		// 燃えている / 燃えかけている / 既に魂として追跡中のエンティティだけを処理する
		// (全 LivingEntity で毎 tick ブロック走査しないための安価なゲート)。
		// isOnFire() ではなく remainingFireTicks も見るのは、着火直後は共有フラグの反映が
		// 1〜数 tick 遅れて揺れるため。 ここで弾いてしまうと standingInSoulFire に到達せず、
		// 「着火した瞬間だけオレンジ、炎が安定してから青」というちらつきが出る。
		boolean burning = entity.isOnFire() || entity.getRemainingFireTicks() > 0;
		if (!burning && !LAST_SYNCED.containsKey(id)) {
			return;
		}

		CompoundTag data = entity.getPersistentData();

		// バニラの Soul Fire ブロック / 点火中 Soul Campfire の上にいる間は、残り炎時間ぶん
		// 魂扱いを延長する (ブロックから離れても残り火が青いままになるように)。
		// 炎フラグを待たずにブロック接触の時点で soul=true を確定させることで、炎オーバーレイが
		// 描画される瞬間には既に青が確定している。
		if (standingInSoulFire(entity)) {
			long until = now + Math.max(entity.getRemainingFireTicks(), 1) + 5L;
			if (until > data.getLong(TAG_SOUL_UNTIL)) {
				data.putLong(TAG_SOUL_UNTIL, until);
			}
		}

		boolean isSoul = false;
		if (data.contains(TAG_SOUL_UNTIL)) {
			isSoul = now <= data.getLong(TAG_SOUL_UNTIL);
			if (!isSoul) {
				data.remove(TAG_SOUL_UNTIL);
			}
		}

		// soul=true は isOnFire() に依存せず送る (炎が無ければクライアントでは何も描かれず無害)。
		// false は魂ウィンドウが切れたときに送る。 炎が消えた場合もウィンドウ(残り炎時間+5)が
		// ほどなく切れて false が飛ぶため、クライアントの isOnFire 遅延に左右されない。
		Boolean prev = LAST_SYNCED.get(id);
		if (isSoul) {
			if (!Boolean.TRUE.equals(prev)) {
				LAST_SYNCED.put(id, Boolean.TRUE);
				sync(entity, true);
			} else if ((now & 15L) == 0L) {
				// 保険の定期再送 (約 16 tick 毎)。 状態変化時の 1 回きりだと、ワールドに
				// 入り直した瞬間などにクライアントのロードと競合して取りこぼされ、以後
				// 再送されずオレンジのままになる。 魂が続く間は再送し続けて青へ復帰させる。
				sync(entity, true);
			}
		} else if (Boolean.TRUE.equals(prev)) {
			LAST_SYNCED.remove(id);
			sync(entity, false);
		}
	}

	private static void sync(LivingEntity entity, boolean soul) {
		TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.send(
				PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
				new SoulFireSyncPacket(entity.getId(), soul));
	}

	/**
	 * プレイヤーがエンティティの追跡を開始したとき、現在の魂フラグを本人へ再送する。
	 *
	 * <p>{@link #sync} は状態変化時にしか送らないため、途中で視界に入った / ワールドに
	 * 入り直して再接続したクライアントには「既に青」の情報が届かない。 追跡開始の時点で
	 * 現在値を送り直すことで、入り直し後もオレンジに戻らないようにする。</p>
	 */
	@SubscribeEvent
	public static void onStartTracking(PlayerEvent.StartTracking event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		if (!(event.getTarget() instanceof LivingEntity target)) return;
		if (Boolean.TRUE.equals(LAST_SYNCED.get(target.getUUID()))) {
			TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.send(
					PacketDistributor.PLAYER.with(() -> player),
					new SoulFireSyncPacket(target.getId(), true));
		}
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
