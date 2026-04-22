package minecraftarmorweapon.mana;

import minecraftarmorweapon.compat.SpellbooksCompat;

import net.minecraft.world.entity.player.Player;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Manaの自然回復。サーバーTickで毎tick MANA_REGEN_PER_TICK回復。
 * Iron's Spellbooks が入っていれば向こうが独自の回復ロジックを持つので、
 * 二重回復を避けるため本ハンドラはスキップする。
 */
@Mod.EventBusSubscriber
public class ManaRegenHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.side.isClient()) return;
        // Iron's Spellbooks 側が回復を担当する
        if (SpellbooksCompat.isLoaded()) return;
        Player p = event.player;
        if (p.isSpectator()) return;
        // 4tick毎にまとめて回復 (0.1/tick → 0.4/4tick で同量。アトリビュート書込を1/4に)
        if (p.tickCount % 4 != 0) return;
        double cur = ManaHelper.getMana(p);
        if (cur >= ManaHelper.MANA_MAX) return;
        ManaHelper.addMana(p, ManaHelper.MANA_REGEN_PER_TICK * 4);
    }
}
