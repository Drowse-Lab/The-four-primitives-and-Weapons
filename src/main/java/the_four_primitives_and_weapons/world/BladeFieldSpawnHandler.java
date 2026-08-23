package the_four_primitives_and_weapons.world;

import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

/** 剣の原では自然発生だけを止める。エッグ・コマンド・スポナー等による召喚は許可する。 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public final class BladeFieldSpawnHandler {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventNaturalSpawns(MobSpawnEvent.PositionCheck event) {
        if (!event.getLevel().getLevel().dimension().equals(BladeDimensionTravelHandler.BLADE_FIELD)) return;
        MobSpawnType type = event.getSpawnType();
        if (type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION || type == MobSpawnType.PATROL)
            event.setResult(Event.Result.DENY);
    }

    private BladeFieldSpawnHandler() {}
}
