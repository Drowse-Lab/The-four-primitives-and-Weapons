package minecraftarmorweapon.procedures;

import net.minecraft.world.level().LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level().ServerLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

public class LokidecoyeffectposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		// Decoy召喚時にスコアボードを初期化
		if (!entity.level().isClientSide() && entity.level() instanceof ServerLevel) {
			ServerLevel serverLevel = (ServerLevel) entity.level();
			serverLevel.getServer().getCommands().performPrefixedCommand(
				new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(),
					serverLevel, 4, entity.getName().getString(), entity.getDisplayName(),
					serverLevel.getServer(), entity),
				"scoreboard objectives add Decoy_Action dummy");

			serverLevel.getServer().getCommands().performPrefixedCommand(
				new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(),
					serverLevel, 4, entity.getName().getString(), entity.getDisplayName(),
					serverLevel.getServer(), entity),
				"scoreboard objectives add Decoy_Spin dummy");
		}
	}
}
