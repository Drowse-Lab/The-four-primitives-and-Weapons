package minecraftarmorweapon.procedures;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

public class OnazitakasaArrowposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure {
	public static void execute(double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double r = 0;
		double alpha = 0;
		double beta = 0;
		entity.getPersistentData().putDouble("X", x);
		entity.getPersistentData().putDouble("Z", z);
		entity.getPersistentData().putDouble("Ypos", y);
		entity.getPersistentData().putDouble("yaw", (entity.getYRot()));
		entity.getPersistentData().putDouble("distance", 3);
		{
			Entity _ent = entity;
			if (!_ent.level.isClientSide() && _ent.getServer() != null) {
				_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
						_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/playsound minecraft:entity.generic.explode block @s");
			}
		}
	}
}
