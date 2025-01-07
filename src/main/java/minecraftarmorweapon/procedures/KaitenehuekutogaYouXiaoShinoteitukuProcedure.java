package minecraftarmorweapon.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.client.Minecraft;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;

import minecraftarmorweapon.entity.SkeltonMobEntity;
import minecraftarmorweapon.entity.OtiruyoEntity;
import minecraftarmorweapon.entity.BlackholeEntity;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class KaitenehuekutogaYouXiaoShinoteitukuProcedure {
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
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.WIND_STEP_EFFECT.get()) : false)) {
			loop4 = Math.toRadians(entity.getYRot());
			xRadius4 = 1;
			zRadius4 = 1;
			for (int index0 = 0; index0 < 50; index0++) {
				loop4 = Math.toRadians(entity.getYRot());
				xRadius4 = xRadius4 + 0.1;
				zRadius4 = zRadius4 + 0.1;
				for (int index1 = 0; index1 < 72; index1++) {
					X4 = x + Math.cos(loop4) * xRadius4;
					Y4 = y + 1;
					Z4 = z + Math.sin(loop4) * zRadius4;
					if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.WIND_STEP_EFFECT.get()) : false) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X4, Y4, Z4), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									"/particle dust 0.129 0.780 0.000 1 ~ ~ ~ 0.1 0.1 0.1 1 25");
					}
					if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WITHER_KATANA.get()) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X4, Y4, Z4), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									"/particle dust 0.1 0.1 0.1 1 ~ ~ ~ 0.1 0.1 0.1 1 25");
					}
					if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS_KATANA.get()) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X4, Y4, Z4), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									"/particle dust 0.835 0.380 1.000 1 ~ ~ ~ 0.1 0.1 0.1 1 100 force");
					} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.RIVERS_OF_BLOOD.get()) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X4, Y4, Z4), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									"/particle dust 0.639 0.169 0.169 1 ~ ~ ~ 0.3 0.1 0.3 0.1 100 force");
					} else {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X4, Y4, Z4), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									"particle minecraft:dust 1 1 1 1 ~ ~ ~ 0.1 0.1 0.1 0 5");
					}
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X4, Y4, Z4), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"particle minecraft:dust 1 1 1 1 ~ ~ ~ 0.1 0.1 0.1 0 5");
					{
						final Vec3 _center = new Vec3(X4, Y4, Z4);
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
								.collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!(entityiterator == entity)) {
								if (!(entityiterator instanceof OtiruyoEntity)) {
									if (!(entityiterator instanceof SkeltonMobEntity)) {
										if (entityiterator instanceof LivingEntity) {
											if (!(world.getDifficulty() == Difficulty.PEACEFUL) && !(new Object() {
												public boolean checkGamemode(Entity _ent) {
													if (_ent instanceof ServerPlayer _serverPlayer) {
														return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
													} else if (_ent.level.isClientSide() && _ent instanceof Player _player) {
														return Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null
																&& Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.CREATIVE;
													}
													return false;
												}
											}.checkGamemode(entity)) && !(new Object() {
												public boolean checkGamemode(Entity _ent) {
													if (_ent instanceof ServerPlayer _serverPlayer) {
														return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
													} else if (_ent.level.isClientSide() && _ent instanceof Player _player) {
														return Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null
																&& Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.SPECTATOR;
													}
													return false;
												}
											}.checkGamemode(entity))) {
												if (entityiterator instanceof Mob _entity && entity instanceof LivingEntity _ent)
													_entity.setTarget(_ent);
											}
											if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
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
											} else {
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KATANA_NIGU_HUMERUS.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.MAGISCHES_FEEN_KATANA.get()) {
													if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.FIREBALL.get()
															|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()
															|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BUBBLESHOT.get()
															|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()
															|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()) {
														if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.FIREBALL.get()) {
															entityiterator.hurt(DamageSource.GENERIC, 5);
															entityiterator.setSecondsOnFire(30);
														}
														if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()) {
															entityiterator.hurt(DamageSource.GENERIC, 10);
														}
														if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BUBBLESHOT.get()) {
															entityiterator.hurt(DamageSource.GENERIC, 5);
															if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
																_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.TISSOKU.get(), 120, 6, true, false));
														}
														if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()) {
															entityiterator.hurt(DamageSource.GENERIC, 6);
															if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
																_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
														}
														if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get()
																|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KATANA_NIGU_HUMERUS.get()) {
															if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()) {
																entityiterator.hurt(DamageSource.GENERIC, 10);
																if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
																	_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
																if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
																	_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.TISSOKU.get(), 120, 6, true, false));
															}
														}
													}
												}
												if (((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKEN.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKENUTIGATANA.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKENSWORD.get())
														&& (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getMobType() == MobType.UNDEAD : false)) {
													entityiterator.hurt(DamageSource.GENERIC, 10);
													if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()) {
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 100, 2, true, false));
													}
												}
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WITHER_KATANA.get()) {
													if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
														_entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 2, false, false));
												}
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS_KATANA.get()) {
													if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS.get()) {
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.DARKNESS_ATTACK_EFFECT.get(), 100, 2, true, false));
													} else {
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.DARKNESS_ATTACK_EFFECT.get(), 100, 1, true, false));
													}
												}
												entityiterator.hurt(DamageSource.GENERIC, ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).getDamageValue());
											}
											dis = Math.abs(xknockback) + Math.abs(yknockback) + Math.abs(zknockback);
										}
									}
								}
							}
						}
					}
					loop4 = loop4 + Math.toRadians(5);
				}
			}
		} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.SCYTHE.get()) {
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()
					|| (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.WIND_STEP_EFFECT.get()) : false)) {
				loop4 = Math.toRadians(entity.getYRot());
				xRadius4 = 1;
				zRadius4 = 1;
				for (int index2 = 0; index2 < 70; index2++) {
					loop4 = Math.toRadians(entity.getYRot());
					xRadius4 = xRadius4 + 0.1;
					zRadius4 = zRadius4 + 0.1;
					for (int index3 = 0; index3 < 72; index3++) {
						X4 = x + Math.cos(loop4) * xRadius4;
						Y4 = y + 1;
						Z4 = z + Math.sin(loop4) * zRadius4;
						if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.WIND_STEP_EFFECT.get()) : false) {
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(
										new CommandSourceStack(CommandSource.NULL, new Vec3(X4, Y4, Z4), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"/particle dust 0.129 0.780 0.000 1 ~ ~ ~ 0.1 0.1 0.1 1 25");
						}
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X4, Y4, Z4), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									"particle minecraft:dust 1 1 1 1 ~ ~ ~ 0.1 0.1 0.1 0 5");
						{
							final Vec3 _center = new Vec3(X4, Y4, Z4);
							List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
									.collect(Collectors.toList());
							for (Entity entityiterator : _entfound) {
								if (!(entityiterator == entity)) {
									if (!(entityiterator instanceof OtiruyoEntity)) {
										if (!(entityiterator instanceof SkeltonMobEntity)) {
											if (entityiterator instanceof LivingEntity) {
												if (!(world.getDifficulty() == Difficulty.PEACEFUL) && !(new Object() {
													public boolean checkGamemode(Entity _ent) {
														if (_ent instanceof ServerPlayer _serverPlayer) {
															return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
														} else if (_ent.level.isClientSide() && _ent instanceof Player _player) {
															return Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null
																	&& Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.CREATIVE;
														}
														return false;
													}
												}.checkGamemode(entity)) && !(new Object() {
													public boolean checkGamemode(Entity _ent) {
														if (_ent instanceof ServerPlayer _serverPlayer) {
															return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
														} else if (_ent.level.isClientSide() && _ent instanceof Player _player) {
															return Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null
																	&& Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.SPECTATOR;
														}
														return false;
													}
												}.checkGamemode(entity))) {
													if (entityiterator instanceof Mob _entity && entity instanceof LivingEntity _ent)
														_entity.setTarget(_ent);
												}
												if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
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
												} else {
													entityiterator.hurt(DamageSource.GENERIC, ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).getDamageValue());
												}
												dis = Math.abs(xknockback) + Math.abs(yknockback) + Math.abs(zknockback);
											}
										}
									}
								}
							}
						}
						loop4 = loop4 + Math.toRadians(5);
					}
				}
			} else {
				loop4 = Math.toRadians(entity.getYRot());
				xRadius4 = 1;
				zRadius4 = 1;
				for (int index4 = 0; index4 < 40; index4++) {
					loop4 = Math.toRadians(entity.getYRot());
					xRadius4 = xRadius4 + 0.1;
					zRadius4 = zRadius4 + 0.1;
					for (int index5 = 0; index5 < 72; index5++) {
						X4 = x + Math.cos(loop4) * xRadius4;
						Y4 = y + 1;
						Z4 = z + Math.sin(loop4) * zRadius4;
						if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.WIND_STEP_EFFECT.get()) : false) {
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(
										new CommandSourceStack(CommandSource.NULL, new Vec3(X4, Y4, Z4), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"/particle dust 0.129 0.780 0.000 1 ~ ~ ~ 0.1 0.1 0.1 1 25");
						}
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X4, Y4, Z4), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									"particle minecraft:dust 1 1 1 1 ~ ~ ~ 0.1 0.1 0.1 0 5");
						{
							final Vec3 _center = new Vec3(X4, Y4, Z4);
							List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
									.collect(Collectors.toList());
							for (Entity entityiterator : _entfound) {
								if (!(entityiterator == entity)) {
									if (!(entityiterator instanceof OtiruyoEntity)) {
										if (!(entityiterator instanceof SkeltonMobEntity)) {
											if (entityiterator instanceof LivingEntity) {
												if (!(world.getDifficulty() == Difficulty.PEACEFUL) && !(new Object() {
													public boolean checkGamemode(Entity _ent) {
														if (_ent instanceof ServerPlayer _serverPlayer) {
															return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
														} else if (_ent.level.isClientSide() && _ent instanceof Player _player) {
															return Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null
																	&& Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.CREATIVE;
														}
														return false;
													}
												}.checkGamemode(entity)) && !(new Object() {
													public boolean checkGamemode(Entity _ent) {
														if (_ent instanceof ServerPlayer _serverPlayer) {
															return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
														} else if (_ent.level.isClientSide() && _ent instanceof Player _player) {
															return Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null
																	&& Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.SPECTATOR;
														}
														return false;
													}
												}.checkGamemode(entity))) {
													if (entityiterator instanceof Mob _entity && entity instanceof LivingEntity _ent)
														_entity.setTarget(_ent);
												}
												if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
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
												} else {
													if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS_KATANA.get()) {
														if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS.get()) {
															if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
																_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.DARKNESS_ATTACK_EFFECT.get(), 100, 2, true, false));
														} else {
															if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
																_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.DARKNESS_ATTACK_EFFECT.get(), 100, 1, true, false));
														}
													}
													if (((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKEN.get()
															|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKENUTIGATANA.get()
															|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKENSWORD.get())
															&& (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getMobType() == MobType.UNDEAD : false)) {
														entityiterator.hurt(DamageSource.GENERIC, 10);
														if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()) {
															if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
																_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 100, 2, true, false));
														}
													}
													if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get()
															|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KATANA_NIGU_HUMERUS.get()
															|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.MAGISCHES_FEEN_KATANA.get()) {
														if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.FIREBALL.get()
																|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()
																|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BUBBLESHOT.get()
																|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()
																|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()) {
															if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.FIREBALL.get()) {
																entityiterator.hurt(DamageSource.GENERIC, 5);
																entityiterator.setSecondsOnFire(30);
															}
															if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()) {
																entityiterator.hurt(DamageSource.GENERIC, 10);
															}
															if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BUBBLESHOT.get()) {
																entityiterator.hurt(DamageSource.GENERIC, 5);
																if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
																	_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.TISSOKU.get(), 120, 6, true, false));
															}
															if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()) {
																entityiterator.hurt(DamageSource.GENERIC, 6);
																if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
																	_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
															}
															if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get()
																	|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KATANA_NIGU_HUMERUS.get()) {
																if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()) {
																	entityiterator.hurt(DamageSource.GENERIC, 10);
																	if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
																		_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
																	if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
																		_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.TISSOKU.get(), 120, 6, true, false));
																}
															}
														}
													}
													entityiterator.hurt(DamageSource.GENERIC, ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).getDamageValue());
												}
												dis = Math.abs(xknockback) + Math.abs(yknockback) + Math.abs(zknockback);
											}
										}
									}
								}
							}
						}
						loop4 = loop4 + Math.toRadians(5);
					}
				}
			}
		} else {
			loop4 = Math.toRadians(entity.getYRot());
			xRadius4 = 1;
			zRadius4 = 1;
			for (int index6 = 0; index6 < 40; index6++) {
				loop4 = Math.toRadians(entity.getYRot());
				xRadius4 = xRadius4 + 0.1;
				zRadius4 = zRadius4 + 0.1;
				for (int index7 = 0; index7 < 72; index7++) {
					X4 = x + Math.cos(loop4) * xRadius4;
					Y4 = y + 1;
					Z4 = z + Math.sin(loop4) * zRadius4;
					if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.WIND_STEP_EFFECT.get()) : false) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X4, Y4, Z4), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									"/particle dust 0.129 0.780 0.000 1 ~ ~ ~ 0.1 0.1 0.1 1 25");
					}
					if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WITHER_KATANA.get()) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X4, Y4, Z4), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									"/particle dust 0.1 0.1 0.1 1 ~ ~ ~ 0.1 0.1 0.1 1 25");
					}
					if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS.get()
							&& ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS_KATANA.get()
									|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get())) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X4, Y4, Z4), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									"/particle dust 0.835 0.380 1.000 1 ~ ~ ~ 0.1 0.1 0.1 1 100 force");
					} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.RIVERS_OF_BLOOD.get()) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X4, Y4, Z4), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									"/particle dust 0.639 0.169 0.169 1 ~ ~ ~ 0.3 0.1 0.3 0.1 100 force");
					} else {
						if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WITHER_KATANA.get()) {
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(
										new CommandSourceStack(CommandSource.NULL, new Vec3(X4, Y4, Z4), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"/particle dust 0.1 0.1 0.1 1 ~ ~ ~ 0.1 0.1 0.1 0 5");
						}
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X4, Y4, Z4), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
									"particle minecraft:dust 1 1 1 1 ~ ~ ~ 0.1 0.1 0.1 0 5");
					}
					{
						final Vec3 _center = new Vec3(X4, Y4, Z4);
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
								.collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!(entityiterator == entity)) {
								if (!(entityiterator instanceof OtiruyoEntity)) {
									if (!(entityiterator instanceof SkeltonMobEntity)) {
										if (entityiterator instanceof LivingEntity) {
											if (!(world.getDifficulty() == Difficulty.PEACEFUL) && !(new Object() {
												public boolean checkGamemode(Entity _ent) {
													if (_ent instanceof ServerPlayer _serverPlayer) {
														return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
													} else if (_ent.level.isClientSide() && _ent instanceof Player _player) {
														return Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null
																&& Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.CREATIVE;
													}
													return false;
												}
											}.checkGamemode(entity)) && !(new Object() {
												public boolean checkGamemode(Entity _ent) {
													if (_ent instanceof ServerPlayer _serverPlayer) {
														return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
													} else if (_ent.level.isClientSide() && _ent instanceof Player _player) {
														return Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null
																&& Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.SPECTATOR;
													}
													return false;
												}
											}.checkGamemode(entity))) {
												if (entityiterator instanceof Mob _entity && entity instanceof LivingEntity _ent)
													_entity.setTarget(_ent);
											}
											if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get()) {
													if (entityiterator instanceof BlackholeEntity) {
														if (!entityiterator.level.isClientSide())
															entityiterator.discard();
													}
												}
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
											} else {
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS_KATANA.get()) {
													if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS.get()) {
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.DARKNESS_ATTACK_EFFECT.get(), 100, 2, true, false));
													} else {
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.DARKNESS_ATTACK_EFFECT.get(), 100, 1, true, false));
													}
												}
												if (((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKEN.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKENUTIGATANA.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKENSWORD.get())
														&& (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getMobType() == MobType.UNDEAD : false)) {
													entityiterator.hurt(DamageSource.GENERIC, 10);
													if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()) {
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 100, 2, true, false));
													}
												}
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KATANA_NIGU_HUMERUS.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.MAGISCHES_FEEN_KATANA.get()) {
													if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.FIREBALL.get()
															|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()
															|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BUBBLESHOT.get()
															|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()
															|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()
															|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS.get()) {
														if (((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get()
																|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS_KATANA.get())
																&& (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS.get()) {
															if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
																_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.DARKNESS_ATTACK_EFFECT.get(), 100, 2, true, false));
														} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS_KATANA.get()) {
															if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
																_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.DARKNESS_ATTACK_EFFECT.get(), 100, 1, true, false));
														}
														if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.FIREBALL.get()) {
															entityiterator.hurt(DamageSource.GENERIC, ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).getDamageValue());
															entityiterator.setSecondsOnFire(30);
														}
														if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()) {
															entityiterator.hurt(DamageSource.GENERIC, (float) (((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).getDamageValue() + 4));
														}
														if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BUBBLESHOT.get()) {
															entityiterator.hurt(DamageSource.GENERIC, ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).getDamageValue());
															if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
																_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.TISSOKU.get(), 120, 6, true, false));
														}
														if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()) {
															entityiterator.hurt(DamageSource.GENERIC, (float) (((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).getDamageValue() + 2));
															if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
																_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
														}
														if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get()
																|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KATANA_NIGU_HUMERUS.get()) {
															if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()) {
																entityiterator.hurt(DamageSource.GENERIC, (float) (((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).getDamageValue() + 5));
																if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
																	_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
																if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
																	_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.TISSOKU.get(), 120, 6, true, false));
															}
														}
														if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get()) {
															if (entityiterator instanceof BlackholeEntity) {
																if (!entityiterator.level.isClientSide())
																	entityiterator.discard();
															}
														}
													}
												}
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WITHER_KATANA.get()) {
													if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
														_entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 1, false, false));
												}
												entityiterator.hurt(DamageSource.GENERIC, ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).getDamageValue());
											}
											dis = Math.abs(xknockback) + Math.abs(yknockback) + Math.abs(zknockback);
										}
									}
								}
							}
						}
					}
					loop4 = loop4 + Math.toRadians(5);
				}
			}
		}
	}
}
