
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package minecraftarmorweapon.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;

import minecraftarmorweapon.entity.TestBowEntity;
import minecraftarmorweapon.entity.SkeltonMobEntity;
import minecraftarmorweapon.entity.Ruami284Entity;
import minecraftarmorweapon.entity.Reisame284Entity;
import minecraftarmorweapon.entity.OtiruyoEntity;
import minecraftarmorweapon.entity.MeteorArrowEntity;
import minecraftarmorweapon.entity.LokiDecoydasuEntity;
import minecraftarmorweapon.entity.KillotiruEntity;
import minecraftarmorweapon.entity.KatanaTobuEntity;
import minecraftarmorweapon.entity.HrmcngsEntity;
import minecraftarmorweapon.entity.FlyingAttackerEntity;
import minecraftarmorweapon.entity.CometKillEntity;
import minecraftarmorweapon.entity.CometEntity;
import minecraftarmorweapon.entity.BlackholeEntity;
import minecraftarmorweapon.entity.AlchemyCraftBlockEntityEntity;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MinecraftArmorWeaponModEntities {
	public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MinecraftArmorWeaponMod.MODID);
	public static final RegistryObject<EntityType<SkeltonMobEntity>> SKELTON_MOB = register("skelton_mob",
			EntityType.Builder.<SkeltonMobEntity>of(SkeltonMobEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(SkeltonMobEntity::new)

					.sized(0.6f, 1.5f));
	public static final RegistryObject<EntityType<OtiruyoEntity>> OTIRUYO = register("otiruyo",
			EntityType.Builder.<OtiruyoEntity>of(OtiruyoEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(OtiruyoEntity::new).fireImmune().sized(1.2f, 3.6f));
	public static final RegistryObject<EntityType<HrmcngsEntity>> HRMCNGS = register("hrmcngs",
			EntityType.Builder.<HrmcngsEntity>of(HrmcngsEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(HrmcngsEntity::new)

					.sized(0.9f, 0.9f));
	public static final RegistryObject<EntityType<Ruami284Entity>> RUAMI_284 = register("ruami_284",
			EntityType.Builder.<Ruami284Entity>of(Ruami284Entity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(Ruami284Entity::new)

					.sized(0.9f, 0.9f));
	public static final RegistryObject<EntityType<KillotiruEntity>> KILLOTIRU = register("killotiru", EntityType.Builder.<KillotiruEntity>of(KillotiruEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64)
			.setUpdateInterval(3).setCustomClientFactory(KillotiruEntity::new).fireImmune().sized(1.2f, 3.6f));
	public static final RegistryObject<EntityType<KatanaTobuEntity>> KATANA_TOBU = register("projectile_katana_tobu",
			EntityType.Builder.<KatanaTobuEntity>of(KatanaTobuEntity::new, MobCategory.MISC).setCustomClientFactory(KatanaTobuEntity::new).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final RegistryObject<EntityType<BlackholeEntity>> BLACKHOLE = register("blackhole",
			EntityType.Builder.<BlackholeEntity>of(BlackholeEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(1).setUpdateInterval(3).setCustomClientFactory(BlackholeEntity::new).fireImmune().sized(0.8f, 0.8f));
	public static final RegistryObject<EntityType<LokiDecoydasuEntity>> LOKI_DECOYDASU = register("projectile_loki_decoydasu",
			EntityType.Builder.<LokiDecoydasuEntity>of(LokiDecoydasuEntity::new, MobCategory.MISC).setCustomClientFactory(LokiDecoydasuEntity::new).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final RegistryObject<EntityType<TestBowEntity>> TEST_BOW = register("projectile_test_bow",
			EntityType.Builder.<TestBowEntity>of(TestBowEntity::new, MobCategory.MISC).setCustomClientFactory(TestBowEntity::new).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final RegistryObject<EntityType<CometEntity>> COMET = register("comet",
			EntityType.Builder.<CometEntity>of(CometEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(0).setUpdateInterval(3).setCustomClientFactory(CometEntity::new).fireImmune().sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<CometKillEntity>> COMET_KILL = register("comet_kill",
			EntityType.Builder.<CometKillEntity>of(CometKillEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(0).setUpdateInterval(3).setCustomClientFactory(CometKillEntity::new).fireImmune().sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<MeteorArrowEntity>> METEOR_ARROW = register("meteor_arrow", EntityType.Builder.<MeteorArrowEntity>of(MeteorArrowEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true)
			.setTrackingRange(0).setUpdateInterval(3).setCustomClientFactory(MeteorArrowEntity::new).fireImmune().sized(0.5f, 0.5f));
	public static final RegistryObject<EntityType<AlchemyCraftBlockEntityEntity>> ALCHEMY_CRAFT_BLOCK_ENTITY = register("alchemy_craft_block_entity",
			EntityType.Builder.<AlchemyCraftBlockEntityEntity>of(AlchemyCraftBlockEntityEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)
					.setCustomClientFactory(AlchemyCraftBlockEntityEntity::new)

					.sized(0.1f, 0.1f));
	public static final RegistryObject<EntityType<Reisame284Entity>> REISAME_284 = register("reisame_284",
			EntityType.Builder.<Reisame284Entity>of(Reisame284Entity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(Reisame284Entity::new)

					.sized(0.9f, 0.9f));
	public static final RegistryObject<EntityType<FlyingAttackerEntity>> FLYING_ATTACKER = register("flying_attacker",
			EntityType.Builder.<FlyingAttackerEntity>of(FlyingAttackerEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(FlyingAttackerEntity::new)

					.sized(0.6f, 1.8f));

	private static <T extends Entity> RegistryObject<EntityType<T>> register(String registryname, EntityType.Builder<T> entityTypeBuilder) {
		return REGISTRY.register(registryname, () -> (EntityType<T>) entityTypeBuilder.build(registryname));
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			SkeltonMobEntity.init();
			OtiruyoEntity.init();
			HrmcngsEntity.init();
			Ruami284Entity.init();
			KillotiruEntity.init();
			BlackholeEntity.init();
			CometEntity.init();
			CometKillEntity.init();
			MeteorArrowEntity.init();
			AlchemyCraftBlockEntityEntity.init();
			Reisame284Entity.init();
			FlyingAttackerEntity.init();
		});
	}

	@SubscribeEvent
	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(SKELTON_MOB.get(), SkeltonMobEntity.createAttributes().build());
		event.put(OTIRUYO.get(), OtiruyoEntity.createAttributes().build());
		event.put(HRMCNGS.get(), HrmcngsEntity.createAttributes().build());
		event.put(RUAMI_284.get(), Ruami284Entity.createAttributes().build());
		event.put(KILLOTIRU.get(), KillotiruEntity.createAttributes().build());
		event.put(BLACKHOLE.get(), BlackholeEntity.createAttributes().build());
		event.put(COMET.get(), CometEntity.createAttributes().build());
		event.put(COMET_KILL.get(), CometKillEntity.createAttributes().build());
		event.put(METEOR_ARROW.get(), MeteorArrowEntity.createAttributes().build());
		event.put(ALCHEMY_CRAFT_BLOCK_ENTITY.get(), AlchemyCraftBlockEntityEntity.createAttributes().build());
		event.put(REISAME_284.get(), Reisame284Entity.createAttributes().build());
		event.put(FLYING_ATTACKER.get(), FlyingAttackerEntity.createAttributes().build());
	}
}
