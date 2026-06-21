package the_four_primitives_and_weapons.procedures;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import the_four_primitives_and_weapons.network.TheFourPrimitivesAndWeaponsModVariables;

public class A2aitemuwoShoudeChituteiruJiannoteitukuProcedure {
	// 軽量化版:
	//   旧: 毎 tick で player capability を 5 回取得 + 7×7×7=343 ブロックの /fill コマンドを毎 tick 実行。
	//   新: 10 tick (0.5秒) に 1 回に間引き、 capability 取得を 1 回に集約、 値マッチで 1 コマンドだけ実行。
	public static void execute(Entity entity) {
		if (entity == null) return;
		if (entity.level().isClientSide()) return;
		if (entity.getServer() == null) return;
		// 10 tick throttle (per-stack inventoryTick で 1 秒に 2 回のみ /fill 実行)
		if ((entity.tickCount % 10) != 0) return;

		double aaa = entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables()).aaa;

		String block = null;
		if      (aaa == 6) block = "minecraft:deepslate";
		else if (aaa == 5) block = "minecraft:air";
		else if (aaa == 4) block = "minecraft:water";
		else if (aaa == 3) block = "minecraft:stone";
		else if (aaa == 2) block = "minecraft:dirt";
		if (block == null) return;

		entity.getServer().getCommands().performPrefixedCommand(
				new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(),
						VersionHelper.getLevel(entity) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(entity) : null,
						4, entity.getName().getString(), entity.getDisplayName(), entity.level().getServer(), entity),
				"/fill ~3 ~3 ~3 ~-3 ~-3 ~-3 " + block);
	}
}
