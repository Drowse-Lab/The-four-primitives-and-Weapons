package minecraftarmorweapon.entity;

import minecraftarmorweapon.init.CustomEntityInit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/**
 * Arsenal (doctor4t) の WeaponRackEntity を Forge 1.20.1 用に 1:1 移植。
 * vanilla の ItemFrame を継承し、displayable タグ判定 / 非表示化トグル / 解除条件をオーバーライド。
 */
public class WeaponRackEntity extends ItemFrame {

	public static final TagKey<Item> DISPLAYABLE = TagKey.create(Registries.ITEM,
		new ResourceLocation("minecraft_armor_weapon", "displayable"));

	private static final Predicate<Entity> HANGING_PREDICATE = e -> e instanceof HangingEntity;

	/** ラックの木の種類。index = NBT に保存する byte 値。順番固定 (oak=0)。 */
	public static final String[] WOOD_TYPES = {
		"oak", "spruce", "birch", "jungle", "acacia",
		"dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"
	};

	private static final EntityDataAccessor<Byte> DATA_WOOD_TYPE =
		SynchedEntityData.defineId(WeaponRackEntity.class, EntityDataSerializers.BYTE);

	// ホットキー編集で蓄積される追加回転 (degrees)。プリセットポーズの上に乗る。
	private static final EntityDataAccessor<Float> DATA_EXTRA_ROT_X =
		SynchedEntityData.defineId(WeaponRackEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_EXTRA_ROT_Y =
		SynchedEntityData.defineId(WeaponRackEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_EXTRA_ROT_Z =
		SynchedEntityData.defineId(WeaponRackEntity.class, EntityDataSerializers.FLOAT);

	// 床ラック専用: 2 個目のアイテムスロット (右側)。
	// 1 個目 (左側) は ItemFrame の DATA_ITEM をそのまま使う。
	private static final EntityDataAccessor<ItemStack> DATA_ITEM_2 =
		SynchedEntityData.defineId(WeaponRackEntity.class, EntityDataSerializers.ITEM_STACK);

	// interactAt で記録した最後のクリック位置 (world coords)。interact で使う。
	private Vec3 lastInteractAt = null;

	public WeaponRackEntity(EntityType<? extends WeaponRackEntity> type, Level level) {
		super(type, level);
	}

	public WeaponRackEntity(Level level, BlockPos pos, Direction facing) {
		this(CustomEntityInit.WEAPON_RACK.get(), level, pos, facing);
	}

	public WeaponRackEntity(EntityType<? extends WeaponRackEntity> type, Level level, BlockPos pos, Direction facing) {
		super(type, level, pos, facing);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_EXTRA_ROT_X, 0f);
		this.entityData.define(DATA_EXTRA_ROT_Y, 0f);
		this.entityData.define(DATA_EXTRA_ROT_Z, 0f);
		this.entityData.define(DATA_ITEM_2, ItemStack.EMPTY);
		this.entityData.define(DATA_WOOD_TYPE, (byte) 0);
	}

	public int getWoodIndex() {
		int idx = this.entityData.get(DATA_WOOD_TYPE) & 0xFF;
		return idx < WOOD_TYPES.length ? idx : 0;
	}

	public String getWoodName() {
		return WOOD_TYPES[this.getWoodIndex()];
	}

	public void setWoodIndex(int index) {
		if (index < 0 || index >= WOOD_TYPES.length) index = 0;
		this.entityData.set(DATA_WOOD_TYPE, (byte) index);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putFloat("ExtraRotX", this.getExtraRotX());
		tag.putFloat("ExtraRotY", this.getExtraRotY());
		tag.putFloat("ExtraRotZ", this.getExtraRotZ());
		if (!this.getItem2().isEmpty()) {
			tag.put("Item2", this.getItem2().save(new CompoundTag()));
		}
		tag.putByte("WoodType", (byte) this.getWoodIndex());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.entityData.set(DATA_EXTRA_ROT_X, tag.getFloat("ExtraRotX"));
		this.entityData.set(DATA_EXTRA_ROT_Y, tag.getFloat("ExtraRotY"));
		this.entityData.set(DATA_EXTRA_ROT_Z, tag.getFloat("ExtraRotZ"));
		if (tag.contains("Item2")) {
			this.setItem2(ItemStack.of(tag.getCompound("Item2")));
		} else {
			this.setItem2(ItemStack.EMPTY);
		}
		// 旧バージョンの NBT 残骸: invisible フラグだけクリア (透明化バグ対策)。
		// rotation は新仕様で 8 段階回転に再利用するので保持。
		this.setInvisible(false);
		// 旧 NBT 互換: WoodType が無ければ oak (=0)。
		if (tag.contains("WoodType")) {
			this.setWoodIndex(tag.getByte("WoodType") & 0xFF);
		}
	}

	public ItemStack getItem2() { return this.entityData.get(DATA_ITEM_2); }
	public void setItem2(ItemStack stack) {
		ItemStack copy = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
		if (!copy.isEmpty()) copy.setCount(1);
		this.entityData.set(DATA_ITEM_2, copy);
	}

	/** 床 + 壁ラックは 2 スロット対応。天井のみ 1 スロット。 */
	public boolean supportsTwoSlots() {
		Direction d = this.getDirection();
		return d == Direction.UP || (d != null && d.getAxis().isHorizontal());
	}

	/**
	 * クリック位置からスロット 2 (右側) かを判定。
	 *   床/天井: hit.x > 0 で右側 (model の長軸 X 上の右)
	 *   壁: facing の counter-clockwise 方向 (= プレイヤー視点の右) に projection
	 */
	private boolean isHittingSlot2Side(Vec3 hit) {
		Direction d = this.getDirection();
		if (d == null) return false;
		if (d.getAxis() == Direction.Axis.Y) {
			return hit.x > 0;
		}
		Direction right = d.getCounterClockWise();
		double dot = hit.x * right.getStepX() + hit.z * right.getStepZ();
		return dot > 0;
	}

	public float getExtraRotX() { return this.entityData.get(DATA_EXTRA_ROT_X); }
	public float getExtraRotY() { return this.entityData.get(DATA_EXTRA_ROT_Y); }
	public float getExtraRotZ() { return this.entityData.get(DATA_EXTRA_ROT_Z); }

	public void setExtraRotX(float v) { this.entityData.set(DATA_EXTRA_ROT_X, normalizeDeg(v)); }
	public void setExtraRotY(float v) { this.entityData.set(DATA_EXTRA_ROT_Y, normalizeDeg(v)); }
	public void setExtraRotZ(float v) { this.entityData.set(DATA_EXTRA_ROT_Z, normalizeDeg(v)); }

	public void resetExtraRot() {
		this.entityData.set(DATA_EXTRA_ROT_X, 0f);
		this.entityData.set(DATA_EXTRA_ROT_Y, 0f);
		this.entityData.set(DATA_EXTRA_ROT_Z, 0f);
	}

	private static float normalizeDeg(float v) {
		v = v % 360f;
		if (v > 180f) v -= 360f;
		if (v < -180f) v += 360f;
		return v;
	}

	@Override
	public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
		// クリック位置を保存して interact() で参照する。Vec3 は entity 座標系のオフセット。
		this.lastInteractAt = vec;
		return InteractionResult.PASS;
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		ItemStack stackInHand = player.getItemInHand(hand);
		boolean twoSlots = this.supportsTwoSlots();

		// クリック位置からスロット判別 (右側 = slot 2)。
		boolean useSlot2 = twoSlots && this.lastInteractAt != null && isHittingSlot2Side(this.lastInteractAt);
		this.lastInteractAt = null;

		ItemStack slotItem = useSlot2 ? this.getItem2() : this.getItem();
		boolean slotHasItem = !slotItem.isEmpty();

		// 1. 設置: 該当スロットが空 + displayable アイテム
		if (!slotHasItem && stackInHand.is(DISPLAYABLE)) {
			if (!this.level().isClientSide()) {
				ItemStack toPlace = stackInHand.copy();
				toPlace.setCount(1);
				if (useSlot2) {
					this.setItem2(toPlace);
				} else {
					this.setItem(toPlace);
				}
				// 設置時は rotation を 0 にリセット (base pose から始まる)、 invisible も外す
				this.setRotation(0);
				this.setInvisible(false);
				if (!player.getAbilities().instabuild) {
					stackInHand.shrink(1);
				}
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide());
		}

		// 2. 回転: 該当スロットに既にアイテムが入っている場合 → rotation++ で次のポーズへ
		//    壁: 8 段階 (45° 刻み) / 床: 6 段階 (pk_racks ポーズ) / 天井: 1 (回転なし)
		if (slotHasItem) {
			if (!this.level().isClientSide()) {
				int max = poseCountForDirection(this.getDirection());
				this.setRotation((this.getRotation() + 1) % Math.max(1, max));
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide());
		}

		return InteractionResult.PASS;
	}

