package minecraftarmorweapon.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.commands.arguments.EntityAnchorArgument;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class KarikarisiterunemaidomaidoehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double pitch = 0;
		double yaw = 0;
		double dis = 0;
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(100 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (entityiterator instanceof Mob) {
					if (entity.getPersistentData().getDouble("Z") <= entityiterator.getZ()) {
						yaw = Math.toDegrees(Math.atan((entityiterator.getX() - entity.getPersistentData().getDouble("X")) / (entityiterator.getZ() - entity.getPersistentData().getDouble("Z")))) * (-1)
								+ Mth.nextDouble(RandomSource.create(), -70, 70);
						pitch = Math
								.toDegrees(Math.atan(((entityiterator.getY() - entity.getPersistentData().getDouble("Y")) - 1)
										/ Math.sqrt(Math.pow(entityiterator.getX() - entity.getPersistentData().getDouble("X"), 2) + Math.pow(entityiterator.getZ() - entity.getPersistentData().getDouble("Z"), 2))))
								* (-1) + Mth.nextDouble(RandomSource.create(), -70, 70);
					} else {
						yaw = (Math.toDegrees(Math.atan((entityiterator.getX() - entity.getPersistentData().getDouble("X")) / (entityiterator.getZ() - entity.getPersistentData().getDouble("Z")))) + 180) * (-1)
								+ Mth.nextDouble(RandomSource.create(), -70, 70);
						pitch = Math
								.toDegrees(Math.atan(((entityiterator.getY() - entity.getPersistentData().getDouble("Y")) - 1)
										/ Math.sqrt(Math.pow(entityiterator.getX() - entity.getPersistentData().getDouble("X"), 2) + Math.pow(entityiterator.getZ() - entity.getPersistentData().getDouble("Z"), 2))))
								* (-1) + Mth.nextDouble(RandomSource.create(), -70, 70);
					}
					for (int index0 = 0; index0 < Mth.nextInt(RandomSource.create(), 4, 8); index0++) {
						if (world instanceof ServerLevel _level)
							_level.sendParticles(ParticleTypes.SQUID_INK, (entity.getPersistentData().getDouble("X") + Math.sin(yaw / ((-180) / Math.PI)) * Math.cos(pitch / ((-180) / Math.PI)) * dis),
									(entity.getPersistentData().getDouble("Y") + Math.sin(pitch / ((-180) / Math.PI)) * dis),
									(entity.getPersistentData().getDouble("Z") + Math.cos(yaw / ((-180) / Math.PI)) * Math.cos(pitch / ((-180) / Math.PI)) * dis), 4, 0.01, 0.01, 0.01, 0);
						dis = dis + 0.5;
					}
					entity.getPersistentData().putDouble("X", (entity.getPersistentData().getDouble("X") + Math.sin(yaw / ((-180) / Math.PI)) * Math.cos(pitch / ((-180) / Math.PI)) * dis));
					entity.getPersistentData().putDouble("Y", (entity.getPersistentData().getDouble("Y") + Math.sin(pitch / ((-180) / Math.PI)) * dis));
					entity.getPersistentData().putDouble("Z", (entity.getPersistentData().getDouble("Z") + Math.cos(yaw / ((-180) / Math.PI)) * Math.cos(pitch / ((-180) / Math.PI)) * dis));
				}
			}
		}
		entity.setDeltaMovement(new Vec3(0, 0, 0));
		{
			final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("X")), (entity.getPersistentData().getDouble("Y")), (entity.getPersistentData().getDouble("Z")));
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(2 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (entityiterator instanceof Mob) {
					entityiterator.hurt(DamageSource.GENERIC, 5);
					if (!entity.level.isClientSide())
						entity.discard();
				}
			}
		}
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(100 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (entityiterator instanceof Mob) {
					entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ())));
				}
			}
		}
	}
}
