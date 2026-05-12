package the_four_primitives_and_weapons.mana;

import the_four_primitives_and_weapons.compat.SpellbooksCompat;

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
        // 4tick毎にまとめて回復 (書込を1/4に)。最大値・回復速度は attribute 経由で取得。
        if (p.tickCount % 4 != 0) return;
        double cur = ManaHelper.getMana(p);
        double max = ManaHelper.maxMana(p);
        if (cur >= max) return;
        ManaHelper.addMana(p, ManaHelper.regenPerTick(p) * 4);
    }
}
