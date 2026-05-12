package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

public class ThunderboltYoukuritukusitatokiProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.THUNDERBOLT.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.THUNDERBOLT.get()) {
			if (((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.KURIKARAKEN.get()
					|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.KURIKARAKENSWORD.get()
					|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.KURIKARAKENUTIGATANA.get())
					&& (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.THUNDERBOLT.get()
					|| ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.KURIKARAKEN.get()
							|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.KURIKARAKENSWORD.get()
							|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.KURIKARAKENUTIGATANA.get())
							&& (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.THUNDERBOLT.get()) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.TUNDERBOLTEFFRCT.get(), 200, 1, true, false));
			} else {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.TUNDERBOLTEFFRCT.get(), 100, 1, true, false));
			}
		}
	}
}
