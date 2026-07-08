package the_four_primitives_and_weapons.damage;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 魂属性の特殊技「Soul Stifler」の瘴気ゾーン。
 *
 * <p>Elden Ring の Soul Stifler を模した挙動: 発生地点に一定時間留まる「魂を蝕む瘴気」の
 * 円形ゾーンを展開し、範囲内の敵に被ダメ増加 (防御ダウン) デバフを付与する。
 * デバフはゾーンから離れても持続する ({@link SpecialDebuffHandler#applyVulnerability})。</p>
 *
 * <p>ゾーンはサーバー tick で駆動し、瘴気パーティクル描画と範囲内デバフ付与を行う。</p>
 */
public final class SoulStiflerZone {

    /** 範囲内に居る間、毎回この長さで被ダメ増加デバフをリフレッシュ (離脱後も持続)。 */
    private static final int VULN_REFRESH_TICKS = 1200; // 60 秒

    private SoulStiflerZone() {}

    private static final ConcurrentLinkedQueue<Zone> ACTIVE = new ConcurrentLinkedQueue<>();

    private static final class Zone {
        final ServerLevel level;
        final double cx, cy, cz;
        final double radius;
        final int duration;
        final UUID caster;
        final float vulnPercent;
        int age;

        Zone(ServerLevel level, Vec3 center, double radius, int duration, UUID caster, float vulnPercent) {
            this.level = level;
            this.cx = center.x;
            this.cy = center.y;
            this.cz = center.z;
            this.radius = radius;
            this.duration = duration;
            this.caster = caster;
            this.vulnPercent = vulnPercent;
        }
    }

    /**
     * 瘴気ゾーンを展開する。
     *
     * @param level        サーバーレベル
     * @param center       中心 ( 通常は術者の足元 or 儀式の中心 )
     * @param radius       半径 ( ブロック )
     * @param durationTicks ゾーン持続 tick ( 200 = 10 秒 )
     * @param caster       術者 UUID ( 範囲判定から除外 )
     * @param elementLevel 属性レベル ( デバフ強度に反映 )
     */
    public static void deploy(ServerLevel level, Vec3 center, double radius,
                              int durationTicks, UUID caster, int elementLevel) {
        if (level == null || center == null || durationTicks <= 0) return;
        float vulnPercent = Math.min(0.25f, 0.15f + Math.max(0, elementLevel - 1) * 0.01f);
        ACTIVE.add(new Zone(level, center, Math.max(1.0, radius), durationTicks, caster, vulnPercent));
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1.1f, 0.6f);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.2f, 0.5f);
    }

    @Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
    public static final class Ticker {
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;
            ACTIVE.removeIf(SoulStiflerZone::tickZone);
        }
    }

    /** @return true ならこのゾーンを除去する ( 期限切れ )。 */
    private static boolean tickZone(Zone z) {
        if (z.level == null || z.age++ >= z.duration) return true;

        // 範囲内の敵に被ダメ増加デバフを付与 ( 2 tick に 1 回で十分 )。
        if ((z.age & 1) == 0) {
            AABB box = new AABB(z.cx - z.radius, z.cy - 2.0, z.cz - z.radius,
                    z.cx + z.radius, z.cy + 3.0, z.cz + z.radius);
            double r2 = z.radius * z.radius;
            for (LivingEntity e : z.level.getEntitiesOfClass(LivingEntity.class, box,
                    le -> le.isAlive() && !le.getUUID().equals(z.caster))) {
                double dx = e.getX() - z.cx, dz = e.getZ() - z.cz;
                if (dx * dx + dz * dz > r2) continue;
                SpecialDebuffHandler.applyVulnerability(e, VULN_REFRESH_TICKS, z.vulnPercent);
                z.level.sendParticles(ParticleTypes.SCULK_SOUL,
                        e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(),
                        2, 0.2, 0.35, 0.2, 0.01);
            }
        }

        // 瘴気の見た目 ( 3 tick に 1 回、 count 指定でパケットを抑える )。
        if (z.age % 3 == 0) {
            z.level.sendParticles(ParticleTypes.SCULK_SOUL,
                    z.cx, z.cy + 0.4, z.cz,
                    (int) (z.radius * 3), z.radius * 0.6, 0.5, z.radius * 0.6, 0.01);
            z.level.sendParticles(ParticleTypes.SOUL,
                    z.cx, z.cy + 0.2, z.cz,
                    (int) (z.radius * 2), z.radius * 0.55, 0.3, z.radius * 0.55, 0.01);
            // 地面の境界リング ( 少数の点描 )。
            int steps = 20;
            for (int i = 0; i < steps; i++) {
                double ang = (2 * Math.PI * i) / steps;
                z.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        z.cx + Math.cos(ang) * z.radius, z.cy + 0.05, z.cz + Math.sin(ang) * z.radius,
                        1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        return false;
    }
}
