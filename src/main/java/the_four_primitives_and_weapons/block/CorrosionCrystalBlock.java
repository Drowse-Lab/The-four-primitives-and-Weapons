package the_four_primitives_and_weapons.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ModDamageSources;

/** ガラス質の侵食結晶。採掘した本人へ侵食Lv1相当の1ダメージを返す。 */
public class CorrosionCrystalBlock extends Block {
    public CorrosionCrystalBlock(Properties properties) { super(properties); }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              net.minecraft.world.level.block.entity.BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!level.isClientSide && !player.isCreative())
            player.hurt(ModDamageSources.ofElement(level, ElementType.CORROSION, player), 1.0F);
    }
}
