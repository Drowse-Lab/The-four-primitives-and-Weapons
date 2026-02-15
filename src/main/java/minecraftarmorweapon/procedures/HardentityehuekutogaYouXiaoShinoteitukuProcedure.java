package minecraftarmorweapon.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.client.Minecraft;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

import java.util.Comparator;

public class HardentityehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		// Additional safety checks
		if (!entity.isAlive() || entity.isRemoved()) {
			return;
		}
		if (entity instanceof WitherSkeleton || entity instanceof Stray || entity instanceof Skeleton) {
			if (!((entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DAS_HERZ_EINER_FEE_ARMOR_HELMET.get())) {
				{
					Entity _entity = entity;
					if (_entity instanceof Player _player) {
						_player.getInventory().armor.set(3, new ItemStack(MinecraftArmorWeaponModItems.DAS_HERZ_EINER_FEE_ARMOR_HELMET.get()));
						_player.getInventory().setChanged();
					} else if (_entity instanceof LivingEntity _living) {
						_living.setItemSlot(EquipmentSlot.HEAD, new ItemStack(MinecraftArmorWeaponModItems.DAS_HERZ_EINER_FEE_ARMOR_HELMET.get()));
					}
				}
				try {
					((entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY)).enchant(Enchantments.ALL_DAMAGE_PROTECTION,
							entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()) ? _livEnt.getEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()).getAmplifier() : 0);
				} catch (Exception e) {
					// Handle enchantment exceptions
				}
			}
			if (!((entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DAS_HERZ_EINER_FEE_ARMOR_CHESTPLATE.get())) {
				{
					Entity _entity = entity;
					if (_entity instanceof Player _player) {
						_player.getInventory().armor.set(2, new ItemStack(MinecraftArmorWeaponModItems.DAS_HERZ_EINER_FEE_ARMOR_CHESTPLATE.get()));
						_player.getInventory().setChanged();
					} else if (_entity instanceof LivingEntity _living) {
						_living.setItemSlot(EquipmentSlot.CHEST, new ItemStack(MinecraftArmorWeaponModItems.DAS_HERZ_EINER_FEE_ARMOR_CHESTPLATE.get()));
					}
				}
				try {
					((entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY)).enchant(Enchantments.ALL_DAMAGE_PROTECTION,
							entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()) ? _livEnt.getEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()).getAmplifier() : 0);
				} catch (Exception e) {
					// Handle enchantment exceptions
				}
			}
			if (!((entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.LEGS) : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DAS_HERZ_EINER_FEE_ARMOR_LEGGINGS.get())) {
				{
					Entity _entity = entity;
					if (_entity instanceof Player _player) {
						_player.getInventory().armor.set(1, new ItemStack(MinecraftArmorWeaponModItems.DAS_HERZ_EINER_FEE_ARMOR_LEGGINGS.get()));
						_player.getInventory().setChanged();
					} else if (_entity instanceof LivingEntity _living) {
						_living.setItemSlot(EquipmentSlot.LEGS, new ItemStack(MinecraftArmorWeaponModItems.DAS_HERZ_EINER_FEE_ARMOR_LEGGINGS.get()));
					}
				}
				try {
					((entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.LEGS) : ItemStack.EMPTY)).enchant(Enchantments.ALL_DAMAGE_PROTECTION,
							entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()) ? _livEnt.getEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()).getAmplifier() : 0);
				} catch (Exception e) {
					// Handle enchantment exceptions
				}
			}
			if (!((entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.FEET) : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DAS_HERZ_EINER_FEE_ARMOR_BOOTS.get())) {
				{
					Entity _entity = entity;
					if (_entity instanceof Player _player) {
						_player.getInventory().armor.set(0, new ItemStack(MinecraftArmorWeaponModItems.DAS_HERZ_EINER_FEE_ARMOR_BOOTS.get()));
						_player.getInventory().setChanged();
					} else if (_entity instanceof LivingEntity _living) {
						_living.setItemSlot(EquipmentSlot.FEET, new ItemStack(MinecraftArmorWeaponModItems.DAS_HERZ_EINER_FEE_ARMOR_BOOTS.get()));
					}
				}
				try {
					((entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.FEET) : ItemStack.EMPTY)).enchant(Enchantments.ALL_DAMAGE_PROTECTION,
							entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()) ? _livEnt.getEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()).getAmplifier() : 0);
				} catch (Exception e) {
					// Handle enchantment exceptions
				}
			}
			// Find nearest player with proper null checks
			Player nearestPlayer = null;
			try {
				nearestPlayer = world.getEntitiesOfClass(Player.class, AABB.ofSize(new Vec3(x, y, z), 64, 64, 64), e -> true).stream()
					.sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(x, y, z)))
					.findFirst().orElse(null);
			} catch (Exception e) {
				// Handle any exceptions during player search
			}
			
			boolean isCreativeOrSpectator = false;
			if (nearestPlayer != null) {
				if (nearestPlayer instanceof ServerPlayer _serverPlayer) {
					GameType gameType = _serverPlayer.gameMode.getGameModeForPlayer();
					isCreativeOrSpectator = (gameType == GameType.CREATIVE || gameType == GameType.SPECTATOR);
				} else if (nearestPlayer.level.isClientSide()) {
					try {
						var connection = Minecraft.getInstance().getConnection();
						if (connection != null) {
							var playerInfo = connection.getPlayerInfo(nearestPlayer.getGameProfile().getId());
							if (playerInfo != null) {
								GameType gameType = playerInfo.getGameMode();
								isCreativeOrSpectator = (gameType == GameType.CREATIVE || gameType == GameType.SPECTATOR);
							}
						}
					} catch (Exception e) {
						// Handle any exceptions during client-side check
					}
				}
			}
			
			if (!isCreativeOrSpectator) {
				if (nearestPlayer != null && entity instanceof Mob _mobEntity) {
					try {
						_mobEntity.setTarget(nearestPlayer);
					} catch (Exception e) {
						// Handle any exceptions during target setting
					}
					if (!world.getEntitiesOfClass(Player.class, AABB.ofSize(new Vec3(x, y, z), 10, 10, 10), e -> true).isEmpty()) {
					if (!((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WITHER_KATANA.get())) {
						if (entity instanceof LivingEntity _livingEntity1) {
							ItemStack _setstack = new ItemStack(MinecraftArmorWeaponModItems.WITHER_KATANA.get());
							_setstack.setCount(1);
							_livingEntity1.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
							if (_livingEntity1 instanceof Player _player)
									_player.getInventory().setChanged();
							}
							((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).enchant(Enchantments.SHARPNESS,
									entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()) ? _livEnt.getEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()).getAmplifier() : 0);
						}
					} else {
						if (!((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == Items.BOW)) {
							if (entity instanceof LivingEntity _livingEntity) {
								ItemStack _setstack = new ItemStack(Items.BOW);
								_setstack.setCount(1);
								_livingEntity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
								if (_livingEntity instanceof Player _player)
									_player.getInventory().setChanged();
							}
							(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().putBoolean("Unbreakable", true);
							if (entity instanceof WitherSkeleton) {
								((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).enchant(Enchantments.FLAMING_ARROWS,
										entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()) ? _livEnt.getEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()).getAmplifier() : 0);
							}
							((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).enchant(Enchantments.POWER_ARROWS,
									entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()) ? _livEnt.getEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()).getAmplifier() : 0);
						}
					}
				}
			}
			// Handle Snow Golem targeting with null checks
			SnowGolem nearestSnowGolem = null;
			try {
				nearestSnowGolem = world.getEntitiesOfClass(SnowGolem.class, AABB.ofSize(new Vec3(x, y, z), 64, 64, 64), e -> true).stream()
					.sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(x, y, z)))
					.findFirst().orElse(null);
			} catch (Exception e) {
				// Handle any exceptions during snow golem search
			}
			
			if (nearestSnowGolem != null) {
				if (entity instanceof Mob _mobEntity2) {
					try {
						_mobEntity2.setTarget(nearestSnowGolem);
					} catch (Exception e) {
						// Handle any exceptions during target setting
					}
				}
				if (!world.getEntitiesOfClass(SnowGolem.class, AABB.ofSize(new Vec3(x, y, z), 10, 10, 10), e -> true).isEmpty()) {
					if (!((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WITHER_KATANA.get())) {
						if (entity instanceof LivingEntity _livingEntity3) {
							ItemStack _setstack = new ItemStack(MinecraftArmorWeaponModItems.WITHER_KATANA.get());
							_setstack.setCount(1);
							_livingEntity3.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
							if (_livingEntity3 instanceof Player _player)
								_player.getInventory().setChanged();
						}
						((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).enchant(Enchantments.SHARPNESS,
								entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()) ? _livEnt.getEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()).getAmplifier() : 0);
					}
				} else {
					if (!((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == Items.BOW)) {
						if (entity instanceof LivingEntity _livingEntity2) {
							ItemStack _setstack = new ItemStack(Items.BOW);
							_setstack.setCount(1);
							_livingEntity2.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
							if (_livingEntity2 instanceof Player _player)
								_player.getInventory().setChanged();
						}
						(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().putBoolean("Unbreakable", true);
						if (entity instanceof WitherSkeleton) {
							((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).enchant(Enchantments.FLAMING_ARROWS,
									entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()) ? _livEnt.getEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()).getAmplifier() : 0);
						}
						((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).enchant(Enchantments.POWER_ARROWS,
								entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()) ? _livEnt.getEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()).getAmplifier() : 0);
					}
				}
			}
			// Handle Iron Golem targeting with null checks
			IronGolem nearestIronGolem = null;
			try {
				nearestIronGolem = world.getEntitiesOfClass(IronGolem.class, AABB.ofSize(new Vec3(x, y, z), 64, 64, 64), e -> true).stream()
					.sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(x, y, z)))
					.findFirst().orElse(null);
			} catch (Exception e) {
				// Handle any exceptions during iron golem search
			}
			
			if (nearestIronGolem != null) {
				if (entity instanceof Mob _mobEntity3) {
					try {
						_mobEntity3.setTarget(nearestIronGolem);
					} catch (Exception e) {
						// Handle any exceptions during target setting
					}
				}
				if (!world.getEntitiesOfClass(IronGolem.class, AABB.ofSize(new Vec3(x, y, z), 10, 10, 10), e -> true).isEmpty()) {
					if (!((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WITHER_KATANA.get())) {
						if (entity instanceof LivingEntity _livingEntity3) {
							ItemStack _setstack = new ItemStack(MinecraftArmorWeaponModItems.WITHER_KATANA.get());
							_setstack.setCount(1);
							_livingEntity3.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
							if (_livingEntity3 instanceof Player _player)
								_player.getInventory().setChanged();
						}
						((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).enchant(Enchantments.SHARPNESS,
								entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()) ? _livEnt.getEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()).getAmplifier() : 0);
					}
				} else {
					if (!((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == Items.BOW)) {
						if (entity instanceof LivingEntity _livingEntity4) {
							ItemStack _setstack = new ItemStack(Items.BOW);
							_setstack.setCount(1);
							_livingEntity4.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
							if (_livingEntity4 instanceof Player _player)
								_player.getInventory().setChanged();
						}
						(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().putBoolean("Unbreakable", true);
						if (entity instanceof WitherSkeleton) {
							((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).enchant(Enchantments.FLAMING_ARROWS,
									entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()) ? _livEnt.getEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()).getAmplifier() : 0);
						}
						((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)).enchant(Enchantments.POWER_ARROWS,
								entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()) ? _livEnt.getEffect(MinecraftArmorWeaponModMobEffects.HARDENTITY.get()).getAmplifier() : 0);
					}
				}
			}
		}
	}
}
