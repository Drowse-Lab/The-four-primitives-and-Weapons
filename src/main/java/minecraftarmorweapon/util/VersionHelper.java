package minecraftarmorweapon.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Cross-version compatibility helper.
 * In 1.19.2: Entity.level is a public field, Entity.level() does not exist.
 * In 1.20.1: Entity.level() method was added, Entity.level field is deprecated.
 * This helper uses the field access which works in both versions.
 */
public final class VersionHelper {

	@SuppressWarnings("deprecation")
	public static Level getLevel(Entity entity) {
		return entity.level;
	}

	private VersionHelper() {}
}
