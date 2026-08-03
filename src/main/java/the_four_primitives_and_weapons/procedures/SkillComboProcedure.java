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
    /**
     * 1 段ごとの間隔。 回転斬り ( {@link the_four_primitives_and_weapons.skill.SpinSlashTickHandler}
     * = 720° を 16 tick ) と同じく「発動したらしばらく技を出し続ける」テンポにする。
     * 2 tick だと一瞬で終わってチャージした実感が無い。
     */
    private static final int HIT_INTERVAL_TICKS = 4;
    /** 初段が出るまでの溜め ( 構え )。 発動 → 即ヒット だと重さが出ないので前置きを入れる。 */
    private static final int STARTUP_TICKS = 3;
    private static final String COMBO_MOTION_ID = "thrust_combo";
    /** チャージ 0 のときの段数 ( = 一撃目・二撃目・三撃目 の 1 巡 )。 */
    private static final int BASE_HITS = 3;
    /** フルチャージで上乗せされる段数。 3 段 → 最大 8 段。 */
    private static final int MAX_EXTRA_HITS = 5;

    /** チャージ率から連撃の段数を決める。 3 巡目以降は 一撃目〜三撃目 を繰り返す。 */
    private static int hitsForCharge(float chargePercent) {
        float c = Math.max(0.0f, Math.min(1.0f, chargePercent));
        return BASE_HITS + Math.round(c * MAX_EXTRA_HITS);
    }

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

        ComboSession session = new ComboSession(motions, hitsForCharge(chargePercent),
                chargePercent, daggerPulse, pulseRange, pulseDash);
        ACTIVE.put(player.getUUID(), session);
        // 即ヒットさせず、 STARTUP_TICKS 分の構えを挟んでから初段を出す ( onPlayerTick が進行させる )。
        playStartupCue(player, session);
    }

    /** 連撃中か ( 他スキルの多重発動を抑止したい場合に参照 )。 */
    public static boolean isComboing(Player player) {
        return player != null && ACTIVE.containsKey(player.getUUID());
    }

    /** 構えの合図。 溜めた分だけ音を高くして、 段数が多いことを分かるようにする。 */
    private static void playStartupCue(Player player, ComboSession session) {
        Level world = player.level();
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS,
                0.5f, 0.8f + session.chargePercent * 0.5f);
        if (world instanceof ServerLevel sl) {
            Vec3 look = MotionExecutor.horizontalLook(player);
            Vec3 p = player.position().add(0, player.getEyeHeight() * 0.6, 0).add(look.scale(0.7));
            sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z,
                    4 + session.totalHits, 0.14, 0.14, 0.14, 0.01);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        // ACTIVE は UUID キーなので、 シングルプレイでは クライアント側プレイヤーの tick でも
        // 同じセッションが引ける。 そこで remove すると サーバー側が 1 段も出せないまま
        // セッションが消える ( = ダメージが一切出ない ) ため、 クライアントは何もしない。
        if (player.level().isClientSide) return;

        ComboSession session = ACTIVE.get(player.getUUID());
        if (session == null) return;
        if (!player.isAlive()) {
            ACTIVE.remove(player.getUUID());
            return;
        }

        // 連撃中は水平移動を殺して、 技を出し切るまでその場に縛る
        // ( 回転斬りが connection.teleport で位置を固定しているのと同じ意図 )。
        // 落下は殺さないので空中で止まったりはしない。
        // ※ 段ごとの踏み込み ( performDaggerPulse の dashStep ) も次 tick で減衰する。
        //   weapon_stats のダガーは thrust.dash = 0 なので現状は影響なし。
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x * 0.2, v.y, v.z * 0.2);
        player.hurtMarked = true;

        session.tick++;
        // 初段だけ構え ( STARTUP_TICKS )、 以降は HIT_INTERVAL_TICKS ごと。
        int wait = (session.index == 0) ? STARTUP_TICKS : HIT_INTERVAL_TICKS;
        if (session.tick < wait) return;
        session.tick = 0;
        runNext(player, session);
        if (session.index >= session.totalHits) {
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
        if (session.index >= session.totalHits) return;
        int hitIndex = session.index++;
        // 段数がチャージで伸びるので、 一撃目〜三撃目を巡回して出す。
        String motionId = session.motions[hitIndex % session.motions.length];
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

        double dashStep = session.pulseDash / Math.max(1, session.totalHits);
        if (dashStep > 0.0) {
            player.setDeltaMovement(player.getDeltaMovement().add(look.scale(dashStep)));
            player.hurtMarked = true;
        }
        player.swing(InteractionHand.MAIN_HAND, true);

        float baseAttack = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float pulseDamage = Math.max(0.75f, baseAttack * 0.28f);
        pulseDamage *= 1.0f + session.chargePercent * 0.25f;
        if (hitIndex == session.totalHits - 1) {
            pulseDamage *= 1.2f;   // 締めの一撃
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
            if (session.chargePercent >= 0.75f && hitIndex == session.totalHits - 1) {
                Vec3 finisher = eye.add(look.scale(session.pulseRange));
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, finisher.x, finisher.y, finisher.z,
                        10, 0.2, 0.18, 0.2, 0.06);
            }
        }
    }

    private static final class ComboSession {
        final String[] motions;
        /** 実際に出す段数 ( チャージで伸びる )。 motions は 3 つを巡回して使う。 */
        final int totalHits;
        final float chargePercent;
        final boolean daggerPulse;
        final double pulseRange;
        final double pulseDash;
        int tick;
        int index;

        ComboSession(String[] motions, int totalHits, float chargePercent,
                     boolean daggerPulse, double pulseRange, double pulseDash) {
            this.motions = motions;
            this.totalHits = totalHits;
            this.chargePercent = chargePercent;
            this.daggerPulse = daggerPulse;
            this.pulseRange = pulseRange;
            this.pulseDash = pulseDash;
        }
    }
}
