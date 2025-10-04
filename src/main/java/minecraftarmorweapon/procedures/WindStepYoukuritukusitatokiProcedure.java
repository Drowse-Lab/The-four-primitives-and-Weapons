package minecraftarmorweapon.procedures;

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

public class WindStepYoukuritukusitatokiProcedure {
	public static void execute(Entity entity) {
		minecraftarmorweapon.MinecraftArmorWeaponMod.LOGGER.info("WindStepYoukuritukusitatokiProcedure.execute() called!");

		if (entity == null) {
			minecraftarmorweapon.MinecraftArmorWeaponMod.LOGGER.warn("WindStep: Entity is null!");
			return;
		}

		minecraftarmorweapon.MinecraftArmorWeaponMod.LOGGER.info("WindStep: Entity: {}, Client side: {}", entity, entity.level.isClientSide);

		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()) {
			minecraftarmorweapon.MinecraftArmorWeaponMod.LOGGER.info("WindStep: Item check passed!");

			if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
				_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.WIND_STEP_EFFECT.get(), 100, 1, true, false));

			// 竜巻エンティティの召喚
			if (!entity.level.isClientSide && entity instanceof Player player) {
				Level level = entity.level;
				ItemStack mainHandItem = player.getMainHandItem();

				// デバッグ: エンティティタイプの確認
				minecraftarmorweapon.MinecraftArmorWeaponMod.LOGGER.info("WindStep: Attempting to spawn tornado...");
				minecraftarmorweapon.MinecraftArmorWeaponMod.LOGGER.info("WindStep: EntityType present: {}", MinecraftArmorWeaponModCustomEntities.TORNADO.isPresent());

				if (!MinecraftArmorWeaponModCustomEntities.TORNADO.isPresent()) {
					minecraftarmorweapon.MinecraftArmorWeaponMod.LOGGER.error("WindStep: TORNADO EntityType is not registered!");
					return;
				}

				// プレイヤーの向いている方向を取得
				Vec3 lookVec = player.getLookAngle();
				// プレイヤーの前方2ブロックに竜巻を生成
				Vec3 spawnPos = player.position().add(lookVec.scale(2.0));

				// 武器のダメージを計算
				float baseDamage = getWeaponDamage(mainHandItem);
				float damage = DamageCalculator.calculateDamage(player, null, baseDamage + 4.0f, mainHandItem);

				// 竜巻エンティティを作成
				TornadoEntity tornado = new TornadoEntity(MinecraftArmorWeaponModCustomEntities.TORNADO.get(), level);
				tornado.setOwner(player);
				tornado.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
				tornado.setDirection(lookVec);
				tornado.setWithElectricity(false); // 感電効果なし
				tornado.setDamage(damage);
				tornado.setWeapon(mainHandItem);
				tornado.setSpeed(1.0f);
				tornado.setLifespan(240); // 12秒間
				tornado.setRadius(5.0f);
				tornado.setMaxHeight(20.0f);

				boolean spawned = level.addFreshEntity(tornado);
				minecraftarmorweapon.MinecraftArmorWeaponMod.LOGGER.info("WindStep: Tornado spawned: {} at {}, {}, {}", spawned, spawnPos.x, spawnPos.y, spawnPos.z);

				// サウンド
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.TRIDENT_RIPTIDE_2, SoundSource.PLAYERS, 1.0f, 1.2f);
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
