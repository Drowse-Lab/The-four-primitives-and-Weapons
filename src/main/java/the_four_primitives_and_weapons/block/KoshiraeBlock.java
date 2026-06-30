package the_four_primitives_and_weapons.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * 拵え台 ( こしらえだい ): 刀/鞘 を入れて 柄巻き・鍔・仕立て を選ぶ作業ブロック。
 * 見た目は横長 ( モデルで2幅相当 )。 右クリックで {@link the_four_primitives_and_weapons.menu.KoshiraeMenu} を開く。
 */
public class KoshiraeBlock extends HorizontalDirectionalBlock {

	private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 1.0D, 16.0D, 13.0D, 15.0D);
	private static final Component TITLE =
			Component.translatable("container.the_four_primitives_and_weapons.koshirae");

	public KoshiraeBlock(Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
								 InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide && player instanceof ServerPlayer sp) {
			NetworkHooks.openScreen(sp, getMenuProvider(state, level, pos), pos);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Nullable
	@Override
	public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return new SimpleMenuProvider((id, inv, p) -> create(id, inv, level, pos), TITLE);
	}

	private static AbstractContainerMenu create(int id, Inventory inv, Level level, BlockPos pos) {
		return new the_four_primitives_and_weapons.menu.KoshiraeMenu(id, inv, ContainerLevelAccess.create(level, pos));
	}
}
