package minecraftarmorweapon.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * 月の満ち欠けによる難易度修正システム。
 *
 * バニラの月相: Level.getMoonPhase() → 0〜7
 *   0: 満月        (満月夜)
 *   1: 居待月      (十八夜)
 *   2: 下弦の月
 *   3: 二十三夜
 *   4: 新月        (最も弱い)
 *   5: 三日月
 *   6: 上弦の月
 *   7: 十三夜
 *
 * 難易度ボーナス (実効AIレベル +) と他ハンドラ (UndeadArmy/PartyNight/Dodge等) へ提供:
 *   満月  : +8 (最悪)
 *   十三夜: +6
 *   上弦  : +4
 *   三日月: +2
 *   新月  : +0 (最弱)
 *   二十三夜:+2
 *   下弦  : +4
 *   居待月: +6
 *
 * 夜入り (13000tick) 時にチャット通知＋効果音。
 */
@Mod.EventBusSubscriber
public class MoonPhaseDifficulty {

    private static final int NIGHT_START = 13000;
    private static final int NIGHT_END = 23000;

    // 月相 → 難易度ボーナス
    private static final int[] PHASE_BONUS = { 8, 6, 4, 2, 0, 2, 4, 6 };
    // 月相 → 表示名
    private static final String[] PHASE_NAME = {
        "§4§l満月", "§c居待月", "§6下弦の月", "§e二十三夜",
        "§8§l新月", "§e三日月", "§6上弦の月", "§c十三夜"
    };
    // 月相 → 色コード（ボスバー等用）
    private static final String[] PHASE_COLOR = {
        "§4", "§c", "§6", "§e", "§8", "§e", "§6", "§c"
    };

    private static final Map<String, Boolean> wasNight = new HashMap<>();

    /** 現在の月相 (0-7) をサーバーのオーバーワールドから取得 */
    public static int getMoonPhase(Level level) {
        if (level == null) return 4;
        return ((ServerLevel) level).getMoonPhase();
    }

    /** 現在の月相による難易度ボーナス (0-8) */
    public static int getDifficultyBonus(Level level) {
        if (level == null) return 0;
        int phase = getMoonPhase(level);
        return PHASE_BONUS[phase & 7];
    }

    /** 月が出ている夜間なら true */
    public static boolean isMoonActive(Level level) {
        if (level == null) return false;
        long dayTime = level.getDayTime() % 24000;
        return dayTime >= NIGHT_START && dayTime <= NIGHT_END;
    }

    /** 月相名 (日本語、色付き) */
    public static String getPhaseName(int phase) {
        return PHASE_NAME[phase & 7];
    }

    /** 月相の色コード */
    public static String getPhaseColor(int phase) {
        return PHASE_COLOR[phase & 7];
    }

    /** サーバーティックで夜入りを検知し通知する */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % 20 != 0) return; // 1秒ごと

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.dimensionType().natural()) continue;
            long dayTime = level.getDayTime() % 24000;
            boolean isNight = dayTime >= NIGHT_START && dayTime <= NIGHT_END;

            String key = level.dimension().location().toString();
            boolean wasNightInLevel = wasNight.getOrDefault(key, false);

            // 夜入り瞬間: 月相を通知
            if (isNight && !wasNightInLevel) {
                int phase = level.getMoonPhase();
                int bonus = PHASE_BONUS[phase];
                String name = PHASE_NAME[phase];

                for (ServerPlayer p : level.players()) {
                    p.sendSystemMessage(Component.literal(
                        "§7今夜は " + name + " §7— 難易度補正 §c+" + bonus));
                    // 満月/十三夜などの高ボーナス時は特別演出
                    if (bonus >= 6) {
                        level.playSound(null, p.blockPosition(),
                            SoundEvents.WOLF_HOWL, SoundSource.AMBIENT,
                            0.5f + bonus * 0.05f, 0.8f);
                    }
                }
            }
            wasNight.put(key, isNight);
        }
    }
}
