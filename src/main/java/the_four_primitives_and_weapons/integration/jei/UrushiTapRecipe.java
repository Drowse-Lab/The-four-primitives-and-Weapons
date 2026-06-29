package the_four_primitives_and_weapons.integration.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 漆の採取 ( 原木に 道具を使う ) を JEI で 3D 表示するためのレシピデータ。
 *
 * @param tools  採取に使える道具 ( 火打石・空きビン )。 スロットで巡回表示。
 * @param result 採れるもの ( 生漆 )。
 * @param block  3D 表示する代表ブロック ( 原木 )。
 */
public class UrushiTapRecipe {

	public final List<ItemStack> tools;
	public final ItemStack result;
	public final BlockState block;

	public UrushiTapRecipe(List<ItemStack> tools, ItemStack result, BlockState block) {
		this.tools = tools;
		this.result = result;
		this.block = block;
	}
}
