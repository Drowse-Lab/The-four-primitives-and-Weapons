package the_four_primitives_and_weapons.entity;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModCustomEntities;
import the_four_primitives_and_weapons.network.ScreenShakePacket;

import java.util.List;

/**
 * 上腕骨刀の特殊技「巨骨の腕」。
 *
 * プレイヤーの肩から生える巨大な骨の腕 (上腕骨→前腕骨→手→指骨)。
 * タイムラインで 薙ぎ払い → 地叩き の自動コンボを行い、
 * 地叩きの着弾点から半径 {@link #SHAKE_RADIUS}m の地揺れ (視界揺れ) を起こす。
 *
 * - 位置はオーナー (プレイヤー) の肩に毎 tick アンカーする (両側で再計算 = クライアント補間ラグ無し)。
 * - ダメージ/揺れ/パーティクルはサーバー側のみ。
 * - アニメーションは {@link #tickCount} から決まるフェーズで駆動 (同期不要)。
 */
public class GiantBoneArmEntity extends Entity {

	private static final EntityDataAccessor<Integer> OWNER_ID =
			SynchedEntityData.defineId(GiantBoneArmEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> CAST_YAW =
			SynchedEntityData.defineId(GiantBoneArmEntity.class, EntityDataSerializers.FLOAT);
	/** 武器の侵食(CORROSION)属性レベル。0=無し。骨表面のガラス被膜の強さに使う。 */
	private static final EntityDataAccessor<Integer> CORROSION_LEVEL =
			SynchedEntityData.defineId(GiantBoneArmEntity.class, EntityDataSerializers.INT);
	/**
	 * 地叩き終端の腕ピッチ(rad)。− が下。手の真下の地面に合わせて算出し、
	 * 手が地表に乗る角度にする (ブロックへのめり込み防止)。クライアント描画で使用。
	 */
	private static final EntityDataAccessor<Float> SLAM_TILT =
			SynchedEntityData.defineId(GiantBoneArmEntity.class, EntityDataSerializers.FLOAT);
	/** SLAM_TILT のクランプ範囲 (下げすぎ/上げすぎ防止)。 */
	private static final float SLAM_TILT_MIN = -0.45f; // 最大の下げ
	private static final float SLAM_TILT_MAX = 0.25f;  // 最大の上げ

	/** 動作モード。0=薙ぎ→叩き コンボ / 1=ガード(両腕で抱きしめて守る)。 */
	private static final EntityDataAccessor<Integer> MODE =
			SynchedEntityData.defineId(GiantBoneArmEntity.class, EntityDataSerializers.INT);
	/** ガードの左右。-1=左腕 / +1=右腕。 */
	private static final EntityDataAccessor<Integer> GUARD_SIDE =
			SynchedEntityData.defineId(GiantBoneArmEntity.class, EntityDataSerializers.INT);
	public static final int MODE_SLAM = 0;
	public static final int MODE_GUARD = 1;
	/** ガードの表示寿命 (tick)。抱え込み→保持→開いて消える。 */
	public static final int GUARD_LIFETIME = 30;

	// ===== タイムライン (tick) =====
	public static final int GROW_END = 12;     // 腕が生え出る
	public static final int SWEEP_END = 34;    // 薙ぎ払い
	public static final int SLAM_END = 58;     // 振り上げ→地叩き
	public static final int LIFETIME = 74;     // 引っ込めて消滅

	public static final int SWEEP_IMPACT = 24; // 薙ぎ払いの当たり判定フレーム
	public static final int SLAM_IMPACT = 50;  // 地叩きの当たり判定フレーム

	// ===== 効果パラメータ =====
	/** 腕の届く距離 (ブロック)。指先まで含めた全長。地叩きの着弾点(指先) = オーナー前方この距離。 */
	public static final double REACH = 10.0;
	/** 薙ぎ払いの扇の半角 (度)。 */
	private static final double SWEEP_ANGLE = 80.0;
	/** 地叩き: 肩→指先の線分まわりの当たり半径 (腕の太さぶん + 余裕)。 */
	private static final double ARM_HIT_RADIUS = 4.0;
	private static final float SWEEP_DAMAGE = 14.0f;
	private static final float SLAM_DAMAGE = 22.0f;
	public static final double SHAKE_RADIUS = 16.0;

	private boolean sweepDone = false;
	private boolean slamDone = false;

	public GiantBoneArmEntity(EntityType<? extends GiantBoneArmEntity> type, Level world) {
		super(type, world);
		this.noPhysics = true;
		this.noCulling = true;
	}

	/** クライアント側スポーン用。 */
	public GiantBoneArmEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(TheFourPrimitivesAndWeaponsModCustomEntities.GIANT_BONE_ARM.get(), world);
	}

