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
 * 武器の得意/不得意技による Attack Cooldown 速度の一時的な調整。
 * MotionExecutor から applyBonus() / applyPenalty() を呼ぶと、ATTACK_SPEED に
 * MULTIPLY_TOTAL モディファイアを付与する。30 tick (1.5 秒) で自動解除されるため、
 * 同じ技を連続使用するか別の preferred/disliked 技を出さない限り通常速度に戻る。
 *
 *   - 得意技: ×1.5 (50% 速い) - 30 tick 持続
 *   - 不得意技: ×0.5 (50% 遅い) - 30 tick 持続
 *   - 通常技 / タイムアウト: モディファイア解除
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class WeaponSpecialtyHandler {

    private static final UUID BONUS_UUID = UUID.fromString("a8b9c0d1-e2f3-4456-789a-bcdef0123456");
    private static final UUID PENALTY_UUID = UUID.fromString("b9c0d1e2-f3a4-5567-89ab-cdef01234567");

    private static final double BONUS_AMOUNT = 0.5;
    private static final double PENALTY_AMOUNT = -0.5;
    private static final int DURATION = 30;

    private static final Map<UUID, Integer> REMAINING_TICKS = new HashMap<>();

    public static void applyBonus(Player player) {
        if (player == null) return;
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr == null) return;
        attr.removeModifier(BONUS_UUID);
        attr.removeModifier(PENALTY_UUID);
        attr.addTransientModifier(new AttributeModifier(
            BONUS_UUID, "weapon_specialty_bonus", BONUS_AMOUNT,
            AttributeModifier.Operation.MULTIPLY_TOTAL));
        REMAINING_TICKS.put(player.getUUID(), DURATION);
    }

    public static void applyPenalty(Player player) {
        if (player == null) return;
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr == null) return;
        attr.removeModifier(BONUS_UUID);
        attr.removeModifier(PENALTY_UUID);
        attr.addTransientModifier(new AttributeModifier(
            PENALTY_UUID, "weapon_specialty_penalty", PENALTY_AMOUNT,
            AttributeModifier.Operation.MULTIPLY_TOTAL));
        REMAINING_TICKS.put(player.getUUID(), DURATION);
    }

    /** 通常技発動時 - すぐにモディファイア解除。 */
    public static void applyNormal(Player player) {
        if (player == null) return;
        clearModifiers(player);
        REMAINING_TICKS.remove(player.getUUID());
    }

    private static void clearModifiers(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr != null) {
            attr.removeModifier(BONUS_UUID);
            attr.removeModifier(PENALTY_UUID);
        }
    }

    /** タイマーを毎 tick デクリメント。0 になったらモディファイアを自動解除。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide()) return;
        UUID id = event.player.getUUID();
        Integer remaining = REMAINING_TICKS.get(id);
        if (remaining == null) return;
        if (remaining <= 1) {
            clearModifiers(event.player);
            REMAINING_TICKS.remove(id);
        } else {
            REMAINING_TICKS.put(id, remaining - 1);
        }
    }
}
