package minecraftarmorweapon.skill;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * BowSkillでNBTタグを付けたArrowエンティティの追加挙動 (爆裂/追尾) を処理する。
 *
 * 最適化:
 *  - 世界全体スキャン廃止。追尾矢はJoin時に弱参照リストへ登録
 *  - 4tickに1回のみ目標探索(間引き)
 *  - 着弾は ProjectileImpactEvent で即処理 (Tickスキャン不要)
 */
@Mod.EventBusSubscriber
public class BowSkillArrowHandler {

    private static final int HOMING_INTERVAL = 4;
    private static final double HOMING_RADIUS = 20.0;

    /** 追尾対象の矢 (弱参照で漏れ防止)。サーバー側のみアクセス。 */
    private static final List<WeakReference<Arrow>> HOMING_ARROWS = new CopyOnWriteArrayList<>();

    /** 追尾対象として登録 */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!arrow.getPersistentData().contains(BowSkill.NBT_HOMING_POWER)) return;
        HOMING_ARROWS.add(new WeakReference<>(arrow));
    }

    /** 退出時は弱参照がnullになるのでリストから消すのはTick側掃除で吸収 */
    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        // 明示削除は重いのでTickで古いエントリ掃除する
    }

    /** 着弾時に爆発させる */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile p = event.getProjectile();
        if (!(p instanceof Arrow arrow)) return;
        if (arrow.level().isClientSide) return;
        if (!arrow.getPersistentData().getBoolean(BowSkill.NBT_EXPLOSIVE)) return;

        Vec3 hit = event.getRayTraceResult().getLocation();
        arrow.level().explode(arrow.getOwner(), hit.x, hit.y, hit.z, 2.0f, Level.ExplosionInteraction.NONE);
        arrow.discard();
        event.setCanceled(true);
    }

    /** 追尾矢のホーミング処理 — 4tick毎に登録済み矢だけ処理 */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide) return;
        if (HOMING_ARROWS.isEmpty()) return;

        long tick = event.level.getGameTime();
        if (tick % HOMING_INTERVAL != 0) return;

        Iterator<WeakReference<Arrow>> it = HOMING_ARROWS.iterator();
        while (it.hasNext()) {
            Arrow arrow = it.next().get();
            if (arrow == null || !arrow.isAlive() || arrow.level() != event.level) {
                // 弱参照/死亡/異levelは放置 → 下の掃除で除去
                continue;
            }
            handleHoming(arrow);
        }

        // 10秒毎に死んだエントリを掃除
        if (tick % 200 == 0) {
            HOMING_ARROWS.removeIf(ref -> {
                Arrow a = ref.get();
                return a == null || !a.isAlive();
            });
        }
    }

    private static void handleHoming(Arrow arrow) {
        float power = arrow.getPersistentData().getFloat(BowSkill.NBT_HOMING_POWER);
        if (power <= 0) return;
        Vec3 motion = arrow.getDeltaMovement();
        if (motion.lengthSqr() < 0.04) return;

        Vec3 pos = arrow.position();
        Vec3 motionNorm = motion.normalize();
        LivingEntity target = arrow.level().getEntitiesOfClass(LivingEntity.class,
                arrow.getBoundingBox().inflate(HOMING_RADIUS)).stream()
            .filter(e -> e != arrow.getOwner() && e.isAlive() && !e.isSpectator())
            .filter(e -> e.getEyePosition().subtract(pos).normalize().dot(motionNorm) > 0.5)
            .min(Comparator.comparingDouble(e -> e.distanceToSqr(arrow)))
            .orElse(null);

        if (target == null) return;

        Vec3 to = target.getEyePosition().subtract(pos).normalize();
        Vec3 newMotion = motionNorm.scale(1.0 - power)
            .add(to.scale(power)).normalize().scale(motion.length());
        arrow.setDeltaMovement(newMotion);
        arrow.hasImpulse = true;
    }
}
