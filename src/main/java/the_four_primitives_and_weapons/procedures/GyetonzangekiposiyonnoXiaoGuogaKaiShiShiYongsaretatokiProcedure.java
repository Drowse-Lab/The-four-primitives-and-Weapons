package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

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
		if (TheFourPrimitivesAndWeaponsModItems.MY_TEST_IRON_KATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| TheFourPrimitivesAndWeaponsModItems.OLD_KATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| TheFourPrimitivesAndWeaponsModItems.DARKNESS_KATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| TheFourPrimitivesAndWeaponsModItems.KURIKARAKEN.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| TheFourPrimitivesAndWeaponsModItems.KURIKARAKENSWORD.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| TheFourPrimitivesAndWeaponsModItems.KURIKARAKENUTIGATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| TheFourPrimitivesAndWeaponsModItems.LUNA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem()
				|| TheFourPrimitivesAndWeaponsModItems.LUNA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| TheFourPrimitivesAndWeaponsModItems.MY_TEST_IRON_KATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem()
				|| TheFourPrimitivesAndWeaponsModItems.PROTOTYPE_KATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| TheFourPrimitivesAndWeaponsModItems.SMALL_SWORD.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| TheFourPrimitivesAndWeaponsModItems.KATANA_NIGU_HUMERUS.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| TheFourPrimitivesAndWeaponsModItems.MAGISCHES_FEEN_KATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
			entity.getPersistentData().putDouble("local", Math.toRadians(entity.getYRot()));
			entity.getPersistentData().putDouble("local1", Math.toRadians(entity.getYRot() + 180));
			entity.getPersistentData().putDouble("helmet", (Mth.nextDouble(RandomSource.create(), -180, 180)));
			entity.getPersistentData().putDouble("X", x);
			entity.getPersistentData().putDouble("Ypos", y);
			entity.getPersistentData().putDouble("Z", z);
			entity.getPersistentData().putDouble("dis", 0);
			entity.getPersistentData().putDouble("yaw", (entity.getYRot()));
			entity.getPersistentData().putDouble("distance", 3);
		} else if (TheFourPrimitivesAndWeaponsModItems.MY_TEST_IRON_KATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(TheFourPrimitivesAndWeaponsModMobEffects.ZANNGEKIKAI.get());
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(TheFourPrimitivesAndWeaponsModMobEffects.ZANNGEKITOKUBETU.get());
			entity.getPersistentData().putDouble("local", Math.toRadians(entity.getYRot()));
			entity.getPersistentData().putDouble("local1", Math.toRadians(entity.getYRot() + 180));
			entity.getPersistentData().putDouble("helmet", (Mth.nextDouble(RandomSource.create(), -180, 180)));
			entity.getPersistentData().putDouble("X", x);
			entity.getPersistentData().putDouble("Ypos", y);
			entity.getPersistentData().putDouble("Z", z);
			entity.getPersistentData().putDouble("dis", 0);
			entity.getPersistentData().putDouble("yaw", (entity.getYRot()));
			entity.getPersistentData().putDouble("distance", 3);
		} else if (TheFourPrimitivesAndWeaponsModItems.LUNA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| TheFourPrimitivesAndWeaponsModItems.KURIKARAKEN.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| TheFourPrimitivesAndWeaponsModItems.KURIKARAKENSWORD.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| TheFourPrimitivesAndWeaponsModItems.KURIKARAKENUTIGATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| TheFourPrimitivesAndWeaponsModItems.MAGISCHES_FEEN_KATANA.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
				|| TheFourPrimitivesAndWeaponsModItems.RIVERS_OF_BLOOD.get() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(TheFourPrimitivesAndWeaponsModMobEffects.TOBE.get());
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(TheFourPrimitivesAndWeaponsModMobEffects.GYETONZANGEKI.get());
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
		} else if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.TOBE.get()) : false) {
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(TheFourPrimitivesAndWeaponsModMobEffects.ZANNGEKIKAI.get());
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(TheFourPrimitivesAndWeaponsModMobEffects.GYETONZANGEKI.get());
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(TheFourPrimitivesAndWeaponsModMobEffects.ZANNGEKITOKUBETU.get());
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
