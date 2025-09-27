package minecraftarmorweapon.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.entity.DarkProjectileEntity;

/**
 * カスタムエンティティの登録用クラス
 * MCreatorによって上書きされないように別クラスとして作成
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CustomEntityInit {

    public static final DeferredRegister<EntityType<?>> CUSTOM_ENTITIES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MinecraftArmorWeaponMod.MODID);

    public static final RegistryObject<EntityType<DarkProjectileEntity>> DARK_PROJECTILE =
        CUSTOM_ENTITIES.register("dark_projectile",
            () -> EntityType.Builder.<DarkProjectileEntity>of(DarkProjectileEntity::new, MobCategory.MISC)
                .setShouldReceiveVelocityUpdates(true)
                .setTrackingRange(64)
                .setUpdateInterval(1)
                .setCustomClientFactory(DarkProjectileEntity::new)
                .sized(0.5f, 0.5f)
                .fireImmune()
                .build("dark_projectile"));

    // Modのコンストラクタやメインクラスで呼び出す必要があります
    public static void register() {
        // この登録メソッドは MinecraftArmorWeaponMod のコンストラクタで呼び出されます
    }
}