package minecraftarmorweapon.item;

import minecraftarmorweapon.entity.WeaponRackEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * WeaponRackEntity を設置するアイテム。
 * vanilla の ItemFrameItem と同等の設置ロジック。
 * woodIndex は WeaponRackEntity.WOOD_TYPES のインデックス (0=oak, 1=spruce, ...)。
 */
public class WeaponRackItem extends Item {

	private final int woodIndex;

	public WeaponRackItem(Item.Properties props) {
		this(props, 0);
	}

	public WeaponRackItem(Item.Properties props, int woodIndex) {
		super(props);
		this.woodIndex = woodIndex;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos clicked = context.getClickedPos();
		Direction face = context.getClickedFace();
		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();

		// クリック先のブロックが置換可能 (snow_layer / grass / fern / water 等) なら、
		// そのブロックを破壊してその位置にラックを置く。それ以外は通常通り隣接位置に。
		BlockPos placePos;
		if (level.getBlockState(clicked).canBeReplaced()) {
			placePos = clicked;
			if (!level.isClientSide()) {
				level.destroyBlock(clicked, true);
			}
		} else {
			placePos = clicked.relative(face);
		}

		if (player != null && !this.canPlace(player, face, stack, placePos)) {
			return InteractionResult.FAIL;
		}
		WeaponRackEntity rack = new WeaponRackEntity(level, placePos, face);
		rack.setWoodIndex(this.woodIndex);

		CompoundTag tag = stack.getTag();
		if (tag != null) {
			EntityType.updateCustomEntityTag(level, player, rack, tag);
		}

		if (rack.survives()) {
			if (!level.isClientSide()) {
				rack.playPlacementSound();
				level.gameEvent(player, GameEvent.ENTITY_PLACE, rack.position());
				if (level instanceof ServerLevel sl) {
					sl.addFreshEntity(rack);
				} else {
					level.addFreshEntity(rack);
				}
				level.playSound(null, rack.getX(), rack.getY(), rack.getZ(),
					SoundEvents.ITEM_FRAME_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
			}
			stack.shrink(1);
			return InteractionResult.sidedSuccess(level.isClientSide());
		}
		return InteractionResult.CONSUME;
	}

	protected boolean canPlace(Player player, Direction face, ItemStack stack, BlockPos pos) {
		return pos.getY() >= player.level().getMinBuildHeight() && player.mayUseItemAt(pos, face, stack);
	}
}
