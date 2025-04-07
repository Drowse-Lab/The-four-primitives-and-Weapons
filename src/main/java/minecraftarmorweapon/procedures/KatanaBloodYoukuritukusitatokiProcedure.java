package minecraftarmorweapon.procedures;

import net.minecraft.world.phys.Vec3;
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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import minecraftarmorweapon.network.MinecraftArmorWeaponModVariables;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;

import minecraftarmorweapon.entity.SkeltonMobEntity;
import minecraftarmorweapon.entity.OtiruyoEntity;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class KatanaBloodYoukuritukusitatokiProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double r = 0;
		double alpha = 0;
		double beta = 0;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.RIVERS_OF_BLOOD.get()) {
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 2) {
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.KAITEN.get(), 1, 1, true, false));
				if (!(entity instanceof Player _playerHasItem ? _playerHasItem.getInventory().contains(new ItemStack(MinecraftArmorWeaponModItems.BLOOD_BOTTLE.get())) : false)) {
					if (entity instanceof LivingEntity _entity)
						_entity.hurt(new DamageSource("\u00A74\u00A7ldevour blood").bypassArmor(), 5);
				} else if (entity instanceof Player _playerHasItem ? _playerHasItem.getInventory().contains(new ItemStack(MinecraftArmorWeaponModItems.BLOOD_BOTTLE.get())) : false) {
					if (entity instanceof Player _player) {
						ItemStack _stktoremove = new ItemStack(MinecraftArmorWeaponModItems.BLOOD_BOTTLE.get());
						_player.getInventory().clearOrCountMatchingItems(p -> _stktoremove.getItem() == p.getItem(), 1, _player.inventoryMenu.getCraftSlots());
					}
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
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.NAGIHARAI.get(), 2, 1, true, false));
				if (!(entity instanceof Player _playerHasItem ? _playerHasItem.getInventory().contains(new ItemStack(MinecraftArmorWeaponModItems.BLOOD_BOTTLE.get())) : false)) {
					if (entity instanceof LivingEntity _entity)
						_entity.hurt(new DamageSource("\u00A74\u00A7ldevour blood").bypassArmor(), 6);
				} else if (entity instanceof Player _playerHasItem ? _playerHasItem.getInventory().contains(new ItemStack(MinecraftArmorWeaponModItems.BLOOD_BOTTLE.get())) : false) {
					if (entity instanceof Player _player) {
						ItemStack _stktoremove = new ItemStack(MinecraftArmorWeaponModItems.BLOOD_BOTTLE.get());
						_player.getInventory().clearOrCountMatchingItems(p -> _stktoremove.getItem() == p.getItem(), 1, _player.inventoryMenu.getCraftSlots());
					}
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 4) {
				if (entity instanceof LivingEntity _entity)
					_entity.swing(InteractionHand.MAIN_HAND, true);
				if (!world.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(new Vec3(x, y, z), 50, 50, 50), e -> true).isEmpty()) {
					entity.getPersistentData().putBoolean("tp", true);
					entity.getPersistentData().putDouble("X", x);
					entity.getPersistentData().putDouble("Y", y);
					entity.getPersistentData().putDouble("Z", z);
					if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 255, true, false));
					MinecraftArmorWeaponMod.queueServerWork(60, () -> {
						if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
							_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.BLOOD_TP.get(), 60, 1, true, false));
					});
					{
						final Vec3 _center = new Vec3(x, y, z);
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(50 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
								.collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!(entityiterator == entity)) {
								if (!(entityiterator instanceof SkeltonMobEntity)) {
									if (!(entityiterator instanceof OtiruyoEntity)) {
										if (entityiterator instanceof LivingEntity) {
											if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
												_entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 150, 255, true, false));
											if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
												_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.EFFECT_BLOOD_TP.get(), 150, 1, true, false));
											if (!(entity instanceof Player _playerHasItem ? _playerHasItem.getInventory().contains(new ItemStack(MinecraftArmorWeaponModItems.BLOOD_BOTTLE.get())) : false)) {
												if (entity instanceof LivingEntity _entity)
													_entity.hurt(new DamageSource("\u00A74\u00A7ldevour blood").bypassArmor(), 7);
											} else if (entity instanceof Player _playerHasItem ? _playerHasItem.getInventory().contains(new ItemStack(MinecraftArmorWeaponModItems.BLOOD_BOTTLE.get())) : false) {
												if (entity instanceof Player _player) {
													ItemStack _stktoremove = new ItemStack(MinecraftArmorWeaponModItems.BLOOD_BOTTLE.get());
													_player.getInventory().clearOrCountMatchingItems(p -> _stktoremove.getItem() == p.getItem(), 1, _player.inventoryMenu.getCraftSlots());
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 5) {
				if (entity instanceof LivingEntity _entity)
					_entity.swing(InteractionHand.MAIN_HAND, true);
				SyugekinanozeeehuekutogaYouXiaoShinoteitukuProcedure.execute(world, x, y, z, entity);
				if (!(entity instanceof Player _playerHasItem ? _playerHasItem.getInventory().contains(new ItemStack(MinecraftArmorWeaponModItems.BLOOD_BOTTLE.get())) : false)) {
					if (entity instanceof LivingEntity _entity)
						_entity.hurt(new DamageSource("\u00A74\u00A7ldevour blood").bypassArmor(), 5);
				} else if (entity instanceof Player _playerHasItem ? _playerHasItem.getInventory().contains(new ItemStack(MinecraftArmorWeaponModItems.BLOOD_BOTTLE.get())) : false) {
					if (entity instanceof Player _player) {
						ItemStack _stktoremove = new ItemStack(MinecraftArmorWeaponModItems.BLOOD_BOTTLE.get());
						_player.getInventory().clearOrCountMatchingItems(p -> _stktoremove.getItem() == p.getItem(), 1, _player.inventoryMenu.getCraftSlots());
					}
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
										}
									} else {
										if (entityiterator instanceof LivingEntity) {
											entityiterator.hurt(DamageSource.GENERIC, 20);
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
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.ZOKUSEIZANNGEKI.get(), 30, 1, true, false));
				if (!(entity instanceof Player _playerHasItem ? _playerHasItem.getInventory().contains(new ItemStack(MinecraftArmorWeaponModItems.BLOOD_BOTTLE.get())) : false)) {
					if (entity instanceof LivingEntity _entity)
						_entity.hurt(new DamageSource("\u00A74\u00A7ldevour blood").bypassArmor(), 5);
				} else if (entity instanceof Player _playerHasItem ? _playerHasItem.getInventory().contains(new ItemStack(MinecraftArmorWeaponModItems.BLOOD_BOTTLE.get())) : false) {
					if (entity instanceof Player _player) {
						ItemStack _stktoremove = new ItemStack(MinecraftArmorWeaponModItems.BLOOD_BOTTLE.get());
						_player.getInventory().clearOrCountMatchingItems(p -> _stktoremove.getItem() == p.getItem(), 1, _player.inventoryMenu.getCraftSlots());
					}
				}
			}
		}
	}
}
