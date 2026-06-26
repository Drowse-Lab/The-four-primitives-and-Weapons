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

	// ===== タイムライン (tick) =====
	public static final int GROW_END = 12;     // 腕が生え出る
	public static final int SWEEP_END = 34;    // 薙ぎ払い
	public static final int SLAM_END = 58;     // 振り上げ→地叩き
	public static final int LIFETIME = 74;     // 引っ込めて消滅

	public static final int SWEEP_IMPACT = 24; // 薙ぎ払いの当たり判定フレーム
	public static final int SLAM_IMPACT = 50;  // 地叩きの当たり判定フレーム

	// ===== 効果パラメータ =====
	/** 腕の届く距離 (ブロック)。地叩きの着弾点 = オーナー前方この距離。 */
	public static final double REACH = 6.0;
	private static final double SWEEP_RADIUS = 7.5;
	private static final double SLAM_RADIUS = 8.0;
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

	/** サーバー側召喚用。 */
	public GiantBoneArmEntity(Level world, Player owner, float castYaw) {
		this(TheFourPrimitivesAndWeaponsModCustomEntities.GIANT_BONE_ARM.get(), world);
		this.entityData.set(OWNER_ID, owner.getId());
		this.entityData.set(CAST_YAW, castYaw);
		anchorToOwner(owner);
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(OWNER_ID, -1);
		this.entityData.define(CAST_YAW, 0.0f);
	}

	public float getCastYaw() {
		return this.entityData.get(CAST_YAW);
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

	/** 薙ぎ払い: 前方の扇状範囲を薙ぐ。横向きノックバック。 */
	private void doSweep(Player owner) {
		Vec3 fwd = forward();
		Vec3 center = owner.position().add(fwd.scale(REACH * 0.6)).add(0, 1.0, 0);
		AABB box = new AABB(center, center).inflate(SWEEP_RADIUS, 4.0, SWEEP_RADIUS);
		for (LivingEntity t : level().getEntitiesOfClass(LivingEntity.class, box, e -> e != owner && e.isAlive())) {
			Vec3 to = t.position().subtract(owner.position());
			if (to.horizontalDistance() > SWEEP_RADIUS)
				continue;
			Vec3 toN = to.lengthSqr() < 1.0e-4 ? fwd : to.normalize();
			if (fwd.dot(new Vec3(toN.x, 0, toN.z)) < Math.cos(Math.toRadians(75)))
				continue;
			hurt(owner, t, SWEEP_DAMAGE);
			// 横薙ぎの吹き飛ばし (前方 + 横ぶれ)
			Vec3 side = new Vec3(-fwd.z, 0, fwd.x);
			t.push(fwd.x * 1.0 + side.x * 0.4, 0.45, fwd.z * 1.0 + side.z * 0.4);
			if (t instanceof ServerPlayer sp)
				sp.hurtMarked = true;
		}
		if (level() instanceof ServerLevel sl) {
			for (int i = 0; i < 40; i++) {
				double a = Math.toRadians(-75 + 150.0 * i / 40.0) + Math.atan2(fwd.z, fwd.x);
				double r = SWEEP_RADIUS * 0.9;
				sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
						owner.getX() + Math.cos(a) * r, center.y, owner.getZ() + Math.sin(a) * r,
						1, 0, 0, 0, 0);
			}
		}
		level().playSound(null, getX(), getY(), getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0f, 0.6f);
	}

	/** 地叩き: 前方着弾点に叩きつけ。強ノックバック + 半径16m 地揺れ。 */
	private void doSlam(Player owner) {
		Vec3 fwd = forward();
		Vec3 impact = owner.position().add(fwd.scale(REACH));
		AABB box = new AABB(impact, impact).inflate(SLAM_RADIUS, 5.0, SLAM_RADIUS);
		for (LivingEntity t : level().getEntitiesOfClass(LivingEntity.class, box, e -> e != owner && e.isAlive())) {
			double d = t.position().distanceTo(impact);
			if (d > SLAM_RADIUS)
				continue;
			float dmg = (float) (SLAM_DAMAGE * (1.0 - 0.4 * (d / SLAM_RADIUS)));
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
