package minecraftarmorweapon.entity;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.core.particles.ParticleTypes;

import java.util.List;

@OnlyIn(value = Dist.CLIENT, _interface = ItemSupplier.class)
public class LokiDisarmEntity extends ThrowableItemProjectile {
	private int tickCount = 0;
	private boolean returning = false;
	private boolean chasing = false;
	private int chasingTicks = 0;
	private LivingEntity chaseTarget = null;
	private Entity shooter = null;

	// 一時的にEntityType.SNOWBALLを使用（後でMCreatorでLOKI_DISARMを追加する必要がある）
	public LokiDisarmEntity(PlayMessages.SpawnEntity packet, Level world) {
		super(EntityType.SNOWBALL, world);
	}

	public LokiDisarmEntity(EntityType<? extends ThrowableItemProjectile> type, Level world) {
		super(type, world);
	}

	public LokiDisarmEntity(EntityType<? extends ThrowableItemProjectile> type, double x, double y, double z, Level world) {
		super(type, x, y, z, world);
	}

	public LokiDisarmEntity(EntityType<? extends ThrowableItemProjectile> type, LivingEntity entity, Level world) {
		super(type, entity, world);
		this.shooter = entity;
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public ItemStack getItem() {
		return new ItemStack(Blocks.IRON_BLOCK);
	}

	@Override
	protected net.minecraft.world.item.Item getDefaultItem() {
		return Blocks.IRON_BLOCK.asItem();
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		Entity entity = result.getEntity();

		if (!returning && !chasing && entity != shooter) {
			// 武装解除: MainHandとOffHandのアイテムをドロップさせる
			if (entity instanceof LivingEntity livingEntity) {
				ItemStack mainHandItem = livingEntity.getMainHandItem();
				ItemStack offHandItem = livingEntity.getOffhandItem();

				if (!mainHandItem.isEmpty()) {
					if (!this.level().isClientSide()) {
						ItemEntity itemEntity = new ItemEntity(this.level(), entity.getX(), entity.getY() + 1, entity.getZ(), mainHandItem.copy());
						itemEntity.setDefaultPickUpDelay();
						this.level().addFreshEntity(itemEntity);
					}
					livingEntity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				}

				if (!offHandItem.isEmpty()) {
					if (!this.level().isClientSide()) {
						ItemEntity itemEntity = new ItemEntity(this.level(), entity.getX(), entity.getY() + 1, entity.getZ(), offHandItem.copy());
						itemEntity.setDefaultPickUpDelay();
						this.level().addFreshEntity(itemEntity);
					}
					livingEntity.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
				}

				// サウンド再生
				this.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
					ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.item.break")),
					SoundSource.PLAYERS, 1.0f, 1.0f);
			}

			// エンティティに当たったらチェイスモードに入る
			chasing = true;
			chasingTicks = 0;
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		super.onHitBlock(result);
		// ブロックに当たったら戻る
		returning = true;
	}

	@Override
	public void tick() {
		super.tick();

		if (this.level().isClientSide()) {
			return;
		}

		tickCount++;

		// パーティクル効果 - smoke と crit
		if (this.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
			serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 1, 0.1, 0.1, 0.1, 0.01);
		}

		// チェイスモード処理
		if (chasing) {
			chasingTicks++;

			// チェイス時間が終わったら戻る
			if (chasingTicks > 40) { // 2秒間追跡
				chasing = false;
				returning = true;
			} else {
				// 近くのLivingEntityを探して追跡
				List<LivingEntity> nearbyEntities = this.level().getEntitiesOfClass(
					LivingEntity.class,
					this.getBoundingBox().inflate(8.0),
					entity -> entity != shooter && entity.isAlive()
				);

				if (!nearbyEntities.isEmpty()) {
					// 最も近いエンティティを追跡
					LivingEntity closestEntity = null;
					double closestDistance = Double.MAX_VALUE;

					for (LivingEntity entity : nearbyEntities) {
						double distance = this.distanceToSqr(entity);
						if (distance < closestDistance) {
							closestDistance = distance;
							closestEntity = entity;
						}
					}

					if (closestEntity != null) {
						chaseTarget = closestEntity;

						// ターゲットに向かって飛ぶ
						double dx = chaseTarget.getX() - this.getX();
						double dy = (chaseTarget.getY() + chaseTarget.getEyeHeight() / 2.0) - this.getY();
						double dz = chaseTarget.getZ() - this.getZ();
						double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

						if (distance > 0.1) {
							double speed = 0.6;
							this.setDeltaMovement(dx / distance * speed, dy / distance * speed, dz / distance * speed);
						}
					}
				} else {
					// 近くにエンティティがいない場合は戻る
					chasing = false;
					returning = true;
				}
			}
		}

		// 一定時間経過後は強制的に戻る
		if (tickCount > 100 && !returning) {
			chasing = false;
			returning = true;
		}

		if (returning && shooter != null && shooter.isAlive()) {
			// 射手に向かって飛ぶ
			double dx = shooter.getX() - this.getX();
			double dy = (shooter.getY() + shooter.getEyeHeight() / 2.0) - this.getY();
			double dz = shooter.getZ() - this.getZ();
			double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

			// 近くのアイテムを引き寄せる
			List<ItemEntity> nearbyItems = this.level().getEntitiesOfClass(
				ItemEntity.class,
				this.getBoundingBox().inflate(3.0)
			);

			for (ItemEntity itemEntity : nearbyItems) {
				double idx = shooter.getX() - itemEntity.getX();
				double idy = (shooter.getY() + 0.5) - itemEntity.getY();
				double idz = shooter.getZ() - itemEntity.getZ();
				double itemDist = Math.sqrt(idx * idx + idy * idy + idz * idz);
				if (itemDist > 0.1) {
					itemEntity.setDeltaMovement(idx / itemDist * 0.2, idy / itemDist * 0.2, idz / itemDist * 0.2);
				}
			}

			if (distance < 1.5) {
				// 射手に到達したら消える
				this.discard();
				return;
			}

			// 戻る速度（距離に応じて加速）
			double speed = Math.min(0.8, 0.3 + (tickCount - 60) * 0.01);
			this.setDeltaMovement(dx / distance * speed, dy / distance * speed, dz / distance * speed);
		} else if (returning && (shooter == null || !shooter.isAlive())) {
			// 射手がいない場合は消える
			this.discard();
		}

		// 寿命チェック (最大200tick = 10秒)
		if (tickCount > 200) {
			this.discard();
		}
	}

