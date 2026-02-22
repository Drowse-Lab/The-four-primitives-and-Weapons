package minecraftarmorweapon.procedures;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
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

public class TobeposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double yknockback = 0;
		double ZRadius2 = 0;
		double dis = 0;
		double zknockback = 0;
		double r = 0;
		double loop = 0;
		double alpha = 0;
		double XRadius2 = 0;
		double X = 0;
		double Y = 0;
		double Z = 0;
		double xknockback = 0;
		double Y_pos = 0;
		double beta = 0;
		beta = entity.getPersistentData().getDouble("beta") / 60;
		entity.getPersistentData().putDouble("Xpos", (entity.getPersistentData().getDouble("X") + Math.sin(Math.toRadians(entity.getPersistentData().getDouble("yaw") + 180)) * entity.getPersistentData().getDouble("distance")));
		entity.getPersistentData().putDouble("Zpos", (entity.getPersistentData().getDouble("Z") + Math.cos(Math.toRadians(entity.getPersistentData().getDouble("yaw"))) * entity.getPersistentData().getDouble("distance")));
		for (int index0 = 0; index0 < 30; index0++) {
			if (world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos") + entity.getPersistentData().getDouble("beta") / 60, entity.getPersistentData().getDouble("Zpos")))
					.canOcclude()) {
				entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos") + entity.getPersistentData().getDouble("beta") / 60));
			} else {
				break;
			}
		}
		for (int index1 = 0; index1 < 30; index1++) {
			if (world.getBlockState(new BlockPos(entity.getPersistentData().getDouble("Xpos"), entity.getPersistentData().getDouble("Ypos") + entity.getPersistentData().getDouble("beta") / 60, entity.getPersistentData().getDouble("Zpos")))
					.canOcclude()) {
				entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos") + entity.getPersistentData().getDouble("beta") / 60));
				break;
			}
			entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos") + entity.getPersistentData().getDouble("beta") / 60));
		}
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.END_ROD, (entity.getPersistentData().getDouble("Xpos")), (entity.getPersistentData().getDouble("Ypos") + entity.getPersistentData().getDouble("beta") / 60),
					(entity.getPersistentData().getDouble("Zpos")), 10, 0.1, 0.1, 0.1, 0);
		{
			final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("Xpos")), (entity.getPersistentData().getDouble("Ypos") + entity.getPersistentData().getDouble("beta") / 60), (entity.getPersistentData().getDouble("Zpos")));
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (!(entityiterator == entity)) {
					if (!(entityiterator instanceof OtiruyoEntity)) {
						if (!(entityiterator instanceof SkeltonMobEntity)) {
							if (entityiterator instanceof LivingEntity) {
								if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
									if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.TUNDERBOLTEFFRCT.get()) : false) {
										if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
											_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 100, 2, true, false));
									}
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
									if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.TUNDERBOLTEFFRCT.get()) : false) {
										if (entityiterator instanceof LivingEntity _entity && !_entity.level.isClientSide())
											_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get(), 100, 2, true, false));
									}
									if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKEN.get()
											&& (entity instanceof LivingEntity _livEnt ? _livEnt.getMobType() == MobType.UNDEAD : false)) {
										entityiterator.hurt(DamageSource.GENERIC, 10);
									}
									entityiterator.hurt(DamageSource.GENERIC, 10);
								}
							}
						}
					}
				}
			}
		}
		entity.getPersistentData().putDouble("distance", (entity.getPersistentData().getDouble("distance") + 0.8));
	}
}
