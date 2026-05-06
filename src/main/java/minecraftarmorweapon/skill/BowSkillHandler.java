package minecraftarmorweapon.skill;

import minecraftarmorweapon.skill.PlayerSkillData.AttackSlot;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 弓スキル統合ハンドラ。
 *
 * 流れ:
 *  - EntityJoinLevelEvent: バニラ弓/クロスボウから生成された Arrow を捕捉し、
 *    所持者プレイヤーの該当武器 RIGHT_CLICK モーションに応じて効果を適用。
 *  - ProjectileImpactEvent: 爆裂矢の着弾時爆発処理。
 *  - LevelTickEvent: 追尾矢のホーミング処理 (4tick ごと)。
 */
@Mod.EventBusSubscriber
public class BowSkillHandler {

    public static final String NBT_APPLIED = "msw_bow_skill_applied";
    public static final String NBT_EXPLOSIVE = "msw_bow_explosive";
    public static final String NBT_HOMING_POWER = "msw_bow_homing";
    public static final String NBT_ARROW_RAIN = "msw_bow_arrow_rain";
    public static final String NBT_WIND = "msw_bow_wind";

    private static final int HOMING_INTERVAL = 4;
    private static final double HOMING_RADIUS = 20.0;

    private static final List<WeakReference<Arrow>> HOMING_ARROWS = new CopyOnWriteArrayList<>();

    @SubscribeEvent
    public static void onArrowSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (arrow.getPersistentData().getBoolean(NBT_APPLIED)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer player)) return;

        ItemStack bow = findBow(player);
        if (bow.isEmpty()) return;

        PlayerSkillData.SkillStorage skillData = PlayerSkillData.getSkillData(player);
        String motion = skillData.getMotionForWeapon(AttackSlot.RIGHT_CLICK, bow);
        if (motion == null) return;

        arrow.getPersistentData().putBoolean(NBT_APPLIED, true);

        switch (motion) {
            case "bow_power_shot" -> {
                arrow.setBaseDamage(arrow.getBaseDamage() + 4.0);
                arrow.setCritArrow(true);
            }
            case "bow_explosive" -> {
                arrow.getPersistentData().putBoolean(NBT_EXPLOSIVE, true);
            }
            case "bow_pierce" -> {
                arrow.setPierceLevel((byte) 5);
                arrow.setBaseDamage(arrow.getBaseDamage() + 1.0);
            }
            case "bow_heavy_blow" -> {
                arrow.setBaseDamage(arrow.getBaseDamage() + 2.0);
            }
            case "bow_homing" -> {
                arrow.getPersistentData().putFloat(NBT_HOMING_POWER, 0.12f);
                HOMING_ARROWS.add(new WeakReference<>(arrow));
            }
            case "bow_rapid_fire" -> {
                Vec3 vel = arrow.getDeltaMovement();
                double speed = vel.length();
                if (speed < 0.05) break;
                for (int i = 0; i < 2; i++) {
                    Arrow extra = new Arrow(arrow.level(), player);
                    extra.shoot(vel.x, vel.y, vel.z, (float) speed, 4.0f);
                    extra.pickup = AbstractArrow.Pickup.DISALLOWED;
                    extra.getPersistentData().putBoolean(NBT_APPLIED, true);
                    arrow.level().addFreshEntity(extra);
                }
            }
            case "bow_arrow_rain" -> {
                arrow.getPersistentData().putBoolean(NBT_ARROW_RAIN, true);
            }
            case "bow_quick_draw" -> {
                Vec3 vel = arrow.getDeltaMovement();
                arrow.setDeltaMovement(vel.scale(1.5));
            }
            case "bow_wind" -> {
                Vec3 vel = arrow.getDeltaMovement();
                arrow.setDeltaMovement(vel.scale(1.5));
                arrow.setNoGravity(true);
                arrow.setKnockback(arrow.getKnockback() + 2);
                arrow.getPersistentData().putBoolean(NBT_WIND, true);
            }
            default -> {}
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile p = event.getProjectile();
        if (!(p instanceof Arrow arrow)) return;
        if (arrow.level().isClientSide) return;

        if (arrow.getPersistentData().getBoolean(NBT_EXPLOSIVE)) {
            Vec3 hit = event.getRayTraceResult().getLocation();
            arrow.level().explode(arrow.getOwner(), hit.x, hit.y, hit.z, 2.0f, Level.ExplosionInteraction.NONE);
            arrow.discard();
            event.setCanceled(true);
            return;
        }

        if (arrow.getPersistentData().getBoolean(NBT_ARROW_RAIN)) {
            Vec3 hit = event.getRayTraceResult().getLocation();
            spawnArrowRain(arrow, hit);
            arrow.discard();
            event.setCanceled(true);
            return;
        }
    }

    private static void spawnArrowRain(Arrow source, Vec3 center) {
        Level level = source.level();
        var owner = source.getOwner();
        var random = level.getRandom();
        for (int i = 0; i < 8; i++) {
            double ox = (random.nextDouble() - 0.5) * 4.0;
            double oz = (random.nextDouble() - 0.5) * 4.0;
            double sx = center.x + ox;
            double sy = center.y + 12.0;
            double sz = center.z + oz;
            Arrow rain = new Arrow(level, sx, sy, sz);
            if (owner instanceof net.minecraft.world.entity.LivingEntity le) rain.setOwner(le);
            rain.shoot(0.0, -1.0, 0.0, 2.5f, 1.5f);
            rain.pickup = AbstractArrow.Pickup.DISALLOWED;
            rain.setBaseDamage(rain.getBaseDamage() + 1.0);
            rain.getPersistentData().putBoolean(NBT_APPLIED, true);
            level.addFreshEntity(rain);
        }
    }

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
            if (arrow == null || !arrow.isAlive() || arrow.level() != event.level) continue;
            handleHoming(arrow);
        }

        if (tick % 200 == 0) {
            HOMING_ARROWS.removeIf(ref -> {
                Arrow a = ref.get();
                return a == null || !a.isAlive();
            });
        }
    }

    private static void handleHoming(Arrow arrow) {
        float power = arrow.getPersistentData().getFloat(NBT_HOMING_POWER);
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

    private static ItemStack findBow(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof BowItem || main.getItem() instanceof CrossbowItem) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof BowItem || off.getItem() instanceof CrossbowItem) return off;
        return ItemStack.EMPTY;
    }
}
