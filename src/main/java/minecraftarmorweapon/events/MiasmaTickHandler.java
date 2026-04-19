package minecraftarmorweapon.events;

import minecraftarmorweapon.damage.MiasmaElementDamageHandler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 瘴気の持続時間タイマーを毎tick更新するハンドラー。
 * 時間切れになったエンティティを miasmaMap から自動削除する。
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class MiasmaTickHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MiasmaElementDamageHandler.miasmaMap.entrySet().removeIf(entry -> {
            MiasmaElementDamageHandler.MiasmaEntry miasma = entry.getValue();
            miasma.remainingTick--;
            return miasma.remainingTick <= 0;
        });
    }
}
