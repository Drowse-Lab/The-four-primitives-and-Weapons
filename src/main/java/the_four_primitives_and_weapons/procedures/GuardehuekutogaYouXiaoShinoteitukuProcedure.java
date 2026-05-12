package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

public class GuardehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (world instanceof ServerLevel _level) {
			for (Entity e : _level.getAllEntities()) {
				if (e instanceof ArmorStand stand && stand.getTags().contains("the_four_primitives_and_weapons_guard_bind")) {
					float newYRot = stand.getYRot() + 40;
					stand.setYRot(newYRot);
					stand.setYBodyRot(newYRot);
					stand.setYHeadRot(newYRot);
				}
			}
		}
		{
			Entity _ent = entity;
			_ent.teleportTo((entity.getPersistentData().getDouble("the_four_primitives_and_weapons:muteki_x_chuzume")), (entity.getPersistentData().getDouble("the_four_primitives_and_weapons:muteki_y_chuzume")),
					(entity.getPersistentData().getDouble("the_four_primitives_and_weapons:muteki_z_chuzume")));
			if (_ent instanceof ServerPlayer _serverPlayer)
				_serverPlayer.connection.teleport((entity.getPersistentData().getDouble("the_four_primitives_and_weapons:muteki_x_chuzume")), (entity.getPersistentData().getDouble("the_four_primitives_and_weapons:muteki_y_chuzume")),
						(entity.getPersistentData().getDouble("the_four_primitives_and_weapons:muteki_z_chuzume")), _ent.getYRot(), _ent.getXRot());
		}
	}
}
