package minecraftarmorweapon.procedures;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import minecraftarmorweapon.entity.FlyingAttackerEntity;
import minecraftarmorweapon.entity.KatanaTobuEntity;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEntities;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;

public class SummonTriggerEffectEffectStartedappliedProcedure {
	public static void execute(Entity entity) {
		if (entity == null || !(entity.level instanceof ServerLevel)) return;
		ServerLevel world = (ServerLevel) entity.level;

		// 召喚者の利き手のアイテムを確認
		if (entity instanceof LivingEntity) {
			LivingEntity livingEntity = (LivingEntity) entity;
			ItemStack mainHandItem = livingEntity.getItemInHand(InteractionHand.MAIN_HAND);
			
			// アイテムを持っていない場合は何もしない
			if (mainHandItem.isEmpty()) {
				return;
			}
			
			// アイテムが剣か発射体かチェック
			if (mainHandItem.getItem() instanceof SwordItem) {
				// 剣の場合はFlyingAttackerを召喚
				FlyingAttackerEntity mob = new FlyingAttackerEntity(
					MinecraftArmorWeaponModEntities.FLYING_ATTACKER.get(), world
				);

				mob.moveTo(entity.getX(), entity.getY() + 2, entity.getZ(), entity.getYRot(), 0);
				mob.setInvisible(true);
				mob.setDisplayItem(mainHandItem);
				mob.setNoGravity(true);
				
				// 召喚者を設定
				mob.setOwner(livingEntity);
				mob.setOwnerUUID(entity.getUUID());
				
				// ターゲットの設定
				LivingEntity target = findTarget(entity, livingEntity);
				if (target != null && target != livingEntity && target.isAlive()) {
					mob.setTargetUUID(target.getUUID());
				}
				
				world.addFreshEntity(mob);
			} else if (isProjectileItem(mainHandItem)) {
				// 矢や発射体アイテムの場合は、敵がいるときのみ召喚
				// ターゲットの検索
				LivingEntity target = findTarget(entity, livingEntity);
				
				// ターゲットが存在する場合のみ召喚
				if (target != null && target != livingEntity && target.isAlive()) {
					// 矢を放つFlyingAttackerを召喚（矢射撃モード）
					FlyingAttackerEntity arrowShooter = new FlyingAttackerEntity(
						MinecraftArmorWeaponModEntities.FLYING_ATTACKER.get(), world
					);

					arrowShooter.moveTo(entity.getX(), entity.getY() + 2, entity.getZ(), entity.getYRot(), 0);
					arrowShooter.setInvisible(true);
					arrowShooter.setNoGravity(true);
					
					// 召喚者を設定
					arrowShooter.setOwner(livingEntity);
					arrowShooter.setOwnerUUID(entity.getUUID());
					
					// 矢を表示用にセット
					arrowShooter.setDisplayItem(mainHandItem);
					
					// 射撃モードフラグを設定
					arrowShooter.getPersistentData().putBoolean("ArrowShootMode", true);
					arrowShooter.getPersistentData().putString("ArrowType", mainHandItem.getItem().toString());
					
					// ターゲットを設定
					arrowShooter.setTargetUUID(target.getUUID());
					
					world.addFreshEntity(arrowShooter);
				}
			}
			
			return;
		}
	}
	
	// アイテムが発射体かどうかチェック
	private static boolean isProjectileItem(ItemStack stack) {
		// トライデント
		if (stack.getItem() instanceof TridentItem) {
			return true;
		}
		
		// バニラの矢
		if (stack.getItem() == Items.ARROW || 
			stack.getItem() == Items.SPECTRAL_ARROW || 
			stack.getItem() == Items.TIPPED_ARROW) {
			return true;
		}
		
		// カスタム矢アイテムのチェック（アイテム名に"arrow"が含まれるもの）
		String itemName = stack.getItem().toString().toLowerCase();
		if (itemName.contains("arrow") || itemName.contains("bolt")) {
			return true;
		}
		
		// 発射体タグのチェック
		if (stack.is(ItemTags.ARROWS)) {
			return true;
		}
		
		return false;
	}
	
	// ターゲット検索ロジックを共通化
	private static LivingEntity findTarget(Entity entity, LivingEntity livingEntity) {
		LivingEntity target = null;
		
		// まず、エンティティが既に敵対しているターゲットを確認
		if (livingEntity instanceof Mob) {
			Mob mobEntity = (Mob) livingEntity;
			target = mobEntity.getTarget();
		}
		
		// ターゲットがいない場合、視線の先のエンティティを確認
		if (target == null) {
			if (entity instanceof Player) {
				Player player = (Player) entity;
				target = getPlayerLookingAt(player, 32.0D);
			} else if (livingEntity instanceof Mob) {
				// モブの場合も視線の先を確認
				target = getMobLookingAt(livingEntity, 32.0D);
			}
		}
		
		return target;
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
	
	// モブが見ているLivingEntityを取得
	private static LivingEntity getMobLookingAt(LivingEntity mob, double range) {
		Vec3 eyePos = mob.getEyePosition(1.0F);
		Vec3 lookVec = mob.getViewVector(1.0F);
		Vec3 endPos = eyePos.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);
		
		// レイキャストで最も近いエンティティを取得
		HitResult hitResult = mob.level.clip(new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mob));
		Vec3 actualEndPos = hitResult.getLocation();
		
		// エンティティを検索
		AABB searchBox = new AABB(eyePos, actualEndPos).inflate(1.0D);
		double closestDistance = range * range;
		LivingEntity closestEntity = null;
		
		for (Entity entity : mob.level.getEntities(mob, searchBox)) {
			if (entity instanceof LivingEntity && entity.isAlive() && entity != mob) {
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