	/** ground=6 (pk_racks準拠) / wall=8 (45°刻み) / ceiling=1 */
	public static int poseCountForDirection(Direction direction) {
		if (direction == Direction.UP) return 6;
		if (direction == Direction.DOWN) return 1;
		return 8;
	}

	@Override
	public boolean isInvisible() {
		// ラックは常に表示する。旧バージョンで Sneak+右クリックで非表示トグルしてた時の
		// NBT (Invisible: true) が残ってても無視して常に可視に。
		return false;
	}

	@Override
	public boolean survives() {
		return this.level().getEntities(this, this.getBoundingBox(), HANGING_PREDICATE).isEmpty();
	}

	/**
	 * デフォルトの ItemFrame hitbox は薄い板状で、立体的なラックには小さすぎる。
	 * facing に応じてラック本体をカバーするように拡張する。
	 *   床 (UP): 1ブロック立方体 (脚〜天板)
	 *   天井 (DOWN): 1ブロック立方体 (チェーン〜フック)
	 *   壁 (横): 0.5ブロック前方 + 1ブロック高 (ポスト + 斜め腕)
	 */
	@Override
	protected void recalculateBoundingBox() {
		super.recalculateBoundingBox();
		Direction dir = this.getDirection();
		if (dir == null) return;
		double cx = this.getX();
		double cy = this.getY();
		double cz = this.getZ();
		AABB box;
		if (dir == Direction.UP) {
			// 床ラック: 床に乗る 1 ブロック立方体
			box = new AABB(cx - 0.5, cy - 0.03, cz - 0.5, cx + 0.5, cy + 0.97, cz + 0.5);
		} else if (dir == Direction.DOWN) {
			// 天井ラック: 天井から下に 1 ブロック
			box = new AABB(cx - 0.5, cy - 0.97, cz - 0.5, cx + 0.5, cy + 0.03, cz + 0.5);
		} else {
			// 壁ラック: 1ブロック高、壁から前方 (facing 方向) に 0.5 ブロック張り出し。
			// entity は壁面から +0.03 内側にあるので、facing 方向に +0.5 伸ばすだけで概ねカバー。
			double sx = dir.getStepX();
			double sz = dir.getStepZ();
			if (dir.getAxis() == Direction.Axis.X) {
				// EAST/WEST: X方向に伸ばす、Y/Zは1ブロック幅
				box = new AABB(
					Math.min(cx, cx + sx * 0.5) - 0.05, cy - 0.5, cz - 0.5,
					Math.max(cx, cx + sx * 0.5) + 0.05, cy + 0.5, cz + 0.5);
			} else {
				// NORTH/SOUTH: Z方向に伸ばす、X/Yは1ブロック幅
				box = new AABB(
					cx - 0.5, cy - 0.5, Math.min(cz, cz + sz * 0.5) - 0.05,
					cx + 0.5, cy + 0.5, Math.max(cz, cz + sz * 0.5) + 0.05);
			}
		}
		this.setBoundingBox(box);
	}

