package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import com.mojang.math.Vector3f;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import minecraftarmorweapon.util.DamageCalculator;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MagicKatanaComboSystemProcedure {

    // コンボデータを保持するマップ
    private static final Map<UUID, ComboData> COMBO_MAP = new HashMap<>();

    // コンボタイムアウト（ミリ秒）
    private static final long COMBO_TIMEOUT = 2000; // 2秒

    // コンボクラス
    private static class ComboData {
        public int comboCount = 0;
        public long lastAttackTime = 0;
        public Vec3 lastDashDirection = Vec3.ZERO;

        public boolean isComboValid() {
            return System.currentTimeMillis() - lastAttackTime < COMBO_TIMEOUT;
        }

        public void updateCombo() {
            if (isComboValid()) {
                comboCount++;
                if (comboCount > 7) comboCount = 1; // 最大7コンボでリセット
            } else {
                comboCount = 1;
            }
            lastAttackTime = System.currentTimeMillis();
        }
    }

    /**
     * 左クリック攻撃時のコンボ処理
     */
    public static void executeComboAttack(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
        if (!(entity instanceof Player player)) return;
        if (!(world instanceof Level level)) return;

        // プレイヤーのコンボデータを取得または作成
        ComboData combo = COMBO_MAP.computeIfAbsent(player.getUUID(), k -> new ComboData());
        combo.updateCombo();

        // コンボ数に応じた攻撃を実行
        switch (combo.comboCount) {
            case 1 -> executeSlashAttack1(level, player, itemstack);
            case 2 -> executeSlashAttack2(level, player, itemstack);
            case 3 -> executeSpinAttack(level, player, itemstack);
            case 4 -> executeDashSlash(level, player, itemstack, combo);
            case 5 -> executeAerialSlash(level, player, itemstack);
            case 6 -> executeMultiSlash(level, player, itemstack);
            case 7 -> executeFinisherSlash(level, player, itemstack);
        }

        // コンボ数を表示
        player.displayClientMessage(Component.literal("§b§l" + combo.comboCount + " COMBO!"), true);
    }

    /**
     * 1コンボ目：基本斬撃
     */
    private static void executeSlashAttack1(Level world, Player player, ItemStack itemstack) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.position().add(0, player.getBbHeight() * 0.5, 0);

        // 斬撃エフェクト
        createSlashEffect(world, startPos, lookVec, 0.2f, 0.5f, 1.0f); // 青色

        // ダメージ処理
        AABB hitBox = new AABB(startPos.add(lookVec.scale(2)).add(-1, -1, -1),
                               startPos.add(lookVec.scale(4)).add(1, 1, 1));

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitBox);
        for (LivingEntity target : targets) {
            if (target != player) {
                DamageCalculator.dealDamage(player, target, 10.0f, itemstack);
                target.knockback(0.3f, -lookVec.x, -lookVec.z);
            }
        }

        // サウンド
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    /**
     * 2コンボ目：横斬り
     */
    private static void executeSlashAttack2(Level world, Player player, ItemStack itemstack) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
        Vec3 startPos = player.position().add(0, player.getBbHeight() * 0.5, 0);

        // 横斬りエフェクト
        for (int i = -3; i <= 3; i++) {
            Vec3 pos = startPos.add(rightVec.scale(i * 0.3)).add(lookVec.scale(2));
            createSlashEffect(world, pos, lookVec, 0.5f, 0.2f, 1.0f); // 紫色
        }

        // 横範囲ダメージ
        AABB hitBox = new AABB(startPos.add(lookVec.scale(2)).add(rightVec.scale(-2)).add(0, -1, 0),
                               startPos.add(lookVec.scale(3)).add(rightVec.scale(2)).add(0, 1, 0));

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitBox);
        for (LivingEntity target : targets) {
            if (target != player) {
                DamageCalculator.dealDamage(player, target, 12.0f, itemstack);
                Vec3 knockVec = target.position().subtract(player.position()).normalize();
                target.knockback(0.4f, -knockVec.x, -knockVec.z);
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /**
     * 3コンボ目：回転斬り
     */
    private static void executeSpinAttack(Level world, Player player, ItemStack itemstack) {
        Vec3 centerPos = player.position().add(0, player.getBbHeight() * 0.5, 0);

        // 回転エフェクト
        for (int angle = 0; angle < 360; angle += 15) {
            double rad = Math.toRadians(angle);
            double x = Math.cos(rad) * 3;
            double z = Math.sin(rad) * 3;
            Vec3 pos = centerPos.add(x, 0, z);

            if (world instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
        }

        // 円形範囲ダメージ
        AABB hitBox = new AABB(centerPos.add(-3, -1, -3), centerPos.add(3, 1, 3));
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitBox);

        for (LivingEntity target : targets) {
            if (target != player && target.position().distanceTo(player.position()) <= 3.5) {
                DamageCalculator.dealDamage(player, target, 15.0f, itemstack);
                Vec3 knockVec = target.position().subtract(player.position()).normalize();
                target.knockback(0.6f, -knockVec.x, -knockVec.z);
            }
        }

        // 回転モーション
        player.setYRot(player.getYRot() + 360);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.8f);
    }

    /**
     * 4コンボ目：ダッシュ斬り
     */
    private static void executeDashSlash(Level world, Player player, ItemStack itemstack, ComboData combo) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 dashVec = lookVec.scale(5);

        // ダッシュエフェクト
        Vec3 startPos = player.position();
        Vec3 endPos = startPos.add(dashVec);

        // 残像エフェクト
        for (int i = 0; i <= 10; i++) {
            Vec3 pos = startPos.lerp(endPos, i / 10.0);
            if (world instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL,
                    pos.x, pos.y + 1, pos.z, 3, 0.1, 0.5, 0.1, 0);
            }
        }

        // テレポート
        player.teleportTo(endPos.x, endPos.y, endPos.z);

        // ダッシュ経路上のダメージ
        AABB hitBox = new AABB(
            Math.min(startPos.x, endPos.x) - 1, Math.min(startPos.y, endPos.y) - 1, Math.min(startPos.z, endPos.z) - 1,
            Math.max(startPos.x, endPos.x) + 1, Math.max(startPos.y, endPos.y) + 2, Math.max(startPos.z, endPos.z) + 1
        );

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitBox);
        for (LivingEntity target : targets) {
            if (target != player) {
                DamageCalculator.dealDamage(player, target, 18.0f, itemstack);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            }
        }

        combo.lastDashDirection = lookVec;

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    /**
     * 5コンボ目：空中斬撃
     */
    private static void executeAerialSlash(Level world, Player player, ItemStack itemstack) {
        // プレイヤーを浮かせる
        player.setDeltaMovement(player.getDeltaMovement().add(0, 0.8, 0));

        Vec3 pos = player.position().add(0, 2, 0);

        // X字斬撃エフェクト
        for (int i = -5; i <= 5; i++) {
            Vec3 pos1 = pos.add(i * 0.3, i * 0.3, 0);
            Vec3 pos2 = pos.add(i * 0.3, -i * 0.3, 0);

            if (world instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                    pos1.x, pos1.y, pos1.z, 1, 0, 0, 0, 0);
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    pos2.x, pos2.y, pos2.z, 1, 0, 0, 0, 0);
            }
        }

        // 下方向への範囲攻撃
        AABB hitBox = new AABB(pos.add(-3, -5, -3), pos.add(3, 1, 3));
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitBox);

        for (LivingEntity target : targets) {
            if (target != player) {
                DamageCalculator.dealDamage(player, target, 20.0f, itemstack);
                target.knockback(0.3f, 0, 0);
                target.setDeltaMovement(target.getDeltaMovement().add(0, -0.5, 0));
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.5f, 1.0f);
    }

    /**
     * 6コンボ目：連続斬撃
     */
    private static void executeMultiSlash(Level world, Player player, ItemStack itemstack) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.position().add(0, player.getBbHeight() * 0.5, 0);

        // 5連続斬撃
        for (int i = 0; i < 5; i++) {
            final int index = i;
            // 遅延実行
            if (world instanceof ServerLevel serverLevel) {
                serverLevel.getServer().execute(() -> {
                    double angle = (index - 2) * 30;
                    Vec3 rotatedVec = rotateVector(lookVec, angle);
                    Vec3 slashPos = startPos.add(rotatedVec.scale(3));

                    createSlashEffect(world, slashPos, rotatedVec, 1.0f, 0.2f, 0.2f); // 赤色

                    // 各斬撃のダメージ
                    AABB hitBox = new AABB(slashPos.add(-1.5, -1.5, -1.5), slashPos.add(1.5, 1.5, 1.5));
                    List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitBox);

                    for (LivingEntity target : targets) {
                        if (target != player) {
                            DamageCalculator.dealDamage(player, target, 8.0f, itemstack);
                        }
                    }

                    world.playSound(null, slashPos.x, slashPos.y, slashPos.z,
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.5f + index * 0.1f);
                });
            }
        }
    }

    /**
     * 7コンボ目：必殺技
     */
    private static void executeFinisherSlash(Level world, Player player, ItemStack itemstack) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.position().add(0, player.getBbHeight() * 0.5, 0);

        // 巨大斬撃波
        for (int distance = 0; distance <= 15; distance++) {
            final int dist = distance;
            if (world instanceof ServerLevel serverLevel) {
                serverLevel.getServer().execute(() -> {
                    Vec3 wavePos = startPos.add(lookVec.scale(dist));

                    // 斬撃波エフェクト
                    for (int height = -2; height <= 2; height++) {
                        for (int width = -2; width <= 2; width++) {
                            Vec3 effectPos = wavePos.add(
                                new Vec3(-lookVec.z, 0, lookVec.x).scale(width * 0.5)
                            ).add(0, height * 0.5, 0);

                            serverLevel.sendParticles(ParticleTypes.END_ROD,
                                effectPos.x, effectPos.y, effectPos.z, 1, 0, 0, 0, 0);

                            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                effectPos.x, effectPos.y, effectPos.z, 2, 0.1, 0.1, 0.1, 0.05);
                        }
                    }

                    // 波のダメージ
                    AABB hitBox = new AABB(wavePos.add(-2, -2, -2), wavePos.add(2, 2, 2));
                    List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitBox);

                    for (LivingEntity target : targets) {
                        if (target != player && !target.hasEffect(MobEffects.GLOWING)) {
                            DamageCalculator.dealDamage(player, target, 35.0f, itemstack);
                            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                            target.knockback(1.5f, -lookVec.x, -lookVec.z);
                        }
                    }
                });
            }
        }

        // 画面効果
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2));

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 0.5f);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.5f, 1.0f);
    }

    /**
     * 斬撃エフェクトを生成
     */
    private static void createSlashEffect(Level world, Vec3 pos, Vec3 direction, float r, float g, float b) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 perpendicular = new Vec3(-direction.z, 0, direction.x).normalize();

        for (int i = -10; i <= 10; i++) {
            Vec3 effectPos = pos.add(perpendicular.scale(i * 0.15));
            serverLevel.sendParticles(
                new DustParticleOptions(new Vector3f(r, g, b), 1.5f),
                effectPos.x, effectPos.y, effectPos.z, 1, 0, 0, 0, 0
            );
        }

        // 追加の輝きエフェクト
        serverLevel.sendParticles(ParticleTypes.FLASH,
            pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
    }

    /**
     * ベクトルをY軸周りに回転
     */
    private static Vec3 rotateVector(Vec3 vec, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        return new Vec3(
            vec.x * cos - vec.z * sin,
            vec.y,
            vec.x * sin + vec.z * cos
        );
    }

    /**
     * 右クリックでコンボリセット
     */
    public static void resetCombo(Entity entity) {
        if (entity instanceof Player player) {
            ComboData combo = COMBO_MAP.get(player.getUUID());
            if (combo != null) {
                combo.comboCount = 0;
                combo.lastAttackTime = 0;
                player.displayClientMessage(Component.literal("§cCombo Reset"), true);
            }
        }
    }

    /**
     * プレイヤーのコンボデータをクリア（ログアウト時など）
     */
    public static void clearPlayerCombo(Player player) {
        COMBO_MAP.remove(player.getUUID());
    }
}