package minecraftarmorweapon.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 武器の適性外攻撃ペナルティ管理。
 * MotionExecutor から applyPenalty() を呼び、tickで以下を行う:
 *   - attackStrengthTicker を半分の速度に減速（クロスヘアの攻撃ゲージが遅くなる）
 *   - ペナルティ残り tick をデクリメント
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class WeaponSpecialtyHandler {

    private static final Map<UUID, Integer> PENALTY_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> BONUS_TICKS = new HashMap<>();
    private static final int PENALTY_DURATION = 30; // 約1.5秒
    private static final int BONUS_DURATION = 30;   // 約1.5秒

    private static final Field ATTACK_STRENGTH_TICKER_FIELD;
    static {
        Field f = null;
        try {
            f = LivingEntity.class.getDeclaredField("attackStrengthTicker");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            try {
                f = Player.class.getDeclaredField("attackStrengthTicker");
                f.setAccessible(true);
            } catch (NoSuchFieldException ignored) {}
        }
        ATTACK_STRENGTH_TICKER_FIELD = f;
    }

    /**
     * 不得意技発動時にペナルティを付与（攻撃ゲージ -50%、約1.5秒）。
     */
    public static void applyPenalty(Player player) {
        PENALTY_TICKS.put(player.getUUID(), PENALTY_DURATION);
        BONUS_TICKS.remove(player.getUUID()); // 競合する場合はペナルティ優先
    }

    /**
     * 得意技発動時にボーナスを付与（攻撃ゲージ +50%、約1.5秒）。
     */
    public static void applyBonus(Player player) {
        if (PENALTY_TICKS.containsKey(player.getUUID())) return; // ペナルティ中は無視
        BONUS_TICKS.put(player.getUUID(), BONUS_DURATION);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        UUID uuid = player.getUUID();

        // ペナルティ処理（ゲージ充填半減）
        Integer penalty = PENALTY_TICKS.get(uuid);
        if (penalty != null) {
            if (penalty <= 0) {
                PENALTY_TICKS.remove(uuid);
            } else {
                if (ATTACK_STRENGTH_TICKER_FIELD != null && (player.tickCount % 2 == 0)) {
                    try {
                        int current = ATTACK_STRENGTH_TICKER_FIELD.getInt(player);
                        if (current > 0) {
                            ATTACK_STRENGTH_TICKER_FIELD.setInt(player, current - 1);
                        }
                    } catch (IllegalAccessException ignored) {}
                }
                PENALTY_TICKS.put(uuid, penalty - 1);
                return;
            }
        }

        // ボーナス処理（ゲージ充填+50% → 1tick おきにもう1加算）
        Integer bonus = BONUS_TICKS.get(uuid);
        if (bonus != null) {
            if (bonus <= 0) {
                BONUS_TICKS.remove(uuid);
            } else {
                if (ATTACK_STRENGTH_TICKER_FIELD != null && (player.tickCount % 2 == 0)) {
                    try {
                        int current = ATTACK_STRENGTH_TICKER_FIELD.getInt(player);
                        ATTACK_STRENGTH_TICKER_FIELD.setInt(player, current + 1);
                    } catch (IllegalAccessException ignored) {}
                }
                BONUS_TICKS.put(uuid, bonus - 1);
            }
        }
    }
}
