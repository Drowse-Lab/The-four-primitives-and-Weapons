package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.entity.Entity;

public class KilloffEffectExpiresProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putBoolean("the_four_primitives_and_weapons:armor_stand_tobasu_kill_off", false);
	}
}
