package minecraftarmorweapon.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
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

public class ZokuseizanngekiehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double random = 0;
		double loop = 0;
		double XRadius2 = 0;
		double ZRadius2 = 0;
		double X = 0;
		double Y = 0;
		double Z = 0;
		double loop1 = 0;
		double Y_pos = 0;
		double Y_pos1 = 0;
		double zknockback = 0;
		double yknockback = 0;
		double xknockback = 0;
		double dis = 0;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.RIVERS_OF_BLOOD.get()) {
			loop = entity.getPersistentData().getDouble("local");
			entity.getPersistentData().putDouble("Xpos", (entity.getPersistentData().getDouble("X") + Math.sin(Math.toRadians(entity.getPersistentData().getDouble("yaw") + 180)) * entity.getPersistentData().getDouble("distance")));
			entity.getPersistentData().putDouble("Zpos", (entity.getPersistentData().getDouble("Z") + Math.cos(Math.toRadians(entity.getPersistentData().getDouble("yaw"))) * entity.getPersistentData().getDouble("distance")));
			XRadius2 = 3;
			ZRadius2 = 3;
			random = entity.getPersistentData().getDouble("random");
			Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
			for (int index0 = 0; index0 < 75; index0++) {
				if (world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))).canOcclude()) {
					entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
					Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
				} else {
					break;
				}
			}
			for (int index1 = 0; index1 < 75; index1++) {
				if (world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))).canOcclude()) {
					entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
					Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
					break;
				}
				entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
				Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
			}
			world.levelEvent(2001, new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos")),
					Block.getId((world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))))));
			for (int index2 = 0; index2 < 36; index2++) {
				X = entity.getPersistentData().getDouble("X") + Math.sin(Math.toRadians(entity.getPersistentData().getDouble("yaw") + 180)) * entity.getPersistentData().getDouble("distance") + Math.cos(loop) * XRadius2;
				Y = Y_pos + 1;
				Z = entity.getPersistentData().getDouble("Z") + Math.cos(Math.toRadians(entity.getPersistentData().getDouble("yaw"))) * entity.getPersistentData().getDouble("distance") + Math.sin(loop) * ZRadius2;
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"particle item redstone ~ ~ ~ 0.1 0.1 0.1 0.1 0.1 100 force");
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"/particle dust 0.639 0.169 0.169 1 ~ ~0.5 ~ 0.3 0.1 0.3 0.1 10 force");
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.SWEEP_ATTACK, X, Y, Z, 3, 0.1, 0.1, 0.1, 0);
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.CLOUD, X, Y, Z, 3, 0.1, 0.1, 0.1, 0);
				{
					final Vec3 _center = new Vec3(X, Y, Z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
							.collect(Collectors.toList());
					for (Entity entityiterator : _entfound) {
						if (!(entityiterator == entity)) {
							if (!(entityiterator instanceof SkeltonMobEntity)) {
								if (!(entityiterator instanceof OtiruyoEntity)) {
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
											entity.getPersistentData().putBoolean("enchantmagickatanadamege", true);
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
															_ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/deta merge entity @s (Health:0)");
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
				loop = loop + Math.toRadians(5);
				Y_pos = Y_pos - 0.0555555555555556;
			}
			entity.getPersistentData().putDouble("distance", (entity.getPersistentData().getDouble("distance") + 0.8));
		} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS_KATANA.get()) {
			loop = entity.getPersistentData().getDouble("local");
			entity.getPersistentData().putDouble("Xpos", (entity.getPersistentData().getDouble("X") + Math.sin(Math.toRadians(entity.getPersistentData().getDouble("yaw") + 180)) * entity.getPersistentData().getDouble("distance")));
			entity.getPersistentData().putDouble("Zpos", (entity.getPersistentData().getDouble("Z") + Math.cos(Math.toRadians(entity.getPersistentData().getDouble("yaw"))) * entity.getPersistentData().getDouble("distance")));
			XRadius2 = 3;
			ZRadius2 = 3;
			random = entity.getPersistentData().getDouble("random");
			Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
			for (int index3 = 0; index3 < 75; index3++) {
				if (world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))).canOcclude()) {
					entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
					Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
				} else {
					break;
				}
			}
			for (int index4 = 0; index4 < 75; index4++) {
				if (world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))).canOcclude()) {
					entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
					Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
					break;
				}
				entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
				Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
			}
			world.levelEvent(2001, new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos")),
					Block.getId((world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))))));
			for (int index5 = 0; index5 < 36; index5++) {
				X = entity.getPersistentData().getDouble("X") + Math.sin(Math.toRadians(entity.getPersistentData().getDouble("yaw") + 180)) * entity.getPersistentData().getDouble("distance") + Math.cos(loop) * XRadius2;
				Y = Y_pos + 1;
				Z = entity.getPersistentData().getDouble("Z") + Math.cos(Math.toRadians(entity.getPersistentData().getDouble("yaw"))) * entity.getPersistentData().getDouble("distance") + Math.sin(loop) * ZRadius2;
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"/particle dust 0.835 0.380 1.000 1 ~ ~0.5 ~ 0.1 0.1 0.1 1 100 force");
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.SWEEP_ATTACK, X, Y, Z, 1, 0.1, 0.1, 0.1, 0);
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.CLOUD, X, Y, Z, 1, 0.1, 0.1, 0.1, 0.01);
				{
					final Vec3 _center = new Vec3(X, Y, Z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
							.collect(Collectors.toList());
					for (Entity entityiterator : _entfound) {
						if (!(entityiterator == entity)) {
							if (!(entityiterator instanceof SkeltonMobEntity)) {
								if (!(entityiterator instanceof OtiruyoEntity)) {
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
										if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS.get()) {
											if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
												_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.DARKNESS_ATTACK_EFFECT.get(), 60, 3, false, false));
										} else {
											if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
												_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.DARKNESS_ATTACK_EFFECT.get(), 60, 1, false, false));
										}
										if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
											entity.getPersistentData().putBoolean("enchantmagickatanadamege", true);
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
															_ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/deta merge entity @s (Health:0)");
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
				loop = loop + Math.toRadians(5);
				Y_pos = Y_pos - 0.0555555555555556;
			}
			entity.getPersistentData().putDouble("distance", (entity.getPersistentData().getDouble("distance") + 0.8));
		} else {
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()
					&& !((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.SMALL_SWORD.get())
					&& !((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get())) {
				loop = entity.getPersistentData().getDouble("local");
				entity.getPersistentData().putDouble("Xpos", (entity.getPersistentData().getDouble("X") + Math.sin(Math.toRadians(entity.getPersistentData().getDouble("yaw") + 180)) * entity.getPersistentData().getDouble("distance")));
				entity.getPersistentData().putDouble("Zpos", (entity.getPersistentData().getDouble("Z") + Math.cos(Math.toRadians(entity.getPersistentData().getDouble("yaw"))) * entity.getPersistentData().getDouble("distance")));
				XRadius2 = 3;
				ZRadius2 = 3;
				random = entity.getPersistentData().getDouble("random");
				Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
				for (int index6 = 0; index6 < 50; index6++) {
					if (world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))).canOcclude()) {
						entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
						Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
					} else {
						break;
					}
				}
				for (int index7 = 0; index7 < 50; index7++) {
					if (world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))).canOcclude()) {
						entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
						Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
						break;
					}
					entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
					Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
				}
				world.levelEvent(2001, new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos")),
						Block.getId((world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))))));
				for (int index8 = 0; index8 < 36; index8++) {
					X = entity.getPersistentData().getDouble("X") + Math.sin(Math.toRadians(entity.getPersistentData().getDouble("yaw") + 180)) * entity.getPersistentData().getDouble("distance") + Math.cos(loop) * XRadius2;
					Y = Y_pos + 1;
					Z = entity.getPersistentData().getDouble("Z") + Math.cos(Math.toRadians(entity.getPersistentData().getDouble("yaw"))) * entity.getPersistentData().getDouble("distance") + Math.sin(loop) * ZRadius2;
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
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()) {
													entityiterator.hurt(DamageSource.GENERIC, (float) (((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue() + 5));
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
				entity.getPersistentData().putDouble("distance", (entity.getPersistentData().getDouble("distance") + 0.8));
			} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get()
					|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.REPLICA_SWORD_OF_LIGHT.get()
					|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KATANA_NIGU_HUMERUS.get()) {
				loop = entity.getPersistentData().getDouble("local");
				entity.getPersistentData().putDouble("Xpos", (entity.getPersistentData().getDouble("X") + Math.sin(Math.toRadians(entity.getPersistentData().getDouble("yaw") + 180)) * entity.getPersistentData().getDouble("distance")));
				entity.getPersistentData().putDouble("Zpos", (entity.getPersistentData().getDouble("Z") + Math.cos(Math.toRadians(entity.getPersistentData().getDouble("yaw"))) * entity.getPersistentData().getDouble("distance")));
				XRadius2 = 3;
				ZRadius2 = 3;
				random = entity.getPersistentData().getDouble("random");
				Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
				for (int index9 = 0; index9 < 75; index9++) {
					if (world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))).canOcclude()) {
						entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
						Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
					} else {
						break;
					}
				}
				for (int index10 = 0; index10 < 75; index10++) {
					if (world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))).canOcclude()) {
						entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
						Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
						break;
					}
					entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
					Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
				}
				world.levelEvent(2001, new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos")),
						Block.getId((world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))))));
				for (int index11 = 0; index11 < 36; index11++) {
					X = entity.getPersistentData().getDouble("X") + Math.sin(Math.toRadians(entity.getPersistentData().getDouble("yaw") + 180)) * entity.getPersistentData().getDouble("distance") + Math.cos(loop) * XRadius2;
					Y = Y_pos + 1;
					Z = entity.getPersistentData().getDouble("Z") + Math.cos(Math.toRadians(entity.getPersistentData().getDouble("yaw"))) * entity.getPersistentData().getDouble("distance") + Math.sin(loop) * ZRadius2;
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
						if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get()
								&& (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS.get()) {
							if (world instanceof ServerLevel _level)
								_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
										"particle flame ~ ~ ~ 0.5 0.1 0.5 .0 20 force @p");
						}
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
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.FIREBALL.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BUBBLESHOT.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS.get()) {
													if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get()
															|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS.get()) {
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.DARKNESS_ATTACK_EFFECT.get(), 120, 6, true, false));
														entityiterator.hurt(DamageSource.GENERIC, (float) ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue());
													}
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
														entityiterator.hurt(DamageSource.GENERIC, (float) (((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue() + 3));
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
													}
													if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()) {
														entityiterator.hurt(DamageSource.GENERIC, (float) (((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue() + 5));
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.TISSOKU.get(), 120, 6, true, false));
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
													}
													if (entityiterator instanceof BlackholeEntity) {
														if (!entityiterator.level.isClientSide())
															entityiterator.discard();
													}
												} else {
													if (entityiterator instanceof BlackholeEntity) {
														if (!entityiterator.level.isClientSide())
															entityiterator.discard();
													}
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
				entity.getPersistentData().putDouble("distance", (entity.getPersistentData().getDouble("distance") + 0.8));
			} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.SMALL_SWORD.get()) {
				loop = entity.getPersistentData().getDouble("local");
				entity.getPersistentData().putDouble("Xpos", (entity.getPersistentData().getDouble("X") + Math.sin(Math.toRadians(entity.getPersistentData().getDouble("yaw") + 180)) * entity.getPersistentData().getDouble("distance")));
				entity.getPersistentData().putDouble("Zpos", (entity.getPersistentData().getDouble("Z") + Math.cos(Math.toRadians(entity.getPersistentData().getDouble("yaw"))) * entity.getPersistentData().getDouble("distance")));
				XRadius2 = 3;
				ZRadius2 = 3;
				random = entity.getPersistentData().getDouble("random");
				Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
				for (int index12 = 0; index12 < 30; index12++) {
					if (world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))).canOcclude()) {
						entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
						Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
					} else {
						break;
					}
				}
				for (int index13 = 0; index13 < 30; index13++) {
					if (world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))).canOcclude()) {
						entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
						Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
						break;
					}
					entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
					Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
				}
				world.levelEvent(2001, new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos")),
						Block.getId((world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))))));
				for (int index14 = 0; index14 < 36; index14++) {
					X = entity.getPersistentData().getDouble("X") + Math.sin(Math.toRadians(entity.getPersistentData().getDouble("yaw") + 180)) * entity.getPersistentData().getDouble("distance") + Math.cos(loop) * XRadius2;
					Y = Y_pos + 1;
					Z = entity.getPersistentData().getDouble("Z") + Math.cos(Math.toRadians(entity.getPersistentData().getDouble("yaw"))) * entity.getPersistentData().getDouble("distance") + Math.sin(loop) * ZRadius2;
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
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.SWEEP_ATTACK, X, Y, Z, 3, 0.1, 0.1, 0.1, 0);
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.CLOUD, X, Y, Z, 3, 0.1, 0.1, 0.1, 0);
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
														entityiterator.hurt(DamageSource.GENERIC, (float) (((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue() + 3));
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
													}
													if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()) {
														entityiterator.hurt(DamageSource.GENERIC, (float) (((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue() + 5));
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.TISSOKU.get(), 120, 6, true, false));
														if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
															_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
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
				entity.getPersistentData().putDouble("distance", (entity.getPersistentData().getDouble("distance") + 0.8));
			} else {
				loop = entity.getPersistentData().getDouble("local");
				entity.getPersistentData().putDouble("Xpos", (entity.getPersistentData().getDouble("X") + Math.sin(Math.toRadians(entity.getPersistentData().getDouble("yaw") + 180)) * entity.getPersistentData().getDouble("distance")));
				entity.getPersistentData().putDouble("Zpos", (entity.getPersistentData().getDouble("Z") + Math.cos(Math.toRadians(entity.getPersistentData().getDouble("yaw"))) * entity.getPersistentData().getDouble("distance")));
				XRadius2 = 3;
				ZRadius2 = 3;
				random = entity.getPersistentData().getDouble("random");
				Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
				for (int index15 = 0; index15 < 50; index15++) {
					if (world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))).canOcclude()) {
						entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
						Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
					} else {
						break;
					}
				}
				for (int index16 = 0; index16 < 50; index16++) {
					if (world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))).canOcclude()) {
						entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
						Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
						break;
					}
					entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
					Y_pos = entity.getPersistentData().getDouble("Ypos") + 1;
				}
				world.levelEvent(2001, new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos")),
						Block.getId((world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos"), entity.getPersistentData().getDouble("Zpos"))))));
				for (int index17 = 0; index17 < 36; index17++) {
					X = entity.getPersistentData().getDouble("X") + Math.sin(Math.toRadians(entity.getPersistentData().getDouble("yaw") + 180)) * entity.getPersistentData().getDouble("distance") + Math.cos(loop) * XRadius2;
					Y = Y_pos + 1;
					Z = entity.getPersistentData().getDouble("Z") + Math.cos(Math.toRadians(entity.getPersistentData().getDouble("yaw"))) * entity.getPersistentData().getDouble("distance") + Math.sin(loop) * ZRadius2;
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
									"particle firework ~ ~ ~ 0.5 0.1 .0 20 force @p");
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
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.FIREBALL.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BUBBLESHOT.get()
														|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()) {
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
														entityiterator.hurt(DamageSource.GENERIC, (float) (((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue() + 3));
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
				entity.getPersistentData().putDouble("distance", (entity.getPersistentData().getDouble("distance") + 0.8));
			}
		}
	}
}
