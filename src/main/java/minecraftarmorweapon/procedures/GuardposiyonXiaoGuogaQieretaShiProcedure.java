package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;

public class GuardposiyonXiaoGuogaQieretaShiProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		if (world instanceof ServerLevel _level) {
			GuardposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure.killAllGuardArmorStands(_level);
		}
	}
}
