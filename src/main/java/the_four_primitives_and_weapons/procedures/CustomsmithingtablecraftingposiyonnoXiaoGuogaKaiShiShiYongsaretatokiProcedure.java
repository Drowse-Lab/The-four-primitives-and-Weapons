package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.entity.Entity;

public class CustomsmithingtablecraftingposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putBoolean("the_four_primitives_and_weapons:custom_smithing_table_crafting", true);
	}
}
