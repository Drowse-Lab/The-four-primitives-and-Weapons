package minecraftarmorweapon.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;

import net.minecraft.client.Minecraft;

import minecraftarmorweapon.entity.SkeltonMobEntity;
import minecraftarmorweapon.entity.OtiruyoEntity;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class BloodTpposiyonXiaoGuogaQieretaShiProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		{
			Entity _ent = entity;
			_ent.teleportTo((entity.getPersistentData().getDouble("X")), (entity.getPersistentData().getDouble("Y")), (entity.getPersistentData().getDouble("Z")));
			if (_ent instanceof ServerPlayer _serverPlayer)
				_serverPlayer.connection.teleport((entity.getPersistentData().getDouble("X")), (entity.getPersistentData().getDouble("Y")), (entity.getPersistentData().getDouble("Z")), _ent.getYRot(), _ent.getXRot());
		}
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(100 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (!(entityiterator == entity)) {
					if (!(entityiterator instanceof SkeltonMobEntity)) {
						if (!(entityiterator instanceof OtiruyoEntity)) {
							if (!(entityiterator instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.EFFECT_BLOOD_TP.get()) : false)) {
								if (!(entityiterator instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MobEffects.GLOWING) : false)) {
									if (entityiterator instanceof LivingEntity) {
										if (world instanceof ServerLevel _level)
											_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ())), Vec2.ZERO, _level, 4,
													"", Component.literal(""), _level.getServer(), null).withSuppressedOutput(), "particle item redstone ~ ~1 ~ 0.3 0.1 0.3 0.1 100 force");
										if (world instanceof ServerLevel _level)
											_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ())), Vec2.ZERO, _level, 4,
													"", Component.literal(""), _level.getServer(), null).withSuppressedOutput(), "particle dust 0.600 0.157 0.110 1 ~ ~ ~ 0.1 0.1 0.1 0 10");
										if (world instanceof ServerLevel _level)
											_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ())), Vec2.ZERO, _level, 4,
													"", Component.literal(""), _level.getServer(), null).withSuppressedOutput(), "particle dust 0.722 0.086 0.086 1 ~ ~ ~ 0.1 0.1 0.1 0 10");
										if (world instanceof ServerLevel _level)
											_level.sendParticles(ParticleTypes.SWEEP_ATTACK, (entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ()), 3, 0.1, 0.1, 0.1, 0.2);
									}
									if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
										if (entityiterator instanceof LivingEntity) {
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
											if (entity instanceof LivingEntity _entity)
												_entity.setHealth((float) ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) + 1));
										}
									} else {
										if (entityiterator instanceof LivingEntity) {
											if (entity instanceof LivingEntity _entity)
												_entity.setHealth((float) ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) + 1));
											entityiterator.hurt(DamageSource.GENERIC, (float) (((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue() + 5));
										}
									}
								}
							}
						}
					}
				}
			}
		}
		if (!(new Object() {
			public boolean checkGamemode(Entity _ent) {
				if (_ent instanceof ServerPlayer _serverPlayer) {
					return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
				} else if (_ent.level.isClientSide() && _ent instanceof Player _player) {
					return Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null && Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.CREATIVE;
				}
				return false;
			}
		}.checkGamemode(entity))) {
			if (entity instanceof Player _player) {
				_player.getAbilities().invulnerable = false;
				_player.onUpdateAbilities();
			}
		}
	}
}
