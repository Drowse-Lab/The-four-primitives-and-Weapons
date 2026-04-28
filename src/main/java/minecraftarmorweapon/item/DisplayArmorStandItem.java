package minecraftarmorweapon.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import minecraftarmorweapon.entity.DisplayArmorStandEntity;
import minecraftarmorweapon.init.CustomEntityInit;

/**
 * DisplayArmorStandEntity を設置するアイテム。
 * クリックされた面で初期ポーズが決まる:
 *   UP    (ブロック上面) → STUCK     (剣が地面に刺さる)
 *   DOWN  (ブロック下面) → HANGING   (天井から吊り下げ)
 *   side                → HORIZONTAL(地面に平行)
 */
public class DisplayArmorStandItem extends Item {

	public DisplayArmorStandItem() {
		super(new Properties().stacksTo(16));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Direction face = context.getClickedFace();
		// 側面に置くと宙に浮くので、上面・下面のみ許可
		if (face != Direction.UP && face != Direction.DOWN) return InteractionResult.FAIL;

		Level level = context.getLevel();
		BlockPos placePos = context.getClickedPos().relative(face);

		if (!level.getBlockState(placePos).canBeReplaced()) return InteractionResult.FAIL;

		int initialPose = poseForFace(face);

		if (level instanceof ServerLevel serverLevel) {
			DisplayArmorStandEntity stand = CustomEntityInit.DISPLAY_ARMOR_STAND.get().create(serverLevel);
			if (stand == null) return InteractionResult.FAIL;

			float yaw = Mth.floor((Mth.wrapDegrees(context.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
			stand.moveTo(placePos.getX() + 0.5, placePos.getY(), placePos.getZ() + 0.5, yaw, 0.0F);
			stand.setYBodyRot(yaw);
			stand.setYHeadRot(yaw);
			stand.setDisplayPose(initialPose);
			// STUCK（剣が地面に刺さる）は「剣を抜くと台座も消える」演出が自然なので
			// デフォルトで AutoRemoveOnEmpty を有効化。他のポーズはfalseのまま。
			if (initialPose == DisplayArmorStandEntity.POSE_STUCK) {
				stand.setAutoRemoveOnEmpty(true);
			}

			serverLevel.addFreshEntityWithPassengers(stand);
			level.playSound(null, stand.getX(), stand.getY(), stand.getZ(),
				SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
			stand.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE, context.getPlayer());
		}

		ItemStack stack = context.getItemInHand();
		Player player = context.getPlayer();
		if (player != null && !player.getAbilities().instabuild) stack.shrink(1);

		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	private static int poseForFace(Direction face) {
		if (face == Direction.UP) return DisplayArmorStandEntity.POSE_STUCK;
		if (face == Direction.DOWN) return DisplayArmorStandEntity.POSE_HANGING;
		return DisplayArmorStandEntity.POSE_HORIZONTAL;
	}
}
