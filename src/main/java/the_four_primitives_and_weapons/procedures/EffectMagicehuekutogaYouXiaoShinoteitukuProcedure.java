package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class EffectMagicehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(5 / 2d), entityiterator -> entityiterator instanceof Mob || entityiterator instanceof ItemEntity
					|| entityiterator instanceof Arrow || entityiterator instanceof SpectralArrow || entityiterator instanceof ThrownTrident || entityiterator instanceof LargeFireball
					|| entityiterator instanceof DragonFireball || entityiterator instanceof Snowball || entityiterator instanceof ThrownEgg || entityiterator instanceof SmallFireball);
			for (Entity entityiterator : _entfound) {
					if (entityiterator.getPersistentData().getBoolean("Check") == false) {
						entityiterator.getPersistentData().putBoolean("Check", true);
						if (entityiterator.distanceToSqr(entity) <= 4) {
							entityiterator.getPersistentData().putBoolean("My arrow?", true);
						} else {
							entityiterator.getPersistentData().putBoolean("My arrow?", false);
						}
					}
					if (entityiterator.getPersistentData().getBoolean("My arrow?") == false) {
						entityiterator.setDeltaMovement(new Vec3(0, 0, 0));
					}
			}
		}
	}
}
