package the_four_primitives_and_weapons.procedures;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import the_four_primitives_and_weapons.network.TheFourPrimitivesAndWeaponsModVariables;

public class AaitemuwoShoudeChituteiruJiannoteitukuProcedure {
	// 軽量化版:
	//   旧: 毎 tick で player capability を 6 回取得 + 27 個の setBlock を 3 枝で実行 (= 最大 81 setBlock / tick)。
	//   新: 10 tick (0.5秒) に 1 回に間引き、 capability 取得を 1 回に集約、 7x3x7 (= 147 ブロック) の /fill 1 発に置換。
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null) return;
		if (entity.level().isClientSide()) return;
		if (entity.getServer() == null) return;
		if ((entity.tickCount % 10) != 0) return;

		double aaa = entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables()).aaa;

		String block;
		if (aaa == 4 || aaa == 6)      block = "minecraft:water";
		else if (aaa == 3 || aaa == 5) block = "minecraft:air";
		else if (aaa == 2)             block = "minecraft:dirt";
		else return;

		entity.getServer().getCommands().performPrefixedCommand(
				new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(),
						VersionHelper.getLevel(entity) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(entity) : null,
						4, entity.getName().getString(), entity.getDisplayName(), entity.level().getServer(), entity),
				"/fill ~3 ~-1 ~3 ~-3 ~-3 ~-3 " + block);
	}
}
