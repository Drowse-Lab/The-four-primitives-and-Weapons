package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Rotations;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import org.joml.Vector3f;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

public class GuardposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.REPLICA_SWORD_OF_LIGHT.get()) {
			if (world instanceof ServerLevel _level) {
				spawnGuardArmorStand(_level, x, y, z, Items.YELLOW_STAINED_GLASS_PANE, 1f, 1f, 0.5f);
			}
		} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.LOKI_THE_TRICKSTER.get()) {
			if (world instanceof ServerLevel _level) {
				spawnGuardArmorStand(_level, x, y, z, Items.LIGHT_GRAY_STAINED_GLASS_PANE, 1f, 1f, 0.5f);
			}
		} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.PROTOTYPE_KATANA.get()) {
			if (world instanceof ServerLevel _level) {
				spawnGuardArmorStand(_level, x, y, z, Items.BLACK_STAINED_GLASS_PANE, 0.5f, 0.5f, 0.5f);
			}
		} else {
			// その他の刀/剣: 白いガラスパネでガードエフェクト
			if (world instanceof ServerLevel _level) {
				spawnGuardArmorStand(_level, x, y, z, Items.WHITE_STAINED_GLASS_PANE, 0.8f, 0.8f, 0.8f);
			}
		}
		entity.getPersistentData().putDouble("the_four_primitives_and_weapons:muteki_x_chuzume", (entity.getX()));
		entity.getPersistentData().putDouble("the_four_primitives_and_weapons:muteki_y_chuzume", (entity.getY()));
		entity.getPersistentData().putDouble("the_four_primitives_and_weapons:muteki_z_chuzume", (entity.getZ()));
		entity.getPersistentData().putDouble("the_four_primitives_and_weapons:muteki_resistance_level",
				(entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MobEffects.DAMAGE_RESISTANCE) ? _livEnt.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() : 0));
		entity.getPersistentData().putDouble("the_four_primitives_and_weapons:muteki_resistance_time",
				(entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MobEffects.DAMAGE_RESISTANCE) ? _livEnt.getEffect(MobEffects.DAMAGE_RESISTANCE).getDuration() : 0));
		entity.getPersistentData().putDouble("the_four_primitives_and_weapons:muteki_knockback_resistance", ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).getBaseValue());
		entity.getPersistentData().putDouble("the_four_primitives_and_weapons:muteki_speed", ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue());
		((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue((-10));
		((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
		{
			Entity _ent = entity;
			_ent.teleportTo((entity.getPersistentData().getDouble("the_four_primitives_and_weapons:muteki_x_chuzume")), (entity.getPersistentData().getDouble("the_four_primitives_and_weapons:muteki_y_chuzume")),
					(entity.getPersistentData().getDouble("the_four_primitives_and_weapons:muteki_z_chuzume")));
			if (_ent instanceof ServerPlayer _serverPlayer)
				_serverPlayer.connection.teleport((entity.getPersistentData().getDouble("the_four_primitives_and_weapons:muteki_x_chuzume")), (entity.getPersistentData().getDouble("the_four_primitives_and_weapons:muteki_y_chuzume")),
						(entity.getPersistentData().getDouble("the_four_primitives_and_weapons:muteki_z_chuzume")), _ent.getYRot(), _ent.getXRot());
		}
		// Replicaガード（amplifier=0）のみResistance Vを付与（完全防御）
		// 通常ガード（amplifier=1）は25%カットのみ（SwordGuardHandlerで処理）
		if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
			MobEffectInstance guardEff = _entity.getEffect(TheFourPrimitivesAndWeaponsModMobEffects.GUARD.get());
			int guardDuration = guardEff != null ? guardEff.getDuration() : 0;
			int guardAmplifier = guardEff != null ? guardEff.getAmplifier() : 0;
			if (guardAmplifier == 0) {
				// Replica: 完全防御
				_entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, guardDuration, 5, true, false));
			}
			_entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, guardDuration, 10, true, false));
		}
		TheFourPrimitivesAndWeaponsMod.queueServerWork(entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.GUARD.get()) ? _livEnt.getEffect(TheFourPrimitivesAndWeaponsModMobEffects.GUARD.get()).getDuration() : 0, () -> {
			killNearestGuardArmorStand(entity);
			((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue((entity.getPersistentData().getDouble("the_four_primitives_and_weapons:muteki_speed")));
			((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue((entity.getPersistentData().getDouble("the_four_primitives_and_weapons:muteki_knockback_resistance")));
			if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MobEffects.WEAKNESS) : false) {
				entity.getPersistentData().putBoolean("the_four_primitives_and_weapons:muteki_weakenss_chuzume_copy", true);
				entity.getPersistentData().putDouble("the_four_primitives_and_weapons:muteki_weakenss_chuzume_copy_level",
						(entity instanceof LivingEntity _livEnt2 && _livEnt2.hasEffect(MobEffects.WEAKNESS) ? _livEnt2.getEffect(MobEffects.WEAKNESS).getAmplifier() : 0));
				entity.getPersistentData().putDouble("the_four_primitives_and_weapons:muteki_weakenss_chuzume_copy_tick",
						(entity instanceof LivingEntity _livEnt2 && _livEnt2.hasEffect(MobEffects.WEAKNESS) ? _livEnt2.getEffect(MobEffects.WEAKNESS).getDuration() : 0));
			}
			if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MobEffects.DAMAGE_RESISTANCE) : false) {
				entity.getPersistentData().putBoolean("the_four_primitives_and_weapons:muteki_the_four_primitives_and_weapons:muteki_resistance_chuzume_copy_chuzume_copy", true);
				entity.getPersistentData().putDouble("the_four_primitives_and_weapons:muteki_the_four_primitives_and_weapons:muteki_resistance_chuzume_copy_chuzume_copy_level",
						(entity instanceof LivingEntity _livEnt2 && _livEnt2.hasEffect(MobEffects.WEAKNESS) ? _livEnt2.getEffect(MobEffects.WEAKNESS).getAmplifier() : 0));
				entity.getPersistentData().putDouble("the_four_primitives_and_weapons:muteki_the_four_primitives_and_weapons:muteki_resistance_chuzume_copy_chuzume_copy_tick",
						(entity instanceof LivingEntity _livEnt2 && _livEnt2.hasEffect(MobEffects.WEAKNESS) ? _livEnt2.getEffect(MobEffects.WEAKNESS).getDuration() : 0));
			}
		});
	}

	private static void spawnGuardArmorStand(ServerLevel level, double x, double y, double z, Item glassPane, float r, float g, float b) {
		ArmorStand armorStand = new ArmorStand(level, x, y, z);
		armorStand.setNoGravity(true);
		armorStand.setInvisible(true);
		armorStand.setInvulnerable(true);
		armorStand.addTag("the_four_primitives_and_weapons_guard_bind");
		armorStand.setLeftArmPose(new Rotations(0f, 90f, -90f));
		armorStand.setRightArmPose(new Rotations(0f, -90f, 90f));
		armorStand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(glassPane));
		armorStand.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(glassPane));
		level.addFreshEntity(armorStand);
		level.playSound(null, x, y, z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 2, 2);
		level.playSound(null, x, y, z, SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.PLAYERS, 1, 1);
		level.sendParticles(new DustParticleOptions(new Vector3f(r, g, b), 0.5f), x, y + 1, z, 35, 0.25, 0.25, 0.25, 1);
	}

	public static void killNearestGuardArmorStand(Entity entity) {
		if (entity.level() instanceof ServerLevel serverLevel) {
			ArmorStand nearest = null;
			double nearestDist = Double.MAX_VALUE;
			for (Entity e : serverLevel.getAllEntities()) {
				if (e instanceof ArmorStand stand && stand.getTags().contains("the_four_primitives_and_weapons_guard_bind")) {
					double dist = stand.distanceToSqr(entity);
					if (dist < nearestDist) {
						nearestDist = dist;
						nearest = stand;
					}
				}
			}
			if (nearest != null) {
				nearest.discard();
			}
		}
	}

	static void killAllGuardArmorStands(ServerLevel level) {
		java.util.List<ArmorStand> toRemove = new java.util.ArrayList<>();
		for (Entity e : level.getAllEntities()) {
			if (e instanceof ArmorStand stand && stand.getTags().contains("the_four_primitives_and_weapons_guard_bind")) {
				toRemove.add(stand);
			}
		}
		for (ArmorStand stand : toRemove) {
			stand.discard();
		}
	}
}
