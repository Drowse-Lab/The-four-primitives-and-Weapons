package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class NgsposiyonXiaoGuogaQieretaShiProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double r = 0;
		if (false) {
			{
				final Vec3 _center = new Vec3(x, y, z);
				// 軽量化: 50 → 16 ブロック半径に縮小し、 ストリームソート前に predicate で
				//   "gyamigyapitonndeyaru == 1" マーカー持ちだけにフィルタしてから処理。
				//   全エンティティ走査の数百件 → 数件規模に。
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class,
						new AABB(_center, _center).inflate(16 / 2d),
						e -> e.getPersistentData().getDouble("gyamigyapitonndeyaru") == 1)
					.stream()
					.sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
					.limit(8) // 念のため上限 8 体
					.collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (entityiterator.getPersistentData().getDouble("gyamigyapitonndeyaru") == 1) {
						{
							Entity _ent = entity;
							_ent.teleportTo((entity.getX() + r * entity.getLookAngle().x), (entity.getY() + 1.5 + r * entity.getLookAngle().y), (entity.getZ() + r * entity.getLookAngle().z));
							if (_ent instanceof ServerPlayer _serverPlayer)
								_serverPlayer.connection.teleport((entity.getX() + r * entity.getLookAngle().x), (entity.getY() + 1.5 + r * entity.getLookAngle().y), (entity.getZ() + r * entity.getLookAngle().z), _ent.getYRot(), _ent.getXRot());
						}
						if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
							_entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 4, true, false));
					}
				}
			}
		}
	}
}
