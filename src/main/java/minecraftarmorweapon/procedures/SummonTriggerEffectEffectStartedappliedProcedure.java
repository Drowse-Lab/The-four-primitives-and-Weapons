package minecraftarmorweapon.procedures;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

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
		
		// 召喚者を設定
		if (entity instanceof LivingEntity) {
			mob.setOwner((LivingEntity) entity);
			
			// プレイヤーが見ているエンティティを攻撃対象に設定
			if (entity instanceof Player) {
				Player player = (Player) entity;
				LivingEntity target = getPlayerLookingAt(player, 32.0D);
				if (target != null && target != player) {
					mob.setDesignatedTarget(target);
				}
			}
		}

		world.addFreshEntity(mob);
	}
	
	// プレイヤーが見ているLivingEntityを取得
	private static LivingEntity getPlayerLookingAt(Player player, double range) {
		Vec3 eyePos = player.getEyePosition(1.0F);
		Vec3 lookVec = player.getViewVector(1.0F);
		Vec3 endPos = eyePos.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);
		
		// レイキャストで最も近いエンティティを取得
		HitResult hitResult = player.level.clip(new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		Vec3 actualEndPos = hitResult.getLocation();
		
		// エンティティを検索
		AABB searchBox = new AABB(eyePos, actualEndPos).inflate(1.0D);
		double closestDistance = range * range;
		LivingEntity closestEntity = null;
		
		for (Entity entity : player.level.getEntities(player, searchBox)) {
			if (entity instanceof LivingEntity && entity.isAlive()) {
				AABB entityBox = entity.getBoundingBox().inflate(0.3D);
				if (entityBox.clip(eyePos, actualEndPos).isPresent()) {
					double distance = entity.distanceToSqr(eyePos);
					if (distance < closestDistance) {
						closestDistance = distance;
						closestEntity = (LivingEntity) entity;
					}
				}
			}
		}
		
		return closestEntity;
	}
}