	/** サーバー側召喚用 (薙ぎ→叩き コンボ)。 */
	public GiantBoneArmEntity(Level world, Player owner, float castYaw, int corrosionLevel) {
		this(TheFourPrimitivesAndWeaponsModCustomEntities.GIANT_BONE_ARM.get(), world);
		this.entityData.set(OWNER_ID, owner.getId());
		this.entityData.set(CAST_YAW, castYaw);
		this.entityData.set(CORROSION_LEVEL, Math.max(0, corrosionLevel));
		this.entityData.set(SLAM_TILT, computeSlamTilt(owner));
		anchorToOwner(owner);
	}

	/** ガードの骨腕を召喚 (左右2本で抱きしめて守る。side: -1=左, +1=右)。 */
	public static GiantBoneArmEntity spawnGuard(Level world, Player owner, int corrosionLevel, int side) {
		GiantBoneArmEntity e = new GiantBoneArmEntity(
				TheFourPrimitivesAndWeaponsModCustomEntities.GIANT_BONE_ARM.get(), world);
		e.entityData.set(OWNER_ID, owner.getId());
		e.entityData.set(CAST_YAW, owner.getYRot());
		e.entityData.set(CORROSION_LEVEL, Math.max(0, corrosionLevel));
		e.entityData.set(MODE, MODE_GUARD);
		e.entityData.set(GUARD_SIDE, side);
		e.anchorToOwner(owner);
		return e;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(OWNER_ID, -1);
		this.entityData.define(CAST_YAW, 0.0f);
		this.entityData.define(CORROSION_LEVEL, 0);
		this.entityData.define(SLAM_TILT, -0.18f);
		this.entityData.define(MODE, MODE_SLAM);
		this.entityData.define(GUARD_SIDE, 1);
	}

	public int getMode() {
		return this.entityData.get(MODE);
	}

	public boolean isGuard() {
		return getMode() == MODE_GUARD;
	}

	/** ガードの左右 (-1=左腕 / +1=右腕)。 */
	public int getGuardSide() {
		return this.entityData.get(GUARD_SIDE);
	}

	/** 地叩き終端の腕ピッチ (− が下)。手が地表に乗る角度。 */
	public float getSlamTilt() {
		return this.entityData.get(SLAM_TILT);
	}

