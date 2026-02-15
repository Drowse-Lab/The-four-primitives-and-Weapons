//
//package minecraftarmorweapon.item;
//
//import net.minecraft.world.level.Level;
//import net.minecraft.world.item.UseAnim;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.entity.projectile.AbstractArrow;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.InteractionResultHolder;
//import net.minecraft.world.InteractionResult;
//import net.minecraft.world.InteractionHand;
//import net.minecraft.server.level.ServerPlayer;
//
//import minecraftarmorweapon.procedures.TestBowRangedItemUsedProcedure;
//
//import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;
//
//import minecraftarmorweapon.entity.TestBowEntity;
//
//public class TestBowItem extends Item {
//	public TestBowItem() {
//		super(new Item.Properties().tab(MinecraftArmorWeaponModTabs.TAB_WEAPON).stacksTo(1));
//	}
//
//	@Override
//	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
//		entity.startUsingItem(hand);
//		return new InteractionResultHolder(InteractionResult.SUCCESS, entity.getItemInHand(hand));
//	}
//
//	@Override
//	public UseAnim getUseAnimation(ItemStack itemstack) {
//    return UseAnim.BOW;
//	}
//
//	@Override
//	public int getUseDuration(ItemStack itemstack) {
//    return 72000;
//	}
//	
//	@Override
//	public void releaseUsing(ItemStack itemstack, Level world, LivingEntity entityLiving, int timeLeft) {
//    	if (!world.isClientSide() && entityLiving instanceof ServerPlayer entity) {
//			double x = entity.getX();
//			double y = entity.getY();
//			double z = entity.getZ();
//			if (true) {
//				TestBowEntity entityarrow = TestBowEntity.shoot(world, entity, world.getRandom(), 1f, 0, 0);
//				itemstack.hurtAndBreak(1, entity, e -> e.broadcastBreakEvent(entity.getUsedItemHand()));
//				entityarrow.pickup = AbstractArrow.Pickup.DISALLOWED;
//				TestBowRangedItemUsedProcedure.execute(world, x, y, z, entity);
//			}
//		}
//	}
//}
package minecraftarmorweapon.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;

import minecraftarmorweapon.procedures.TestBowRangedItemUsedProcedure;

import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;

import minecraftarmorweapon.entity.TestBowEntity;

public class TestBowItem extends Item {
	public TestBowItem() {
		super(new Item.Properties().tab(MinecraftArmorWeaponModTabs.TAB_WEAPON).stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		entity.startUsingItem(hand);
		return new InteractionResultHolder(InteractionResult.SUCCESS, entity.getItemInHand(hand));
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemstack) {
		return UseAnim.BOW;
	}

	@Override
	public int getUseDuration(ItemStack itemstack) {
		return 72000;
	}

	@Override
	public void releaseUsing(ItemStack itemstack, Level world, LivingEntity entityLiving, int timeLeft) {
		if (!world.isClientSide() && entityLiving instanceof ServerPlayer entity) {
			double x = entity.getX();
			double y = entity.getY();
			double z = entity.getZ();

			// 弓を引いていた時間を計算
			int charge = this.getUseDuration(itemstack) - timeLeft;
			float force = getArrowVelocity(charge);

			// 引き絞りが十分かどうかチェック
			if (force >= 0.1) {
				TestBowEntity entityarrow = TestBowEntity.shoot(world, entity, world.getRandom(), force, 0, 0);
				itemstack.hurtAndBreak(1, entity, e -> e.broadcastBreakEvent(entity.getUsedItemHand()));
				entityarrow.pickup = AbstractArrow.Pickup.DISALLOWED;

				// 追加のカスタムプロシージャーを実行
				TestBowRangedItemUsedProcedure.execute(world, x, y, z, entity);
			}
		}
	}

	// 引き絞りに応じた矢の速度を計算
	public static float getArrowVelocity(int charge) {
		float velocity = (float) charge / 20.0F; // 引き絞り時間を基に計算
		velocity = (velocity * velocity + velocity * 2.0F) / 3.0F; // 非線形で力を調整

		if (velocity > 1.0F) {
			velocity = 1.0F; // 最大速度を制限
		}

		return velocity;
	}
	
}
