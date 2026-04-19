package minecraftarmorweapon.mana;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * グレイズ — 弾幕系ゲームの「ギリギリ回避」。
 *
 * 1) 飛翔体が0.8〜2.0ブロック以内を通過した場合 → グレイズ判定 (+MP)
 * 2) 被弾せずに直前(8tick以内)で攻撃モーション/接触をスレ違った場合 → グレイズ
 *
 * 同じ飛翔体IDから連続加算しないようクールダウン管理。
 */
@Mod.EventBusSubscriber
public class GrazeHandler {

    private static final double GRAZE_OUTER = 2.0;   // この距離以内 = グレイズ
    private static final double GRAZE_INNER = 0.6;   // この距離以内 = 命中扱い (グレイズ判定しない)
    private static final double GRAZE_MP_GAIN = 4.0;
    private static final int    PER_PROJECTILE_COOLDOWN_TICKS = 20;

    /** 飛翔体ID → 最後にグレイズしたtick (連続加算防止) */
    private static final Map<Integer, Integer> grazedAt = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.side.isClient()) return;
        Player p = event.player;
        if (p.isSpectator() || p.isCreative()) return;

        int now = p.tickCount;

        // プレイヤー周囲にあるProjectileをチェック
        for (Projectile proj : p.level().getEntitiesOfClass(Projectile.class,
                p.getBoundingBox().inflate(GRAZE_OUTER))) {
            if (!proj.isAlive()) continue;
            if (proj.getOwner() == p) continue;
            double d = Math.sqrt(p.distanceToSqr(proj));
            if (d < GRAZE_INNER || d > GRAZE_OUTER) continue;

            int last = grazedAt.getOrDefault(proj.getId(), -9999);
            if (now - last < PER_PROJECTILE_COOLDOWN_TICKS) continue;
            grazedAt.put(proj.getId(), now);

            triggerGraze(p, "§d✦ グレイズ");
        }

        // 古いエントリを掃除 (たまにでよい)
        if (now % 200 == 0) {
            grazedAt.entrySet().removeIf(e -> now - e.getValue() > 200);
        }
    }

    /**
     * 被弾しなかった攻撃 (キャンセル/回避) もグレイズ扱い。
     * LivingHurtEventでダメージが0になった or amountが0の場合に発火。
     */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (p.level().isClientSide) return;
        if (p.isCreative() || p.isSpectator()) return;
        // ダメージが極小 (0.5未満) → ほぼ回避と見なす
        if (event.getAmount() >= 0.5f) return;
        triggerGraze(p, "§d✦ グレイズ (回避)");
    }

    private static void triggerGraze(Player p, String label) {
        ManaHelper.addMana(p, GRAZE_MP_GAIN);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 2.0f);
        if (p instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal(label + " §7+§b" + (int)GRAZE_MP_GAIN + " MP"), true);
        }
    }
}
