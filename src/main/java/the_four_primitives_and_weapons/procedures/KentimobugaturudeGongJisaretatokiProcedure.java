package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;

public class KentimobugaturudeGongJisaretatokiProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.setCustomName(Component.literal("test"));
	}
}
