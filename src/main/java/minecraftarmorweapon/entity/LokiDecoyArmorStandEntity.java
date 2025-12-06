package minecraftarmorweapon.entity;

import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

/**
 * Loki Decoyのカスタムアーマースタンド
 * mcfunctionを使わず、すべてJavaで実装
 */
public class LokiDecoyArmorStandEntity extends ArmorStand {
    private static final int LIFETIME = 180; // 9秒 (180 ticks)
    private static final int SPIN_START = 150; // 7.5秒後から回転開始
    private static final int SPIN_PHASE_2 = 160; // 8秒
    private static final int SPIN_PHASE_3 = 170; // 8.5秒
    private static final double TAUNT_RADIUS = 10.0;

    private int tickCount = 0;
    private float headRotation = 0.0f;
    private int spinPhase = 0;

    public LokiDecoyArmorStandEntity(EntityType<? extends ArmorStand> type, Level world) {
        super(type, world);
        this.setInvulnerable(true);
        // NBTで設定
        CompoundTag nbt = this.getPersistentData();
        nbt.putBoolean("ShowArms", true);
        nbt.putBoolean("NoBasePlate", true);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level.isClientSide()) {
            return;
        }

        tickCount++;

        // 挑発機能: 近くの敵をターゲットにする
        if (tickCount % 10 == 0) { // 0.5秒ごとにチェック
            tauntNearbyMobs();
        }

        // 爆発前の演出
        if (tickCount >= SPIN_START) {
            handleSpinning();
            spawnParticles();
        }

        // 爆発
        if (tickCount >= LIFETIME) {
            explode();
            this.discard();
        }
    }

    /**
     * 近くの敵MOBを挑発してターゲットにする
     */
    private void tauntNearbyMobs() {
        AABB box = new AABB(
            this.getX() - TAUNT_RADIUS, this.getY() - TAUNT_RADIUS, this.getZ() - TAUNT_RADIUS,
            this.getX() + TAUNT_RADIUS, this.getY() + TAUNT_RADIUS, this.getZ() + TAUNT_RADIUS
        );

        List<Mob> mobs = this.level.getEntitiesOfClass(Mob.class, box, mob -> {
            return mob.isAlive() && !mob.getUUID().equals(this.getUUID());
        });

        for (Mob mob : mobs) {
            mob.setTarget(this);
            // 発光エフェクトを付与
            mob.setGlowingTag(true);
        }
    }

    /**
     * 回転処理
     */
    private void handleSpinning() {
        // 回転速度を段階的に上げる
        if (tickCount == SPIN_START) {
            spinPhase = 1;
            playSoundEffect(SoundEvents.NOTE_BLOCK_PLING, 2.0f, 1.0f);
        } else if (tickCount == SPIN_PHASE_2) {
            spinPhase = 2;
            playSoundEffect(SoundEvents.NOTE_BLOCK_PLING, 2.0f, 1.5f);
        } else if (tickCount == SPIN_PHASE_3) {
            spinPhase = 3;
            playSoundEffect(SoundEvents.NOTE_BLOCK_PLING, 2.0f, 2.0f);
        }

        // 回転速度
        float rotationSpeed = switch (spinPhase) {
            case 1 -> 20.0f;
            case 2 -> 40.0f;
            case 3 -> 60.0f;
            default -> 0.0f;
        };

        headRotation += rotationSpeed;
        if (headRotation >= 360.0f) {
            headRotation -= 360.0f;
        }

        // 頭の回転を設定
        CompoundTag poseTag = new CompoundTag();
        poseTag.putFloat("0", 0.0f);
        poseTag.putFloat("1", headRotation);
        poseTag.putFloat("2", 0.1f);

        CompoundTag headTag = new CompoundTag();
        headTag.put("Head", poseTag);
        this.getPersistentData().put("Pose", headTag);
    }

    /**
     * パーティクル生成
     */
    private void spawnParticles() {
        if (this.level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                ParticleTypes.SMOKE,
                this.getX(), this.getY() + 1.8, this.getZ(),
                2, 0.0, 0.0, 0.0, 0.0
            );
        }
    }

    /**
     * 爆発処理
     */
    private void explode() {
        if (!this.level.isClientSide()) {
            this.level.explode(
                null,
                this.getX(), this.getY(), this.getZ(),
                4.0f,
                Explosion.BlockInteraction.DESTROY
            );

            this.level.playSound(
                null,
                this.blockPosition(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS,
                4.0f, 1.0f
            );
        }
    }

    /**
     * サウンド再生（publicメソッド）
     */
    public void playSoundEffect(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        this.level.playSound(
            null,
            this.blockPosition(),
            sound,
            SoundSource.BLOCKS,
            volume, pitch
        );
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // 無敵
        return false;
    }
}
