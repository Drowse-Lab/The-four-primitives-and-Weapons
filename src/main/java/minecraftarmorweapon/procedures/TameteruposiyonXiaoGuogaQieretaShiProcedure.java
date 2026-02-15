package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

public class TameteruposiyonXiaoGuogaQieretaShiProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		MinecraftArmorWeaponMod.queueServerWork(2, () -> {
			entity.getPersistentData().putDouble("minecraft_armor_weapon:test_bow_tameteru", 0);
		});
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.TEST_BOW.get()) {
			(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().putDouble("CustomModelData", 0);
		}
	}
}
