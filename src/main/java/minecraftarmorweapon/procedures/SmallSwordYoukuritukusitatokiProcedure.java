package minecraftarmorweapon.procedures;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import minecraftarmorweapon.network.MinecraftArmorWeaponModVariables;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;

import minecraftarmorweapon.entity.SkeltonMobEntity;
import minecraftarmorweapon.entity.OtiruyoEntity;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class SmallSwordYoukuritukusitatokiProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double r = 0;
		double alpha = 0;
		double beta = 0;
		double loop = 0;
		double XRadius2 = 0;
		double ZRadius2 = 0;
		double X = 0;
		double Y = 0;
		double Z = 0;
		double Y_pos = 0;
		double XRadius3 = 0;
		double ZRadius3 = 0;
		double Z1 = 0;
		double loop2 = 0;
		double Y1 = 0;
		double X1 = 0;
		double Y_pos3 = 0;
		double zknockback = 0;
		double yknockback = 0;
		double xknockback = 0;
		double dis = 0;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.SMALL_SWORD.get()) {
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 2) {
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.KAITEN.get(), 1, 1, true, false));
				if (!(entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.KURUTIMENASI.get()) : false)) {
					if (entity instanceof Player _player)
						_player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem(), 100);
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 3) {
				if (entity instanceof LivingEntity _entity)
					_entity.swing(InteractionHand.MAIN_HAND, true);
				entity.getPersistentData().putDouble("X", x);
				entity.getPersistentData().putDouble("Z", z);
				entity.getPersistentData().putDouble("Ypos", y);
				entity.getPersistentData().putDouble("yaw", (entity.getYRot()));
				entity.getPersistentData().putDouble("distance", 3);
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.NAGIHARAI_2.get(), 2, 1, true, false));
				if (!(entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.KURUTIMENASI.get()) : false)) {
					if (entity instanceof Player _player)
						_player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem(), 50);
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 4) {
				if (entity instanceof LivingEntity _entity)
					_entity.swing(InteractionHand.MAIN_HAND, true);
				r = 1;
				alpha = entity.getYRot();
				beta = entity.getXRot();
				for (int index0 = 0; index0 < 20; index0++) {
					if (world
							.getBlockState(new BlockPos(x - r * Math.cos(Math.toRadians(beta)) * Math.sin(Math.toRadians(alpha)), (y + 1) - r * Math.sin(Math.toRadians(beta)), z + r * Math.cos(Math.toRadians(beta)) * Math.cos(Math.toRadians(alpha))))
							.canOcclude()) {
						break;
					}
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.CRIT, (x - r * Math.cos(Math.toRadians(beta)) * Math.sin(Math.toRadians(alpha))), ((y + 1) - r * Math.sin(Math.toRadians(beta))),
								(z + r * Math.cos(Math.toRadians(beta)) * Math.cos(Math.toRadians(alpha))), 2, 0.1, 0.1, 0.1, 0.1);
					if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.WIND_STEP_EFFECT.get()) : false) {
						{
							final Vec3 _center = new Vec3((x - r * Math.cos(Math.toRadians(beta)) * Math.sin(Math.toRadians(alpha))), ((y + 1) - r * Math.sin(Math.toRadians(beta))),
									(z + r * Math.cos(Math.toRadians(beta)) * Math.cos(Math.toRadians(alpha))));
							List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
									.collect(Collectors.toList());
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
																	VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent),
																	"/deta merge entity @s (Health:0)");
														}
													}
												} else {
													entityiterator.hurt(DamageSource.GENERIC, (float) ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue());
												}
											}
										}
									}
								}
							}
						}
					} else {
						{
							final Vec3 _center = new Vec3((x - r * Math.cos(Math.toRadians(beta)) * Math.sin(Math.toRadians(alpha))), ((y + 1) - r * Math.sin(Math.toRadians(beta))),
									(z + r * Math.cos(Math.toRadians(beta)) * Math.cos(Math.toRadians(alpha))));
							List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
									.collect(Collectors.toList());
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
																	VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent),
																	"/deta merge entity @s (Health:0)");
														}
													}
												} else {
													entityiterator.hurt(DamageSource.GENERIC, (float) ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue());
												}
											}
										}
									}
								}
							}
						}
					}
					r = r + 0.2;
				}
				if (!(entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.KURUTIMENASI.get()) : false)) {
					if (entity instanceof Player _player)
						_player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem(), 30);
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 5) {
				if (entity instanceof LivingEntity _entity)
					_entity.swing(InteractionHand.MAIN_HAND, true);
				SyugekinanozeeehuekutogaYouXiaoShinoteitukuProcedure.execute(world, x, y, z, entity);
				if (!(entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.KURUTIMENASI.get()) : false)) {
					if (entity instanceof Player _player)
						_player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem(), 50);
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 6) {
				if (entity instanceof LivingEntity _entity)
					_entity.swing(InteractionHand.MAIN_HAND, true);
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(5 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
							.collect(Collectors.toList());
					for (Entity entityiterator : _entfound) {
						if (!(entityiterator == entity)) {
							if (!(entityiterator instanceof SkeltonMobEntity)) {
								if (!(entityiterator instanceof OtiruyoEntity)) {
									if (entityiterator instanceof LivingEntity) {
										if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
											entity.getPersistentData().putBoolean("enchantmagickatanadamege", true);
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
											if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.FIREBALL.get()
													|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()
													|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BUBBLESHOT.get()
													|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()
													|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()) {
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.FIREBALL.get()) {
													entityiterator.hurt(DamageSource.GENERIC, (float) ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue());
													entityiterator.setSecondsOnFire(30);
												}
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()) {
													if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
														_entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 1, true, false));
													entityiterator.hurt(DamageSource.GENERIC, (float) (((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue() + 5));
												}
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()) {
													entityiterator.hurt(DamageSource.GENERIC, (float) (((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue() + 2));
													if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
														_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 120, 1, true, false));
												}
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BUBBLESHOT.get()) {
													entityiterator.hurt(DamageSource.GENERIC, (float) ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue());
													if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
														_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.TISSOKU.get(), 120, 1, true, false));
												}
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()) {
													if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
														_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 120, 1, true, false));
													entityiterator.hurt(DamageSource.GENERIC, (float) (((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue() + 5));
													if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
														_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.TISSOKU.get(), 120, 1, true, false));
												}
											} else {
												entityiterator.hurt(DamageSource.GENERIC, (float) ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue());
											}
										}
									}
								}
							}
						}
					}
				}
				entity.getPersistentData().putDouble("X", x);
				entity.getPersistentData().putDouble("Z", z);
				entity.getPersistentData().putDouble("Ypos", y);
				entity.getPersistentData().putDouble("yaw", (entity.getYRot()));
				entity.getPersistentData().putDouble("distance", 3);
				loop = Math.toRadians(entity.getYRot());
				XRadius2 = 2;
				ZRadius2 = 2;
				Y_pos = y + 1;
				for (int index1 = 0; index1 < 36; index1++) {
					X = x + Math.cos(loop) * XRadius2;
					Y = Y_pos;
					Z = z + Math.sin(loop) * ZRadius2;
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.SWEEP_ATTACK, X, Y, Z, 3, 0.1, 0.1, 0.1, 0);
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.CLOUD, X, Y, Z, 3, 0.1, 0.1, 0.1, 0);
					if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.DEMONIZED.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									"particle item redstone ~ ~0.5 ~ 0.3 0.1 0.3 0.1 100 force");
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									"/particle dust 0.639 0.169 0.169 1 ~ ~0.5 ~ 0.3 0.1 0.3 0.1 10 force");
					} else {
						if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.FIREBALL.get()) {
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"particle flame ~ ~ ~ 0.5 0.1 0.5 .0 20 force @p");
						}
						if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BUBBLESHOT.get()) {
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"particle block lapis_block ~ ~ ~ 0.5 0.1 0.5 .0 20 force @p");
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"particle bubble ~ ~ ~ 0.5 0.1 0.5 .0 50 force @p");
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"particle dolphin ~ ~ ~ 0.5 0.1 0.5 .0 50 force @p");
						}
						if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()) {
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"particle firework ~ ~ ~ 0.5 0.1 0.5 .0 20 force @p");
						}
						if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()) {
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"particle falling_dust ~ ~ ~ 0.5 0.1 0.5 .0 20 force @p");
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"particle sneeze ~ ~ ~ 0.5 0.1 0.5 .0 20 force @p");
						}
						if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()) {
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"particle block lapis_block ~ ~ ~ 0.5 0.1 0.5 .0 10 force @p");
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"particle bubble ~ ~ ~ 0.5 0.1 0.5 .0 25 force @p");
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"particle dolphin ~ ~ ~ 0.5 0.1 0.5 .0 25 force @p");
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"particle falling_dust ~ ~ ~ 0.5 0.1 0.5 .0 10 force @p");
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"particle sneeze ~ ~ ~ 0.5 0.1 0.5 .0 10 force @p");
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"particle firework ~ ~ ~ 0.5 0.1 0.5 .0 10 force @p");
						}
					}
					{
						final Vec3 _center = new Vec3(X, Y, Z);
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
								.collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!(entityiterator == entity)) {
								if (!(entityiterator instanceof SkeltonMobEntity)) {
									if (!(entityiterator instanceof OtiruyoEntity)) {
										if (entityiterator instanceof LivingEntity) {
											if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
												entity.getPersistentData().putBoolean("enchantmagickatanadamege", true);
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
																VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent),
																"/deta merge entity @s (Health:0)");
													}
												}
											} else {
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.FIREBALL.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BUBBLESHOT.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()) {
													if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.FIREBALL.get()) {
														entityiterator.hurt(DamageSource.GENERIC, (float) ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue());
														entityiterator.setSecondsOnFire(30);
													}
													if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()) {
														entityiterator.hurt(DamageSource.GENERIC, (float) (((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue() + 5));
													}
													if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BUBBLESHOT.get()) {
														entityiterator.hurt(DamageSource.GENERIC, (float) ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue());
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.TISSOKU.get(), 120, 6, true, false));
													}
													if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()) {
														entityiterator.hurt(DamageSource.GENERIC, (float) (((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue() + 2));
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
													}
													if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()) {
														entityiterator.hurt(DamageSource.GENERIC, (float) (((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue() + 5));
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.TISSOKU.get(), 120, 6, true, false));
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
													}
												} else {
													entityiterator.hurt(DamageSource.GENERIC, (float) ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue());
												}
											}
										}
									}
								}
							}
						}
					}
					loop = loop + Math.toRadians(5);
					Y_pos = Y_pos - 0.0555555555555556;
				}
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.ZOKUSEIZANNGEKI.get(), 30, 1, true, false));
				if (entity instanceof Player _player)
					_player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem(), 50);
			}
		}
	}
}
