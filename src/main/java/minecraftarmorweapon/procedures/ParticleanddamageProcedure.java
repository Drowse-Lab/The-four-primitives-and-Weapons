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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;

import minecraftarmorweapon.entity.SkeltonMobEntity;
import minecraftarmorweapon.entity.OtiruyoEntity;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class ParticleanddamageProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double zknockback = 0;
		double r = 0;
		double yknockback = 0;
		double alpha = 0;
		double xknockback = 0;
		double beta = 0;
		double dis = 0;
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SWEEP_ATTACK, (entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos")), (entity.getPersistentData().getDouble("z_pos")), 1, 0.1, 0.1, 0.1, 0);
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SWEEP_ATTACK, (entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 0.5), (entity.getPersistentData().getDouble("z_pos")), 1, 0.1, 0.1, 0.1, 0);
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SWEEP_ATTACK, (entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 1), (entity.getPersistentData().getDouble("z_pos")), 1, 0.1, 0.1, 0.1, 0);
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SWEEP_ATTACK, (entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 1.5), (entity.getPersistentData().getDouble("z_pos")), 1, 0.1, 0.1, 0.1, 0);
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SWEEP_ATTACK, (entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 2), (entity.getPersistentData().getDouble("z_pos")), 1, 0.1, 0.1, 0.1, 0);
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SWEEP_ATTACK, (entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 2.5), (entity.getPersistentData().getDouble("z_pos")), 1, 0.1, 0.1, 0.1, 0);
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SWEEP_ATTACK, (entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 3), (entity.getPersistentData().getDouble("z_pos")), 1, 0.1, 0.1, 0.1, 0);
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SWEEP_ATTACK, (entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 3.5), (entity.getPersistentData().getDouble("z_pos")), 1, 0.1, 0.1, 0.1, 0);
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SWEEP_ATTACK, (entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 4), (entity.getPersistentData().getDouble("z_pos")), 1, 0.1, 0.1, 0.1, 0);
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SWEEP_ATTACK, (entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 4.5), (entity.getPersistentData().getDouble("z_pos")), 1, 0.1, 0.1, 0.1, 0);
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SWEEP_ATTACK, (entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 5), (entity.getPersistentData().getDouble("z_pos")), 1, 0.1, 0.1, 0.1, 0);
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SWEEP_ATTACK, (entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 5.5), (entity.getPersistentData().getDouble("z_pos")), 1, 0.1, 0.1, 0.1, 0);
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SWEEP_ATTACK, (entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 6), (entity.getPersistentData().getDouble("z_pos")), 1, 0.1, 0.1, 0.1, 0);
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SWEEP_ATTACK, (entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 6.5), (entity.getPersistentData().getDouble("z_pos")), 1, 0.1, 0.1, 0.1, 0);
		{
			final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos")), (entity.getPersistentData().getDouble("z_pos")));
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (!(entityiterator == entity)) {
					if (!(entityiterator instanceof OtiruyoEntity)) {
						if (!(entityiterator instanceof SkeltonMobEntity)) {
							if (entityiterator instanceof LivingEntity) {
								if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
									{
										Entity _ent = entityiterator;
										if (!_ent.level.isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/kill @s");
										}
									}
									{
										Entity _ent = entityiterator;
										if (!_ent.level.isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/deta merge entity @s (Health:0)");
										}
									}
								} else {
									entityiterator.hurt(DamageSource.GENERIC, 300);
								}
							}
						}
					}
				}
			}
		}
		{
			final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 1), (entity.getPersistentData().getDouble("z_pos")));
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (!(entityiterator == entity)) {
					if (!(entityiterator instanceof OtiruyoEntity)) {
						if (!(entityiterator instanceof SkeltonMobEntity)) {
							if (entityiterator instanceof LivingEntity) {
								if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
									{
										Entity _ent = entityiterator;
										if (!_ent.level.isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/kill @s");
										}
									}
									{
										Entity _ent = entityiterator;
										if (!_ent.level.isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/deta merge entity @s (Health:0)");
										}
									}
								} else {
									entityiterator.hurt(DamageSource.GENERIC, 300);
								}
							}
						}
					}
				}
			}
		}
		{
			final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 2), (entity.getPersistentData().getDouble("z_pos")));
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (!(entityiterator == entity)) {
					if (!(entityiterator instanceof OtiruyoEntity)) {
						if (!(entityiterator instanceof SkeltonMobEntity)) {
							if (entityiterator instanceof LivingEntity) {
								if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
									{
										Entity _ent = entityiterator;
										if (!_ent.level.isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/kill @s");
										}
									}
									{
										Entity _ent = entityiterator;
										if (!_ent.level.isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/deta merge entity @s (Health:0)");
										}
									}
								} else {
									entityiterator.hurt(DamageSource.GENERIC, 300);
								}
							}
						}
					}
				}
			}
		}
		{
			final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 3), (entity.getPersistentData().getDouble("z_pos")));
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (!(entityiterator == entity)) {
					if (!(entityiterator instanceof OtiruyoEntity)) {
						if (!(entityiterator instanceof SkeltonMobEntity)) {
							if (entityiterator instanceof LivingEntity) {
								if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
									{
										Entity _ent = entityiterator;
										if (!_ent.level.isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/kill @s");
										}
									}
									{
										Entity _ent = entityiterator;
										if (!_ent.level.isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/deta merge entity @s (Health:0)");
										}
									}
								} else {
									entityiterator.hurt(DamageSource.GENERIC, 300);
								}
							}
						}
					}
				}
			}
		}
		{
			final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 4), (entity.getPersistentData().getDouble("z_pos")));
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (!(entityiterator == entity)) {
					if (!(entityiterator instanceof OtiruyoEntity)) {
						if (!(entityiterator instanceof SkeltonMobEntity)) {
							if (entityiterator instanceof LivingEntity) {
								if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
									{
										Entity _ent = entityiterator;
										if (!_ent.level.isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/kill @s");
										}
									}
									{
										Entity _ent = entityiterator;
										if (!_ent.level.isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/deta merge entity @s (Health:0)");
										}
									}
								} else {
									entityiterator.hurt(DamageSource.GENERIC, 300);
								}
							}
						}
					}
				}
			}
		}
		{
			final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 5), (entity.getPersistentData().getDouble("z_pos")));
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (!(entityiterator == entity)) {
					if (!(entityiterator instanceof OtiruyoEntity)) {
						if (!(entityiterator instanceof SkeltonMobEntity)) {
							if (entityiterator instanceof LivingEntity) {
								if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
									{
										Entity _ent = entityiterator;
										if (!_ent.level.isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/kill @s");
										}
									}
									{
										Entity _ent = entityiterator;
										if (!_ent.level.isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/deta merge entity @s (Health:0)");
										}
									}
								} else {
									entityiterator.hurt(DamageSource.GENERIC, 300);
								}
							}
						}
					}
				}
			}
		}
		{
			final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("x_pos")), (entity.getPersistentData().getDouble("y_pos") + 6), (entity.getPersistentData().getDouble("z_pos")));
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (!(entityiterator == entity)) {
					if (!(entityiterator instanceof OtiruyoEntity)) {
						if (!(entityiterator instanceof SkeltonMobEntity)) {
							if (entityiterator instanceof LivingEntity) {
								if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
									{
										Entity _ent = entityiterator;
										if (!_ent.level.isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/kill @s");
										}
									}
									{
										Entity _ent = entityiterator;
										if (!_ent.level.isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/deta merge entity @s (Health:0)");
										}
									}
								} else {
									entityiterator.hurt(DamageSource.GENERIC, 300);
								}
							}
						}
					}
				}
			}
		}
	}
}
