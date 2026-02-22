package minecraftarmorweapon.procedures;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.init.MinecraftArmorWeaponModCustomEntities;
import minecraftarmorweapon.entity.TornadoEntity;
import minecraftarmorweapon.util.DamageCalculator;

public class StormYoukuritukusitatokiProcedure {
	public static void execute(Entity entity) {
		minecraftarmorweapon.MinecraftArmorWeaponMod.LOGGER.info("StormYoukuritukusitatokiProcedure.execute() called!");

		if (entity == null) {
			minecraftarmorweapon.MinecraftArmorWeaponMod.LOGGER.warn("Storm: Entity is null!");
			return;
		}

		minecraftarmorweapon.MinecraftArmorWeaponMod.LOGGER.info("Storm: Entity: {}, Client side: {}", entity, entity.level.isClientSide);
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()) {
			minecraftarmorweapon.MinecraftArmorWeaponMod.LOGGER.info("Storm: Item check passed!");
			if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
				_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.STORM_EFFECT.get(), 100, 2, true, false));

			// 竜巻エンティティの召喚（感電効果付き）
			if (!entity.level.isClientSide && entity instanceof Player player) {
				Level level = VersionHelper.getLevel(entity);
				ItemStack mainHandItem = player.getMainHandItem();

				// デバッグ: エンティティタイプの確認
				minecraftarmorweapon.MinecraftArmorWeaponMod.LOGGER.info("Storm: Attempting to spawn tornadoes...");
				minecraftarmorweapon.MinecraftArmorWeaponMod.LOGGER.info("Storm: EntityType present: {}", MinecraftArmorWeaponModCustomEntities.TORNADO.isPresent());

				if (!MinecraftArmorWeaponModCustomEntities.TORNADO.isPresent()) {
					minecraftarmorweapon.MinecraftArmorWeaponMod.LOGGER.error("Storm: TORNADO EntityType is not registered!");
					return;
				}

				// プレイヤーの向いている方向を取得
				Vec3 lookVec = player.getLookAngle();
				// プレイヤーの前方2ブロックに竜巻を生成
				Vec3 spawnPos = player.position().add(lookVec.scale(2.0));

				// 武器のダメージを計算
				float baseDamage = getWeaponDamage(mainHandItem);
				float damage = DamageCalculator.calculateDamage(player, null, baseDamage + 5.0f, mainHandItem);

				// 中央の竜巻エンティティを作成
				TornadoEntity tornado = new TornadoEntity(MinecraftArmorWeaponModCustomEntities.TORNADO.get(), level);
				tornado.setOwner(player);
				tornado.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
				tornado.setDirection(lookVec);
				tornado.setWithElectricity(true); // 感電効果あり
				tornado.setDamage(damage);
				tornado.setWeapon(mainHandItem);
				tornado.setSpeed(0.8f);
				tornado.setLifespan(200); // 10秒間
				tornado.setRadius(4.0f);
				tornado.setMaxHeight(15.0f);

				boolean spawned = level.addFreshEntity(tornado);
				minecraftarmorweapon.MinecraftArmorWeaponMod.LOGGER.info("Storm: Center tornado spawned: {} at {}, {}, {}", spawned, spawnPos.x, spawnPos.y, spawnPos.z);

				// 追加の竜巻を生成（扇状に3つ）
				for (int i = -1; i <= 1; i++) {
					if (i == 0) continue; // 中央は既に生成済み

					// 左右15度ずつずらした方向
					double angle = Math.toRadians(i * 15);
					double newX = lookVec.x * Math.cos(angle) - lookVec.z * Math.sin(angle);
					double newZ = lookVec.x * Math.sin(angle) + lookVec.z * Math.cos(angle);
					Vec3 newDirection = new Vec3(newX, lookVec.y, newZ).normalize();
					Vec3 sideStartPos = player.position().add(newDirection.scale(2.0));

					TornadoEntity sideTornado = new TornadoEntity(MinecraftArmorWeaponModCustomEntities.TORNADO.get(), level);
					sideTornado.setOwner(player);
					sideTornado.setPos(sideStartPos.x, sideStartPos.y, sideStartPos.z);
					sideTornado.setDirection(newDirection);
					sideTornado.setWithElectricity(true);
					sideTornado.setDamage(damage * 0.8f);
					sideTornado.setWeapon(mainHandItem);
					sideTornado.setSpeed(0.8f);
					sideTornado.setLifespan(160); // 少し短め
					sideTornado.setRadius(3.0f); // 少し小さめ
					sideTornado.setMaxHeight(12.0f);

					boolean sideSpawned = level.addFreshEntity(sideTornado);
					minecraftarmorweapon.MinecraftArmorWeaponMod.LOGGER.info("Storm: Side tornado {} spawned: {}", i, sideSpawned);
				}

				// サウンド
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.0f);
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.0f, 0.8f);
			}
		}
	}

	private static float getWeaponDamage(ItemStack weapon) {
		if (weapon.isEmpty()) return 4.0f;

		float baseDamage = 0.0f;

		if (weapon.getItem() instanceof net.minecraft.world.item.SwordItem swordItem) {
			baseDamage = swordItem.getDamage();
		} else {
			var attributes = weapon.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
			if (attributes.containsKey(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
				for (var modifier : attributes.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
					baseDamage += (float) modifier.getAmount();
				}
			}
			if (baseDamage == 0.0f) {
				baseDamage = 7.0f;
			}
		}

		return baseDamage;
	}
}
