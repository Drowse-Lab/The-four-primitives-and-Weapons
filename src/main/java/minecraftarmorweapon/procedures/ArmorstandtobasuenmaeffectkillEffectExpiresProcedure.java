package minecraftarmorweapon.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

public class ArmorstandtobasuenmaeffectkillEffectExpiresProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (!(entity instanceof Player)) {
			if (!entity.level().isClientSide())
				entity.discard();
		}
	}
}
