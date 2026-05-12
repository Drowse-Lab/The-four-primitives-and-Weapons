package the_four_primitives_and_weapons.event;

import the_four_primitives_and_weapons.config.DebugConfig;
import the_four_primitives_and_weapons.entity.RecrossHookEntity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Re:Cross Hookshot のデバッグログ出力 — 動きを {@code logs/recross_hookshot.log} に記録.
 *
 * 元データパック ({@code Re_Cross_Hookshot}) の挙動と Java 版の挙動を比較できるよう、
 * 各 tick の player + hook 状態を行単位で出力.
 */
public class RecrossDebugLogger {

    private static PrintWriter writer;
    private static int sessionId = 0;
    private static int tickCounter = 0;
    private static long sessionStartMillis = 0;
    private static UUID currentPlayerUuid;
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void startSession(Player owner, Vec3 hookPos) {
        if (!DebugConfig.recrossHookshotDebugEnabled) return;
        try {
            close();
            File logFile = getLogFile();
            writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true), 8192));
            sessionId++;
            tickCounter = 0;
            sessionStartMillis = System.currentTimeMillis();
            currentPlayerUuid = owner.getUUID();

            Vec3 toVec = hookPos.subtract(owner.position());
            double horizDist = Math.sqrt(toVec.x * toVec.x + toVec.z * toVec.z);
            double dist = toVec.length();

            writer.println();
            writer.printf("=== JAVA SESSION %d START at %s ===%n",
                sessionId, TIME_FMT.format(new Date()));
            writer.printf("Player: name=%s uuid=%s pos=(%.2f,%.2f,%.2f) yaw=%.2f pitch=%.2f%n",
                owner.getName().getString(), owner.getUUID(),
                owner.getX(), owner.getY(), owner.getZ(),
                owner.getYRot(), owner.getXRot());
            writer.printf("Anchor: pos=(%.2f,%.2f,%.2f) horizDist=%.2f dy=%.2f dist=%.2f%n",
                hookPos.x, hookPos.y, hookPos.z, horizDist, toVec.y, dist);
            writer.println("--");
        } catch (IOException e) {
            // ignore
        }
    }

    public static void logTick(String event, Player p, RecrossHookEntity hook) {
        if (!DebugConfig.recrossHookshotDebugEnabled) return;
        if (writer == null && hook != null && hook.getAnchorPos() != null) {
            startSession(p, hook.getAnchorPos());
        }
        if (writer == null) return;
        if (!p.getUUID().equals(currentPlayerUuid)) return;
        try {
            Vec3 vel = p.getDeltaMovement();
            String hookInfo = "";
            if (hook != null) {
                hookInfo = String.format(" hook=(%.2f,%.2f,%.2f) state=%s",
                    hook.getX(), hook.getY(), hook.getZ(), hook.getState());
            }
            writer.printf(
                "T+%d [%s] pos=(%.2f,%.2f,%.2f) vel=(%.3f,%.3f,%.3f speed=%.3f) noGrav=%s onGround=%s%s%n",
                tickCounter, event,
                p.getX(), p.getY(), p.getZ(),
                vel.x, vel.y, vel.z, vel.length(),
                p.isNoGravity(), p.onGround(), hookInfo);
            tickCounter++;
        } catch (Exception e) {
            // ignore
        }
    }

    /** active session があれば終了処理. なければ no-op. */
    public static void endIfActive(Player p, String reason) {
        if (writer == null) return;
        if (currentPlayerUuid == null || !p.getUUID().equals(currentPlayerUuid)) return;
        try {
            Vec3 vel = p.getDeltaMovement();
            long durMs = System.currentTimeMillis() - sessionStartMillis;
            writer.printf("=== JAVA SESSION %d END at %s (duration=%dms) ===%n",
                sessionId, TIME_FMT.format(new Date()), durMs);
            writer.printf("Reason: %s%n", reason);
            writer.printf("Final: pos=(%.2f,%.2f,%.2f) vel=(%.3f,%.3f,%.3f speed=%.3f) noGrav=%s%n",
                p.getX(), p.getY(), p.getZ(),
                vel.x, vel.y, vel.z, vel.length(),
                p.isNoGravity());
            writer.printf("Total ticks: %d%n", tickCounter);
            writer.flush();
        } catch (Exception e) {
            // ignore
        }
        close();
    }

    private static void close() {
        if (writer != null) {
            try { writer.close(); } catch (Exception e) { /* ignore */ }
            writer = null;
            currentPlayerUuid = null;
        }
    }

    private static File getLogFile() {
        File dir = FMLPaths.GAMEDIR.get().resolve("logs").toFile();
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "recross_hookshot.log");
    }
}
