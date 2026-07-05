package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.skill.MotionExecutor;
import the_four_primitives_and_weapons.skill.PlayerSkillData;
import the_four_primitives_and_weapons.skill.PlayerSkillData.AttackSlot;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill screen の first_hit / second_hit / third_hit を高速で順番に発動する連撃。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public final class SkillComboProcedure {

    private SkillComboProcedure() {}

    private static final Map<UUID, ComboSession> ACTIVE = new ConcurrentHashMap<>();
    private static final int HIT_INTERVAL_TICKS = 1;
    private static final String COMBO_MOTION_ID = "thrust_combo";

    public static void execute(Player player, float chargePercent) {
        if (player == null || player.level().isClientSide) return;

        PlayerSkillData.SkillStorage skillData = PlayerSkillData.getSkillData(player);
        String[] motions = new String[] {
                resolveMotion(skillData, player, AttackSlot.FIRST_HIT, "thrust"),
                resolveMotion(skillData, player, AttackSlot.SECOND_HIT, "upper_left_slash"),
                resolveMotion(skillData, player, AttackSlot.THIRD_HIT, "upper_right_slash")
        };

        ComboSession session = new ComboSession(motions, chargePercent);
        ACTIVE.put(player.getUUID(), session);
        runNext(player, session);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        ComboSession session = ACTIVE.get(player.getUUID());
        if (session == null) return;
        if (player.level().isClientSide || !player.isAlive()) {
            ACTIVE.remove(player.getUUID());
            return;
        }

        session.tick++;
        if (session.tick < HIT_INTERVAL_TICKS) return;
        session.tick = 0;
        runNext(player, session);
        if (session.index >= session.motions.length) {
            ACTIVE.remove(player.getUUID());
        }
    }

    private static String resolveMotion(PlayerSkillData.SkillStorage skillData, Player player,
                                        AttackSlot slot, String fallback) {
        String motionId = skillData.getMotionForWeapon(slot, player.getMainHandItem());
        if (motionId == null || motionId.isEmpty() || COMBO_MOTION_ID.equals(motionId)) {
            return fallback;
        }
        return motionId;
    }

    private static void runNext(Player player, ComboSession session) {
        if (session.index >= session.motions.length) return;
        String motionId = session.motions[session.index++];
        MotionExecutor.executeMotion(motionId, player, session.chargePercent);
    }

    private static final class ComboSession {
        final String[] motions;
        final float chargePercent;
        int tick;
        int index;

        ComboSession(String[] motions, float chargePercent) {
            this.motions = motions;
            this.chargePercent = chargePercent;
        }
    }
}
