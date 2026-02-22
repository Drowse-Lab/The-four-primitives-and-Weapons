package minecraftarmorweapon.procedures;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class BowMultishotMamotProcedure {
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, VersionHelper.getLevel(event.player), event.player);
		}
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double a = 0;
		double b = 0;
		double c = 0;
		if ((entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BOGGED_OUTER_HELMET.get()
				&& (entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BOGGED_OUTER_CHESTPLATE.get()
				&& (entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.LEGS) : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BOGGED_OUTER_LEGGINGS.get()
				&& (entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.FEET) : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BOGGED_OUTER_BOOTS.get()
				|| (entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STRAY_OUTER_ARMOR_HELMET.get()
						&& (entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STRAY_OUTER_ARMOR_CHESTPLATE.get()
						&& (entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.LEGS) : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STRAY_OUTER_ARMOR_LEGGINGS.get()) {
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getOrCreateTag().getDouble("minecraft_armor_weapon:multishotbowpower") > 1) {
				if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY)) != 0) {
					a = entity.getYRot();
					b = entity.getXRot();
					c = 10;
					if (world instanceof ServerLevel projectileLevel) {
						Projectile _entityToSpawn = new Object() {
							public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
								AbstractArrow entityToSpawn = new Arrow(EntityType.ARROW, level);
								entityToSpawn.setOwner(shooter);
								entityToSpawn.setBaseDamage(damage);
								entityToSpawn.setKnockback(knockback);
								entityToSpawn.setSecondsOnFire(100);
								entityToSpawn.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
								return entityToSpawn;
							}
						}.getArrow(projectileLevel, entity, 5, 1);
						_entityToSpawn.setPos((entity.getX()), (entity.getY() + 1.5), (entity.getZ()));
						_entityToSpawn.shoot(((-1) * (Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a)) + Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
								((-1) * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b))),
								(Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90))),
								(float) ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().getDouble("minecraft_armor_weapon:multishotbowpower")), 0);
						projectileLevel.addFreshEntity(_entityToSpawn);
					}
					c = (-1) * c;
					if (world instanceof ServerLevel projectileLevel) {
						Projectile _entityToSpawn = new Object() {
							public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
								AbstractArrow entityToSpawn = new Arrow(EntityType.ARROW, level);
								entityToSpawn.setOwner(shooter);
								entityToSpawn.setBaseDamage(damage);
								entityToSpawn.setKnockback(knockback);
								entityToSpawn.setSecondsOnFire(100);
								entityToSpawn.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
								return entityToSpawn;
							}
						}.getArrow(projectileLevel, entity, 5, 1);
						_entityToSpawn.setPos((entity.getX()), (entity.getY() + 1.5), (entity.getZ()));
						_entityToSpawn.shoot(((-1) * (Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a)) + Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
								((-1) * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b))),
								(Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90))),
								(float) ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().getDouble("minecraft_armor_weapon:multishotbowpower")), 0);
						projectileLevel.addFreshEntity(_entityToSpawn);
					}
					(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().putDouble("minecraft_armor_weapon:multishotbowpower", 0);
					(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().putDouble("minecraft_armor_weapon:multishotbownumber", 0);
				} else {
					a = entity.getYRot();
					b = entity.getXRot();
					c = 10;
					if (world instanceof ServerLevel projectileLevel) {
						Projectile _entityToSpawn = new Object() {
							public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
								AbstractArrow entityToSpawn = new Arrow(EntityType.ARROW, level);
								entityToSpawn.setOwner(shooter);
								entityToSpawn.setBaseDamage(damage);
								entityToSpawn.setKnockback(knockback);
								entityToSpawn.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
								return entityToSpawn;
							}
						}.getArrow(projectileLevel, entity, 5, 1);
						_entityToSpawn.setPos((entity.getX()), (entity.getY() + 1.5), (entity.getZ()));
						_entityToSpawn.shoot(((-1) * (Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a)) + Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
								((-1) * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b))),
								(Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90))),
								(float) ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().getDouble("minecraft_armor_weapon:multishotbowpower")), 0);
						projectileLevel.addFreshEntity(_entityToSpawn);
					}
					c = (-1) * c;
					if (world instanceof ServerLevel projectileLevel) {
						Projectile _entityToSpawn = new Object() {
							public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
								AbstractArrow entityToSpawn = new Arrow(EntityType.ARROW, level);
								entityToSpawn.setOwner(shooter);
								entityToSpawn.setBaseDamage(damage);
								entityToSpawn.setKnockback(knockback);
								entityToSpawn.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
								return entityToSpawn;
							}
						}.getArrow(projectileLevel, entity, 5, 1);
						_entityToSpawn.setPos((entity.getX()), (entity.getY() + 1.5), (entity.getZ()));
						_entityToSpawn.shoot(((-1) * (Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a)) + Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
								((-1) * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b))),
								(Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90))),
								(float) ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().getDouble("minecraft_armor_weapon:multishotbowpower")), 0);
						projectileLevel.addFreshEntity(_entityToSpawn);
					}
					(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().putDouble("minecraft_armor_weapon:multishotbowpower", 0);
					(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().putDouble("minecraft_armor_weapon:multishotbownumber", 0);
				}
			}
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().getDouble("minecraft_armor_weapon:multishotbowpower") > 1) {
				if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
					a = entity.getYRot();
					b = entity.getXRot();
					c = 10;
					if (world instanceof ServerLevel projectileLevel) {
						Projectile _entityToSpawn = new Object() {
							public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
								AbstractArrow entityToSpawn = new Arrow(EntityType.ARROW, level);
								entityToSpawn.setOwner(shooter);
								entityToSpawn.setBaseDamage(damage);
								entityToSpawn.setKnockback(knockback);
								entityToSpawn.setSecondsOnFire(100);
								entityToSpawn.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
								return entityToSpawn;
							}
						}.getArrow(projectileLevel, entity, 5, 1);
						_entityToSpawn.setPos((entity.getX()), (entity.getY() + 1.5), (entity.getZ()));
						_entityToSpawn.shoot(((-1) * (Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a)) + Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
								((-1) * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b))),
								(Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90))),
								(float) ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().getDouble("minecraft_armor_weapon:multishotbowpower")), 0);
						projectileLevel.addFreshEntity(_entityToSpawn);
					}
					c = (-1) * c;
					if (world instanceof ServerLevel projectileLevel) {
						Projectile _entityToSpawn = new Object() {
							public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
								AbstractArrow entityToSpawn = new Arrow(EntityType.ARROW, level);
								entityToSpawn.setOwner(shooter);
								entityToSpawn.setBaseDamage(damage);
								entityToSpawn.setKnockback(knockback);
								entityToSpawn.setSecondsOnFire(100);
								entityToSpawn.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
								return entityToSpawn;
							}
						}.getArrow(projectileLevel, entity, 5, 1);
						_entityToSpawn.setPos((entity.getX()), (entity.getY() + 1.5), (entity.getZ()));
						_entityToSpawn.shoot(((-1) * (Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a)) + Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
								((-1) * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b))),
								(Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90))),
								(float) ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().getDouble("minecraft_armor_weapon:multishotbowpower")), 0);
						projectileLevel.addFreshEntity(_entityToSpawn);
					}
					(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().putDouble("minecraft_armor_weapon:multishotbowpower", 0);
					(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().putDouble("minecraft_armor_weapon:multishotbownumber", 0);
				} else {
					a = entity.getYRot();
					b = entity.getXRot();
					c = 10;
					if (world instanceof ServerLevel projectileLevel) {
						Projectile _entityToSpawn = new Object() {
							public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
								AbstractArrow entityToSpawn = new Arrow(EntityType.ARROW, level);
								entityToSpawn.setOwner(shooter);
								entityToSpawn.setBaseDamage(damage);
								entityToSpawn.setKnockback(knockback);
								entityToSpawn.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
								return entityToSpawn;
							}
						}.getArrow(projectileLevel, entity, 5, 1);
						_entityToSpawn.setPos((entity.getX()), (entity.getY() + 1.5), (entity.getZ()));
						_entityToSpawn.shoot(((-1) * (Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a)) + Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
								((-1) * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b))),
								(Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90))),
								(float) ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().getDouble("minecraft_armor_weapon:multishotbowpower")), 0);
						projectileLevel.addFreshEntity(_entityToSpawn);
					}
					c = (-1) * c;
					if (world instanceof ServerLevel projectileLevel) {
						Projectile _entityToSpawn = new Object() {
							public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
								AbstractArrow entityToSpawn = new Arrow(EntityType.ARROW, level);
								entityToSpawn.setOwner(shooter);
								entityToSpawn.setBaseDamage(damage);
								entityToSpawn.setKnockback(knockback);
								entityToSpawn.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
								return entityToSpawn;
							}
						}.getArrow(projectileLevel, entity, 5, 1);
						_entityToSpawn.setPos((entity.getX()), (entity.getY() + 1.5), (entity.getZ()));
						_entityToSpawn.shoot(((-1) * (Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a)) + Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
								((-1) * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b))),
								(Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90))),
								(float) ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().getDouble("minecraft_armor_weapon:multishotbowpower")), 0);
						projectileLevel.addFreshEntity(_entityToSpawn);
					}
					(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().putDouble("minecraft_armor_weapon:multishotbowpower", 0);
					(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().putDouble("minecraft_armor_weapon:multishotbownumber", 0);
				}
			}
		}
	}
}
