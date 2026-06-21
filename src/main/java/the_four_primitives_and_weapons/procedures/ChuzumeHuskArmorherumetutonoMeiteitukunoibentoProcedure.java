package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

public class ChuzumeHuskArmorherumetutonoMeiteitukunoibentoProcedure {
	// 軽量化:
	//   旧: 4 部位の onArmorTick から毎 tick 呼ばれ、 4 つの getItemBySlot + addEffect。
	//   新: LivingEntity キャスト 1 回, ItemStack 取得を変数キャッシュ, refresh-guard で addEffect スパム停止。
	public static void execute(Entity entity) {
		if (entity == null) return;
		if (!(entity instanceof LivingEntity living)) return;
		if (living.level().isClientSide()) return;

		if (living.getItemBySlot(EquipmentSlot.HEAD).getItem() != TheFourPrimitivesAndWeaponsModItems.CHUZUME_HUSK_ARMOR_HELMET.get())   return;
		if (living.getItemBySlot(EquipmentSlot.CHEST).getItem() != TheFourPrimitivesAndWeaponsModItems.CHUZUME_HUSK_ARMOR_CHESTPLATE.get()) return;
		if (living.getItemBySlot(EquipmentSlot.LEGS).getItem() != TheFourPrimitivesAndWeaponsModItems.CHUZUME_HUSK_ARMOR_LEGGINGS.get())   return;
		if (living.getItemBySlot(EquipmentSlot.FEET).getItem() != TheFourPrimitivesAndWeaponsModItems.CHUZUME_HUSK_ARMOR_BOOTS.get())      return;

		// 同じ tick の他部位から再度呼ばれた時の addEffect 重複を回避。
		MobEffectInstance ex = living.getEffect(TheFourPrimitivesAndWeaponsModMobEffects.CHUZUME_HUSK_ARMOR_KNOCK_BACK.get());
		if (ex != null && ex.getAmplifier() >= 1 && ex.getDuration() > 4) return;

		living.addEffect(new MobEffectInstance(
				TheFourPrimitivesAndWeaponsModMobEffects.CHUZUME_HUSK_ARMOR_KNOCK_BACK.get(),
				10, 1, false, false));
	}
}
