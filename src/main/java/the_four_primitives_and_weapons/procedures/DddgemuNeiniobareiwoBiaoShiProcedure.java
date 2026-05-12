package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.entity.Entity;

import the_four_primitives_and_weapons.network.TheFourPrimitivesAndWeaponsModVariables;

public class DddgemuNeiniobareiwoBiaoShiProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		return (entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).ddd;
	}
}
