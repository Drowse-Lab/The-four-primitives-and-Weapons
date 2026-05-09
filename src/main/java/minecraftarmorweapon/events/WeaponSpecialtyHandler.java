package minecraftarmorweapon.events;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 武器の得意/不得意技による Attack Cooldown 速度調整。
 * MotionExecutor から applyBonus() / applyPenalty() を呼ぶと、
 * Attributes.ATTACK_SPEED に MULTIPLY_TOTAL モディファイアを付与し、
 * クロスヘアの攻撃ゲージ充填速度が変化する。
 *
 *   - 得意技: ×1.5 (50% 速い)
 *   - 通常技: 変更なし
 *   - 不得意技: ×0.5 (50% 遅い)
 *
 * 効果は次の preferred/disliked 技を出すか、武器を持ち変えるまで持続する
 * (TICK_DURATION で自然減衰せずに、別の motion 発動・武器変更まで継続)。
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class WeaponSpecialtyHandler {

    private static final UUID BONUS_UUID = UUID.fromString("a8b9c0d1-e2f3-4456-789a-bcdef0123456");
    private static final UUID PENALTY_UUID = UUID.fromString("b9c0d1e2-f3a4-5567-89ab-cdef01234567");

    private static final double BONUS_AMOUNT = 0.5;    // ×1.5
    private static final double PENALTY_AMOUNT = -0.5; // ×0.5

    /** 各プレイヤーの現在のステート (BONUS / PENALTY / NONE) を記憶。 */
    private enum Mode { NONE, BONUS, PENALTY }
    private static final Map<UUID, Mode> ACTIVE_MODE = new HashMap<>();

    /**
     * 得意技発動時に呼ばれる - 攻撃速度 ×1.5。
     */
    public static void applyBonus(Player player) {
        UUID id = player.getUUID();
        if (ACTIVE_MODE.get(id) == Mode.BONUS) return; // 既にBONUS適用中
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr == null) return;
        // 既存のモディファイアをクリア (ペナルティ→ボーナス切替対応)
        attr.removeModifier(BONUS_UUID);
        attr.removeModifier(PENALTY_UUID);
        attr.addTransientModifier(new AttributeModifier(
            BONUS_UUID, "weapon_specialty_bonus", BONUS_AMOUNT,
            AttributeModifier.Operation.MULTIPLY_TOTAL));
        ACTIVE_MODE.put(id, Mode.BONUS);
    }

    /**
     * 不得意技発動時に呼ばれる - 攻撃速度 ×0.5。
     */
    public static void applyPenalty(Player player) {
        UUID id = player.getUUID();
        if (ACTIVE_MODE.get(id) == Mode.PENALTY) return;
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr == null) return;
        attr.removeModifier(BONUS_UUID);
        attr.removeModifier(PENALTY_UUID);
        attr.addTransientModifier(new AttributeModifier(
            PENALTY_UUID, "weapon_specialty_penalty", PENALTY_AMOUNT,
            AttributeModifier.Operation.MULTIPLY_TOTAL));
        ACTIVE_MODE.put(id, Mode.PENALTY);
    }

    /**
     * 通常技発動時 (preferred/disliked 以外) - 既存のボーナス/ペナルティを解除。
     */
    public static void applyNormal(Player player) {
        UUID id = player.getUUID();
        if (ACTIVE_MODE.get(id) == Mode.NONE || !ACTIVE_MODE.containsKey(id)) return;
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr != null) {
            attr.removeModifier(BONUS_UUID);
            attr.removeModifier(PENALTY_UUID);
        }
        ACTIVE_MODE.put(id, Mode.NONE);
    }

    /**
     * プレイヤーがログアウトしたら HashMap から除去 (メモリリーク防止)。
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // 100tick (5秒) おきに、まだプレイヤーが武器を持っていない/Modeがズレていれば自然リセット。
        if (event.player.tickCount % 100 != 0) return;
        Player player = event.player;
        if (player.getMainHandItem().isEmpty()) {
            applyNormal(player);
        }
    }
}
