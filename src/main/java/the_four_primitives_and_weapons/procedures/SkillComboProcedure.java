package the_four_primitives_and_weapons.procedures;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.skill.MotionExecutor;
import the_four_primitives_and_weapons.skill.PlayerSkillData;
import the_four_primitives_and_weapons.skill.PlayerSkillData.AttackSlot;

import java.util.List;
import java.util.Locale;
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
    private static final int HIT_INTERVAL_TICKS = 2;
    private static final String COMBO_MOTION_ID = "thrust_combo";

    public static void execute(Player player, float chargePercent) {
        if (player == null || player.level().isClientSide) return;

        PlayerSkillData.SkillStorage skillData = PlayerSkillData.getSkillData(player);
        ItemStack weapon = player.getMainHandItem();
        the_four_primitives_and_weapons.skill.WeaponStatsRegistry.WeaponStats stats =
                the_four_primitives_and_weapons.skill.WeaponStatsRegistry.getStats(weapon);
        boolean daggerPulse = isDaggerLike(weapon) || (stats != null && stats.thrust != null);
        double pulseRange = stats != null && stats.thrust != null ? stats.thrust.range : 2.4;
        double pulseDash = stats != null && stats.thrust != null ? stats.thrust.dash : 0.36;
        String[] motions = new String[] {
                resolveMotion(skillData, player, AttackSlot.FIRST_HIT, "thrust"),
                resolveMotion(skillData, player, AttackSlot.SECOND_HIT, "upper_left_slash"),
                resolveMotion(skillData, player, AttackSlot.THIRD_HIT, "upper_right_slash")
        };

        ComboSession session = new ComboSession(motions, chargePercent, daggerPulse, pulseRange, pulseDash);
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
        int hitIndex = session.index++;
        String motionId = session.motions[hitIndex];
        // 連撃は「通常の一撃目〜三撃目」を高速で出す技。チャージ倍率は短剣パルス側にだけ乗せる。
        MotionExecutor.executeMotion(motionId, player, 0.0f);
        if (session.daggerPulse) {
            performDaggerPulse(player, session, hitIndex);
        }
    }

    private static boolean isDaggerLike(ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) return false;
        the_four_primitives_and_weapons.skill.WeaponTypeRegistry.WeaponTypeData type =
                the_four_primitives_and_weapons.skill.WeaponTypeRegistry.getTypeForItem(weapon);
        if (type != null && type.getId().toLowerCase(Locale.ROOT).contains("dagger")) {
            return true;
        }
        return weapon.getItem().getClass().getSimpleName().toLowerCase(Locale.ROOT).contains("dagger");
    }

    private static void performDaggerPulse(Player player, ComboSession session, int hitIndex) {
        Level world = player.level();
        Vec3 look = MotionExecutor.horizontalLook(player);
        Vec3 eye = player.position().add(0, player.getEyeHeight() * 0.6, 0);
        Vec3 origin = player.position();
        Vec3 end = origin.add(look.scale(session.pulseRange));
        AABB area = new AABB(origin, end).inflate(0.9, 0.9, 0.9);

        double dashStep = session.pulseDash / Math.max(1, session.motions.length);
        if (dashStep > 0.0) {
            player.setDeltaMovement(player.getDeltaMovement().add(look.scale(dashStep)));
            player.hurtMarked = true;
        }
        player.swing(InteractionHand.MAIN_HAND, true);

        float baseAttack = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float pulseDamage = Math.max(0.75f, baseAttack * 0.28f);
        pulseDamage *= 1.0f + session.chargePercent * 0.25f;
        if (hitIndex == session.motions.length - 1) {
            pulseDamage *= 1.2f;
        }

        int hitCount = 0;
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isSpectator() && !e.isAlliedTo(player));
        for (LivingEntity target : targets) {
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(eye);
            double distance = to.length();
            if (distance < 0.001 || distance > session.pulseRange + 0.9) continue;
            if (to.scale(1.0 / distance).dot(look) < 0.35) continue;

            target.invulnerableTime = 0;
            if (target.hurt(world.damageSources().playerAttack(player), pulseDamage)) {
                target.knockback(0.08f + hitIndex * 0.03f, -look.x, -look.z);
                hitCount++;
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.65f, 1.6f + hitIndex * 0.18f);
        if (hitCount > 0) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.45f, 1.35f + hitIndex * 0.15f);
        }

        if (world instanceof ServerLevel sl) {
            Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
            double side = (hitIndex - 1) * 0.22;
            for (double d = 0.35; d <= session.pulseRange; d += 0.28) {
                Vec3 p = eye.add(look.scale(d)).add(right.scale(side * d));
                sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 2, 0.02, 0.02, 0.02, 0.0);
            }
            Vec3 sweep = eye.add(look.scale(0.8 + hitIndex * 0.35)).add(right.scale(side));
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, sweep.x, sweep.y, sweep.z, 1, 0.04, 0.04, 0.04, 0.0);
            if (session.chargePercent >= 0.75f && hitIndex == session.motions.length - 1) {
                Vec3 finisher = eye.add(look.scale(session.pulseRange));
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, finisher.x, finisher.y, finisher.z,
                        10, 0.2, 0.18, 0.2, 0.06);
            }
        }
    }

    private static final class ComboSession {
        final String[] motions;
        final float chargePercent;
        final boolean daggerPulse;
        final double pulseRange;
        final double pulseDash;
        int tick;
        int index;

        ComboSession(String[] motions, float chargePercent, boolean daggerPulse, double pulseRange, double pulseDash) {
            this.motions = motions;
            this.chargePercent = chargePercent;
            this.daggerPulse = daggerPulse;
            this.pulseRange = pulseRange;
            this.pulseDash = pulseDash;
        }
    }
}
