package minecraftarmorweapon.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.entity.CommonSoldierEntity;

/**
 * カスタムエンティティの登録用クラス（A-Life AI Mobs専用）
 * MCreatorによって上書きされないように別クラスとして作成
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CustomEntityInit {

    public static final DeferredRegister<EntityType<?>> CUSTOM_ENTITIES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MinecraftArmorWeaponMod.MODID);

    public static final DeferredRegister<Item> CUSTOM_ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, MinecraftArmorWeaponMod.MODID);

    public static final RegistryObject<EntityType<CommonSoldierEntity>> COMMON_SOLDIER =
        CUSTOM_ENTITIES.register("common_soldier",
            () -> EntityType.Builder.<CommonSoldierEntity>of(CommonSoldierEntity::new, MobCategory.MONSTER)
                .setShouldReceiveVelocityUpdates(true)
                .setTrackingRange(64)
                .setUpdateInterval(3)
                .sized(0.6f, 1.8f)
                .build("common_soldier"));

    // スポーンエッグ
    public static final RegistryObject<Item> COMMON_SOLDIER_SPAWN_EGG =
        CUSTOM_ITEMS.register("common_soldier_spawn_egg",
            () -> new ForgeSpawnEggItem(COMMON_SOLDIER, 0x8B8B8B, 0x4A4A4A,
                new Item.Properties()));

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(COMMON_SOLDIER.get(), CommonSoldierEntity.createAttributes().build());
    }

    // Modのコンストラクタやメインクラスで呼び出す必要があります
    public static void register() {
        // この登録メソッドは MinecraftArmorWeaponMod のコンストラクタで呼び出されます
    }
}