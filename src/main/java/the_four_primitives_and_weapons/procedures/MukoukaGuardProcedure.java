package the_four_primitives_and_weapons.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import net.minecraft.world.entity.Entity;

import the_four_primitives_and_weapons.entity.OtiruyoEntity;
import the_four_primitives_and_weapons.entity.KillotiruEntity;
import the_four_primitives_and_weapons.entity.BlackholeEntity;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class MukoukaGuardProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingAttackEvent event) {
		if (event != null && event.getEntity() != null) {
			execute(event, event.getEntity());
		}
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof BlackholeEntity) {
			if (event != null && event.isCancelable()) {
				event.setCanceled(true);
			}
		}
		if (entity instanceof KillotiruEntity) {
			if (event != null && event.isCancelable()) {
				event.setCanceled(true);
			}
		}
		if (entity instanceof OtiruyoEntity) {
			if (event != null && event.isCancelable()) {
				event.setCanceled(true);
			}
		}
	}
}
