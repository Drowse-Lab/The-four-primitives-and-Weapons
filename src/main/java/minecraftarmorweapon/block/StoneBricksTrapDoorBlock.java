
package minecraftarmorweapon.block;

import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.material.MapColor;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Collections;

public class StoneBricksTrapDoorBlock extends TrapDoorBlock {
	public StoneBricksTrapDoorBlock() {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).sound(SoundType.STONE).strength(3f, 10f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false).dynamicShape(), BlockSetType.STONE);
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 0;
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		List<ItemStack> dropsOriginal = super.getDrops(state, builder);
		if (!dropsOriginal.isEmpty())
			return dropsOriginal;
		return Collections.singletonList(new ItemStack(this, 1));
	}
}
