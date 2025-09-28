package minecraftarmorweapon.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PlayMessages;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;

import minecraftarmorweapon.entity.TornadoEntity;
import minecraftarmorweapon.entity.DarkProjectileEntity;
import minecraftarmorweapon.MinecraftArmorWeaponMod;

/**
 * カスタムエンティティの登録用クラス
 * MCreatorによって自動生成されるファイルとは別に管理
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MinecraftArmorWeaponModCustomEntities {
    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MinecraftArmorWeaponMod.MODID);

    public static final RegistryObject<EntityType<TornadoEntity>> TORNADO = REGISTRY.register("tornado",
            () -> EntityType.Builder.<TornadoEntity>of(TornadoEntity::new, MobCategory.MISC)
                    .setShouldReceiveVelocityUpdates(true)
                    .setTrackingRange(128)
                    .setUpdateInterval(1)
                    .setCustomClientFactory(TornadoEntity::new)
                    .sized(1.0f, 1.0f)
                    .build("tornado"));

    public static final RegistryObject<EntityType<DarkProjectileEntity>> DARK_PROJECTILE = REGISTRY.register("dark_projectile",
            () -> EntityType.Builder.<DarkProjectileEntity>of(DarkProjectileEntity::new, MobCategory.MISC)
                    .setShouldReceiveVelocityUpdates(true)
                    .setTrackingRange(64)
                    .setUpdateInterval(1)
                    .setCustomClientFactory(DarkProjectileEntity::new)
                    .sized(0.5f, 0.5f)
                    .build("dark_projectile"));
}