package the_four_primitives_and_weapons.procedures;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class SliceGuardOnEffectActiveTickProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double dis4 = 0;
		double dis3 = 0;
		double dis2 = 0;
		double dis1 = 0;
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (entityiterator instanceof Arrow || entityiterator instanceof SpectralArrow || entityiterator instanceof ThrownTrident || entityiterator instanceof LargeFireball || entityiterator instanceof DragonFireball
						|| entityiterator instanceof Snowball || entityiterator instanceof ThrownEgg || entityiterator instanceof WitherSkull || entityiterator instanceof ShulkerBullet || entityiterator instanceof LlamaSpit
						|| entityiterator instanceof LlamaSpit || entityiterator instanceof ThrownEnderpearl) {
					if (entityiterator.getPersistentData().getBoolean("Check") == false) {
						entityiterator.getPersistentData().putBoolean("Check", true);
						dis1 = Math.sqrt(Math.pow(entityiterator.getX() - entity.getX(), 0.4) + Math.pow(entityiterator.getY() - entity.getY(), 0.4) + Math.pow(entityiterator.getZ() - entity.getZ(), 0.4));
						if (entityiterator.getPersistentData().getBoolean("Check2") == false) {
							entityiterator.getPersistentData().putBoolean("Check2", true);
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.KITTERUKITTERU.get(), 2, 1, true, false));
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "kill");
								}
							}
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/data merge entity @s (Health:0)");
								}
							}
						} else {
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.KITTERUKITTERU.get(), 2, 1, true, false));
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "kill");
								}
							}
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/data merge entity @s (Health:0)");
								}
							}
						}
					}
				}
			}
		}
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(2 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (entityiterator instanceof Arrow || entityiterator instanceof SpectralArrow || entityiterator instanceof ThrownTrident || entityiterator instanceof LargeFireball || entityiterator instanceof DragonFireball
						|| entityiterator instanceof Snowball || entityiterator instanceof ThrownEgg || entityiterator instanceof WitherSkull || entityiterator instanceof ShulkerBullet || entityiterator instanceof LlamaSpit
						|| entityiterator instanceof LlamaSpit || entityiterator instanceof ThrownEnderpearl) {
					if (entityiterator.getPersistentData().getBoolean("Check") == false) {
						entityiterator.getPersistentData().putBoolean("Check", true);
						dis2 = Math.sqrt(Math.pow(entityiterator.getX() - entity.getX(), 0.8) + Math.pow(entityiterator.getY() - entity.getY(), 0.8) + Math.pow(entityiterator.getZ() - entity.getZ(), 0.8));
						if (entityiterator.getPersistentData().getBoolean("Check2") == false) {
							entityiterator.getPersistentData().putBoolean("Check2", true);
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.KITTERUKITTERU.get(), 2, 1, true, false));
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "kill");
								}
							}
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/deta merge entity @s (Health:0)");
								}
							}
						} else {
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.KITTERUKITTERU.get(), 2, 1, true, false));
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "kill");
								}
							}
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/deta merge entity @s (Health:0)");
								}
							}
						}
					}
				}
			}
		}
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (entityiterator instanceof Arrow || entityiterator instanceof SpectralArrow || entityiterator instanceof ThrownTrident || entityiterator instanceof LargeFireball || entityiterator instanceof DragonFireball
						|| entityiterator instanceof Snowball || entityiterator instanceof ThrownEgg || entityiterator instanceof WitherSkull || entityiterator instanceof ShulkerBullet || entityiterator instanceof LlamaSpit
						|| entityiterator instanceof LlamaSpit || entityiterator instanceof ThrownEnderpearl) {
					if (entityiterator.getPersistentData().getBoolean("Check") == false) {
						entityiterator.getPersistentData().putBoolean("Check", true);
						dis3 = Math.sqrt(Math.pow(entityiterator.getX() - entity.getX(), 1.2) + Math.pow(entityiterator.getY() - entity.getY(), 1.2) + Math.pow(entityiterator.getZ() - entity.getZ(), 1.2));
						if (entityiterator.getPersistentData().getBoolean("Check2") == false) {
							entityiterator.getPersistentData().putBoolean("Check2", true);
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.KITTERUKITTERU.get(), 2, 1, true, false));
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "kill");
								}
							}
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/deta merge entity @s (Health:0)");
								}
							}
						} else {
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.KITTERUKITTERU.get(), 2, 1, true, false));
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "kill");
								}
							}
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/deta merge entity @s (Health:0)");
								}
							}
						}
					}
				}
			}
		}
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (entityiterator instanceof Arrow || entityiterator instanceof SpectralArrow || entityiterator instanceof ThrownTrident || entityiterator instanceof LargeFireball || entityiterator instanceof DragonFireball
						|| entityiterator instanceof Snowball || entityiterator instanceof ThrownEgg || entityiterator instanceof WitherSkull || entityiterator instanceof ShulkerBullet || entityiterator instanceof LlamaSpit
						|| entityiterator instanceof LlamaSpit || entityiterator instanceof ThrownEnderpearl) {
					if (entityiterator.getPersistentData().getBoolean("Check") == false) {
						entityiterator.getPersistentData().putBoolean("Check", true);
						dis4 = Math.sqrt(Math.pow(entityiterator.getX() - entity.getX(), 1.6) + Math.pow(entityiterator.getY() - entity.getY(), 1.6) + Math.pow(entityiterator.getZ() - entity.getZ(), 1.6));
						if (entityiterator.getPersistentData().getBoolean("Check2") == false) {
							entityiterator.getPersistentData().putBoolean("Check2", true);
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.KITTERUKITTERU.get(), 2, 1, true, false));
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "kill");
								}
							}
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/deta merge entity @s (Health:0)");
								}
							}
						} else {
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.KITTERUKITTERU.get(), 2, 1, true, false));
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "kill");
								}
							}
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/deta merge entity @s (Health:0)");
								}
							}
						}
					}
				}
			}
		}
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(5 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (entityiterator instanceof Arrow || entityiterator instanceof SpectralArrow || entityiterator instanceof ThrownTrident || entityiterator instanceof LargeFireball || entityiterator instanceof DragonFireball
						|| entityiterator instanceof Snowball || entityiterator instanceof ThrownEgg || entityiterator instanceof WitherSkull || entityiterator instanceof ShulkerBullet || entityiterator instanceof LlamaSpit
						|| entityiterator instanceof LlamaSpit || entityiterator instanceof ThrownEnderpearl) {
					if (entityiterator.getPersistentData().getBoolean("Check") == false) {
						entityiterator.getPersistentData().putBoolean("Check", true);
						dis4 = Math.sqrt(Math.pow(entityiterator.getX() - entity.getX(), 1.6) + Math.pow(entityiterator.getY() - entity.getY(), 1.6) + Math.pow(entityiterator.getZ() - entity.getZ(), 1.6));
						if (entityiterator.getPersistentData().getBoolean("Check2") == false) {
							entityiterator.getPersistentData().putBoolean("Check2", true);
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.KITTERUKITTERU.get(), 2, 1, true, false));
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "kill");
								}
							}
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/deta merge entity @s (Health:0)");
								}
							}
						} else {
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.KITTERUKITTERU.get(), 2, 1, true, false));
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "kill");
								}
							}
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/deta merge entity @s (Health:0)");
								}
							}
						}
					}
				}
			}
		}
		if (entity instanceof LivingEntity _entity)
			_entity.removeEffect(MobEffects.WITHER);
		if (entity instanceof LivingEntity _entity)
			_entity.removeEffect(MobEffects.BLINDNESS);
		if (entity instanceof LivingEntity _entity)
			_entity.removeEffect(MobEffects.HUNGER);
		if (entity instanceof LivingEntity _entity)
			_entity.removeEffect(MobEffects.LEVITATION);
		if (entity instanceof LivingEntity _entity)
			_entity.removeEffect(MobEffects.DIG_SLOWDOWN);
		if (entity instanceof LivingEntity _entity)
			_entity.removeEffect(MobEffects.CONFUSION);
		if (entity instanceof LivingEntity _entity)
			_entity.removeEffect(MobEffects.POISON);
		if (entity instanceof LivingEntity _entity)
			_entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
		if (entity instanceof LivingEntity _entity)
			_entity.removeEffect(MobEffects.HARM);
		if (entity instanceof LivingEntity _entity)
			_entity.removeEffect(MobEffects.WEAKNESS);
	}
}
