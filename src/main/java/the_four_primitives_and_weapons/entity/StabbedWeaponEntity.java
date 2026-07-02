package the_four_primitives_and_weapons.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModCustomEntities;

/**
 * 地面に突き刺さった武器 ( 戦場の建築用 )。
 * 武器アイテムを持って地面の上面をスニーク右クリックで設置、 右クリック / 攻撃で回収できる。
 */
public class StabbedWeaponEntity extends Entity {

	private static final EntityDataAccessor<ItemStack> DATA_ITEM =
			SynchedEntityData.defineId(StabbedWeaponEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<Float> DATA_YAW =
			SynchedEntityData.defineId(StabbedWeaponEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_TILT =
			SynchedEntityData.defineId(StabbedWeaponEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_RADIUS =
			SynchedEntityData.defineId(StabbedWeaponEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_ROLL =
			SynchedEntityData.defineId(StabbedWeaponEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_SCALE =
			SynchedEntityData.defineId(StabbedWeaponEntity.class, EntityDataSerializers.FLOAT);

	public StabbedWeaponEntity(EntityType<? extends StabbedWeaponEntity> type, Level level) {
		super(type, level);
	}

	public StabbedWeaponEntity(Level level) {
		this(TheFourPrimitivesAndWeaponsModCustomEntities.STABBED_WEAPON.get(), level);
	}

	public StabbedWeaponEntity(PlayMessages.SpawnEntity packet, Level level) {
		this(TheFourPrimitivesAndWeaponsModCustomEntities.STABBED_WEAPON.get(), level);
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(DATA_ITEM, ItemStack.EMPTY);
		this.entityData.define(DATA_YAW, 0f);
		this.entityData.define(DATA_TILT, 12f);
		this.entityData.define(DATA_RADIUS, 1.0f); // 当たり判定/編集球の半径 ( 武器ごとに変更可 )
		this.entityData.define(DATA_ROLL, 0f);      // ロール ( 軸まわりの回転 )
		this.entityData.define(DATA_SCALE, 1.0f);   // 表示スケール ( 武器ごとに変更可 )
	}

	public void setItem(ItemStack stack) {
		ItemStack one = stack.copy();
		one.setCount(1);
		this.entityData.set(DATA_ITEM, one);
	}

	public ItemStack getItem() {
		return this.entityData.get(DATA_ITEM);
	}

	public void setStabYaw(float yaw) {
		this.entityData.set(DATA_YAW, yaw);
	}

	public float getStabYaw() {
		return this.entityData.get(DATA_YAW);
	}

	public void setTilt(float tilt) {
		this.entityData.set(DATA_TILT, tilt);
	}

	public float getTilt() {
		return this.entityData.get(DATA_TILT);
	}

	public void setRadius(float r) {
		this.entityData.set(DATA_RADIUS, Math.max(0.3f, Math.min(3.0f, r)));
		this.setBoundingBox(makeBoundingBox()); // 半径変更を当たり判定に即反映
	}

	public float getRadius() {
		return this.entityData.get(DATA_RADIUS);
	}

	public void setRoll(float roll) {
		this.entityData.set(DATA_ROLL, roll);
	}

	/** 表示スケール ( 0.2〜3.0 )。 見た目のみ。 当たり判定は半径 ( setRadius ) で別管理。 */
	public void setScale(float sc) {
		this.entityData.set(DATA_SCALE, Math.max(0.2f, Math.min(3.0f, sc)));
	}

	public float getScale() {
		return this.entityData.get(DATA_SCALE);
	}

	public float getRoll() {
		return this.entityData.get(DATA_ROLL);
	}

	@Override
	public boolean isPickable() {
		return !this.isRemoved(); // 右クリック / 攻撃でターゲット可能
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean isNoGravity() {
		return true; // 地面に刺さったまま静止
	}

	/**
	 * 当たり判定を「武器の軸 ( 向き・傾き )」 に沿わせる。 刃先 ( 下＋前 ) と 柄 ( 上＋後ろ ) の
	 * 2 端点を結ぶ線分を内包する AABB にするので、 表示した武器に沿った細長い判定になる
	 * ( 半径 DATA_RADIUS で長さ・太さが伸縮 )。
	 */
	@Override
	protected net.minecraft.world.phys.AABB makeBoundingBox() {
		if (this.entityData == null) {
			return new net.minecraft.world.phys.AABB(
					getX() - 0.5, getY() - 0.5, getZ() - 0.5, getX() + 0.5, getY() + 0.5, getZ() + 0.5);
		}
		double r = getRadius();
		double len = 1.0 * r;                 // 武器の半長 ( 刃側 )
		double tr = Math.toRadians(getTilt());
		double yr = Math.toRadians(getStabYaw());
		double horiz = Math.sin(tr);          // 前方向成分
		double vert = Math.cos(tr);           // 下方向成分
		double fx = -Math.sin(yr) * horiz;
		double fz = Math.cos(yr) * horiz;
		// 刃先 ( 下＋前 ) と 柄 ( 上＋後ろ・短め )
		double tipX = getX() + fx * len,        tipY = getY() - vert * len,        tipZ = getZ() + fz * len;
		double hndX = getX() - fx * len * 0.55, hndY = getY() + vert * len * 0.55, hndZ = getZ() - fz * len * 0.55;
		double th = 0.2;                      // 太さ ( 薄くして武器の棒形状に密着 )
		return new net.minecraft.world.phys.AABB(
				Math.min(tipX, hndX) - th, Math.min(tipY, hndY) - th, Math.min(tipZ, hndZ) - th,
				Math.max(tipX, hndX) + th, Math.max(tipY, hndY) + th, Math.max(tipZ, hndZ) + th);
	}

	@Override
	public void tick() {
		super.tick();
		this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); // 動かない
		this.setBoundingBox(makeBoundingBox()); // 半径変更 ( 同期 ) を当たり判定に反映
	}

	/** 右クリックで回収 ( 視線が実際に武器 ( 棒 ) に当たっている時だけ )。 */
	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (!lookHitsWeapon(player, 6.0)) return InteractionResult.PASS; // 武器に当たっていない
		if (!this.level().isClientSide) {
			retrieveTo(player);
		}
		return InteractionResult.sidedSuccess(this.level().isClientSide);
	}

	/** 攻撃でも回収 ( 視線が実際に武器に当たっている時だけ )。 */
	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (this.level().isClientSide || this.isRemoved()) return false;
		if (source.getEntity() instanceof Player player) {
			if (!lookHitsWeapon(player, 7.0)) return false; // 武器に当たっていない → 無視
			retrieveTo(player);
			return true;
		}
		return false;
	}

	private boolean lookHitsWeapon(Player player, double maxDist) {
		return clipWeapon(player.getEyePosition(1.0f), player.getViewVector(1.0f), maxDist) >= 0;
	}

	// ─────────────────────────────────────────────────────────────
	// OBB ( カプセル ) 精密ヒット判定: 武器の線分 ( 柄→刃先 ) に視線レイが近いか
	// ─────────────────────────────────────────────────────────────

	/** 武器の軸線分 [ 柄, 刃先 ] を 向き・傾き から求める。 */
	public net.minecraft.world.phys.Vec3[] weaponSegment() {
		double r = getRadius();
		double len = 1.0 * r;
		double tr = Math.toRadians(getTilt());
		double yr = Math.toRadians(getStabYaw());
		double horiz = Math.sin(tr), vert = Math.cos(tr);
		double fx = -Math.sin(yr) * horiz, fz = Math.cos(yr) * horiz;
		net.minecraft.world.phys.Vec3 tip = new net.minecraft.world.phys.Vec3(
				getX() + fx * len, getY() - vert * len, getZ() + fz * len);
		net.minecraft.world.phys.Vec3 hnd = new net.minecraft.world.phys.Vec3(
				getX() - fx * len * 0.55, getY() + vert * len * 0.55, getZ() - fz * len * 0.55);
		return new net.minecraft.world.phys.Vec3[]{hnd, tip};
	}

	/**
	 * 視線レイ ( origin, dir(正規化), maxDist ) が武器の棒 ( 太さ TH ) に当たれば、
	 * レイ上の距離を返す。 当たらなければ -1。
	 */
	public double clipWeapon(net.minecraft.world.phys.Vec3 origin, net.minecraft.world.phys.Vec3 dir, double maxDist) {
		net.minecraft.world.phys.Vec3[] seg = weaponSegment();
		net.minecraft.world.phys.Vec3 q1 = origin.add(dir.x * maxDist, dir.y * maxDist, dir.z * maxDist);
		double th = 0.3 + getRadius() * 0.12; // 当たり太さ ( 判定半径で少し太く )
		double[] res = closestSegSeg(origin, q1, seg[0], seg[1]);
		if (res[0] <= th * th) return res[1] * maxDist;
		return -1;
	}

	/** 2 線分の最近距離² と、 1本目上の位置 s(0..1) を返す ( Ericson )。 */
	private static double[] closestSegSeg(net.minecraft.world.phys.Vec3 p1, net.minecraft.world.phys.Vec3 q1,
	                                      net.minecraft.world.phys.Vec3 p2, net.minecraft.world.phys.Vec3 q2) {
		net.minecraft.world.phys.Vec3 d1 = q1.subtract(p1);
		net.minecraft.world.phys.Vec3 d2 = q2.subtract(p2);
		net.minecraft.world.phys.Vec3 r = p1.subtract(p2);
		double a = d1.lengthSqr();
		double e = d2.lengthSqr();
		double f = d2.dot(r);
		double EPS = 1.0e-8;
		double s, t;
		if (a <= EPS && e <= EPS) {
			s = 0; t = 0;
		} else if (a <= EPS) {
			s = 0; t = clamp01(f / e);
		} else {
			double c = d1.dot(r);
			if (e <= EPS) {
				t = 0; s = clamp01(-c / a);
			} else {
				double b = d1.dot(d2);
				double denom = a * e - b * b;
				s = (denom != 0) ? clamp01((b * f - c * e) / denom) : 0;
				t = (b * s + f) / e;
				if (t < 0) { t = 0; s = clamp01(-c / a); }
				else if (t > 1) { t = 1; s = clamp01((b - c) / a); }
			}
		}
		net.minecraft.world.phys.Vec3 c1 = p1.add(d1.scale(s));
		net.minecraft.world.phys.Vec3 c2 = p2.add(d2.scale(t));
		return new double[]{c1.subtract(c2).lengthSqr(), s};
	}

	private static double clamp01(double v) {
		return v < 0 ? 0 : (v > 1 ? 1 : v);
	}

	private void retrieveTo(Player player) {
		ItemStack drop = getItem();
		if (!drop.isEmpty()) {
			ItemStack give = drop.copy();
			if (!player.addItem(give)) player.drop(give, false);
		}
		this.level().playSound(null, getX(), getY(), getZ(),
				SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.7f, 1.2f);
		this.discard();
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		if (tag.contains("StabItem")) setItem(ItemStack.of(tag.getCompound("StabItem")));
		setStabYaw(tag.getFloat("StabYaw"));
		if (tag.contains("StabTilt")) setTilt(tag.getFloat("StabTilt"));
		if (tag.contains("StabRadius")) setRadius(tag.getFloat("StabRadius"));
		if (tag.contains("StabRoll")) setRoll(tag.getFloat("StabRoll"));
		if (tag.contains("StabScale")) setScale(tag.getFloat("StabScale"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		if (!getItem().isEmpty()) tag.put("StabItem", getItem().save(new CompoundTag()));
		tag.putFloat("StabYaw", getStabYaw());
		tag.putFloat("StabTilt", getTilt());
		tag.putFloat("StabRadius", getRadius());
		tag.putFloat("StabRoll", getRoll());
		tag.putFloat("StabScale", getScale());
	}
}
