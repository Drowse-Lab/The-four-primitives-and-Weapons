package minecraftarmorweapon.procedures;

import minecraftarmorweapon.util.VersionHelper;

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
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class SummonTriggerEffectEffectStartedappliedProcedure {
	// エンティティごとにスポーン済みのFlyingAttackerを追跡
	private static final Map<UUID, List<UUID>> spawnedEntitiesMap = new HashMap<>();
	
	// 外部からアクセスできるようにメソッドを追加
	public static List<UUID> getSpawnedEntities(UUID entityUUID) {
		return spawnedEntitiesMap.get(entityUUID);
	}
	
	public static void clearSpawnedEntities(UUID entityUUID) {
		spawnedEntitiesMap.remove(entityUUID);
	}
	
	public static void execute(Entity entity) {
		execute(entity, 0);
	}
	
	public static void execute(Entity entity, int effectLevel) {
		if (entity == null || !(VersionHelper.getLevel(entity) instanceof ServerLevel)) return;
		ServerLevel world = (ServerLevel) VersionHelper.getLevel(entity);
		
		// エフェクトレベルに基づいて召喚数を決定（レベル0なら1体、レベル1なら2体、など）
		int spawnCount = effectLevel + 1;
		
		// すでにこのエンティティがFlyingAttackerをスポーン済みかチェック
		UUID entityUUID = entity.getUUID();
		if (spawnedEntitiesMap.containsKey(entityUUID)) {
			// スポーン済みのエンティティが生きているかチェック
			List<UUID> spawnedList = spawnedEntitiesMap.get(entityUUID);
			int aliveCount = 0;
			for (UUID spawnedUUID : spawnedList) {
				Entity spawnedEntity = ((ServerLevel) VersionHelper.getLevel(entity)).getEntity(spawnedUUID);
				if (spawnedEntity != null && spawnedEntity.isAlive()) {
					aliveCount++;
				}
			}
			// 必要な数だけ生きている場合は新規スポーンしない
			if (aliveCount >= spawnCount) {
				return;
			}
			// 追加でスポーンが必要な場合は差分だけスポーン
			spawnCount = spawnCount - aliveCount;
		}

		// 召喚者の利き手のアイテムを確認
		if (entity instanceof LivingEntity) {
			LivingEntity livingEntity = (LivingEntity) entity;
			ItemStack mainHandItem = livingEntity.getItemInHand(InteractionHand.MAIN_HAND);
			
			// アイテムを持っていない場合は何もしない
			if (mainHandItem.isEmpty()) {
				return;
			}
			
			// スポーン済みリストを取得または作成
			List<UUID> spawnedList = spawnedEntitiesMap.getOrDefault(entityUUID, new ArrayList<>());
			
			// 指定数だけFlyingAttackerを召喚
			for (int i = 0; i < spawnCount; i++) {
				// アイテムが剣か発射体かチェック
				if (mainHandItem.getItem() instanceof SwordItem) {
					// 剣の場合はFlyingAttackerを召喚
					FlyingAttackerEntity mob = new FlyingAttackerEntity(
						MinecraftArmorWeaponModEntities.FLYING_ATTACKER.get(), world
					);

					// 召喚者の周りのランダムな位置に配置（空いている場所を探す）
					double angle = world.random.nextFloat() * Math.PI * 2;
					double distance = 2.0 + world.random.nextFloat() * 2.0;
					double spawnX = entity.getX() + Math.cos(angle) * distance;
					double spawnY = entity.getY() + 2;
					double spawnZ = entity.getZ() + Math.sin(angle) * distance;
					
					// スポーン位置が安全か確認（ブロックにめり込まないように）
					for (int yOffset = 0; yOffset <= 3; yOffset++) {
						if (!world.getBlockState(new net.minecraft.core.BlockPos(spawnX, spawnY + yOffset, spawnZ)).getMaterial().isSolid()) {
							spawnY = spawnY + yOffset;
							break;
						}
					}
					
					mob.moveTo(spawnX, spawnY, spawnZ, entity.getYRot(), entity.getXRot());
					mob.setInvisible(true);
					mob.setDisplayItem(mainHandItem.copy());  // 明示的にコピーを渡す
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
					
					// スポーン済みリストに追加
					spawnedList.add(mob.getUUID());
				} else if (isProjectileItem(mainHandItem)) {
					// 矢や発射体アイテムの場合
					// デバッグ用ログ
					System.out.println("Projectile item detected: " + mainHandItem.getItem());
					
					// 矢を放つFlyingAttackerを召喚（矢射撃モード）
					FlyingAttackerEntity arrowShooter = new FlyingAttackerEntity(
						MinecraftArmorWeaponModEntities.FLYING_ATTACKER.get(), world
					);

					// 召喚者の周りのランダムな位置に配置（空いている場所を探す）
					double angleArrow = world.random.nextFloat() * Math.PI * 2;
					double distanceArrow = 2.0 + world.random.nextFloat() * 2.0;
					double spawnXArrow = entity.getX() + Math.cos(angleArrow) * distanceArrow;
					double spawnYArrow = entity.getY() + 2;
					double spawnZArrow = entity.getZ() + Math.sin(angleArrow) * distanceArrow;
					
					// スポーン位置が安全か確認（ブロックにめり込まないように）
					for (int yOffset = 0; yOffset <= 3; yOffset++) {
						if (!world.getBlockState(new net.minecraft.core.BlockPos(spawnXArrow, spawnYArrow + yOffset, spawnZArrow)).getMaterial().isSolid()) {
							spawnYArrow = spawnYArrow + yOffset;
							break;
						}
					}
					
					arrowShooter.moveTo(spawnXArrow, spawnYArrow, spawnZArrow, entity.getYRot(), entity.getXRot());
					arrowShooter.setInvisible(true);
					arrowShooter.setNoGravity(true);
					
					// 召喚者を設定
					arrowShooter.setOwner(livingEntity);
					arrowShooter.setOwnerUUID(entity.getUUID());
					
					// 矢を表示用にセット
					arrowShooter.setDisplayItem(mainHandItem.copy());  // 明示的にコピーを渡す
					
					// 射撃モードフラグを設定
					arrowShooter.getPersistentData().putBoolean("ArrowShootMode", true);
					arrowShooter.getPersistentData().putString("ArrowType", mainHandItem.getItem().toString());
					
					// ターゲットの検索
					LivingEntity target = findTarget(entity, livingEntity);
					
					// デバッグ用ログ
					if (target == null) {
						System.out.println("No target found for projectile - will wait for target");
					} else {
						System.out.println("Target found: " + target.getClass().getSimpleName() + " at " + target.position());
						// ターゲットを設定
						arrowShooter.setTargetUUID(target.getUUID());
					}
					
					world.addFreshEntity(arrowShooter);
					
					// スポーン済みリストに追加
					spawnedList.add(arrowShooter.getUUID());
				}
			}
			
			// スポーン済みマップを更新
			spawnedEntitiesMap.put(entityUUID, spawnedList);
			
			return;
		}
	}
	
	// アイテムが発射体かどうかチェック
	private static boolean isProjectileItem(ItemStack stack) {
		// デバッグ：アイテム名を表示
		System.out.println("Checking if item is projectile: " + stack.getItem());
		
		// トライデント
		if (stack.getItem() instanceof TridentItem) {
			System.out.println("Item is a trident");
			return true;
		}
		
		// バニラの矢
		if (stack.getItem() == Items.ARROW || 
			stack.getItem() == Items.SPECTRAL_ARROW || 
			stack.getItem() == Items.TIPPED_ARROW) {
			System.out.println("Item is a vanilla arrow");
			return true;
		}
		
		// カスタム矢アイテムのチェック（アイテム名に"arrow"が含まれるもの）
		String itemName = stack.getItem().toString().toLowerCase();
		if (itemName.contains("arrow") || itemName.contains("bolt")) {
			System.out.println("Item name contains 'arrow' or 'bolt': " + itemName);
			return true;
		}
		
		// 発射体タグのチェック
		if (stack.is(ItemTags.ARROWS)) {
			System.out.println("Item has ARROWS tag");
			return true;
		}
		
		System.out.println("Item is NOT a projectile");
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