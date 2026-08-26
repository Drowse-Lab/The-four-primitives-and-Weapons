
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package the_four_primitives_and_weapons.init;

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

import the_four_primitives_and_weapons.entity.SkeltonMobEntity;
import the_four_primitives_and_weapons.entity.MeteorArrowEntity;
import the_four_primitives_and_weapons.entity.LunaCompanionEntity;
import the_four_primitives_and_weapons.entity.KatanaTobuEntity;
import the_four_primitives_and_weapons.entity.FlyingAttackerEntity;
import the_four_primitives_and_weapons.entity.CometKillEntity;
import the_four_primitives_and_weapons.entity.CometEntity;
import the_four_primitives_and_weapons.entity.BlackholeEntity;
import the_four_primitives_and_weapons.entity.AlchemyCraftBlockEntityEntity;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class TheFourPrimitivesAndWeaponsModEntities {
	public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TheFourPrimitivesAndWeaponsMod.MODID);
	public static final RegistryObject<EntityType<SkeltonMobEntity>> SKELTON_MOB = register("skelton_mob",
			EntityType.Builder.<SkeltonMobEntity>of(SkeltonMobEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(SkeltonMobEntity::new)

					.sized(0.6f, 1.5f));
	public static final RegistryObject<EntityType<KatanaTobuEntity>> KATANA_TOBU = register("projectile_katana_tobu",
			EntityType.Builder.<KatanaTobuEntity>of(KatanaTobuEntity::new, MobCategory.MISC).setCustomClientFactory(KatanaTobuEntity::new).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
	public static final RegistryObject<EntityType<LunaCompanionEntity>> LUNA_COMPANION = register("luna_companion",
			EntityType.Builder.<LunaCompanionEntity>of(LunaCompanionEntity::new, MobCategory.CREATURE)
					.setShouldReceiveVelocityUpdates(true).setTrackingRange(48).setUpdateInterval(2)
					.setCustomClientFactory(LunaCompanionEntity::new).fireImmune().sized(0.6f, 1.2f));
	public static final RegistryObject<EntityType<BlackholeEntity>> BLACKHOLE = register("blackhole",
			EntityType.Builder.<BlackholeEntity>of(BlackholeEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(1).setUpdateInterval(3).setCustomClientFactory(BlackholeEntity::new).fireImmune().sized(0.8f, 0.8f));
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
	public static final RegistryObject<EntityType<FlyingAttackerEntity>> FLYING_ATTACKER = register("flying_attacker",
			EntityType.Builder.<FlyingAttackerEntity>of(FlyingAttackerEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(FlyingAttackerEntity::new)

					.sized(0.6f, 1.8f));

	public static final RegistryObject<EntityType<the_four_primitives_and_weapons.entity.GateProjectileEntity>> GATE_PROJECTILE = register("gate_projectile",
			EntityType.Builder.<the_four_primitives_and_weapons.entity.GateProjectileEntity>of(the_four_primitives_and_weapons.entity.GateProjectileEntity::new, MobCategory.MISC)
					.setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1)
					.sized(0.25f, 0.25f));

	private static <T extends Entity> RegistryObject<EntityType<T>> register(String registryname, EntityType.Builder<T> entityTypeBuilder) {
		return REGISTRY.register(registryname, () -> (EntityType<T>) entityTypeBuilder.build(registryname));
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			SkeltonMobEntity.init();
			BlackholeEntity.init();
			CometEntity.init();
			CometKillEntity.init();
			MeteorArrowEntity.init();
			AlchemyCraftBlockEntityEntity.init();
			FlyingAttackerEntity.init();
		});
	}

	@SubscribeEvent
	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(SKELTON_MOB.get(), SkeltonMobEntity.createAttributes().build());
		event.put(LUNA_COMPANION.get(), LunaCompanionEntity.createAttributes().build());
		event.put(BLACKHOLE.get(), BlackholeEntity.createAttributes().build());
		event.put(COMET.get(), CometEntity.createAttributes().build());
		event.put(COMET_KILL.get(), CometKillEntity.createAttributes().build());
		event.put(METEOR_ARROW.get(), MeteorArrowEntity.createAttributes().build());
		event.put(ALCHEMY_CRAFT_BLOCK_ENTITY.get(), AlchemyCraftBlockEntityEntity.createAttributes().build());
		event.put(FLYING_ATTACKER.get(), FlyingAttackerEntity.createAttributes().build());
	}
}
