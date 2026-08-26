package the_four_primitives_and_weapons.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.entity.LunaCompanionEntity;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModEntities;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public final class LunaCompanionHandler {
    private LunaCompanionHandler() {}

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (event.getEntity().getItem().getItem() != TheFourPrimitivesAndWeaponsModItems.LUNA.get()
                || !(event.getPlayer().level() instanceof ServerLevel level)) return;
        LunaCompanionEntity luna = TheFourPrimitivesAndWeaponsModEntities.LUNA_COMPANION.get().create(level);
        if (luna == null) return;
        luna.bind(event.getPlayer(), event.getEntity().getItem());
        luna.moveTo(event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getPlayer().getYRot(), 0.0F);
        event.setCanceled(true);
        event.getEntity().discard();
        level.addFreshEntity(luna);
    }
}
