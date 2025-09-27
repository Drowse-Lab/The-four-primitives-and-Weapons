package minecraftarmorweapon.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;

import minecraftarmorweapon.entity.SkeltonMobEntity;
import minecraftarmorweapon.entity.OtiruyoEntity;
import minecraftarmorweapon.entity.KillotiruEntity;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class SwordOfNightShotProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (entity.getPersistentData().getDouble("sword_of_night_shot_number_of_remaining_ammunition_score_2") > 0) {
			entity.getPersistentData().putDouble("minecraft_armor_weapon:r", 1);
			entity.getPersistentData().putDouble("minecraft_armor_weapon:alpha", (entity.getYRot()));
			entity.getPersistentData().putDouble("minecraft_armor_weapon:beta", (entity.getXRot()));
			entity.getPersistentData().putDouble("minecraft_armor_weapon:caunt", 100);
			for (int index0 = 0; index0 < (int) entity.getPersistentData().getDouble("minecraft_armor_weapon:caunt"); index0++) {
				if (!(!world.getEntitiesOfClass(OtiruyoEntity.class,
						AABB.ofSize(new Vec3(
								(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
										* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
								((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
								(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
										* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
								1, 1, 1),
						e -> true).isEmpty())
						&& !(!world.getEntitiesOfClass(KillotiruEntity.class,
								AABB.ofSize(new Vec3(
										(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
												* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
										((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
										(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
												* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
										1, 1, 1),
								e -> true).isEmpty())
						&& !(!world.getEntitiesOfClass(SkeltonMobEntity.class,
								AABB.ofSize(new Vec3(
										(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
												* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
										((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
										(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
												* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
										1, 1, 1),
								e -> true).isEmpty())
						&& !world.getEntitiesOfClass(Mob.class,
								AABB.ofSize(new Vec3(
										(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
												* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
										((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
										(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
												* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
										0.1, 0.1, 0.1),
								e -> true).isEmpty()) {
					{
						final Vec3 _center = new Vec3(
								(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
										* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
								((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
								(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
										* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))));
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(0.1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
								.collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!(entityiterator == entity)) {
								if (!(entityiterator instanceof SkeltonMobEntity)) {
									if (!(entityiterator instanceof OtiruyoEntity)) {
										if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
											if (entityiterator instanceof Mob) {
												// Killエンチャントの処理（82.4%の確率）
												if (Math.random() < 0.824) {
													{
														Entity _ent = entityiterator;
														if (!_ent.level.isClientSide() && _ent.getServer() != null) {
															_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
																	_ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/kill @s");
														}
													}
													if (entityiterator instanceof LivingEntity _livingEntity) {
														_livingEntity.setHealth(0f);
													}
												} else {
													// 通常ダメージ
													entityiterator.hurt(DamageSource.GENERIC, 8);
												}
											}
										} else {
											if (entityiterator instanceof Mob) {
												entityiterator.hurt(DamageSource.GENERIC, 5);
											}
										}
									}
								}
							}
						}
					}
				}
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(
							new CommandSourceStack(CommandSource.NULL,
									new Vec3(
											(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
													* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
											((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
											(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
													* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
									Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"particle minecraft:dust 0.7 0.17 1 0.75 ~ ~ ~ 0.1 0.1 0.1 0 1 force");
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(
							new CommandSourceStack(CommandSource.NULL,
									new Vec3(
											(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
													* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
											((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
											(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
													* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
									Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"particle minecraft:crit ~ ~ ~ 0 0 0 0.02 1 force");
				if (!(!(!world.getEntitiesOfClass(OtiruyoEntity.class,
						AABB.ofSize(new Vec3(
								(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
										* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
								((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
								(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
										* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
								1, 1, 1),
						e -> true).isEmpty())
						&& !(!world.getEntitiesOfClass(KillotiruEntity.class,
								AABB.ofSize(new Vec3(
										(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
												* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
										((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
										(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
												* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
										1, 1, 1),
								e -> true).isEmpty())
						&& !(!world.getEntitiesOfClass(SkeltonMobEntity.class,
								AABB.ofSize(new Vec3(
										(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
												* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
										((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
										(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
												* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
										1, 1, 1),
								e -> true).isEmpty())
						&& !world.getEntitiesOfClass(Mob.class,
								AABB.ofSize(new Vec3(
										(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
												* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
										((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
										(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
												* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
										0.1, 0.1, 0.1),
								e -> true).isEmpty())) {
					entity.getPersistentData().putDouble("minecraft_armor_weapon:r", (entity.getPersistentData().getDouble("minecraft_armor_weapon:r") + 0.2));
				} else {
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands()
								.performPrefixedCommand(new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
												((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
												(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(), "particle minecraft:crit ~ ~ ~ 0 0 0 0.3 15 force");
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands()
								.performPrefixedCommand(new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
												((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
												(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(), "particle minecraft:dust 0.7 0.17 1 1 ~ ~ ~ 0.25 0.25 0.25 1 15 force");
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands()
								.performPrefixedCommand(new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
												((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
												(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(), "playsound minecraft:entity.firework_rocket.blast neutral @a ~ ~ ~ 2 1.2");
					{
						final Vec3 _center = new Vec3(
								(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
										* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
								((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
								(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
										* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))));
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(2 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
								.collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!(entityiterator == entity)) {
								if (!(entityiterator instanceof SkeltonMobEntity)) {
									if (!(entityiterator instanceof OtiruyoEntity)) {
										if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
											if (entityiterator instanceof Mob) {
												{
													Entity _ent = entityiterator;
													if (!_ent.level.isClientSide() && _ent.getServer() != null) {
														_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
																_ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/kill @s");
													}
												}
												{
													Entity _ent = entityiterator;
													if (!_ent.level.isClientSide() && _ent.getServer() != null) {
														_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
																_ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent),
																"/deta merge entity @s (Health:0)");
													}
												}
											}
										} else {
											if (entityiterator instanceof Mob) {
												entityiterator.hurt(DamageSource.GENERIC, 7);
											}
										}
									}
								}
							}
						}
					}
					break;
				}
				if (world.getBlockState(new BlockPos(
						x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
								* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))),
						(y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta"))),
						z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
								* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))))
						.canOcclude()) {
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands()
								.performPrefixedCommand(new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
												((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
												(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(), "particle minecraft:crit ~ ~ ~ 0 0 0 0.3 15 force");
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands()
								.performPrefixedCommand(new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
												((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
												(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(), "particle minecraft:dust 0.7 0.17 1 1 ~ ~ ~ 0.25 0.25 0.25 1 15 force");
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands()
								.performPrefixedCommand(new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
												((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
												(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(), "playsound minecraft:entity.firework_rocket.blast neutral @a ~ ~ ~ 2 1.2");
					{
						final Vec3 _center = new Vec3(
								(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
										* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
								((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
								(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
										* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))));
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(2 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
								.collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!(entityiterator == entity)) {
								if (!(entityiterator instanceof SkeltonMobEntity)) {
									if (!(entityiterator instanceof OtiruyoEntity)) {
										if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
											if (entityiterator instanceof LivingEntity) {
												{
													Entity _ent = entityiterator;
													if (!_ent.level.isClientSide() && _ent.getServer() != null) {
														_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
																_ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/kill @s");
													}
												}
												if (entity instanceof LivingEntity _entity)
													_entity.setHealth(0);
											}
										} else {
											if (entityiterator instanceof LivingEntity) {
												entityiterator.hurt(DamageSource.GENERIC, (float) ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue());
											}
										}
									}
								}
							}
						}
					}
					break;
				}
				if (entity.getPersistentData().getDouble("minecraft_armor_weapon:r") >= entity.getPersistentData().getDouble("minecraft_armor_weapon:caunt") * 0.2) {
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands()
								.performPrefixedCommand(new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
												((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
												(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(), "particle minecraft:dust 0.7 0.17 1 1 ~ ~ ~ 0.25 0.25 0.25 1 15 force");
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands()
								.performPrefixedCommand(new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
												((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
												(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(), "particle minecraft:crit ~ ~ ~ 0 0 0 0.3 15 force");
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands()
								.performPrefixedCommand(new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
												((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
												(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
														* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha"))))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(), "playsound minecraft:entity.firework_rocket.blast neutral @a ~ ~ ~ 2 1.2");
					{
						final Vec3 _center = new Vec3(
								(x - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
										* Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))),
								((y + 1) - entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.sin(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))),
								(z + entity.getPersistentData().getDouble("minecraft_armor_weapon:r") * Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:beta")))
										* Math.cos(Math.toRadians(entity.getPersistentData().getDouble("minecraft_armor_weapon:alpha")))));
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(2 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
								.collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!(entityiterator == entity)) {
								if (!(entityiterator instanceof SkeltonMobEntity)) {
									if (!(entityiterator instanceof OtiruyoEntity)) {
										if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
											if (entityiterator instanceof LivingEntity) {
												{
													Entity _ent = entityiterator;
													if (!_ent.level.isClientSide() && _ent.getServer() != null) {
														_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
																_ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/kill @s");
													}
												}
												if (entity instanceof LivingEntity _entity)
													_entity.setHealth(0);
											}
										} else {
											if (entityiterator instanceof LivingEntity) {
												entityiterator.hurt(DamageSource.GENERIC, (float) ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue());
											}
										}
									}
								}
							}
						}
					}
					break;
				}
			}
			if (entity.getPersistentData().getDouble("sword_of_night_shot_number_of_remaining_ammunition_score_2") == 1) {
				{
					Entity _ent = entity;
					if (!_ent.level.isClientSide() && _ent.getServer() != null) {
						_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
								_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "playsound minecraft:ui.button.click player @a ~ ~ ~ 1 1.5");
					}
				}
				{
					Entity _ent = entity;
					if (!_ent.level.isClientSide() && _ent.getServer() != null) {
						_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
								_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "playsound minecraft:block.piston.contract player @a ~ ~ ~ 1 1.5");
					}
				}
			}
			{
				Entity _ent = entity;
				if (!_ent.level.isClientSide() && _ent.getServer() != null) {
					_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
							_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "playsound minecraft:entity.ghast.shoot player @a ~ ~ ~ 1 2");
				}
			}
			{
				Entity _ent = entity;
				if (!_ent.level.isClientSide() && _ent.getServer() != null) {
					_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
							_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "playsound minecraft:entity.arrow.shoot player @a ~ ~ ~ 1 1.2");
				}
			}
			{
				Entity _ent = entity;
				if (!_ent.level.isClientSide() && _ent.getServer() != null) {
					_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
							_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "playsound minecraft:entity.player.attack.sweep player @a ~ ~ ~ 1 1.2");
				}
			}
			{
				Entity _ent = entity;
				if (!_ent.level.isClientSide() && _ent.getServer() != null) {
					_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
							_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "playsound minecraft:entity.experience_orb.pickup player @a ~ ~ ~ 1 2");
				}
			}
		} else {
			{
				Entity _ent = entity;
				if (!_ent.level.isClientSide() && _ent.getServer() != null) {
					_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
							_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "playsound minecraft:block.iron_trapdoor.close player @a ~ ~ ~ 1 1.5");
				}
			}
			{
				Entity _ent = entity;
				if (!_ent.level.isClientSide() && _ent.getServer() != null) {
					_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
							_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "playsound minecraft:block.iron_trapdoor.open player @a ~ ~ ~ 1 1.5");
				}
			}
		}
		if (!(entity.getPersistentData().getBoolean("sword_of_night_shot_number_of_remaining_ammunition_reload") == true)) {
			entity.getPersistentData().putDouble("sword_of_night_shot_number_of_remaining_ammunition_score_2", (entity.getPersistentData().getDouble("sword_of_night_shot_number_of_remaining_ammunition_score_2") - 1));
		}
		if (!(entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.KURUTIMENASI.get()) : false)) {
			if (entity.getPersistentData().getDouble("sword_of_night_shot_number_of_remaining_ammunition_score_2") == 3) {
				{
					Entity _ent = entity;
					if (!_ent.level.isClientSide() && _ent.getServer() != null) {
						_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
								_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "title @s actionbar {\"text\":\"| 3 |\",\"color\":\"green\"}");
					}
				}
			}
			if (entity.getPersistentData().getDouble("sword_of_night_shot_number_of_remaining_ammunition_score_2") == 2) {
				{
					Entity _ent = entity;
					if (!_ent.level.isClientSide() && _ent.getServer() != null) {
						_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
								_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "title @s actionbar {\"text\":\"| 2 |\",\"color\":\"yellow\"}");
					}
				}
			}
			if (entity.getPersistentData().getDouble("sword_of_night_shot_number_of_remaining_ammunition_score_2") == 1) {
				{
					Entity _ent = entity;
					if (!_ent.level.isClientSide() && _ent.getServer() != null) {
						_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
								_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "title @s actionbar {\"text\":\"| 1 |\",\"color\":\"red\"}");
					}
				}
			}
			if (entity.getPersistentData().getDouble("sword_of_night_shot_number_of_remaining_ammunition_score_2") <= 0) {
				if (!(entity.getPersistentData().getBoolean("sword_of_night_shot_number_of_remaining_ammunition_reload") == true)) {
					{
						Entity _ent = entity;
						if (!_ent.level.isClientSide() && _ent.getServer() != null) {
							_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
									_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "title @s actionbar {\"text\":\"| Reloading... |\",\"color\":\"gray\"}");
						}
					}
					entity.getPersistentData().putBoolean("sword_of_night_shot_number_of_remaining_ammunition_reload", true);
					{
						Entity _ent = entity;
						if (!_ent.level.isClientSide() && _ent.getServer() != null) {
							_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
									_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "playsound minecraft:block.piston.extend player @a ~ ~ ~ 1 1.5");
						}
					}
					MinecraftArmorWeaponMod.queueServerWork(10, () -> {
						{
							Entity _ent = entity;
							if (!_ent.level.isClientSide() && _ent.getServer() != null) {
								_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
										_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "title @s actionbar {\"text\":\"| Reloading.. |\",\"color\":\"gray\"}");
							}
						}
					});
					MinecraftArmorWeaponMod.queueServerWork(20, () -> {
						{
							Entity _ent = entity;
							if (!_ent.level.isClientSide() && _ent.getServer() != null) {
								_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
										_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "title @s actionbar {\"text\":\"| Reloading. |\",\"color\":\"gray\"}");
							}
						}
					});
					MinecraftArmorWeaponMod.queueServerWork(30, () -> {
						{
							Entity _ent = entity;
							if (!_ent.level.isClientSide() && _ent.getServer() != null) {
								_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
										_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "title @s actionbar {\"text\":\"| Reloading |\",\"color\":\"gray\"}");
							}
						}
					});
					MinecraftArmorWeaponMod.queueServerWork(40, () -> {
						entity.getPersistentData().putDouble("sword_of_night_shot_number_of_remaining_ammunition_score_2", 4);
						entity.getPersistentData().putBoolean("sword_of_night_shot_number_of_remaining_ammunition_reload", false);
						{
							Entity _ent = entity;
							if (!_ent.level.isClientSide() && _ent.getServer() != null) {
								_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
										_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "title @s actionbar {\"text\":\"| 4 |\",\"color\":\"aqua\"}");
							}
						}
					});
				}
			}
		} else {
			if (entity.getPersistentData().getDouble("sword_of_night_shot_number_of_remaining_ammunition_score_2") == 3) {
				{
					Entity _ent = entity;
					if (!_ent.level.isClientSide() && _ent.getServer() != null) {
						_ent.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4, _ent.getName().getString(), _ent.getDisplayName(),
										_ent.level.getServer(), _ent),
								"title @s actionbar [\"\",{\"text\":\"|\",\"color\":\"dark_green\"},{\"text\":\" I\",\"color\":\"dark_aqua\"},{\"text\":\"n\",\"color\":\"dark_red\"},{\"text\":\"f\",\"color\":\"dark_purple\"},{\"text\":\"i\",\"color\":\"gold\"},{\"text\":\"n\",\"color\":\"blue\"},{\"text\":\"i\",\"color\":\"green\"},{\"text\":\"t\",\"color\":\"aqua\"},{\"text\":\"y\",\"color\":\"red\"},{\"text\":\" |\",\"color\":\"light_purple\"}]");
					}
				}
			}
			if (entity.getPersistentData().getDouble("sword_of_night_shot_number_of_remaining_ammunition_score_2") == 2) {
				{
					Entity _ent = entity;
					if (!_ent.level.isClientSide() && _ent.getServer() != null) {
						_ent.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4, _ent.getName().getString(), _ent.getDisplayName(),
										_ent.level.getServer(), _ent),
								"title @s actionbar [\"\",{\"text\":\"|\",\"color\":\"dark_aqua\"},{\"text\":\" I\",\"color\":\"dark_red\"},{\"text\":\"n\",\"color\":\"dark_purple\"},{\"text\":\"f\",\"color\":\"gold\"},{\"text\":\"i\",\"color\":\"blue\"},{\"text\":\"n\",\"color\":\"green\"},{\"text\":\"i\",\"color\":\"aqua\"},{\"text\":\"t\",\"color\":\"red\"},{\"text\":\"y\",\"color\":\"light_purple\"},{\"text\":\" |\",\"color\":\"yellow\"}]");
					}
				}
			}
			if (entity.getPersistentData().getDouble("sword_of_night_shot_number_of_remaining_ammunition_score_2") == 1) {
				{
					Entity _ent = entity;
					if (!_ent.level.isClientSide() && _ent.getServer() != null) {
						_ent.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4, _ent.getName().getString(), _ent.getDisplayName(),
										_ent.level.getServer(), _ent),
								"title @s actionbar [\"\",{\"text\":\"|\",\"color\":\"dark_red\"},{\"text\":\" I\",\"color\":\"dark_purple\"},{\"text\":\"n\",\"color\":\"gold\"},{\"text\":\"f\",\"color\":\"blue\"},{\"text\":\"i\",\"color\":\"green\"},{\"text\":\"n\",\"color\":\"aqua\"},{\"text\":\"i\",\"color\":\"red\"},{\"text\":\"t\",\"color\":\"light_purple\"},{\"text\":\"y\",\"color\":\"yellow\"},{\"text\":\" |\",\"color\":\"dark_blue\"}]");
					}
				}
			}
			if (entity.getPersistentData().getDouble("sword_of_night_shot_number_of_remaining_ammunition_score_2") <= 0) {
				entity.getPersistentData().putDouble("sword_of_night_shot_number_of_remaining_ammunition_score_2", 4);
				entity.getPersistentData().putBoolean("sword_of_night_shot_number_of_remaining_ammunition_reload", false);
				{
					Entity _ent = entity;
					if (!_ent.level.isClientSide() && _ent.getServer() != null) {
						_ent.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4, _ent.getName().getString(), _ent.getDisplayName(),
										_ent.level.getServer(), _ent),
								"title @s actionbar [\"\",{\"text\":\"| \",\"color\":\"dark_blue\"},{\"text\":\"I\",\"color\":\"dark_green\"},{\"text\":\"n\",\"color\":\"dark_aqua\"},{\"text\":\"f\",\"color\":\"dark_red\"},{\"text\":\"i\",\"color\":\"dark_purple\"},{\"text\":\"n\",\"color\":\"gold\"},{\"text\":\"i\",\"color\":\"blue\"},{\"text\":\"t\",\"color\":\"green\"},{\"text\":\"y \",\"color\":\"aqua\"},{\"text\":\"|\",\"color\":\"red\"}]");
					}
				}
			}
		}
	}
}
