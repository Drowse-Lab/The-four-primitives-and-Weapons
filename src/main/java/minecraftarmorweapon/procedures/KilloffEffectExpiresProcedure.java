package minecraftarmorweapon.procedures;

import net.minecraft.world.entity.Entity;

public class KilloffEffectExpiresProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putBoolean("minecraft_armor_weapon:armor_stand_tobasu_kill_off", false);
	}
}
