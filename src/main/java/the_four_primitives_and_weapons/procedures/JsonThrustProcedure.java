package the_four_primitives_and_weapons.procedures;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

import the_four_primitives_and_weapons.skill.WeaponStatsRegistry.ThrustConfig;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON ( weapon_stats の "thrust" ) 駆動の「突き連撃」チャージ攻撃。
 *
 * <p>短い前方への踏み込み＋前方コーン内の敵へ {@code hits} 回の多段ヒット。
 * {@code range} を小さくすると「奥行きの短い突き」になる ( ダガー向け )。
 * 既存の直刀突き ( {@link TyokutouThrustAttackProcedure} ) には触れない独立実装。</p>
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public final class JsonThrustProcedure {

    private JsonThrustProcedure() {}

    private static final Map<UUID, ComboSession> ACTIVE = new ConcurrentHashMap<>();
    private static final int FAST_OPENING_HITS = 3;
    private static final int FAST_HIT_INTERVAL_TICKS = 1;
    private static final int FINISHER_INTERVAL_TICKS = 4;

    public static void execute(Player player, float chargePercent, ThrustConfig cfg) {
        if (player == null || cfg == null) return;
        Level world = player.level();
        if (world.isClientSide) return;

        // 突きの奥行き = thrust.range に、武器の attack_range ボーナス ( タイプ既定/item上書き ) を加算。
        // これで attack_range を変えれば突きの長さも一緒に伸縮する。 最低 0.5 は確保。
        // チャージでは範囲は伸ばさない ( チャージはダメージのみ強化 )。
        double reachBonus = the_four_primitives_and_weapons.skill.WeaponStatsRegistry
                .attackRangeBonus(player.getMainHandItem());
        double range = Math.max(0.5, cfg.range + reachBonus);

        // 1ヒットのダメージ: JSON指定が無ければ武器の攻撃力
        float dmg = cfg.damage > 0f
                ? cfg.damage
                : (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Float cooldownScale = the_four_primitives_and_weapons.util.DamageCalculator.getCooldownScaleContext();
        if (cooldownScale != null) {
            dmg *= 0.2f + cooldownScale * cooldownScale * 0.8f;
        }
        dmg *= 1.0f + chargePercent * 0.5f;

        ACTIVE.put(player.getUUID(), new ComboSession(
                hitsForCharge(cfg, chargePercent),
                range,
                Math.max(0.0, cfg.knockback),
                Math.max(0.0, cfg.dash),
                dmg,
                chargePercent));
        doHit(player, ACTIVE.get(player.getUUID()));
    }

    /**
     * 実際に出す段数。 チャージ 0 ( 通常の一撃目 ) は 1 段、 フルチャージで {@code cfg.hits} 段。
     *
     * <p>{@code thrust.hits} は元々「突き連撃」= チャージ攻撃向けの設定だが、
     * {@code MotionExecutor} の {@code case "thrust"} は通常攻撃の突きもここへ流す。
     * 段数を固定にすると <b>短押しの一撃目が毎回 4 連打になる</b> ため、 チャージ率で伸ばす。</p>
     */
    private static int hitsForCharge(ThrustConfig cfg, float chargePercent) {
        int max = Math.max(1, cfg.hits);
        float c = Math.max(0.0f, Math.min(1.0f, chargePercent));
        return Math.max(1, 1 + Math.round(c * (max - 1)));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        // シングルプレイでは クライアント側プレイヤーも同じ UUID なので、 ここで remove すると
        // サーバー側の連撃が初段だけで打ち切られる。 クライアントは何もしない。
        if (player.level().isClientSide) return;

        ComboSession session = ACTIVE.get(player.getUUID());
        if (session == null) return;
        if (!player.isAlive()) {
            ACTIVE.remove(player.getUUID());
            return;
        }

        session.tick++;
        if (session.doneHits >= session.totalHits) {
            ACTIVE.remove(player.getUUID());
            return;
        }
        if (session.tick < nextDelayTicks(session)) return;
        session.tick = 0;
        doHit(player, session);
        if (session.doneHits >= session.totalHits) {
            ACTIVE.remove(player.getUUID());
        }
    }

    private static int nextDelayTicks(ComboSession session) {
        return session.doneHits < FAST_OPENING_HITS
                ? FAST_HIT_INTERVAL_TICKS
                : FINISHER_INTERVAL_TICKS;
    }

    private static void doHit(Player player, ComboSession session) {
        Level world = player.level();
        Vec3 look = the_four_primitives_and_weapons.skill.MotionExecutor.horizontalLook(player);
        Vec3 eye = player.position().add(0, player.getEyeHeight() * 0.6, 0);
        Vec3 origin = player.position();
        Vec3 end = origin.add(look.scale(session.range));
        AABB area = new AABB(origin, end).inflate(1.0, 1.0, 1.0);
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isSpectator() && !e.isAlliedTo(player));

        // 短い踏み込みを各段に分けて入れる。連撃感を出しつつ、瞬間移動ほど進まない量に抑える。
        double dashStep = session.dash / Math.max(1, session.totalHits);
        player.setDeltaMovement(player.getDeltaMovement().add(look.scale(dashStep)));
        player.hurtMarked = true;
        player.swing(player.getUsedItemHand(), true);

        int hitIndex = session.doneHits;
        for (LivingEntity target : targets) {
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(eye);
            if (to.length() > session.range + 1.0) continue;
            if (to.normalize().dot(look) < 0.4) continue; // 前方コーンのみ

            target.invulnerableTime = 0; // 多段ヒットを通す
            target.hurt(world.damageSources().playerAttack(player), session.damage);
            target.knockback((float) session.knockback, -look.x, -look.z);
        }
        session.doneHits++;

        // 演出
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.45f + hitIndex * 0.12f);
        if (world instanceof ServerLevel sl) {
            // 全ての突き共通の見た目 ( 前方へ伸びる線 )。 斬撃の扇と区別が付くようにする。
            // 連撃は段ごとに線が伸びる方が突きらしいので、 初段のみに絞らず毎段出す。
            the_four_primitives_and_weapons.skill.MotionExecutor.thrustLine(
                    sl, player, look, player.position(), session.range);
            if (session.chargePercent >= 0.75f && session.doneHits >= session.totalHits) {
                Vec3 p = eye.add(look.scale(session.range));
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, p.x, p.y, p.z, 12, 0.25, 0.2, 0.25, 0.08);
            }
        }
    }

    private static final class ComboSession {
        final int totalHits;
        final double range;
        final double knockback;
        final double dash;
        final float damage;
        final float chargePercent;
        int tick;
        int doneHits;

        ComboSession(int totalHits, double range, double knockback, double dash, float damage, float chargePercent) {
            this.totalHits = totalHits;
            this.range = range;
            this.knockback = knockback;
            this.dash = dash;
            this.damage = damage;
            this.chargePercent = chargePercent;
        }
    }
}
