package the_four_primitives_and_weapons.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;

import the_four_primitives_and_weapons.entity.TornadoEntity;
import the_four_primitives_and_weapons.entity.DarkProjectileEntity;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

/**
 * カスタムエンティティの登録用クラス
 * MCreatorによって自動生成されるファイルとは別に管理
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class TheFourPrimitivesAndWeaponsModCustomEntities {
    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TheFourPrimitivesAndWeaponsMod.MODID);

    public static final RegistryObject<EntityType<TornadoEntity>> TORNADO = REGISTRY.register("tornado",
            () -> EntityType.Builder.<TornadoEntity>of(TornadoEntity::new, MobCategory.MISC)
                    .setShouldReceiveVelocityUpdates(true)
                    .setTrackingRange(128)
                    .setUpdateInterval(1)
                    .setCustomClientFactory((spawnEntity, level) -> new TornadoEntity(spawnEntity, level))
                    .sized(1.0f, 1.0f)
                    .build("tornado"));

    public static final RegistryObject<EntityType<DarkProjectileEntity>> DARK_PROJECTILE = REGISTRY.register("dark_projectile",
            () -> EntityType.Builder.<DarkProjectileEntity>of(DarkProjectileEntity::new, MobCategory.MISC)
                    .setShouldReceiveVelocityUpdates(true)
                    .setTrackingRange(64)
                    .setUpdateInterval(1)
                    .setCustomClientFactory((spawnEntity, level) -> new DarkProjectileEntity(spawnEntity, level))
                    .sized(0.5f, 0.5f)
                    .build("dark_projectile"));
}
