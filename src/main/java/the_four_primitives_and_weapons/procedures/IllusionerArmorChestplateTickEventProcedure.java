package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

public class IllusionerArmorChestplateTickEventProcedure {
	// 軽量化:
	//   旧: 4 部位の onArmorTick から毎 tick 呼ばれ、 4 つの getItemBySlot + addEffect。
	//   新: LivingEntity キャスト 1 回, ItemStack 取得を変数キャッシュ, refresh-guard で addEffect スパム停止。
	public static void execute(Entity entity) {
		if (entity == null) return;
		if (!(entity instanceof LivingEntity living)) return;
		if (living.level().isClientSide()) return;

		if (living.getItemBySlot(EquipmentSlot.HEAD).getItem() != TheFourPrimitivesAndWeaponsModItems.ILLUSIONER_ARMOR_HELMET.get())   return;
		if (living.getItemBySlot(EquipmentSlot.CHEST).getItem() != TheFourPrimitivesAndWeaponsModItems.ILLUSIONER_ARMOR_CHESTPLATE.get()) return;
		if (living.getItemBySlot(EquipmentSlot.LEGS).getItem() != TheFourPrimitivesAndWeaponsModItems.ILLUSIONER_ARMOR_LEGGINGS.get())   return;
		if (living.getItemBySlot(EquipmentSlot.FEET).getItem() != TheFourPrimitivesAndWeaponsModItems.ILLUSIONER_ARMOR_BOOTS.get())      return;

		MobEffectInstance ex = living.getEffect(TheFourPrimitivesAndWeaponsModMobEffects.BOW_ATTACK.get());
		if (ex != null && ex.getAmplifier() >= 1 && ex.getDuration() > 6) return;

		living.addEffect(new MobEffectInstance(
				TheFourPrimitivesAndWeaponsModMobEffects.BOW_ATTACK.get(),
				12, 1, true, false));
	}
}