	/**
	 * 手の真下 (前方 REACH 地点) の地表高さから、手がその地表に乗る叩き角を算出。
	 * 腕長 ≒ REACH とみなし、肩から指先までの落差を sin で角度化する。
	 */
	private float computeSlamTilt(Player owner) {
		Vec3 fwd = forward();
		Vec3 impact = owner.position().add(fwd.scale(REACH));
		double shoulderY = owner.getY() + owner.getBbHeight() * 0.85;
		double groundY = shoulderY - REACH; // 取得失敗時の保険 (ほぼ真下扱いにはしない)
		if (level() instanceof ServerLevel sl) {
			groundY = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					Mth.floor(impact.x), Mth.floor(impact.z));
		}
		// 指先 worldY = 肩 + REACH*sin(tilt) = 地表 → sin = (地表 - 肩) / REACH
		double sin = Mth.clamp((groundY - shoulderY) / REACH, -1.0, 1.0);
		float tilt = (float) Math.asin(sin); // − = 下
		return Mth.clamp(tilt, SLAM_TILT_MIN, SLAM_TILT_MAX);
	}

	public float getCastYaw() {
		return this.entityData.get(CAST_YAW);
	}

	/** 武器の侵食属性レベル (0=無し)。骨表面のガラス被膜表現に使う。 */
	public int getCorrosionLevel() {
		return this.entityData.get(CORROSION_LEVEL);
	}

	public Player getOwner() {
		int id = this.entityData.get(OWNER_ID);
		if (id < 0)
			return null;
		Entity e = this.level().getEntity(id);
		return (e instanceof Player p) ? p : null;
	}

	/** 前方向ベクトル (cast 時の水平方向)。 */
	public Vec3 forward() {
		double yawRad = Math.toRadians(getCastYaw());
		return new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
	}

	private void anchorToOwner(Player owner) {
		if (isGuard()) {
			// ガード: プレイヤーの足元中心にアンカー (描画側で頭上へ持ち上げて包む)。
			setPos(owner.getX(), owner.getY(), owner.getZ());
			return;
		}
		// プレイヤーの右肩から前方へ生やす。
		Vec3 fwd = forward();
		Vec3 right = rightVec();
		double shoulderY = owner.getY() + owner.getBbHeight() * 0.85;
		double sideOffset = 0.55; // 右肩へのオフセット
		setPos(owner.getX() + fwd.x * 0.3 + right.x * sideOffset,
				shoulderY,
				owner.getZ() + fwd.z * 0.3 + right.z * sideOffset);
	}

	/** プレイヤーの右方向ベクトル (cast 時)。 */
	public Vec3 rightVec() {
		double yawRad = Math.toRadians(getCastYaw());
		return new Vec3(-Math.cos(yawRad), 0, -Math.sin(yawRad));
	}

	@Override
	public void tick() {
		super.tick();

		Player owner = getOwner();
		if (owner == null || !owner.isAlive()) {
			if (!level().isClientSide)
				discard();
			return;
		}

		anchorToOwner(owner);

		// ガード: 攻撃判定なし。寿命で消えるだけ (ダメージ軽減は SwordGuardHandler 側)。
		if (isGuard()) {
			if (!level().isClientSide) {
				if (tickCount == 1) {
					level().playSound(null, getX(), getY(), getZ(),
							SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 1.2f, 1.6f);
				}
				if (tickCount >= GUARD_LIFETIME) {
					discard();
				}
			}
			return;
		}

		if (!level().isClientSide) {
			if (tickCount == 1) {
				level().playSound(null, getX(), getY(), getZ(),
						SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.4f, 1.4f);
			}
			if (tickCount == SWEEP_IMPACT && !sweepDone) {
				sweepDone = true;
				doSweep(owner);
			}
			if (tickCount == SLAM_IMPACT && !slamDone) {
				slamDone = true;
				doSlam(owner);
			}
			if (tickCount >= LIFETIME) {
				discard();
			}
		}
	}

	/** 薙ぎ払い: 肩から指先 (REACH) までの前方扇状範囲を薙ぐ。横向きノックバック。 */
	private void doSweep(Player owner) {
		Vec3 fwd = forward();
		Vec3 origin = owner.position();
		// 肩〜指先の全長 + 縦の振れ幅をカバー
		AABB box = new AABB(origin, origin).inflate(REACH, 5.0, REACH);
		double minDot = Math.cos(Math.toRadians(SWEEP_ANGLE));
		for (LivingEntity t : level().getEntitiesOfClass(LivingEntity.class, box, e -> e != owner && e.isAlive())) {
			Vec3 to = t.position().subtract(origin);
			if (to.horizontalDistance() > REACH)
				continue;
			Vec3 toN = to.lengthSqr() < 1.0e-4 ? fwd : to.normalize();
			if (fwd.dot(new Vec3(toN.x, 0, toN.z)) < minDot)
				continue;
			hurt(owner, t, SWEEP_DAMAGE);
			// 横薙ぎの吹き飛ばし (前方 + 横ぶれ)
			Vec3 side = new Vec3(-fwd.z, 0, fwd.x);
			t.push(fwd.x * 1.0 + side.x * 0.4, 0.45, fwd.z * 1.0 + side.z * 0.4);
			if (t instanceof ServerPlayer sp)
				sp.hurtMarked = true;
		}
		level().playSound(null, getX(), getY(), getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0f, 0.6f);
	}

	/** 地叩き: 肩→指先の腕全体で叩きつけ。強ノックバック + 半径16m 地揺れ。 */
	private void doSlam(Player owner) {
		Vec3 fwd = forward();
		Vec3 shoulder = owner.position().add(0, owner.getBbHeight() * 0.7, 0);
		Vec3 impact = owner.position().add(fwd.scale(REACH));
		// 着弾点(指先)を地面 (地表) にクランプ — 当たり判定/演出が地中にめり込まないように
		if (level() instanceof ServerLevel slg) {
			int gy = slg.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					Mth.floor(impact.x), Mth.floor(impact.z));
			impact = new Vec3(impact.x, gy, impact.z);
		}
		// 肩→指先 の線分まわり (カプセル) で当たり判定 = 腕全体に判定
		AABB box = new AABB(
				Math.min(shoulder.x, impact.x), Math.min(shoulder.y, impact.y), Math.min(shoulder.z, impact.z),
				Math.max(shoulder.x, impact.x), Math.max(shoulder.y, impact.y), Math.max(shoulder.z, impact.z))
				.inflate(ARM_HIT_RADIUS);
		for (LivingEntity t : level().getEntitiesOfClass(LivingEntity.class, box, e -> e != owner && e.isAlive())) {
			Vec3 tp = t.position().add(0, t.getBbHeight() * 0.5, 0);
			double d = distToSegment(tp, shoulder, impact);
			if (d > ARM_HIT_RADIUS)
				continue;
			float dmg = (float) (SLAM_DAMAGE * (1.0 - 0.35 * (d / ARM_HIT_RADIUS)));
			hurt(owner, t, dmg);
			Vec3 out = t.position().subtract(impact);
			Vec3 outN = out.lengthSqr() < 1.0e-4 ? fwd : out.normalize();
			t.push(outN.x * 1.2, 0.9, outN.z * 1.2);
			if (t instanceof ServerPlayer sp)
				sp.hurtMarked = true;
		}

		if (level() instanceof ServerLevel sl) {
			BlockPos ground = BlockPos.containing(impact.x, impact.y - 0.2, impact.z);
			BlockState bs = sl.getBlockState(ground);
			if (bs.isAir())
				bs = sl.getBlockState(ground.below());
			// 衝撃の土煙
			sl.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 0.2, impact.z, 6, 1.5, 0.3, 1.5, 0.0);
			if (!bs.isAir()) {
				BlockParticleOption opt = new BlockParticleOption(ParticleTypes.BLOCK, bs);
				for (int ring = 1; ring <= 3; ring++) {
					double rr = ring * 2.2;
					int n = 18 + ring * 8;
					for (int i = 0; i < n; i++) {
						double a = Math.PI * 2 * i / n;
						sl.sendParticles(opt,
								impact.x + Math.cos(a) * rr, impact.y + 0.1, impact.z + Math.sin(a) * rr,
								2, 0.2, 0.25, 0.2, 0.15);
					}
				}
			}
			sl.sendParticles(ParticleTypes.SONIC_BOOM, impact.x, impact.y + 0.5, impact.z, 1, 0, 0, 0, 0);
		}

		level().playSound(null, impact.x, impact.y, impact.z,
				SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.5f, 0.5f);
		level().playSound(null, impact.x, impact.y, impact.z,
				SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.PLAYERS, 2.0f, 0.5f);

		// 半径16mの視界揺れ (距離で減衰)
		if (level() instanceof ServerLevel sl) {
			for (ServerPlayer sp : sl.players()) {
				double d = sp.position().distanceTo(impact);
				if (d > SHAKE_RADIUS)
					continue;
				float intensity = (float) (1.0 - d / SHAKE_RADIUS); // 0..1
				TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.send(
						net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
						new ScreenShakePacket(intensity, 16));
			}
		}
	}

	private void hurt(Player owner, LivingEntity target, float amount) {
		target.hurt(level().damageSources().playerAttack(owner), amount);
	}

	/** 点 p から線分 a-b までの最短距離。 */
	private static double distToSegment(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double len2 = ab.lengthSqr();
		if (len2 < 1.0e-6)
			return p.distanceTo(a);
		double t = p.subtract(a).dot(ab) / len2;
		t = Math.max(0.0, Math.min(1.0, t));
		return p.distanceTo(a.add(ab.scale(t)));
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double dist) {
		return dist < 4096; // 64m
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
	}
}
