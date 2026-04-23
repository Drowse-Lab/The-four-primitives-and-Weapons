package minecraftarmorweapon.event;

import minecraftarmorweapon.command.CustomDifficultyCommand;
import minecraftarmorweapon.command.CustomDifficultyCommand.CustomDifficulty;
import minecraftarmorweapon.config.EquipmentTierRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 難易度ベースの多面的イベントハンドラ
 * True Crafter Mode準拠: HP倍率・バフ効果・ベッド制限・援軍・壁越し感知・落下耐性
 */
@Mod.EventBusSubscriber
public class CustomDifficultyEventHandler {

    // HP倍率の適用済みエンティティを記録
    private static final Set<UUID> healthScaled = ConcurrentHashMap.newKeySet();
    // バフ適用済みエンティティ
    private static final Set<UUID> buffApplied = ConcurrentHashMap.newKeySet();
    // 援軍スポーン済みエンティティ
    private static final Set<UUID> reinforcementSpawned = ConcurrentHashMap.newKeySet();

    // ============================
    // 1. Mob→プレイヤーのダメージ倍率
    // ============================
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player && event.getSource().getEntity() instanceof Mob) {
            float multiplier = CustomDifficultyCommand.getDamageMultiplier();
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    // ============================
    // 2. Mobスポーン時: HP倍率・バフ効果・援軍スポーン
    // ============================
    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getEntity() == null || !(event.getEntity() instanceof Monster monster)) {
            return;
        }
        if (monster.level().isClientSide) {
            return;
        }

        CustomDifficulty diff = CustomDifficultyCommand.getCurrentDifficulty();
        UUID id = monster.getUUID();

        // --- HP倍率の適用 ---
        if (diff.getHealthMultiplier() > 1.0f && !healthScaled.contains(id)) {
            if (monster.getAttribute(Attributes.MAX_HEALTH) != null) {
                double baseHealth = monster.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
                double newHealth = baseHealth * diff.getHealthMultiplier();
                monster.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newHealth);
                monster.setHealth((float) newHealth);
                healthScaled.add(id);
            }
        }

        // --- 敵対範囲の拡大 (aiLevelに応じて) ---
        if (diff.getAiLevel() >= 1 && monster.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            // aiLevel 0=16(バニラ), 1=24, 2=32, 3=48, 4=64, 5=80
            double baseRange = monster.getAttribute(Attributes.FOLLOW_RANGE).getBaseValue();
            double newRange = baseRange + diff.getAiLevel() * 8.0;
            monster.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(newRange);
        }

        // --- バフ効果の付与 ---
        if (diff.getBuffEffectChance() > 0 && !buffApplied.contains(id)) {
            if (monster.getRandom().nextFloat() < diff.getBuffEffectChance()) {
                applyRandomBuffs(monster, diff.getAiLevel());
                buffApplied.add(id);
            }
        }

        // --- 装備ティアの適用（JSONベース） ---
        if (diff.getEquipmentTier() > 0) {
            EquipmentTierRegistry.applyEquipment(monster, diff.getEquipmentTier());
        }

        // --- 援軍スポーン ---
        if (diff.getReinforceChance() > 0 && !reinforcementSpawned.contains(id)) {
            if (monster.getRandom().nextFloat() < diff.getReinforceChance()) {
                spawnReinforcement(monster);
                reinforcementSpawned.add(id);
            }
        }
    }

    /**
     * ランダムなバフ効果をMobに付与（aiLevelに応じて強度が変化）
     */
    private static void applyRandomBuffs(Monster monster, int aiLevel) {
        int duration = 999999; // 実質永続
        // aiLevelに応じてエフェクトレベルが上がる（lv1=0, lv2=0, lv3=1, lv4=2, lv5=3）
        int amplifier = Math.max(0, aiLevel - 2);

        // aiLevelに応じて付与するバフの数を増やす（lv1=1, lv2=2, lv3=2, lv4=3, lv5=4）
        int buffCount = 1 + aiLevel / 2;
        // lv5では追加ボーナス
        if (aiLevel >= 5) buffCount += 1;
        float roll;

        for (int i = 0; i < buffCount; i++) {
            roll = monster.getRandom().nextFloat();

            if (roll < 0.15f) {
                // 耐性: lv3=I, lv4=II, lv5=III
                monster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, Math.min(amplifier, 3)));
            } else if (roll < 0.28f) {
                // 移動速度: キャップなし
                monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, amplifier));
            } else if (roll < 0.40f) {
                // 攻撃力: lv3=I, lv4=II, lv5=III
                monster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, Math.min(amplifier, 3)));
            } else if (roll < 0.50f) {
                // 火炎耐性
                monster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0));
            } else if (roll < 0.60f) {
                // 再生: lv3=I, lv4=II, lv5=II
                monster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, Math.min(amplifier, 2)));
            } else if (roll < 0.70f) {
                // 吸収: キャップなし
                monster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier + 1));
            } else if (roll < 0.78f) {
                // 透明化
                monster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0));
            } else if (roll < 0.88f) {
                // 体力増強: キャップなし（lv5でHP+80）
                monster.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, duration, amplifier + 1));
                monster.setHealth(monster.getMaxHealth()); // 増加分を即座に回復
            } else {
                // ダメージ増加
                monster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, amplifier));
            }
        }
    }

    /**
     * 援軍をスポーンさせる（元Mobの種類に応じて援軍の種類を決定）
     */
    private static void spawnReinforcement(Monster source) {
        if (!(source.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        EntityType<?> reinforceType = null;

        if (source instanceof Zombie) {
            // ゾンビ → ハスク or ゾンビ追加
            reinforceType = source.getRandom().nextFloat() < 0.3f ? EntityType.HUSK : EntityType.ZOMBIE;
        } else if (source instanceof Skeleton) {
            // スケルトン → ストレイ or スケルトン追加
            reinforceType = source.getRandom().nextFloat() < 0.3f ? EntityType.STRAY : EntityType.SKELETON;
        } else if (source instanceof Spider) {
            // クモ → 洞窟グモ
            reinforceType = EntityType.CAVE_SPIDER;
        } else if (source instanceof Creeper) {
            // クリーパー → クリーパー追加
            reinforceType = EntityType.CREEPER;
        } else if (source instanceof Witch) {
            // ウィッチ → ヴィンディケーター
            reinforceType = EntityType.VINDICATOR;
        } else if (source instanceof AbstractSkeleton) {
            reinforceType = EntityType.SKELETON;
        }

        if (reinforceType == null) {
            return;
        }

        // 元Mobの近くにスポーン
        BlockPos pos = source.blockPosition();
        double x = pos.getX() + (source.getRandom().nextDouble() - 0.5) * 4;
        double z = pos.getZ() + (source.getRandom().nextDouble() - 0.5) * 4;

        var entity = reinforceType.create(serverLevel);
        if (entity != null) {
            entity.moveTo(x, pos.getY(), z, source.getRandom().nextFloat() * 360f, 0f);
            serverLevel.addFreshEntity(entity);
        }
    }

    // ============================
    // 3. ベッド睡眠の制限
    // ============================
    @SubscribeEvent
    public static void onPlayerSleep(PlayerSleepInBedEvent event) {
        CustomDifficulty diff = CustomDifficultyCommand.getCurrentDifficulty();
        if (!diff.isBedSleepEnabled()) {
            event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
            if (event.getEntity() != null && !event.getEntity().level().isClientSide) {
                event.getEntity().displayClientMessage(
                    Component.literal("§4").append(Component.translatable("difficulty.minecraft_armor_weapon.bed_disabled")),
                    true
                );
            }
        }
    }

    // ============================
    // 4. 壁越し感知（追跡範囲の拡大）
    // ============================
    @SubscribeEvent
    public static void onMobTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }
        if (monster.level().isClientSide) {
            return;
        }

        // 高頻度で走る必要のない処理は 20tick (1秒) に 1 回まで削減。
        // Monster は常に多数存在するのでここを絞るだけでサーバー tick 負荷が大幅減。
        boolean slowTick = (monster.tickCount % 20) == 0;
        boolean fastTick = true; // 落下ダメージ無効は追跡中のみなので毎tick必要

        CustomDifficulty diff = CustomDifficultyCommand.getCurrentDifficulty();

        // 壁越し感知: 追跡範囲を拡大 (1秒毎でOK)
        if (slowTick && diff.isWallSenseEnabled() && monster.getTarget() == null) {
            var attr = monster.getAttribute(Attributes.FOLLOW_RANGE);
            if (attr != null) {
                double currentRange = attr.getBaseValue();
                double boostedRange = 32.0 + diff.getAiLevel() * 8.0; // aiLevel 3→56, 5→72
                if (currentRange < boostedRange) {
                    attr.setBaseValue(boostedRange);
                }
            }
        }

        // 落下ダメージ無効: 追跡中の敵は落下ダメージを受けない
        if (fastTick && diff.isFallDmgImmune() && monster.getTarget() != null) {
            monster.fallDistance = 0;
        }
    }

    // ============================
    // 5. メモリクリーンアップ
    // ============================
    @SubscribeEvent
    public static void onMobDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof Monster) {
            UUID id = event.getEntity().getUUID();
            healthScaled.remove(id);
            buffApplied.remove(id);
            reinforcementSpawned.remove(id);
        }
    }
}
