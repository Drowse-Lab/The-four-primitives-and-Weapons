package minecraftarmorweapon.procedures;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

public class GyetonzangekiposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure {
	public static void execute(double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double Radius = 0;
		double Ypos = 0;
		double Z1 = 0;
		double loop2 = 0;
		double Z2 = 0;
		double Y = 0;
		double X1 = 0;
		double X2 = 0;
		double loop1 = 0;
		if (MinecraftArmorWeaponModItems.MY_TEST_IRON_KATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.OLD_KATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.DARKNESS_KATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.KURIKARAKEN.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.KURIKARAKENSWORD.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.KURIKARAKENUTIGATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.LUNA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.LUNA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.MY_TEST_IRON_KATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.REPLICA_SWORD_OF_LIGHT.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.SMALL_SWORD.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.KATANA_NIGU_HUMERUS.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.MAGISCHES_FEEN_KATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
			entity.getPersistentData().putDouble("local", Math.toRadians(entity.getYRot()));
			entity.getPersistentData().putDouble("local1", Math.toRadians(entity.getYRot() + 180));
			entity.getPersistentData().putDouble("helmet", (Mth.nextDouble(RandomSource.create(), -180, 180)));
			entity.getPersistentData().putDouble("X", x);
			entity.getPersistentData().putDouble("Ypos", y);
			entity.getPersistentData().putDouble("Z", z);
			entity.getPersistentData().putDouble("dis", 0);
			entity.getPersistentData().putDouble("yaw", (entity.getYRot()));
			entity.getPersistentData().putDouble("distance", 3);
		} else if (MinecraftArmorWeaponModItems.MY_TEST_IRON_KATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(MinecraftArmorWeaponModMobEffects.ZANNGEKIKAI.get());
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(MinecraftArmorWeaponModMobEffects.ZANNGEKITOKUBETU.get());
			entity.getPersistentData().putDouble("local", Math.toRadians(entity.getYRot()));
			entity.getPersistentData().putDouble("local1", Math.toRadians(entity.getYRot() + 180));
			entity.getPersistentData().putDouble("helmet", (Mth.nextDouble(RandomSource.create(), -180, 180)));
			entity.getPersistentData().putDouble("X", x);
			entity.getPersistentData().putDouble("Ypos", y);
			entity.getPersistentData().putDouble("Z", z);
			entity.getPersistentData().putDouble("dis", 0);
			entity.getPersistentData().putDouble("yaw", (entity.getYRot()));
			entity.getPersistentData().putDouble("distance", 3);
		} else if (MinecraftArmorWeaponModItems.LUNA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.KURIKARAKEN.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.KURIKARAKENSWORD.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.KURIKARAKENUTIGATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.MAGISCHES_FEEN_KATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| MinecraftArmorWeaponModItems.RIVERS_OF_BLOOD.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(MinecraftArmorWeaponModMobEffects.TOBE.get());
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(MinecraftArmorWeaponModMobEffects.GYETONZANGEKI.get());
			entity.getPersistentData().putDouble("local", Math.toRadians(entity.getYRot()));
			entity.getPersistentData().putDouble("local1", Math.toRadians(entity.getYRot()));
			entity.getPersistentData().putDouble("helmet", (Mth.nextDouble(RandomSource.create(), -180, 180)));
			entity.getPersistentData().putDouble("X1", x);
			entity.getPersistentData().putDouble("X", x);
			entity.getPersistentData().putDouble("Ypos1", y);
			entity.getPersistentData().putDouble("Ypos", y);
			entity.getPersistentData().putDouble("Z1", z);
			entity.getPersistentData().putDouble("Z", z);
			entity.getPersistentData().putDouble("dis", 0);
			entity.getPersistentData().putDouble("yaw1", (entity.getYRot()));
			entity.getPersistentData().putDouble("yaw", (entity.getYRot()));
			entity.getPersistentData().putDouble("distance1", 3);
			entity.getPersistentData().putDouble("distance", 3);
		} else if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.TOBE.get()) : false) {
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(MinecraftArmorWeaponModMobEffects.ZANNGEKIKAI.get());
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(MinecraftArmorWeaponModMobEffects.GYETONZANGEKI.get());
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(MinecraftArmorWeaponModMobEffects.ZANNGEKITOKUBETU.get());
			entity.getPersistentData().putDouble("local", Math.toRadians(entity.getYRot()));
			entity.getPersistentData().putDouble("local1", Math.toRadians(entity.getYRot() + 180));
			entity.getPersistentData().putDouble("beta", (entity.getXRot()));
			entity.getPersistentData().putDouble("helmet", (Mth.nextDouble(RandomSource.create(), -180, 180)));
			entity.getPersistentData().putDouble("X", x);
			entity.getPersistentData().putDouble("Ypos", y);
			entity.getPersistentData().putDouble("Z", z);
			entity.getPersistentData().putDouble("dis", 0);
			entity.getPersistentData().putDouble("yaw", (entity.getYRot()));
			entity.getPersistentData().putDouble("distance", 3);
		}
	}
}
