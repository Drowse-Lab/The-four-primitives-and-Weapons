package minecraftarmorweapon.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

public class GuardehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (world instanceof ServerLevel _level)
			_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
					"execute as @e[tag=minecraft_armor_weapon_guard_bind] at @s run tp @s ~ ~ ~ ~40 ~");
		{
			Entity _ent = entity;
			_ent.teleportTo((entity.getPersistentData().getDouble("minecraft_armor_weapon:muteki_x_chuzume")), (entity.getPersistentData().getDouble("minecraft_armor_weapon:muteki_y_chuzume")),
					(entity.getPersistentData().getDouble("minecraft_armor_weapon:muteki_z_chuzume")));
			if (_ent instanceof ServerPlayer _serverPlayer)
				_serverPlayer.connection.teleport((entity.getPersistentData().getDouble("minecraft_armor_weapon:muteki_x_chuzume")), (entity.getPersistentData().getDouble("minecraft_armor_weapon:muteki_y_chuzume")),
						(entity.getPersistentData().getDouble("minecraft_armor_weapon:muteki_z_chuzume")), _ent.getYRot(), _ent.getXRot());
		}
	}
}
