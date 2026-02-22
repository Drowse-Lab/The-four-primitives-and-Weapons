package minecraftarmorweapon.procedures;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import java.util.UUID;
import java.util.List;

public class SummonTriggerEffectEffectExpiresProcedure {
	public static void execute(Entity entity) {
		if (entity == null) return;
		
		UUID entityUUID = entity.getUUID();
		
		// スポーン済みリストから削除
		List<UUID> spawnedList = SummonTriggerEffectEffectStartedappliedProcedure.getSpawnedEntities(entityUUID);
		if (spawnedList != null && VersionHelper.getLevel(entity) instanceof ServerLevel) {
			ServerLevel world = (ServerLevel) VersionHelper.getLevel(entity);
			
			// スポーンしたエンティティを全て削除
			for (UUID spawnedUUID : spawnedList) {
				Entity spawnedEntity = world.getEntity(spawnedUUID);
				if (spawnedEntity != null && spawnedEntity.isAlive()) {
					spawnedEntity.discard();
				}
			}
		}
		
		// リストをクリア
		SummonTriggerEffectEffectStartedappliedProcedure.clearSpawnedEntities(entityUUID);
	}
}