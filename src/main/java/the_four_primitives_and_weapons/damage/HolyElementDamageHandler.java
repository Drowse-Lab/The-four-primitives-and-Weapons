package the_four_primitives_and_weapons.damage;

import the_four_primitives_and_weapons.util.VersionHelper;
import net.minecraft.world.item.ItemStack;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聖属性ダメージハンドラー
 * - アンデットに多くダメージが入る
 */
public class HolyElementDamageHandler {

    // 聖属性ダメージを受けたエンティティを追跡（トーテム貫通用）
    private static final Map<UUID, Long> holyDamageTargets = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> holyGlowTargets = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> holyGlowPreviousState = new ConcurrentHashMap<>();

    // 基礎ダメージ倍率
    private static final float BASE_DAMAGE_MULTIPLIER = 1.1f;
    // アンデットへのダメージ倍率
    private static final float UNDEAD_DAMAGE_MULTIPLIER = 2.5f;
    // レベルごとの追加ダメージ倍率
    private static final float LEVEL_DAMAGE_MULTIPLIER = 0.3f;
    private static final int HOLY_GLOW_DURATION_TICKS = 100;

    // ── 回復阻害 ( 聖なる裁き ) ──────────────────────────────────────
    // 瘴気と同じ回復阻害 state ( MiasmaHealMixin が参照 ) を共有するが、
    // 聖は完全阻害せず 上限 60% / 持続も短め。 瘴気の方が強い場合は上書きしない。
    private static final int   HEAL_BLOCK_DURATION_BASE      = 80;   // 4秒
    private static final int   HEAL_BLOCK_DURATION_PER_LEVEL = 40;   // +2秒/Lv
    private static final int   HEAL_BLOCK_DURATION_MAX       = 240;  // 12秒
    private static final float HEAL_BLOCK_RATE_PER_LEVEL     = 0.12f;
    private static final float HEAL_BLOCK_RATE_MAX           = 0.60f;

    /**
     * エンティティがアンデットかどうかを判定
     * @param entity 判定するエンティティ
     * @return アンデットの場合true
     */
    private static boolean isUndead(LivingEntity entity) {
        // 個別のエンティティタイプで判定
        return entity instanceof Zombie ||
               entity instanceof Skeleton ||
               entity instanceof WitherSkeleton ||
               entity instanceof Stray ||
               entity instanceof Husk ||
               entity instanceof Phantom ||
               entity instanceof Drowned ||
               entity instanceof ZombieVillager ||
               entity instanceof ZombifiedPiglin ||
               entity instanceof WitherBoss;
    }

