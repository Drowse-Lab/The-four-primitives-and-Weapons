package minecraftarmorweapon.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.client.Minecraft;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;

import minecraftarmorweapon.entity.SkeltonMobEntity;
import minecraftarmorweapon.entity.OtiruyoEntity;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class SyugekiOnEffectActiveTickProcedure {
	public static void execute(LevelAccessor world, double y, Entity entity) {
		if (entity == null)
			return;
		double Zknockback = 0;
		double Yknockback = 0;
		double Xknockback = 0;
		double dis = 0;
		double dis1 = 0;
		double r = 0;
		double alpha = 0;
		double beta = 0;
		double random = 0;
		double loop = 0;
		double XRadius2 = 0;
		double ZRadius2 = 0;
		double X = 0;
		double Y = 0;
		double Z = 0;
		double Y_pos = 0;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WARABITETOU.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.NINJATOU.get()) {
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WARABITETOU.get()
					|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.NINJATOU.get()) {
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(X, Y, Z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"particle minecraft:item iron_ingot ~ ~1 ~ 0.5 0.5 0.5 .0 20 force @p");
			}
			loop = Math.toRadians(entity.getYRot());
			XRadius2 = 3;
			ZRadius2 = 3;
			Y_pos = y + 1;
			for (int index0 = 0; index0 < 72; index0++) {
				X = entity.getX() + Math.cos(loop) * XRadius2;
				Y = Y_pos;
				Z = entity.getZ() + Math.sin(loop) * ZRadius2;
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
															_ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/data merge entity @s (Health:0)");
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
		} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.SCYTHE.get()) {
			loop = Math.toRadians(entity.getYRot());
			XRadius2 = 3;
			ZRadius2 = 3;
			Y_pos = y + 1;
			for (int index1 = 0; index1 < 36; index1++) {
				X = entity.getX() + Math.cos(loop) * XRadius2;
				Y = Y_pos;
				Z = entity.getZ() + Math.sin(loop) * ZRadius2;
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
															_ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/data merge entity @s (Health:0)");
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
		} else {
			loop = Math.toRadians(entity.getYRot());
			XRadius2 = 3;
			ZRadius2 = 3;
			Y_pos = y + 1;
			for (int index2 = 0; index2 < 36; index2++) {
				X = entity.getX() + Math.cos(loop) * XRadius2;
				Y = Y_pos;
				Z = entity.getZ() + Math.sin(loop) * ZRadius2;
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.SWEEP_ATTACK, X, Y, Z, 3, 0.1, 0.1, 0.1, 0);
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.CLOUD, X, Y, Z, 3, 0.1, 0.1, 0.1, 0);
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WITHER_KATANA.get()) {
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.SMOKE, X, Y, Z, 10, 0.5, 0, 0.5, 0);
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
										if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WITHER_KATANA.get()) {
											if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
												_entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 2));
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
															_ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/data merge entity @s (Health:0)");
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
		}
	}
}