	// TODO: MCreatorでLOKI_DISARMエンティティを登録後、このメソッドを有効化
	public static LokiDisarmEntity shoot(Level world, LivingEntity entity, RandomSource random, float power, double damage, int knockback) {
		// 一時的にLOKI_DECOYDASUを使用
		LokiDisarmEntity entityProjectile = new LokiDisarmEntity(EntityType.SNOWBALL, entity, world);
		entityProjectile.shooter = entity;
		entityProjectile.shoot(entity.getViewVector(1).x, entity.getViewVector(1).y, entity.getViewVector(1).z, power, 0);
		world.addFreshEntity(entityProjectile);
		world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
			ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.snowball.throw")),
			SoundSource.PLAYERS, 1, 1f / (random.nextFloat() * 0.5f + 1) + (power / 2));
		return entityProjectile;
	}

	public static LokiDisarmEntity shoot(LivingEntity entity, LivingEntity target) {
		// 一時的にLOKI_DECOYDASUを使用
		LokiDisarmEntity entityProjectile = new LokiDisarmEntity(EntityType.SNOWBALL, entity, VersionHelper.getLevel(entity));
		entityProjectile.shooter = entity;
		double dx = target.getX() - entity.getX();
		double dy = target.getY() + target.getEyeHeight() - 1.1;
		double dz = target.getZ() - entity.getZ();
		entityProjectile.shoot(dx, dy - entityProjectile.getY() + Math.hypot(dx, dz) * 0.2F, dz, 1.5f, 12.0F);
		entity.level().addFreshEntity(entityProjectile);
		entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
			ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.snowball.throw")),
			SoundSource.PLAYERS, 1, 1f / (RandomSource.create().nextFloat() * 0.5f + 1));
		return entityProjectile;
	}
}
