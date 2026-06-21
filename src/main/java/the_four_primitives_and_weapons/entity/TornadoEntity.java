package the_four_primitives_and_weapons.entity;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import the_four_primitives_and_weapons.util.DamageCalculator;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModCustomEntities;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class TornadoEntity extends Entity {

    private static final EntityDataAccessor<Boolean> WITH_ELECTRICITY = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.FLOAT);

    private Player owner;
    private Vec3 moveDirection;
    private float maxTravelDistance = 24.0f; // 最大飛距離（ブロック数）
    private float tornadoSpeed = 0.3f; // 竜巻の速度（ブロック/tick）
    private int lifespan = 200; // 最大寿命（安全装置）
    private float tornadoHeight = 8.0f; // 竜巻の縦の長さ（8ブロック）
    private float tornadoRadius = 2.0f; // 竜巻の半径
    private float maxHeight = 8.0f;
    private float radius = 16.0f; // 引き寄せ範囲（元のコマンドと同じ）
    private List<LivingEntity> affectedEntities = new ArrayList<>();
    private int tickCount = 0;
    private ItemStack weaponStack = ItemStack.EMPTY;
    private Random random = new Random();
    private Vec3 startPosition; // 開始位置

    public TornadoEntity(EntityType<? extends TornadoEntity> type, Level world) {
        super(type, world);
        this.noPhysics = true;
        this.noCulling = true;
    }

    // Client-side spawn constructor
    public TornadoEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(TheFourPrimitivesAndWeaponsModCustomEntities.TORNADO.get(), world);
    }

    public TornadoEntity(Level world, Player owner, Vec3 position, Vec3 direction, boolean withElectricity, float damage, ItemStack weapon) {
        this(TheFourPrimitivesAndWeaponsModCustomEntities.TORNADO.get(), world);
        this.owner = owner;
        this.moveDirection = direction.normalize();
        this.weaponStack = weapon.copy();
        this.startPosition = position; // 開始位置を記録
        setPos(position.x, position.y, position.z);
        entityData.set(WITH_ELECTRICITY, withElectricity);
        entityData.set(DAMAGE, damage);
        entityData.set(SPEED, tornadoSpeed);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(WITH_ELECTRICITY, false);
        entityData.define(DAMAGE, 10.0f);
        entityData.define(SPEED, tornadoSpeed);
    }

    @Override
    public void tick() {
        super.tick();
        tickCount++;

        // デバッグ: 最初のtickでログ出力
        if (tickCount == 1) {
            the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod.LOGGER.info("TornadoEntity: First tick! Position: {}, {}, {}, Side: {}",
                getX(), getY(), getZ(), level().isClientSide ? "CLIENT" : "SERVER");

            // 最初のtickで生成音を再生
            if (!level().isClientSide) {
                level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 4.0f, 0.8f);
            }

            // 開始位置が未設定の場合（ロード時など）
            if (startPosition == null) {
                startPosition = position();
            }
        }

        // 飛距離チェック
        if (startPosition != null) {
            double traveledDistance = position().distanceTo(startPosition);
            if (traveledDistance >= maxTravelDistance) {
                the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod.LOGGER.info("TornadoEntity: Max travel distance reached! Distance: {}", traveledDistance);
                if (!level().isClientSide) {
                    spawnDespawnEffect();
                }
                discard();
                return;
            }
        }

        // 寿命チェック（安全装置）
        if (tickCount >= lifespan) {
            the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod.LOGGER.info("TornadoEntity: Lifespan reached, discarding");
            if (!level().isClientSide) {
                spawnDespawnEffect();
            }
            discard();
            return;
        }

        // ブロック衝突チェック
        if (!level().isClientSide && checkBlockCollision()) {
            spawnDespawnEffect();
            discard();
            return;
        }

        // 移動（両側で同期）
        moveAlongPath();

        if (!level().isClientSide) {
            // サーバー側の処理

            // エフェクトと攻撃処理
            if (tickCount % 2 == 0) {
                applyTornadoEffects();
            }

            // ダメージ処理（5tickごと）
            if (tickCount % 5 == 0) {
                damageNearbyEntities();
            }

            // サーバー側でパーティクルを生成してクライアントに送信
            // (旧: 2 tick ごと → 4 tick ごとに変更。 体感差はほぼ無いがネットワーク負荷半減)
            if (tickCount % 4 == 0 && level() instanceof ServerLevel serverLevel) {
                createServerParticles(serverLevel);
            }

            // 竜巻の音を定期的に再生
            if (tickCount % 10 == 0) {
                level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 4.0f, 0.8f);
            }

            // Storm 系 (withElectricity=true) の竜巻内で雷を生成
            //   30 tick (1.5 秒) ごとに 1 本、 竜巻内のランダム位置に visual_only lightning を落とす。
            //   visual_only=true で建物破壊なし。 ダメージは TornadoEntity 本体の damageNearbyEntities が担当。
            if (entityData.get(WITH_ELECTRICITY) && tickCount % 30 == 0 && level() instanceof ServerLevel sl) {
                spawnInternalLightning(sl);
            }
        }
        // 旧: クライアント側でも createVisualEffects() を呼んでいたが、
        //     サーバーから sendParticles で broadcast したものと重複して
        //     ローカル client は 2 倍のパーティクルを描画 → 重い + 雲が増殖。
        //     server 側送信だけに統一して負荷を半減。
    }

    private void moveAlongPath() {
        // moveDirectionがnullの場合はデフォルト方向を設定
        if (moveDirection == null) {
            moveDirection = new Vec3(0, 0, 1);
        }

        Vec3 currentPos = position();
        float speed = entityData.get(SPEED);

        // 水平成分が極小 (真上 / 真下に向けて撃った時) は moveDirection をそのまま 3D で使う。
        // そうしないと normalize() がゼロを返し、 竜巻がその場で静止 = "発生場所に残るやつ" バグ。
        double horizMagSq = moveDirection.x * moveDirection.x + moveDirection.z * moveDirection.z;
        if (horizMagSq < 1.0E-4) {
            // 真上 / 真下向き — Y 軸込みで移動 (上昇 / 下降する竜巻)
            Vec3 dir3d = moveDirection.normalize();
            Vec3 newPos = currentPos.add(dir3d.scale(speed));
            setPos(newPos.x, newPos.y, newPos.z);
            return;
        }

        // 通常: Y 成分を切り捨てて水平のみで移動 (地表を走る竜巻)
        Vec3 horizontalDirection = new Vec3(moveDirection.x, 0, moveDirection.z).normalize();
        Vec3 newPos = currentPos.add(horizontalDirection.scale(speed));
        setPos(newPos.x, currentPos.y, newPos.z);
    }

    private boolean checkBlockCollision() {
        Vec3 pos = position();
        // 竜巻の中心部分（コア）がブロックに当たったかチェック
        // 高さの中心部分（3-5ブロック目）のみをチェック
        int solidBlockCount = 0;
        int totalCheckPoints = 0;

        // 竜巻の中心高さ（3-5ブロック）のみチェック
        for (double h = 3.0; h <= 5.0; h += 1.0) {
            double currentRadius = 1.0 + (h * 0.1);

            // 中心部分（半径の60%）を8方向でチェック
            for (int i = 0; i < 8; i++) {
                double angle = (i / 8.0) * Math.PI * 2;
                double xOffset = Math.cos(angle) * currentRadius * 0.6;
                double zOffset = Math.sin(angle) * currentRadius * 0.6;

                int blockX = (int) Math.floor(pos.x + xOffset);
                int blockY = (int) Math.floor(pos.y + h);
                int blockZ = (int) Math.floor(pos.z + zOffset);

                totalCheckPoints++;

                if (!level().getBlockState(new net.minecraft.core.BlockPos(blockX, blockY, blockZ)).isAir()) {
                    solidBlockCount++;
                }
            }
        }

        // 60%以上がブロックに当たっている場合
        if (totalCheckPoints > 0 && (solidBlockCount / (double)totalCheckPoints) >= 0.6) {
            the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod.LOGGER.info("TornadoEntity: Block collision detected! solidBlockCount={}, totalCheckPoints={}, ratio={}",
                solidBlockCount, totalCheckPoints, (solidBlockCount / (double)totalCheckPoints));
            return true;
        }

        return false;
    }

    private void spawnDespawnEffect() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        Vec3 pos = position();

        // 爆発パーティクル
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
            pos.x, pos.y + maxHeight / 2, pos.z,
            10, radius / 2, maxHeight / 4, radius / 2, 0.1);

        // POOFパーティクル（煙）
        serverLevel.sendParticles(ParticleTypes.POOF,
            pos.x, pos.y + tornadoHeight / 2, pos.z,
            50, 1, tornadoHeight / 4, 1, 0.2);

        // 消滅音を再生
        level().playSound(null, pos.x, pos.y, pos.z,
            SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.5f, 1.2f);
    }

    private void applyTornadoEffects() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        Vec3 pos = position();
        boolean withElectricity = entityData.get(WITH_ELECTRICITY);

        // 竜巻の物理効果（元のコマンドの範囲：distance=..16）
        AABB searchArea = new AABB(
            pos.x - radius, pos.y - 1, pos.z - radius,
            pos.x + radius, pos.y + maxHeight, pos.z + radius
        );

        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> entity != owner && entity.isAlive());

        for (LivingEntity target : targets) {
            Vec3 targetPos = target.position();
            double distance = pos.distanceTo(targetPos);

            if (distance <= radius) {
                // Slow Falling効果（元のコマンドと同じ）
                target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 100, false, false));
                // Glowing効果（元のコマンドと同じ）
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20, 100, false, false));

                // 竜巻に巻き込む
                liftAndRotateEntity(target, pos);

                // 感電エフェクト（StormItemの場合）
                if (withElectricity) {
                    // Snowflakeパーティクル
                    serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                        target.getX(), target.getY(), target.getZ(),
                        1, 0.1, 0, 0.1, 0.001);
                }

                // 影響を受けたエンティティのリストに追加
                if (!affectedEntities.contains(target)) {
                    affectedEntities.add(target);
                }
            }
        }
    }

    private void damageNearbyEntities() {
        if (!(level() instanceof ServerLevel)) return;

        Vec3 pos = position();
        float damage = entityData.get(DAMAGE);

        // ダメージ範囲内のエンティティを検索
        AABB damageArea = new AABB(
            pos.x - radius, pos.y - 1, pos.z - radius,
            pos.x + radius, pos.y + maxHeight * 0.7, pos.z + radius
        );

        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, damageArea,
            entity -> entity != owner && affectedEntities.contains(entity));

        for (LivingEntity target : targets) {
            // ダメージを与える
            if (owner != null) {
                DamageCalculator.dealDamage(owner, target, damage, weaponStack);
            } else {
                target.hurt(target.damageSources().magic(), damage);
            }
        }
    }

    /**
     * Storm 系竜巻の内部で雷を生成する (visual_only)。
     * 中心からランダム横方向 + 竜巻高さ範囲の上部 70% に配置 — 竜巻の中で稲妻が落ちる演出。
     */
    private void spawnInternalLightning(ServerLevel sl) {
        double ang = random.nextDouble() * Math.PI * 2;
        double r = random.nextDouble() * tornadoRadius * 0.8;
        double dx = Math.cos(ang) * r;
        double dz = Math.sin(ang) * r;
        // 上部寄りに落とす (竜巻の上から下に走る稲妻の感)
        double dy = tornadoHeight * (0.3 + random.nextDouble() * 0.6);
        double x = getX() + dx;
        double y = getY() + dy;
        double z = getZ() + dz;

        net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt == null) return;
        bolt.moveTo(x, y, z, 0f, 0f);
        bolt.setVisualOnly(true); // ブロック破壊なし、 ダメージは tornado 本体が担当
        if (owner instanceof net.minecraft.server.level.ServerPlayer sp) {
            bolt.setCause(sp);
        }
        sl.addFreshEntity(bolt);
    }

    private void liftAndRotateEntity(LivingEntity entity, Vec3 tornadoCenter) {
        Vec3 entityPos = entity.position();
        Vec3 toCenter = tornadoCenter.subtract(entityPos);
        double horizontalDistance = Math.sqrt(toCenter.x * toCenter.x + toCenter.z * toCenter.z);

        // 元のコマンドの動き：tp @s ^2 ^0.3 ^0.6 facing entity tornado
        // エンティティを竜巻の方向に引き寄せながら、横と上に移動させる

        // 竜巻の方向を向く
        Vec3 direction = toCenter.normalize();

        // 横方向の移動（回転）- ^2（右に2ブロック）
        double angle = Math.atan2(toCenter.z, toCenter.x);
        double sideX = Math.cos(angle + Math.PI / 2) * 2.0;
        double sideZ = Math.sin(angle + Math.PI / 2) * 2.0;

        // 上方向の移動 - ^0.3（上に0.3ブロック）
        double upMotion = 0.3;

        // 前方向の移動 - ^0.6（前に0.6ブロック、竜巻の方向）
        double forwardMotion = 0.6;

        // 運動量を設定
        entity.setDeltaMovement(
            direction.x * forwardMotion + sideX * 0.1,
            upMotion,
            direction.z * forwardMotion + sideZ * 0.1
        );

        // 落下ダメージを無効化
        entity.fallDistance = 0;

        // 竜巻の回転に合わせてプレイヤーを回転させる。
        //   竜巻の螺旋アニメは tickCount * SPIN_DEG_PER_TICK で進む (createServerParticles と同じ周期)。
        //   - 通常 entity: setYRot だけだと client に伝わらないので setYBodyRot + setYHeadRot もセット。
        //   - ServerPlayer: 視点を実際に回すには connection.teleport で yaw を強制する必要がある。
        float SPIN_DEG_PER_TICK = 35f; // ~ 半秒で一回転
        float newYaw = (tickCount * SPIN_DEG_PER_TICK) % 360f;
        entity.setYRot(newYaw);
        entity.yRotO = newYaw;
        entity.setYBodyRot(newYaw);
        entity.setYHeadRot(newYaw);
        if (entity instanceof net.minecraft.server.level.ServerPlayer sp) {
            // Vanilla の Connection.teleport は yaw / pitch を強制する。 同じ位置に飛ばすので
            // 移動は発生せず、 視点だけが回転する。
            sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), newYaw, sp.getXRot());
        }
    }

    private void createServerParticles(ServerLevel serverLevel) {
        Vec3 pos = position();
        boolean withElectricity = entityData.get(WITH_ELECTRICITY);
        double timeOffset = tickCount * 0.3;

        // 縦型竜巻エフェクト (軽量化版):
        //   - 行の刻みを 1.0 → 1.5 に広げて 9 行 → 6 行
        //   - 各行のパーティクル数を currentRadius*8 → currentRadius*4 に半減
        //   - POOF の速度を 5 (高速で遠くへ飛ぶ) → 0.01 に大幅減速 (鬱陶しい雲飛散の修正)
        //   - POOF の spread を (1, currentRadius, 0.1) → (0.05, 0.05, 0.05) に縮小
        //   - SWEEP_ATTACK を 2 個に 1 個 → 4 個に 1 個に削減
        for (double h = 0; h <= tornadoHeight; h += 1.5) {
            double currentRadius = 1.0 + (h * 0.1);
            int particleCount = Math.max(1, (int)(currentRadius * 4));
            for (int i = 0; i < particleCount; i++) {
                double angle = (i / (double)particleCount) * Math.PI * 2 + h * 0.5 + timeOffset;
                double xOffset = Math.cos(angle) * currentRadius;
                double zOffset = Math.sin(angle) * currentRadius;

                // POOF: count=1 のみ、 ほぼ動かない (speed 0.01, spread 0.05)
                serverLevel.sendParticles(ParticleTypes.POOF,
                    pos.x + xOffset, pos.y + h, pos.z + zOffset,
                    1, 0.05, 0.05, 0.05, 0.01);

                // SWEEP_ATTACK は 4 個に 1 個に削減
                if (i % 4 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        pos.x + xOffset, pos.y + h, pos.z + zOffset,
                        1, 0, 0, 0, 0);
                }

                // 感電 (Storm): 4 個に 1 個に削減
                if (withElectricity && i % 4 == 0) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        pos.x + xOffset, pos.y + h, pos.z + zOffset,
                        1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }

        // 中心の雲パーティクル: count 5→2, spread 縮小
        serverLevel.sendParticles(ParticleTypes.CLOUD,
            pos.x, pos.y + tornadoHeight / 2, pos.z,
            2, 0.2, 0.5, 0.2, 0.01);

        // 地面の巻き上げ効果: 10 個 → 6 個
        for (int i = 0; i < 6; i++) {
            double angle = (i / 6.0) * Math.PI * 2 + timeOffset;
            double groundRadius = 1.2;
            serverLevel.sendParticles(ParticleTypes.POOF,
                pos.x + Math.cos(angle) * groundRadius,
                pos.y + 0.1,
                pos.z + Math.sin(angle) * groundRadius,
                1, 0, 0, 0, 0.02);
        }
    }

    private void createVisualEffects() {
        Vec3 pos = position();
        boolean withElectricity = entityData.get(WITH_ELECTRICITY);
        double timeOffset = tickCount * 0.3; // 回転速度を上げる

        // 縦型竜巻エフェクト（上に行くほど広がる）
        for (double h = 0; h <= tornadoHeight; h += 1.0) {
            // 上に行くほど広がる（元のコマンドと同じ）
            // h=0: 1.0, h=1: 1.1, h=2: 1.2, ..., h=8: 1.8
            double currentRadius = 1.0 + (h * 0.1);

            // 螺旋状のパーティクル
            int particleCount = (int)(currentRadius * 12);
            for (int i = 0; i < particleCount; i++) {
                double angle = (i / (double)particleCount) * Math.PI * 2 + h * 0.5 + timeOffset;
                double xOffset = Math.cos(angle) * currentRadius;
                double zOffset = Math.sin(angle) * currentRadius;

                // poof パーティクル
                level().addParticle(ParticleTypes.POOF,
                    pos.x + xOffset, pos.y + h, pos.z + zOffset,
                    xOffset * 0.05, 0.1, zOffset * 0.05);

                // sweep_attack パーティクル
                if (i % 2 == 0) {
                    level().addParticle(ParticleTypes.SWEEP_ATTACK,
                        pos.x + xOffset, pos.y + h, pos.z + zOffset,
                        0, 0, 0);
                }

                // 感電エフェクト（StormItemの場合）
                if (withElectricity && i % 3 == 0 && Math.random() < 0.3) {
                    level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                        pos.x + xOffset, pos.y + h, pos.z + zOffset,
                        0, 0, 0);
                }
            }
        }

        // 中心の雲パーティクル
        for (int i = 0; i < 3; i++) {
            level().addParticle(ParticleTypes.CLOUD,
                pos.x + (random.nextDouble() - 0.5) * 0.5,
                pos.y + tornadoHeight / 2 + (random.nextDouble() - 0.5) * 1.0,
                pos.z + (random.nextDouble() - 0.5) * 0.5,
                0, 0.1, 0);
        }

        // 地面の巻き上げ効果
        for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0) * Math.PI * 2 + timeOffset;
            double groundRadius = 1.2;
            level().addParticle(ParticleTypes.POOF,
                pos.x + Math.cos(angle) * groundRadius,
                pos.y + 0.1,
                pos.z + Math.sin(angle) * groundRadius,
                0, 0.05, 0);
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();

        // 竜巻が消える時のエフェクト
        if (level() instanceof ServerLevel serverLevel) {
            Vec3 pos = position();
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                pos.x, pos.y + maxHeight / 2, pos.z,
                10, radius / 2, maxHeight / 4, radius / 2, 0.1);

            level().playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.5f, 1.2f);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        lifespan = compound.getInt("Lifespan");
        tickCount = compound.getInt("TickCount");
        radius = compound.getFloat("Radius");
        maxHeight = compound.getFloat("MaxHeight");

        // 開始位置の読み込み
        if (compound.contains("StartPosX")) {
            startPosition = new Vec3(
                compound.getDouble("StartPosX"),
                compound.getDouble("StartPosY"),
                compound.getDouble("StartPosZ")
            );
        }

        // moveDirectionの読み込み
        if (compound.contains("DirectionX")) {
            moveDirection = new Vec3(
                compound.getDouble("DirectionX"),
                compound.getDouble("DirectionY"),
                compound.getDouble("DirectionZ")
            );
        } else {
            // デフォルト値（前方向）
            moveDirection = new Vec3(0, 0, 1);
        }

        if (compound.hasUUID("Owner")) {
            if (level() instanceof ServerLevel serverLevel) {
                Entity entity = serverLevel.getEntity(compound.getUUID("Owner"));
                if (entity instanceof Player) {
                    owner = (Player) entity;
                }
            }
        }
        if (compound.contains("Weapon")) {
            weaponStack = ItemStack.of(compound.getCompound("Weapon"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("Lifespan", lifespan);
        compound.putInt("TickCount", tickCount);
        compound.putFloat("Radius", radius);
        compound.putFloat("MaxHeight", maxHeight);

        // 開始位置の保存
        if (startPosition != null) {
            compound.putDouble("StartPosX", startPosition.x);
            compound.putDouble("StartPosY", startPosition.y);
            compound.putDouble("StartPosZ", startPosition.z);
        }

        // moveDirectionの保存
        if (moveDirection != null) {
            compound.putDouble("DirectionX", moveDirection.x);
            compound.putDouble("DirectionY", moveDirection.y);
            compound.putDouble("DirectionZ", moveDirection.z);
        }

        if (owner != null) {
            compound.putUUID("Owner", owner.getUUID());
        }
        if (!weaponStack.isEmpty()) {
            compound.put("Weapon", weaponStack.save(new CompoundTag()));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public void setSpeed(float speed) {
        entityData.set(SPEED, speed);
    }

    public void setDamage(float damage) {
        entityData.set(DAMAGE, damage);
    }

    public void setLifespan(int ticks) {
        this.lifespan = ticks;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void setMaxHeight(float height) {
        this.maxHeight = height;
    }

    public void setOwner(Player player) {
        this.owner = player;
    }

    public void setDirection(Vec3 direction) {
        this.moveDirection = direction.normalize();
    }

    public void setWithElectricity(boolean withElectricity) {
        entityData.set(WITH_ELECTRICITY, withElectricity);
    }

    public void setWeapon(ItemStack weapon) {
        this.weaponStack = weapon.copy();
    }
}
