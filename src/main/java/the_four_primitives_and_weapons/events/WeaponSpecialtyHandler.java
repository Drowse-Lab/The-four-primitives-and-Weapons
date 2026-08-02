package the_four_primitives_and_weapons.events;

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
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class WeaponSpecialtyHandler {

    private static final UUID BONUS_UUID = UUID.fromString("a8b9c0d1-e2f3-4456-789a-bcdef0123456");
    private static final UUID PENALTY_UUID = UUID.fromString("b9c0d1e2-f3a4-5567-89ab-cdef01234567");
    // 持続的 ( 武器を持ってる間ずっと有効 ) な modifier。 motion 発動時 boost とは別 UUID。
    private static final UUID SUSTAINED_BONUS_UUID   = UUID.fromString("c1d2e3f4-a5b6-4778-9abc-def012345678");
    private static final UUID SUSTAINED_PENALTY_UUID = UUID.fromString("d2e3f4a5-b6c7-4889-abcd-ef0123456789");

    private static final double BONUS_AMOUNT = 0.5;
    private static final double PENALTY_AMOUNT = -0.5;
    private static final double SUSTAINED_BONUS_AMOUNT = 0.25;   // 持続版は控えめ ( +25% )
    private static final double SUSTAINED_PENALTY_AMOUNT = -0.25; // 持続版は控えめ ( -25% )
    private static final int DURATION = 30;

    private static final Map<UUID, Integer> REMAINING_TICKS = new HashMap<>();
    private enum SustainedState { NONE, BONUS, PENALTY }

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

    /** タイマーを毎 tick デクリメント + 持続 modifier の更新 ( server side )。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide()) return;
        Player player = event.player;
        UUID id = player.getUUID();

        // 短期 boost のタイマー
        Integer remaining = REMAINING_TICKS.get(id);
        if (remaining != null) {
            if (remaining <= 1) {
                clearModifiers(player);
                REMAINING_TICKS.remove(id);
            } else {
                REMAINING_TICKS.put(id, remaining - 1);
            }
        }

        // 持続 modifier ( 数 tick おきで OK、 毎 tick だと重い )
        if (player.tickCount % 10 == 0) {
            updateSustainedModifier(player);
        }
    }

    /**
     * 持続 modifier の更新:
     *   - 武器の WeaponType と combat slot first_hit motion を見て、
     *     preferred → +25%、 disliked → -25%、 normal → 解除
     * 武器を持ち替えた / motion 設定を変えた / 武器を外したタイミングで自動更新。
     */
    private static void updateSustainedModifier(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr == null) return;

        SustainedState desired = SustainedState.NONE;
        net.minecraft.world.item.ItemStack hand = player.getMainHandItem();
        the_four_primitives_and_weapons.skill.WeaponTypeRegistry.WeaponTypeData wt =
                the_four_primitives_and_weapons.skill.WeaponTypeRegistry.getTypeForItem(hand);
        if (wt != null) {
            String motionId = player.getCapability(
                    the_four_primitives_and_weapons.skill.PlayerSkillData.SKILL_CAPABILITY)
                    .map(sd -> sd.getMotionForWeapon(
                            the_four_primitives_and_weapons.skill.PlayerSkillData.AttackSlot.FIRST_HIT, hand))
                    .orElse(null);
            if (motionId != null && !motionId.isEmpty()) {
                if (wt.isPreferredCombatMotion(motionId)) desired = SustainedState.BONUS;
                else if (wt.isDislikedCombatMotion(motionId)) desired = SustainedState.PENALTY;
            }
        }

        // 現在の状態は「キャッシュ」ではなく AttributeInstance の実物から判定する。
        // キャッシュだと 死亡リスポーン / 再ログイン で AttributeMap が作り直されて
        // modifier が消えたのに キャッシュだけ残り、 二度と再付与されなくなる。
        SustainedState current = SustainedState.NONE;
        if (attr.getModifier(SUSTAINED_BONUS_UUID) != null) current = SustainedState.BONUS;
        else if (attr.getModifier(SUSTAINED_PENALTY_UUID) != null) current = SustainedState.PENALTY;
        if (current == desired) return; // 変化なし

        // 解除してから再適用
        attr.removeModifier(SUSTAINED_BONUS_UUID);
        attr.removeModifier(SUSTAINED_PENALTY_UUID);
        if (desired == SustainedState.BONUS) {
            attr.addTransientModifier(new AttributeModifier(
                    SUSTAINED_BONUS_UUID, "weapon_specialty_sustained_bonus",
                    SUSTAINED_BONUS_AMOUNT, AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (desired == SustainedState.PENALTY) {
            attr.addTransientModifier(new AttributeModifier(
                    SUSTAINED_PENALTY_UUID, "weapon_specialty_sustained_penalty",
                    SUSTAINED_PENALTY_AMOUNT, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    /** ログアウト時に短期 boost のタイマーを破棄 ( UUID キーの静的マップを溜め込まない )。 */
    @SubscribeEvent
    public static void onLoggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        REMAINING_TICKS.remove(event.getEntity().getUUID());
    }
}
