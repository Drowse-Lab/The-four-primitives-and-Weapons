package minecraftarmorweapon.entity;

import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Rotations;
import net.minecraft.sounds.SoundEvents;

import minecraftarmorweapon.init.CustomEntityInit;

/**
 * 透明なアーマースタンド: 武器の3D展示用。
 * 常時 Invisible + ShowArms + NoBasePlate + 無重力 + 無敵。
 * 通常のアーマースタンドと同じく、右クリックで武器を着脱できる。
 * Sneak+右クリックで表示ポーズを循環。
 *
 * AutoRemoveOnEmpty NBTフラグが true のときは、メインハンドの武器が外された瞬間に
 * 展示台アイテムを1つドロップして自身を消滅させる。
 * 「剣が刺さってる岩を抜くと台座も消える」演出に使う。
 */
public class DisplayArmorStandEntity extends ArmorStand {

	public static final int POSE_HORIZONTAL = 0;
	public static final int POSE_HANGING = 1;
	public static final int POSE_STUCK = 2;
	public static final int POSE_COUNT = 3;

	private static final EntityDataAccessor<Integer> DATA_DISPLAY_POSE =
		SynchedEntityData.defineId(DisplayArmorStandEntity.class, EntityDataSerializers.INT);

	private boolean autoRemoveOnEmpty = false;
	private boolean hadWeapon = false;

	public DisplayArmorStandEntity(EntityType<? extends ArmorStand> type, Level world) {
		super(type, world);
		this.setInvisible(true);
		this.setInvulnerable(true);
		this.setNoGravity(true);

		CompoundTag tag = new CompoundTag();
		this.addAdditionalSaveData(tag);
		tag.putBoolean("Invisible", true);
		tag.putBoolean("ShowArms", true);
		tag.putBoolean("NoBasePlate", true);
		tag.putBoolean("NoGravity", true);
		tag.putBoolean("Invulnerable", true);
		this.readAdditionalSaveData(tag);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_DISPLAY_POSE, POSE_HORIZONTAL);
	}

	public int getDisplayPose() {
		return this.entityData.get(DATA_DISPLAY_POSE);
	}

	public void setDisplayPose(int pose) {
		int normalized = ((pose % POSE_COUNT) + POSE_COUNT) % POSE_COUNT;
		this.entityData.set(DATA_DISPLAY_POSE, normalized);
		applyArmPoseFor(normalized);
	}

	public boolean isAutoRemoveOnEmpty() {
		return this.autoRemoveOnEmpty;
	}

	public void setAutoRemoveOnEmpty(boolean v) {
		this.autoRemoveOnEmpty = v;
	}

	/**
	 * HORIZONTAL: 右腕を完全に真下(0,0,0)に。
	 * これで剣がエンティティ局所座標でちょうど真上を向き、Z軸-90°回転で水平になる。
	 * 他ポーズはバニラ標準の(-10,0,-10)。
	 */
	private void applyArmPoseFor(int pose) {
		if (pose == POSE_HORIZONTAL) {
			this.setRightArmPose(new Rotations(0.0F, 0.0F, 0.0F));
		} else {
			this.setRightArmPose(new Rotations(-10.0F, 0.0F, -10.0F));
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("DisplayPose", getDisplayPose());
		tag.putBoolean("AutoRemoveOnEmpty", this.autoRemoveOnEmpty);
		tag.putBoolean("HadWeapon", this.hadWeapon);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("DisplayPose")) {
			int p = tag.getInt("DisplayPose");
			this.entityData.set(DATA_DISPLAY_POSE, ((p % POSE_COUNT) + POSE_COUNT) % POSE_COUNT);
		}
		if (tag.contains("AutoRemoveOnEmpty")) {
			this.autoRemoveOnEmpty = tag.getBoolean("AutoRemoveOnEmpty");
		}
		if (tag.contains("HadWeapon")) {
			this.hadWeapon = tag.getBoolean("HadWeapon");
		}
	}

	@Override
	public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
		if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
			if (!this.level().isClientSide()) {
				setDisplayPose(getDisplayPose() + 1);
				this.playSound(SoundEvents.ARMOR_STAND_PLACE, 0.6F, 1.2F);
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide());
		}
		return super.interactAt(player, vec, hand);
	}

	@Override
	public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
		super.setItemSlot(slot, stack);
		if (slot == EquipmentSlot.MAINHAND) {
			boolean nowHasWeapon = !stack.isEmpty();
			if (this.hadWeapon && !nowHasWeapon && this.autoRemoveOnEmpty && !this.level().isClientSide()) {
				ItemStack drop = new ItemStack(CustomEntityInit.DISPLAY_ARMOR_STAND_ITEM.get());
				this.spawnAtLocation(drop);
				this.discard();
				return;
			}
			this.hadWeapon = nowHasWeapon;
		}
	}
}
