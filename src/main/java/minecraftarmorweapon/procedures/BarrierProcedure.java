package minecraftarmorweapon.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class BarrierProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double a = 0;
		double r = 0;
		double b = 0;
		double dis4 = 0;
		double dis3 = 0;
		double dis2 = 0;
		double dis1 = 0;
		double dis = 0;
		if (entity.getPersistentData().getBoolean("barrierhrmcngs") == true) {
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.LUNA.get()) {
				{
					final Vec3 _center = new Vec3(x, (y + 1), z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1.5 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
							.collect(Collectors.toList());
					for (Entity entityiterator : _entfound) {
						if (entityiterator instanceof Arrow || entityiterator instanceof SpectralArrow || entityiterator instanceof ThrownTrident || entityiterator instanceof LargeFireball || entityiterator instanceof DragonFireball
								|| entityiterator instanceof Snowball || entityiterator instanceof ThrownEgg || entityiterator instanceof SmallFireball) {
							entityiterator.getPersistentData().putBoolean("barrier", true);
						}
					}
				}
			}
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.LUNA.get()
					|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.LUNA.get()) {
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.LUNA.get()) {
					{
						ItemStack _ist = (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY);
						if (_ist.hurt(1, RandomSource.create(), null)) {
							_ist.shrink(1);
							_ist.setDamageValue(0);
						}
					}
				} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.LUNA.get()) {
					{
						ItemStack _ist = (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY);
						if (_ist.hurt(1, RandomSource.create(), null)) {
							_ist.shrink(1);
							_ist.setDamageValue(0);
						}
					}
				}
				r = 1.5;
				a = Math.random() * 12;
				b = 90;
				for (int index0 = 0; index0 < 30; index0++) {
					for (int index1 = 0; index1 < 16; index1++) {
						b = b + 12;
					}
					b = 90;
					a = a + 12;
				}
				{
					final Vec3 _center = new Vec3(x, (y + 1), z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(10 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
							.collect(Collectors.toList());
					for (Entity entityiterator : _entfound) {
						if (entityiterator.getPersistentData().getBoolean("barrier") != true) {
							if (!(entityiterator == entity)) {
								if (entityiterator instanceof Arrow || entityiterator instanceof SpectralArrow || entityiterator instanceof ThrownTrident || entityiterator instanceof LargeFireball || entityiterator instanceof DragonFireball
										|| entityiterator instanceof Snowball || entityiterator instanceof ThrownEgg || entityiterator instanceof SmallFireball) {
									entityiterator.setDeltaMovement(new Vec3(0, 0, 0));
								}
							}
						}
					}
				}
			}
		}
	}
}
