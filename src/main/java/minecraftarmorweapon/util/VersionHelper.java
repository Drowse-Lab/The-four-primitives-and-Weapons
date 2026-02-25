package minecraftarmorweapon.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Cross-version compatibility helper.
 * In 1.20.1: Entity.level() method is the standard API.
 */
public final class VersionHelper {

	public static Level getLevel(Entity entity) {
		return entity.level();
	}

	private VersionHelper() {}
}
