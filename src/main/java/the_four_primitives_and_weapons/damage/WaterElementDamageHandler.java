package the_four_primitives_and_weapons.damage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 水属性ダメージハンドラー
 *
 * 設計方針:
 *   - 牛乳で消えるし mob effect の "モヤ" が出てしまう MobEffects.MOVEMENT_SLOWDOWN は使わない。
 *   - 移動速度低下は {@link #slowedMap} と Attribute Modifier で管理。
 *   - 消火 ( target + 周囲 entity + 周囲の火ブロック ) はそのまま。
 */
public class WaterElementDamageHandler {

    private static final float BASE_MULTIPLIER       = 1.0f;

    // 移動速度低下
    private static final int   BASE_SLOW_DURATION    = 80;   // 4秒
    private static final int   SLOW_DURATION_PER_LV  = 40;   // +2秒/Lv
    private static final double SLOW_PER_LEVEL       = 0.20; // -20% / Lv (MULTIPLY_TOTAL)
    private static final double SLOW_MAX             = 0.70; // 上限 -70%
    private static final UUID  WATER_SLOW_UUID       =
            UUID.fromString("c5d6e7f8-a9b0-1234-5678-9abcdef01234");

    // 消火範囲 (ブロック)
    private static final double EXTINGUISH_RADIUS_BASE   = 4.0;
    private static final double EXTINGUISH_RADIUS_PER_LV = 0.5;
    private static final double EXTINGUISH_RADIUS_CAP    = 10.0;

    // 内部 slow state (牛乳で消えない)
    public static class SlowEntry {
        public int remainingTick;
        public int level;
        public SlowEntry(int remainingTick, int level) {
            this.remainingTick = remainingTick;
            this.level = level;
        }
    }
    public static final Map<UUID, SlowEntry> slowedMap = new ConcurrentHashMap<>();

    /** ResetMax 等から呼ばれる: 水属性の slow を即時解除 */
    public static void clear(LivingEntity entity) {
        if (entity == null) return;
        slowedMap.remove(entity.getUUID());
        try {
            AttributeInstance attr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attr != null) {
                try { attr.removeModifier(WATER_SLOW_UUID); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    // ────────────────────────────────────────────────────────────────

    public static float handleWaterDamage(LivingEntity attacker,
                                          LivingEntity target,
                                          ItemStack weapon,
                                          float baseDmg) {
        int level = ElementalDamageUtils.getElementLevel(weapon);
        applyWaterSlow(target, level);
        extinguishAround(target, level);
        return baseDmg * BASE_MULTIPLIER;
    }

    /** レベル指定で水属性ダメージ計算 (魔導書経由用) */
    public static float calculateDamage(LivingEntity target, float baseDmg, int level) {
        applyWaterSlow(target, Math.max(level, 1));
        extinguishAround(target, level);
        return baseDmg * BASE_MULTIPLIER;
    }

    private static void applyWaterSlow(LivingEntity target, int level) {
        if (target == null) return;
        int duration = BASE_SLOW_DURATION + SLOW_DURATION_PER_LV * Math.max(level - 1, 0);
        // 内部 state 更新 (より強い level / より長い duration を採用)
        UUID id = target.getUUID();
        SlowEntry entry = slowedMap.get(id);
        if (entry == null) {
            slowedMap.put(id, new SlowEntry(duration, level));
        } else {
            entry.remainingTick = Math.max(entry.remainingTick, duration);
            entry.level = Math.max(entry.level, level);
        }
        // attribute modifier 付与 (牛乳で消えない、 モヤも出ない)
        try {
            AttributeInstance attr = target.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attr != null) {
                AttributeModifier existing = attr.getModifier(WATER_SLOW_UUID);
                if (existing != null) {
                    try { attr.removeModifier(existing); } catch (Throwable ignored) {}
                }
                double amount = Math.min(SLOW_PER_LEVEL * Math.max(level, 1), SLOW_MAX);
                try {
                    attr.addTransientModifier(new AttributeModifier(
                            WATER_SLOW_UUID,
                            "water_slow",
                            -amount,
                            AttributeModifier.Operation.MULTIPLY_TOTAL));
                } catch (IllegalArgumentException dup) {
                    // 既に付与済 → そのまま
                }
            }
        } catch (Throwable ignored) {}
    }

    // ────────────────────────────────────────────────────────────────
    // 消火 ( 元の挙動 )
    // ────────────────────────────────────────────────────────────────

    private static void extinguishAround(LivingEntity target, int level) {
        Level world = target.level();
        if (world.isClientSide()) return;

        double radius = EXTINGUISH_RADIUS_BASE
                + EXTINGUISH_RADIUS_PER_LV * Math.max(level - 1, 0);
        radius = Math.min(radius, EXTINGUISH_RADIUS_CAP);

        if (target.isOnFire()) target.clearFire();

        AABB box = target.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity le : nearby) {
            if (le.isOnFire()) le.clearFire();
        }

        BlockPos center = target.blockPosition();
        int r = (int) Math.ceil(radius);
        double r2 = radius * radius;
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (pos.distSqr(center) > r2) continue;
                    BlockState state = world.getBlockState(pos);
                    if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                        world.removeBlock(pos, false);
                    }
                }
            }
        }

        if (world instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SPLASH,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    20, radius * 0.4, radius * 0.3, radius * 0.4, 0.1);
            sl.sendParticles(ParticleTypes.CLOUD,
                    target.getX(), target.getY() + 0.2, target.getZ(),
                    10, radius * 0.5, 0.1, radius * 0.5, 0.05);
        }
    }

    // ────────────────────────────────────────────────────────────────
    // Tick handler — slowedMap の期限管理 + attribute 後始末
    // ────────────────────────────────────────────────────────────────

    @Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
    public static class WaterTickHandler {

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (event.getServer() == null) return;

            List<UUID> toExpire = new ArrayList<>();
            slowedMap.forEach((id, entry) -> {
                if (entry == null) {
                    toExpire.add(id);
                    return;
                }
                entry.remainingTick--;
                if (entry.remainingTick <= 0) toExpire.add(id);
            });

            for (UUID id : toExpire) {
                try {
                    for (ServerLevel level : event.getServer().getAllLevels()) {
                        Entity e = level.getEntity(id);
                        if (e instanceof LivingEntity living) {
                            AttributeInstance attr = living.getAttribute(Attributes.MOVEMENT_SPEED);
                            if (attr != null) {
                                try { attr.removeModifier(WATER_SLOW_UUID); }
                                catch (Throwable ignored) {}
                            }
                            break;
                        }
                    }
                } catch (Throwable ignored) {}
                slowedMap.remove(id);
            }
        }
    }
}
