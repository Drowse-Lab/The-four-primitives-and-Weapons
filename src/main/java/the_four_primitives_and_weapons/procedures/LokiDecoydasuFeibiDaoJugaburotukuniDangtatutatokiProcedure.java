package the_four_primitives_and_weapons.procedures;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

public class LokiDecoydasuFeibiDaoJugaburotukuniDangtatutatokiProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (world instanceof ServerLevel _level)
			_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, (y + 3), z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
					"summon minecraft:armor_stand ~ ~ ~ {Invulnerable:1b,ShowArms:1b,NoBasePlate:1b,Tags:[\"Loki_Decoy\",\"Loki_Decoy_Taunt\"],Pose:{LeftArm:[0f,5f,-90f],RightArm:[0f,-5f,90f],LeftLeg:[0f,-5f,0f],RightLeg:[0f,5f,0f],Head:[0f,0f,0.1f]},DisabledSlots:4144959,ArmorItems:[{id:\"minecraft:leather_boots\",Count:1b,tag:{display:{color:11573855}}},{id:\"minecraft:leather_leggings\",Count:1b},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:11573855}}},{id:\"minecraft:player_head\",Count:1b,tag:{SkullOwner:{Id:[I;-2006638556,212421221,-1198641951,-1500527049],Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTg3YjhlYjEzZDk5MjU3NTlhMmU1ZjkwOTRhNWU1YzUwZDJlZWI0YWQ2ZWJiMjk0YjVjNzA2NDU5OTA3NmQwOCJ9fX0=\"}]}}}}]}");
		TheFourPrimitivesAndWeaponsMod.queueServerWork(2, () -> {
			{
				Entity _ent = entity;
				if (!_ent.level().isClientSide() && _ent.getServer() != null) {
					_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
							_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "effect give @e[type=minecraft:armor_stand,tag=Loki_Decoy] the_four_primitives_and_weapons:lokidecoyeffect 10 1");
				}
			}
		});
	}
}
