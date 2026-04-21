package minecraftarmorweapon.mana;

import net.minecraft.world.entity.player.Player;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Manaの自然回復。サーバーTickで毎tick MANA_REGEN_PER_TICK回復。
 */
@Mod.EventBusSubscriber
public class ManaRegenHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.side.isClient()) return;
        Player p = event.player;
        if (p.isSpectator()) return;
        // 4tick毎にまとめて回復 (0.1/tick → 0.4/4tick で同量。アトリビュート書込を1/4に)
        if (p.tickCount % 4 != 0) return;
        double cur = ManaHelper.getMana(p);
        if (cur >= ManaHelper.MANA_MAX) return;
        ManaHelper.addMana(p, ManaHelper.MANA_REGEN_PER_TICK * 4);
    }
}
