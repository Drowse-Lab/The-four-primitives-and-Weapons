
package the_four_primitives_and_weapons.block;

import net.minecraft.world.level.storage.loot.LootParams;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

import the_four_primitives_and_weapons.procedures.AlchemyCraftBlockBlockAddedProcedure;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModBlocks;

import java.util.List;
import java.util.Collections;

public class AlchemyCraftBlockBlock extends Block {
	public AlchemyCraftBlockBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(1f, 10f));
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 15;
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		List<ItemStack> dropsOriginal = super.getDrops(state, builder);
		if (!dropsOriginal.isEmpty())
			return dropsOriginal;
		return Collections.singletonList(new ItemStack(TheFourPrimitivesAndWeaponsModBlocks.MAGIC_POT.get()));
	}

	@Override
	public void onPlace(BlockState blockstate, Level world, BlockPos pos, BlockState oldState, boolean moving) {
		super.onPlace(blockstate, world, pos, oldState, moving);
		AlchemyCraftBlockBlockAddedProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
	}
}
