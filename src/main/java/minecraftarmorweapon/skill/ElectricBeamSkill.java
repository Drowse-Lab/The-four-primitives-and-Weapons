package minecraftarmorweapon.skill;

import minecraftarmorweapon.block.ElectricConductBlock;
import minecraftarmorweapon.damage.ElementType;
import minecraftarmorweapon.damage.ElectricElementDamageHandler;
import minecraftarmorweapon.util.VersionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * ELECTRIC beam skill (Forge API version)
 */
public final class ElectricBeamSkill {

	private ElectricBeamSkill() {}

	public static void fire(Player player) {
		Level level = VersionHelper.getLevel(player);
		if (level.isClientSide()) return;

		Vec3 start = player.getEyePosition();
		Vec3 direction = player.getLookAngle().normalize();

		double maxDistance = 50.0;
		double damage = 8.0;

		Vec3 end = start.add(direction.scale(maxDistance));

		// ray trace for blocks
		BlockHitResult blockHit = level.clip(new ClipContext(
				start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

		Vec3 reachEnd = (blockHit.getType() != HitResult.Type.MISS)
				? blockHit.getLocation()
				: end;

		// check entities along the beam
		AABB searchArea = new AABB(start, reachEnd).inflate(1.0);
		List<Entity> entities = level.getEntities(player, searchArea,
				e -> e instanceof LivingEntity && e.isAlive() && e != player);

		LivingEntity hitEntity = null;
		double closestDist = maxDistance * maxDistance;

		for (Entity entity : entities) {
			Optional<Vec3> hitVec = entity.getBoundingBox().clip(start, reachEnd);
			if (hitVec.isPresent()) {
				double dist = start.distanceToSqr(hitVec.get());
				if (dist < closestDist) {
					closestDist = dist;
					hitEntity = (LivingEntity) entity;
				}
			}
		}

		// beam particles
		if (level instanceof ServerLevel serverLevel) {
			Vec3 particleEnd = (hitEntity != null) ? hitEntity.position().add(0, hitEntity.getBbHeight() / 2, 0) : reachEnd;
			Vec3 diff = particleEnd.subtract(start);
			double length = diff.length();
			Vec3 step = diff.normalize().scale(0.5);
			Vec3 pos = start;

			for (double d = 0; d < length; d += 0.5) {
				serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
						pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
				pos = pos.add(step);
			}
		}

		// entity hit
		if (hitEntity != null) {
			ElectricElementDamageHandler.apply(hitEntity, damage, ElementType.ELECTRIC);
		}

		// block hit - conduct electricity
		if (blockHit.getType() != HitResult.Type.MISS) {
			BlockPos hitPos = blockHit.getBlockPos();
			ElectricConductBlock.conduct(level, hitPos, damage);
		}
	}
}
