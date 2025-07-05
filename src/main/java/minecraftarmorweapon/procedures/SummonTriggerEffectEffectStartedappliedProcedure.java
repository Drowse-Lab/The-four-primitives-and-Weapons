package minecraftarmorweapon.procedures;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import minecraftarmorweapon.entity.FlyingAttackerEntity;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEntities;

public class SummonTriggerEffectEffectStartedappliedProcedure {
	public static void execute(Entity entity) {
		if (entity == null || !(entity.level instanceof ServerLevel)) return;
		ServerLevel world = (ServerLevel) entity.level;

		FlyingAttackerEntity mob = new FlyingAttackerEntity(
			MinecraftArmorWeaponModEntities.FLYING_ATTACKER.get(), world
		);

		mob.moveTo(entity.getX(), entity.getY() + 2, entity.getZ(), entity.getYRot(), 0);
		mob.setInvisible(true);
		mob.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
		mob.setNoGravity(true);

		world.addFreshEntity(mob);
	}
}