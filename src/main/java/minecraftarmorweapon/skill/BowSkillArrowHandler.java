package minecraftarmorweapon.skill;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;

/**
 * BowSkillでNBTタグを付けたArrowエンティティの追加挙動 (爆裂/追尾) を処理する。
 * カスタムEntityタイプを増やさずに済むようTick + ProjectileImpactイベントで監視する。
 */
@Mod.EventBusSubscriber
public class BowSkillArrowHandler {

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

    /** 追尾矢のホーミング処理 */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide) return;
        Level level = event.level;

        for (Arrow arrow : level.getEntitiesOfClass(Arrow.class,
                new AABB(-30000000, -64, -30000000, 30000000, 320, 30000000))) {
            if (!arrow.isAlive()) continue;
            if (!arrow.getPersistentData().contains(BowSkill.NBT_HOMING_POWER)) continue;
            handleHoming(arrow);
        }
    }

    private static void handleHoming(Arrow arrow) {
        float power = arrow.getPersistentData().getFloat(BowSkill.NBT_HOMING_POWER);
        if (power <= 0) return;
        Vec3 motion = arrow.getDeltaMovement();
        if (motion.lengthSqr() < 0.04) return; // 着弾後はほぼゼロ → 終了

        Vec3 pos = arrow.position();
        LivingEntity target = arrow.level().getEntitiesOfClass(LivingEntity.class,
                arrow.getBoundingBox().inflate(20.0)).stream()
            .filter(e -> e != arrow.getOwner() && e.isAlive() && !e.isSpectator())
            .filter(e -> {
                Vec3 to = e.getEyePosition().subtract(pos).normalize();
                return to.dot(motion.normalize()) > 0.5;
            })
            .min(Comparator.comparingDouble(e -> e.distanceToSqr(arrow)))
            .orElse(null);

        if (target == null) return;

        Vec3 to = target.getEyePosition().subtract(pos).normalize();
        Vec3 newMotion = motion.normalize().scale(1.0 - power)
            .add(to.scale(power)).normalize().scale(motion.length());
        arrow.setDeltaMovement(newMotion);
        arrow.hasImpulse = true;
    }
}
