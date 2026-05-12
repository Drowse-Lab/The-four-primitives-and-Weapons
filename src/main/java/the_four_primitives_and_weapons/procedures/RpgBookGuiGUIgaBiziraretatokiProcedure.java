package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.entity.Entity;

public class RpgBookGuiGUIgaBiziraretatokiProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity.getPersistentData().getBoolean("the_four_primitives_and_weapons:QuiverItemsyorisunna") == true) {
			entity.getPersistentData().putBoolean("the_four_primitives_and_weapons:QuiverItemsyorisunna", false);
		}
	}
}
