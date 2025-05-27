package minecraftarmorweapon.entity;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.enchantment.Enchantments;

import javax.annotation.Nullable;

public class NiseGenEiKenEntity extends PathfinderMob {
    public NiseGenEiKenEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(MinecraftArmorWeaponModEntities.NISE_GEN_EI_KEN.get(), world);
    }

    public NiseGenEiKenEntity(EntityType<NiseGenEiKenEntity> type, Level world) {
        super(type, world);
        this.setNoAi(false);
        this.setPersistenceRequired();
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private int lifeTicks = 40; // 約2秒で消滅（20tick=1秒）
    private Vec3 moveDirection = null;

    public void setMoveDirection(Vec3 dir) {
        this.moveDirection = dir.normalize();
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (moveDirection != null) {
            this.setDeltaMovement(moveDirection.scale(0.7)); // 速度調整
            this.moveRelative(0.7f, moveDirection);
        }
        // 水の影響を受けない
        this.setNoGravity(true);
        // 寿命
        if (--lifeTicks <= 0) {
            this.discard();
        }
        // 衝突判定
        for (Entity entity : this.level.getEntities(this, this.getBoundingBox().inflate(0.2))) {
            if (entity != this && entity instanceof LivingEntity living && !(entity instanceof Player)) {
                // ダメージ処理
                float damage = 8.0f;
                ItemStack sword = this.getItemBySlot(EquipmentSlot.MAINHAND);
                if (sword != null && sword.isEnchanted()) {
                    // _kill_エンチャントのレベルを参照（仮: KILLエンチャントが存在する場合）
                    int killLevel = sword.getEnchantmentLevel(Enchantments.KILL);
                    if (killLevel > 0) damage += killLevel * 4;
                }
                living.hurt(DamageSource.mobAttack(this), damage);
                this.discard();
                break;
            }
        }
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entityIn) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(1.0f);
    }

    public static void init() {
    }

    public static AttributeSupplier.Builder createAttributes() {
        AttributeSupplier.Builder builder = Mob.createMobAttributes();
        builder = builder.add(Attributes.MOVEMENT_SPEED, 1.0);
        builder = builder.add(Attributes.MAX_HEALTH, 1);
        builder = builder.add(Attributes.ARMOR, 0);
        builder = builder.add(Attributes.ATTACK_DAMAGE, 8);
        builder = builder.add(Attributes.FOLLOW_RANGE, 0);
        builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 1000);
        return builder;
    }

    public void setSword(ItemStack sword) {
        sword.enchant(Enchantments.KILL, 1); // _kill_エンチャントを付与（仮）
        this.setItemSlot(EquipmentSlot.MAINHAND, sword);
    }
}
