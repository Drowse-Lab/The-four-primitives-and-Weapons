package the_four_primitives_and_weapons.procedures;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

public class AaaYoukuritukusitatokiProcedure {
	// 軽量化:
	//   旧: 毎 tick で /fill ~3 ~3 ~3 ~-3 ~-3 ~-3 end_stone (= 343 ブロック書き込み) を実行。
	//   新: 10 tick (0.5秒) に 1 回に間引き。
	public static void execute(Entity entity) {
		if (entity == null) return;
		if (entity.level().isClientSide()) return;
		if (entity.getServer() == null) return;
		if ((entity.tickCount % 10) != 0) return;

		entity.getServer().getCommands().performPrefixedCommand(
				new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(),
						VersionHelper.getLevel(entity) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(entity) : null,
						4, entity.getName().getString(), entity.getDisplayName(), entity.level().getServer(), entity),
				"/fill ~3 ~3 ~3 ~-3 ~-3 ~-3 minecraft:end_stone");
	}
}
