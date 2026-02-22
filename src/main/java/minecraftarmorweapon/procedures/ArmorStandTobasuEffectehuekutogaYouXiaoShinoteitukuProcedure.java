package minecraftarmorweapon.procedures;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import minecraftarmorweapon.entity.SkeltonMobEntity;
import minecraftarmorweapon.entity.OtiruyoEntity;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class ArmorStandTobasuEffectehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double zknockback = 0;
		double yknockback = 0;
		double loop = 0;
		double XRadius2 = 0;
		double ZRadius2 = 0;
		double X = 0;
		double Y = 0;
		double Z = 0;
		double xknockback = 0;
		double Y_pos = 0;
		double dis = 0;
		double zPos = 0;
		double yPos = 0;
		double xPos = 0;
		double rad = 0;
		double rad_now = 0;
		double radius = 0;
		double r = 0;
		double alpha = 0;
		double beta = 0;
		{
			Entity _ent = entity;
			if (!_ent.level.isClientSide() && _ent.getServer() != null) {
				_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
						_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/particle cloud ~ ~ ~ 0.5 0.1 0.5 .0 10 force");
			}
		}
		{
			Entity _ent = entity;
			if (!_ent.level.isClientSide() && _ent.getServer() != null) {
				_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
						_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/particle sweep_attack ~ ~0.5 ~ 0.5 0.1 0.5 .0 10 force");
			}
		}
		r = 1;
		alpha = entity.getYRot();
		beta = entity.getXRot();
		for (int index0 = 0; index0 < 2; index0++) {
			if (world.getBlockState(new BlockPos(x - r * Math.cos(Math.toRadians(beta)) * Math.sin(Math.toRadians(alpha)), (y - 0.3) - r * Math.sin(Math.toRadians(beta)), z + r * Math.cos(Math.toRadians(beta)) * Math.cos(Math.toRadians(alpha))))
					.canOcclude()) {
				if (!entity.level.isClientSide())
					entity.discard();
				break;
			}
			r = r + 0.2;
		}
		if (world instanceof Level _level) {
			if (!_level.isClientSide()) {
				_level.playSound(null, new BlockPos(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.player.attack.sweep")), SoundSource.PLAYERS, 1, 1);
			} else {
				_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.player.attack.sweep")), SoundSource.PLAYERS, 1, 1, false);
			}
		}
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (!(entityiterator == entity)) {
					if (!(entityiterator instanceof OtiruyoEntity)) {
						if (!(entityiterator instanceof SkeltonMobEntity)) {
							if (!(entityiterator.getPersistentData().getBoolean("minecraft_armor_weapon:armor_stand_tobasu_kill_off") == true)) {
								if (entityiterator instanceof LivingEntity) {
									entityiterator.hurt(DamageSource.GENERIC, 5);
								}
							}
						}
					}
				}
			}
		}
	}
}
