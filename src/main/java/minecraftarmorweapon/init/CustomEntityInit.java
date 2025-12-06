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
import minecraftarmorweapon.entity.EliteSoldierEntity;
import minecraftarmorweapon.entity.SingularityEntity;
import minecraftarmorweapon.entity.HeroicTierEntity;

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

    // Tier 2: エリート兵
    public static final RegistryObject<EntityType<EliteSoldierEntity>> ELITE_SOLDIER =
        CUSTOM_ENTITIES.register("elite_soldier",
            () -> EntityType.Builder.<EliteSoldierEntity>of(EliteSoldierEntity::new, MobCategory.MONSTER)
                .setShouldReceiveVelocityUpdates(true)
                .setTrackingRange(64)
                .setUpdateInterval(3)
                .sized(0.6f, 1.8f)
                .build("elite_soldier"));

    // Tier 3: 特異点
    public static final RegistryObject<EntityType<SingularityEntity>> SINGULARITY =
        CUSTOM_ENTITIES.register("singularity",
            () -> EntityType.Builder.<SingularityEntity>of(SingularityEntity::new, MobCategory.MONSTER)
                .setShouldReceiveVelocityUpdates(true)
                .setTrackingRange(64)
                .setUpdateInterval(3)
                .sized(0.6f, 1.8f)
                .build("singularity"));

    // Tier 4: 英雄級
    public static final RegistryObject<EntityType<HeroicTierEntity>> HEROIC_TIER =
        CUSTOM_ENTITIES.register("heroic_tier",
            () -> EntityType.Builder.<HeroicTierEntity>of(HeroicTierEntity::new, MobCategory.MONSTER)
                .setShouldReceiveVelocityUpdates(true)
                .setTrackingRange(64)
                .setUpdateInterval(3)
                .sized(0.6f, 1.8f)
                .build("heroic_tier"));

    // スポーンエッグ
    public static final RegistryObject<Item> COMMON_SOLDIER_SPAWN_EGG =
        CUSTOM_ITEMS.register("common_soldier_spawn_egg",
            () -> new ForgeSpawnEggItem(COMMON_SOLDIER, 0x8B8B8B, 0x4A4A4A,
                new Item.Properties()));

    public static final RegistryObject<Item> ELITE_SOLDIER_SPAWN_EGG =
        CUSTOM_ITEMS.register("elite_soldier_spawn_egg",
            () -> new ForgeSpawnEggItem(ELITE_SOLDIER, 0x4169E1, 0x1E3A8A,
                new Item.Properties()));

    public static final RegistryObject<Item> SINGULARITY_SPAWN_EGG =
        CUSTOM_ITEMS.register("singularity_spawn_egg",
            () -> new ForgeSpawnEggItem(SINGULARITY, 0x9400D3, 0x4B0082,
                new Item.Properties()));

    public static final RegistryObject<Item> HEROIC_TIER_SPAWN_EGG =
        CUSTOM_ITEMS.register("heroic_tier_spawn_egg",
            () -> new ForgeSpawnEggItem(HEROIC_TIER, 0xFFD700, 0xFF8C00,
                new Item.Properties()));

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(COMMON_SOLDIER.get(), CommonSoldierEntity.createAttributes().build());
        event.put(ELITE_SOLDIER.get(), EliteSoldierEntity.createAttributes().build());
        event.put(SINGULARITY.get(), SingularityEntity.createAttributes().build());
        event.put(HEROIC_TIER.get(), HeroicTierEntity.createAttributes().build());
    }

    // Modのコンストラクタやメインクラスで呼び出す必要があります
    public static void register() {
        // この登録メソッドは MinecraftArmorWeaponMod のコンストラクタで呼び出されます
    }
}