	@Override
	protected ItemStack getFrameItemStack() {
		return new ItemStack(itemForWoodIndex(this.getWoodIndex()));
	}

	private static Item itemForWoodIndex(int idx) {
		return switch (idx) {
			case 1 -> CustomEntityInit.WEAPON_RACK_SPRUCE.get();
			case 2 -> CustomEntityInit.WEAPON_RACK_BIRCH.get();
			case 3 -> CustomEntityInit.WEAPON_RACK_JUNGLE.get();
			case 4 -> CustomEntityInit.WEAPON_RACK_ACACIA.get();
			case 5 -> CustomEntityInit.WEAPON_RACK_DARK_OAK.get();
			case 6 -> CustomEntityInit.WEAPON_RACK_MANGROVE.get();
			case 7 -> CustomEntityInit.WEAPON_RACK_CHERRY.get();
			case 8 -> CustomEntityInit.WEAPON_RACK_BAMBOO.get();
			case 9 -> CustomEntityInit.WEAPON_RACK_CRIMSON.get();
			case 10 -> CustomEntityInit.WEAPON_RACK_WARPED.get();
			default -> CustomEntityInit.WEAPON_RACK_ITEM.get();
		};
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		// 両スロットが空のとき + プレイヤーが non-sneak で攻撃 → 無敵 (壊れない)
		return source.getEntity() instanceof Player p
			&& this.getItem().isEmpty()
			&& this.getItem2().isEmpty()
			&& !p.isShiftKeyDown();
	}

	/**
	 * 両スロットの drop を処理する hurt オーバーライド。
	 *   slot 1 にアイテムあり → slot 1 をドロップ (フレームは残る)
	 *   slot 1 空 + slot 2 にアイテムあり → slot 2 をドロップ (フレームは残る)
	 *   両方空 → vanilla に委ねる (フレーム破壊)
	 */
	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (this.isInvulnerableTo(source)) return false;
		if (!source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) && !this.level().isClientSide()) {
			boolean creative = source.getEntity() instanceof Player p && p.getAbilities().instabuild;
			if (!this.getItem().isEmpty()) {
				ItemStack item = this.getItem().copy();
				this.setItem(ItemStack.EMPTY);
				if (!creative) spawnDrop(item);
				return true;
			}
			if (!this.getItem2().isEmpty()) {
				ItemStack item = this.getItem2().copy();
				this.setItem2(ItemStack.EMPTY);
				if (!creative) spawnDrop(item);
				return true;
			}
		}
		return super.hurt(source, amount);
	}

	private void spawnDrop(ItemStack stack) {
		ItemEntity drop = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), stack);
		drop.setDefaultPickUpDelay();
		this.level().addFreshEntity(drop);
	}
}
