package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

import the_four_primitives_and_weapons.entity.SkeltonMobEntity;
import the_four_primitives_and_weapons.entity.BlackholeEntity;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class BlackholeenteiteinotitukuGengXinShiProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double dx = 0;
		double dy = 0;
		double dz = 0;
		MukoukaGuardProcedure.execute(entity);
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(10 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (!(entityiterator == entity)) {
					if (true) {
						if (!(entityiterator instanceof SkeltonMobEntity)) {
							if (!(entityiterator instanceof BlackholeEntity)) {
								if (!(entityiterator instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.BLACK_HOLE_EFFECT.get()) : false)) {
									if (!((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.DARKNESS.get())
											&& !((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.DARKNESS_KATANA.get())
											&& !((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.PROTOTYPE_KATANA.get())) {
										dx = entity.getX() - entityiterator.getX();
										dy = entity.getY() - entityiterator.getY();
										dz = entity.getZ() - entityiterator.getZ();
										for (int index0 = 0; index0 < 20; index0++) {
											entityiterator.setDeltaMovement(new Vec3((dx / 20), (dy / 20), (dz / 20)));
										}
									}
								}
							}
						}
					}
				}
			}
		}
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (!(entityiterator == entity)) {
					if (true) {
						if (!(entityiterator instanceof SkeltonMobEntity)) {
							if (!(entityiterator instanceof BlackholeEntity)) {
								if (!(entityiterator instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.BLACK_HOLE_EFFECT.get()) : false)) {
									if (entityiterator instanceof LivingEntity) {
										if (!((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.DARKNESS.get())
												&& !((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.DARKNESS_KATANA.get())
												&& !((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.PROTOTYPE_KATANA.get())) {
											TheFourPrimitivesAndWeaponsMod.queueServerWork(1, () -> {
												entityiterator.hurt(entityiterator.damageSources().magic(), (float) 0.5);
											});
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
}