    /**
     * 聖属性ダメージを計算
     * @param target ターゲットエンティティ
     * @param originalDamage 元のダメージ
     * @param elementLevel 属性レベル
     * @return 計算後のダメージ
     */
    public static float calculateDamage(LivingEntity target, float originalDamage, int elementLevel) {
        float damageMultiplier = BASE_DAMAGE_MULTIPLIER;

        // アンデットの場合、大幅にダメージ増加
        if (isUndead(target)) {
            damageMultiplier = UNDEAD_DAMAGE_MULTIPLIER;

            // レベルによる追加ダメージ
            damageMultiplier += (elementLevel * LEVEL_DAMAGE_MULTIPLIER);

            // 炎上効果を付与（聖なる炎）
            if (elementLevel >= 2) {
                target.setSecondsOnFire(5);
            }

            // MobEffect ではなく glowing tag を時間管理して付与する。
            applyTimedHolyGlow(target, HOLY_GLOW_DURATION_TICKS);

            // 聖なる光のパーティクル（アンデッド専用の強化版）
            if (VersionHelper.getLevel(target) instanceof ServerLevel serverLevel) {
                // エンチャントの輝き（金色）
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    30, 0.4, 0.6, 0.4, 0.15);

                // 光の粒子
                serverLevel.sendParticles(ParticleTypes.GLOW,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    20, 0.3, 0.5, 0.3, 0.1);

                // 聖なる炎
                if (elementLevel >= 2) {
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        15, 0.2, 0.3, 0.2, 0.05);
                }
            }
        } else {
            // 通常の聖属性エフェクト
            if (VersionHelper.getLevel(target) instanceof ServerLevel serverLevel) {
                // 軽い光のエフェクト
                serverLevel.sendParticles(ParticleTypes.WAX_ON,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    10, 0.3, 0.4, 0.3, 0.05);
            }
        }

        // 聖なる裁き: 対象の回復を阻害する
        applyHealBlock(target, elementLevel);

        // トーテム貫通用: このエンティティが聖属性ダメージを受けたことを記録
        holyDamageTargets.put(target.getUUID(), target.level().getGameTime());

        return originalDamage * damageMultiplier;
    }

    /**
     * 回復阻害を付与する。 瘴気と同じ state ( {@link MiasmaElementDamageHandler#apply} ) を使うので
     * {@code MiasmaHealMixin} がそのまま回復量をカットし、 属性デバフ消去ポーション等でも解除できる。
     * 既に強い阻害 ( = 瘴気 ) が乗っている場合は弱めない ( apply が max を採用 )。
     */
    private static void applyHealBlock(LivingEntity target, int elementLevel) {
        if (target == null || elementLevel <= 0) return;

        int duration = Math.min(HEAL_BLOCK_DURATION_MAX,
                HEAL_BLOCK_DURATION_BASE + HEAL_BLOCK_DURATION_PER_LEVEL * (elementLevel - 1));
        float rate = Math.min(HEAL_BLOCK_RATE_MAX, HEAL_BLOCK_RATE_PER_LEVEL * elementLevel);
        MiasmaElementDamageHandler.apply(target, duration, rate);

        if (VersionHelper.getLevel(target) instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.WAX_OFF,
                    target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ(),
                    6, 0.25, 0.3, 0.25, 0.02);
        }
    }

    private static void applyTimedHolyGlow(LivingEntity target, int durationTicks) {
        if (target == null || target.level().isClientSide()) return;
        holyGlowPreviousState.putIfAbsent(target.getUUID(), target.isCurrentlyGlowing());
        target.setGlowingTag(true);
        long expireTick = target.level().getGameTime() + Math.max(1, durationTicks);
        holyGlowTargets.merge(target.getUUID(), expireTick, Math::max);
    }

    @Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
    public static class HolyGlowTickHandler {
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (event.getServer() == null) return;

            long now = event.getServer().overworld().getGameTime();
            holyGlowTargets.entrySet().removeIf(entry -> {
                Long expireTick = entry.getValue();
                if (expireTick != null && now < expireTick) return false;

                for (ServerLevel level : event.getServer().getAllLevels()) {
                    Entity entity = level.getEntity(entry.getKey());
                    if (entity instanceof LivingEntity living) {
                        Boolean previous = holyGlowPreviousState.remove(entry.getKey());
                        living.setGlowingTag(previous != null && previous);
                        break;
                    }
                }
                holyGlowPreviousState.remove(entry.getKey());
                return true;
            });
        }
    }

    /**
     * 聖属性ダメージを受けたエンティティかチェック（トーテム貫通用）
     * Mixinから呼ばれる
     */
    public static boolean shouldBypassTotem(UUID targetUUID, long currentGameTime) {
        Long hitTime = holyDamageTargets.remove(targetUUID);
        if (hitTime == null) return false;
        // 2tick以内のデータのみ有効
        return currentGameTime - hitTime <= 1;
    }

    /**
     * エンティティに���属性ダメージを与え��
     * @param target ターゲットエンティティ
     * @param damage ダメージ量
     * @param source ダメージソース元
     * @param level 属性レベル
     */
    public static void applyHolyDamage(LivingEntity target, float damage, LivingEntity source, int level) {
        // カスタム DamageType: the_four_primitives_and_weapons:holy
        DamageSource ds = ModDamageSources.ofElement(target.level(), ElementType.HOLY, source);
        IElementalDamageSource elementalSource = (IElementalDamageSource) ds;
        elementalSource.setElementType(ElementType.HOLY);
        elementalSource.setElementLevel(level);

        target.hurt(ds, damage);
    }

    public static float handleHolyDamage(LivingEntity attacker, LivingEntity target, ItemStack weapon, float baseDmg) {
        int level = ElementalDamageUtils.getElementLevel(weapon);
        float damage = calculateDamage(target, baseDmg, level);
        applyHolyDamage(target, damage - baseDmg, attacker, level);
        return damage;
    }
}
