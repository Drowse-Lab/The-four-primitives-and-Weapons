package minecraftarmorweapon.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level().LevelAccessor;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level().ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;

import minecraftarmorweapon.entity.SkeltonMobEntity;
import minecraftarmorweapon.entity.OtiruyoEntity;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class ZanngekikaiehuekutogaYouXiaoShinoteitukuProcedure {
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
		double X1 = 0;
		double Y1 = 0;
		double Z1 = 0;
		double XRadius3 = 0;
		double ZRadius3 = 0;
		loop1 = entity.getPersistentData().getDouble("local1");
		entity.getPersistentData().putDouble("Xpos1", (entity.getPersistentData().getDouble("X1") + Math.sin(Math.toRadians(entity.getPersistentData().getDouble("yaw1") + 180)) * entity.getPersistentData().getDouble("distance1")));
		entity.getPersistentData().putDouble("Zpos1", (entity.getPersistentData().getDouble("Z1") + Math.cos(Math.toRadians(entity.getPersistentData().getDouble("yaw1"))) * entity.getPersistentData().getDouble("distance1")));
		XRadius3 = 3;
		ZRadius3 = 3;
		Y_pos1 = entity.getPersistentData().getDouble("Ypos1") + 3;
		for (int index0 = 0; index0 < 100; index0++) {
			if (world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos1"), entity.getPersistentData().getDouble("Ypos1"), entity.getPersistentData().getDouble("Zpos1"))).canOcclude()) {
				entity.getPersistentData().putDouble("Ypos1", (entity.getPersistentData().getDouble("Ypos1")));
				Y_pos = entity.getPersistentData().getDouble("Ypos1") + 3;
			} else {
				break;
			}
		}
		for (int index1 = 0; index1 < 100; index1++) {
			if (world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos1"), entity.getPersistentData().getDouble("Ypos1"), entity.getPersistentData().getDouble("Zpos1"))).canOcclude()) {
				entity.getPersistentData().putDouble("Ypos1", (entity.getPersistentData().getDouble("Ypos1")));
				Y_pos1 = entity.getPersistentData().getDouble("Ypos1") + 1;
				break;
			}
			entity.getPersistentData().putDouble("Ypos1", (entity.getPersistentData().getDouble("Ypos1")));
			Y_pos1 = entity.getPersistentData().getDouble("Ypos1") + 1;
		}
		for (int index2 = 0; index2 < 36; index2++) {
			X1 = entity.getPersistentData().getDouble("X1") + Math.sin(Math.toRadians(entity.getPersistentData().getDouble("yaw1") + 180)) * entity.getPersistentData().getDouble("distance1") + Math.cos(loop1) * XRadius3;
			Y1 = Y_pos1 + 3;
			Z1 = entity.getPersistentData().getDouble("Z1") + Math.cos(Math.toRadians(entity.getPersistentData().getDouble("yaw1"))) * entity.getPersistentData().getDouble("distance1") + Math.sin(loop1) * ZRadius3;
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.SWEEP_ATTACK, X1, Y1, Z1, 3, 0.1, 0.1, 0.1, 0);
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.CLOUD, X1, Y1, Z1, 3, 0.1, 0.1, 0.1, 0);
			{
				final Vec3 _center = new Vec3(X1, Y1, Z1);
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
						.collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!(entityiterator == entity)) {
						if (!(entityiterator instanceof OtiruyoEntity)) {
							if (!(entityiterator instanceof SkeltonMobEntity)) {
								if (entityiterator instanceof Mob) {
									if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
										if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.TUNDERBOLTEFFRCT.get()) : false) {
											if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
												_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 100, 2, true, false));
										}
										{
											Entity _ent = entityiterator;
											if (!_ent.level().isClientSide() && _ent.getServer() != null) {
												_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
														_ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/kill @s");
											}
										}
										{
											Entity _ent = entityiterator;
											if (!_ent.level().isClientSide() && _ent.getServer() != null) {
												_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
														_ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/deta merge entity @s (Health:0)");
											}
										}
									} else {
										if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.TUNDERBOLTEFFRCT.get()) : false) {
											if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
												_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 100, 2, true, false));
										}
										if (((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKENSWORD.get()
												|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKENUTIGATANA.get()
												|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKEN.get())
												&& (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getMobType() == MobType.UNDEAD : false)
												&& EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SMITE, (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
											entityiterator.hurt(DamageSource.GENERIC, (float) (((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue()
													+ EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SMITE, (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) * 2.5));
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
			loop1 = loop1 + Math.toRadians(5);
			Y_pos1 = Y_pos - 0.1666666666666667;
		}
		entity.getPersistentData().putDouble("distance1", (entity.getPersistentData().getDouble("distance1") + 0.8));
	}
}
