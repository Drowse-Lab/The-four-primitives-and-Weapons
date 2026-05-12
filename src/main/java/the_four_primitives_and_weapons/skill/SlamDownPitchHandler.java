package the_four_primitives_and_weapons.skill;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 振り下ろし技 (slam_down) 用のピッチ (上下視点) アニメーション。
 * 視点を上 (-START_PITCH) → 下 (+END_PITCH) にスムーズに動かす。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public class SlamDownPitchHandler {

    private static final Map<UUID, PitchSession> ACTIVE = new ConcurrentHashMap<>();

    /** 開始時の pitch (= 視点が上向き)。負の値 = 上。 */
    private static final float START_PITCH = -35f;
    /** 終了時の pitch (= 視点が下向き)。正の値 = 下。 */
    private static final float END_PITCH = 55f;
    /** アニメーション総 tick 数 (短い斬撃感を出すため小さめ)。 */
    private static final int TOTAL_TICKS = 5;

    public static void start(Player player) {
        if (player == null) return;
        ACTIVE.put(player.getUUID(), new PitchSession(player.getYRot()));
        // 開始フレームで上向きにスナップ
        applyPitch(player, START_PITCH, player.getYRot());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PitchSession s = ACTIVE.get(event.player.getUUID());
        if (s == null) return;

        s.elapsed++;
        float t = (float) s.elapsed / TOTAL_TICKS;
        if (t >= 1.0f) {
            applyPitch(event.player, END_PITCH, s.startYaw);
            ACTIVE.remove(event.player.getUUID());
            return;
        }
        // ease-out: 序盤速く、終盤ゆっくり (振り下ろしの加速感)
        float eased = 1.0f - (1.0f - t) * (1.0f - t);
        float pitch = START_PITCH + (END_PITCH - START_PITCH) * eased;
        applyPitch(event.player, pitch, s.startYaw);
    }

    private static void applyPitch(Player player, float pitch, float yaw) {
        if (player instanceof ServerPlayer sp) {
            sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), yaw, pitch,
                java.util.Set.of());
        } else {
            player.setXRot(pitch);
            player.setYRot(yaw);
        }
    }

    private static class PitchSession {
        final float startYaw;
        int elapsed = 0;

        PitchSession(float startYaw) {
            this.startYaw = startYaw;
        }
    }
}
