package minecraftarmorweapon.procedures;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

public class DespornKentimobugaturudeGongJisaretatokiProcedure {
	public static void execute(Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (!(sourceentity instanceof Player)) {
			if (!(sourceentity instanceof ServerPlayer)) {
				if (!entity.level.isClientSide())
					entity.discard();
			} else {
				{
					Entity _ent = entity;
					if (!_ent.level.isClientSide() && _ent.getServer() != null) {
						_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
								_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "kill @s");
					}
				}
			}
		} else {
			{
				Entity _ent = entity;
				if (!_ent.level.isClientSide() && _ent.getServer() != null) {
					_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
							_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "kill @s");
				}
			}
		}
	}
}
