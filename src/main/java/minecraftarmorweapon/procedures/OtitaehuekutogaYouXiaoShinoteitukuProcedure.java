package minecraftarmorweapon.procedures;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;

import minecraftarmorweapon.entity.SkeltonMobEntity;
import minecraftarmorweapon.entity.OtiruyoEntity;
import minecraftarmorweapon.entity.KillotiruEntity;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class OtitaehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double zRadius3 = 0;
		double zRadius2 = 0;
		double yknockback = 0;
		double zRadius = 0;
		double xRadius2 = 0;
		double xRadius3 = 0;
		double xRadius4 = 0;
		double zRadius4 = 0;
		double dis = 0;
		double loop = 0;
		double X = 0;
		double Z2 = 0;
		double Y = 0;
		double Z3 = 0;
		double Z4 = 0;
		double Z = 0;
		double X2 = 0;
		double X3 = 0;
		double X4 = 0;
		double xknockback = 0;
		double loop2 = 0;
		double loop3 = 0;
		double zknockback = 0;
		double xRadius = 0;
		double loop4 = 0;
		double Y2 = 0;
		double Y3 = 0;
		double Y4 = 0;
		if (entity instanceof KillotiruEntity) {
			{
				final Vec3 _center = new Vec3(x, y, z);
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(20 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
						.collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!(entityiterator.getPersistentData().getBoolean("F to the P from Shibuya city") == true) || entityiterator.getPersistentData().getBoolean("F to the P from Shibuya city") == false) {
						if (!(entityiterator == entity)) {
							if (!(entityiterator instanceof OtiruyoEntity)) {
								if (!(entityiterator instanceof SkeltonMobEntity)) {
									if (entityiterator instanceof LivingEntity) {
										{
											Entity _ent = entityiterator;
											if (!_ent.level().isClientSide() && _ent.getServer() != null) {
												_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
														VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/kill @s");
											}
										}
										{
											Entity _ent = entityiterator;
											if (!_ent.level().isClientSide() && _ent.getServer() != null) {
												_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
														VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/deta merge entity @s (Health:0)");
											}
										}
										dis = Math.abs(xknockback) + Math.abs(yknockback) + Math.abs(zknockback);
									}
								}
							}
						}
					}
				}
			}
			MinecraftArmorWeaponMod.queueServerWork(2, () -> {
				if (!entity.level().isClientSide())
					entity.discard();
			});
		} else {
			{
				final Vec3 _center = new Vec3(x, y, z);
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(20 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
						.collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!(entityiterator.getPersistentData().getBoolean("F to the P from Shibuya city") == true) || entityiterator.getPersistentData().getBoolean("F to the P from Shibuya city") == false) {
						if (!(entityiterator == entity)) {
							if (!(entityiterator instanceof OtiruyoEntity)) {
								if (!(entityiterator instanceof SkeltonMobEntity)) {
									if (entityiterator instanceof LivingEntity) {
										if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
											{
												Entity _ent = entityiterator;
												if (!_ent.level().isClientSide() && _ent.getServer() != null) {
													_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
															VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/kill @s");
												}
											}
											{
												Entity _ent = entityiterator;
												if (!_ent.level().isClientSide() && _ent.getServer() != null) {
													_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
															VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/deta merge entity @s (Health:0)");
												}
											}
										} else {
											entityiterator.hurt(entityiterator.damageSources().generic(), 10);
										}
										dis = Math.abs(xknockback) + Math.abs(yknockback) + Math.abs(zknockback);
									}
								}
							}
						}
					}
				}
			}
			MinecraftArmorWeaponMod.queueServerWork(2, () -> {
				if (!entity.level().isClientSide())
					entity.discard();
			});
		}
	}
}
