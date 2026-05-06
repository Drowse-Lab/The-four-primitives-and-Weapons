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
import minecraftarmorweapon.entity.DebugMobEntity;
import minecraftarmorweapon.entity.AngelTrioEntity;
import minecraftarmorweapon.entity.ThrowingKnifeEntity;
import minecraftarmorweapon.entity.WeaponRackEntity;
import minecraftarmorweapon.item.UndeadArmyBanishItem;
import minecraftarmorweapon.item.ThrowingKnifeItem;
import minecraftarmorweapon.item.GripKnifeItem;
import minecraftarmorweapon.item.StunKnifeItem;
import minecraftarmorweapon.item.ScrewKnifeItem;
import minecraftarmorweapon.item.HomingKnifeItem;

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

    // 天使の三人組 — 3つの別EntityType
    public static final RegistryObject<EntityType<AngelTrioEntity>> ANGEL_SERIOUS =
        CUSTOM_ENTITIES.register("angel_serious",
            () -> EntityType.Builder.<AngelTrioEntity>of(
                (type, world) -> { var e = new AngelTrioEntity(type, world); e.setPersonality(0); return e; },
                MobCategory.CREATURE)
                .setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)
                .sized(0.6f, 1.8f).build("angel_serious"));

    public static final RegistryObject<EntityType<AngelTrioEntity>> ANGEL_MOCKER1 =
        CUSTOM_ENTITIES.register("angel_mocker1",
            () -> EntityType.Builder.<AngelTrioEntity>of(
                (type, world) -> { var e = new AngelTrioEntity(type, world); e.setPersonality(1); return e; },
                MobCategory.CREATURE)
                .setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)
                .sized(0.6f, 1.8f).build("angel_mocker1"));

    public static final RegistryObject<EntityType<AngelTrioEntity>> ANGEL_MOCKER2 =
        CUSTOM_ENTITIES.register("angel_mocker2",
            () -> EntityType.Builder.<AngelTrioEntity>of(
                (type, world) -> { var e = new AngelTrioEntity(type, world); e.setPersonality(2); return e; },
                MobCategory.CREATURE)
                .setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)
                .sized(0.6f, 1.8f).build("angel_mocker2"));

    // デバッグ用Mob（サンドバッグ）
    public static final RegistryObject<EntityType<DebugMobEntity>> DEBUG_MOB =
        CUSTOM_ENTITIES.register("debug_mob",
            () -> EntityType.Builder.<DebugMobEntity>of(DebugMobEntity::new, MobCategory.MISC)
                .setShouldReceiveVelocityUpdates(true)
                .setTrackingRange(64)
                .setUpdateInterval(3)
                .sized(0.6f, 1.8f)
                .build("debug_mob"));

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

    public static final RegistryObject<Item> DEBUG_MOB_SPAWN_EGG =
        CUSTOM_ITEMS.register("debug_mob_spawn_egg",
            () -> new ForgeSpawnEggItem(DEBUG_MOB, 0x00FF00, 0xFF0000,
                new Item.Properties()));

    public static final RegistryObject<Item> ANGEL_SERIOUS_SPAWN_EGG =
        CUSTOM_ITEMS.register("angel_serious_spawn_egg",
            () -> new ForgeSpawnEggItem(ANGEL_SERIOUS, 0xFFFFFF, 0x50B060,
                new Item.Properties()));

    public static final RegistryObject<Item> ANGEL_MOCKER1_SPAWN_EGG =
        CUSTOM_ITEMS.register("angel_mocker1_spawn_egg",
            () -> new ForgeSpawnEggItem(ANGEL_MOCKER1, 0xFFFFFF, 0xD0D0E0,
                new Item.Properties()));

    public static final RegistryObject<Item> ANGEL_MOCKER2_SPAWN_EGG =
        CUSTOM_ITEMS.register("angel_mocker2_spawn_egg",
            () -> new ForgeSpawnEggItem(ANGEL_MOCKER2, 0xFFFFFF, 0x601820,
                new Item.Properties()));

    // アンデットアーミーを中断させるアイテム「聖なる鐘」
    public static final RegistryObject<Item> UNDEAD_ARMY_BANISH =
        CUSTOM_ITEMS.register("undead_army_banish",
            () -> new UndeadArmyBanishItem());

    // 投げナイフ飛翔体
    public static final RegistryObject<EntityType<ThrowingKnifeEntity>> THROWING_KNIFE_ENTITY =
        CUSTOM_ENTITIES.register("throwing_knife",
            () -> EntityType.Builder.<ThrowingKnifeEntity>of(ThrowingKnifeEntity::new, MobCategory.MISC)
                .setShouldReceiveVelocityUpdates(true)
                .setTrackingRange(64)
                .setUpdateInterval(2)
                .sized(0.5f, 0.5f)
                .build("throwing_knife"));

    // 投げナイフアイテム
    public static final RegistryObject<Item> THROWING_KNIFE =
        CUSTOM_ITEMS.register("throwing_knife",
            () -> new ThrowingKnifeItem());

    // 投げナイフ — 咲夜系バリアント
    public static final RegistryObject<Item> GRIP_KNIFE =
        CUSTOM_ITEMS.register("grip_knife", () -> new GripKnifeItem());
    public static final RegistryObject<Item> STUN_KNIFE =
        CUSTOM_ITEMS.register("stun_knife", () -> new StunKnifeItem());
    public static final RegistryObject<Item> SCREW_KNIFE =
        CUSTOM_ITEMS.register("screw_knife", () -> new ScrewKnifeItem());
    public static final RegistryObject<Item> HOMING_KNIFE =
        CUSTOM_ITEMS.register("homing_knife", () -> new HomingKnifeItem());

    // 投げナイフランチャー — 技選択 (モード/本数) で多段投擲する武器
    public static final RegistryObject<Item> KNIFE_LAUNCHER =
        CUSTOM_ITEMS.register("knife_launcher",
            () -> new minecraftarmorweapon.item.KnifeLauncherItem());

    // ガイドブック — ワールド初回入場時に支給、操作/技/mob を画像付きで紹介
    public static final RegistryObject<Item> GUIDE_BOOK =
        CUSTOM_ITEMS.register("guide_book",
            () -> new minecraftarmorweapon.item.GuideBookItem());

    // マナポーション — 飲むと MP 回復 (Iron's Spellbooks が入ってれば向こうの mana に routed)
    public static final RegistryObject<Item> MANA_POTION =
        CUSTOM_ITEMS.register("mana_potion",
            () -> new minecraftarmorweapon.item.ManaPotionItem());

    // Arsenal式 武器ラック (ItemFrame ベース)
    public static final RegistryObject<EntityType<WeaponRackEntity>> WEAPON_RACK =
        CUSTOM_ENTITIES.register("weapon_rack",
            () -> EntityType.Builder.<WeaponRackEntity>of(WeaponRackEntity::new, MobCategory.MISC)
                .setShouldReceiveVelocityUpdates(false)
                .setTrackingRange(64)
                .setUpdateInterval(20)
                .sized(0.75f, 0.75f)
                .build("weapon_rack"));

    public static final RegistryObject<Item> WEAPON_RACK_ITEM =
        CUSTOM_ITEMS.register("weapon_rack",
            () -> new minecraftarmorweapon.item.WeaponRackItem(new Item.Properties(), 0));

    // 木の種類別ラック (oak は WEAPON_RACK_ITEM = woodIndex 0)。
    public static final RegistryObject<Item> WEAPON_RACK_SPRUCE =
        CUSTOM_ITEMS.register("weapon_rack_spruce",
            () -> new minecraftarmorweapon.item.WeaponRackItem(new Item.Properties(), 1));
    public static final RegistryObject<Item> WEAPON_RACK_BIRCH =
        CUSTOM_ITEMS.register("weapon_rack_birch",
            () -> new minecraftarmorweapon.item.WeaponRackItem(new Item.Properties(), 2));
    public static final RegistryObject<Item> WEAPON_RACK_JUNGLE =
        CUSTOM_ITEMS.register("weapon_rack_jungle",
            () -> new minecraftarmorweapon.item.WeaponRackItem(new Item.Properties(), 3));
    public static final RegistryObject<Item> WEAPON_RACK_ACACIA =
        CUSTOM_ITEMS.register("weapon_rack_acacia",
            () -> new minecraftarmorweapon.item.WeaponRackItem(new Item.Properties(), 4));
    public static final RegistryObject<Item> WEAPON_RACK_DARK_OAK =
        CUSTOM_ITEMS.register("weapon_rack_dark_oak",
            () -> new minecraftarmorweapon.item.WeaponRackItem(new Item.Properties(), 5));
    public static final RegistryObject<Item> WEAPON_RACK_MANGROVE =
        CUSTOM_ITEMS.register("weapon_rack_mangrove",
            () -> new minecraftarmorweapon.item.WeaponRackItem(new Item.Properties(), 6));
    public static final RegistryObject<Item> WEAPON_RACK_CHERRY =
        CUSTOM_ITEMS.register("weapon_rack_cherry",
            () -> new minecraftarmorweapon.item.WeaponRackItem(new Item.Properties(), 7));
    public static final RegistryObject<Item> WEAPON_RACK_BAMBOO =
        CUSTOM_ITEMS.register("weapon_rack_bamboo",
            () -> new minecraftarmorweapon.item.WeaponRackItem(new Item.Properties(), 8));
    public static final RegistryObject<Item> WEAPON_RACK_CRIMSON =
        CUSTOM_ITEMS.register("weapon_rack_crimson",
            () -> new minecraftarmorweapon.item.WeaponRackItem(new Item.Properties(), 9));
    public static final RegistryObject<Item> WEAPON_RACK_WARPED =
        CUSTOM_ITEMS.register("weapon_rack_warped",
            () -> new minecraftarmorweapon.item.WeaponRackItem(new Item.Properties(), 10));

    // Re:Cross Hookshot (Java port of Chuzume's data pack — short range, sneak-float)
    public static final RegistryObject<EntityType<minecraftarmorweapon.entity.RecrossHookEntity>> RECROSS_HOOK_ENTITY =
        CUSTOM_ENTITIES.register("recross_hook",
            () -> EntityType.Builder.<minecraftarmorweapon.entity.RecrossHookEntity>of(
                    minecraftarmorweapon.entity.RecrossHookEntity::new, MobCategory.MISC)
                .setShouldReceiveVelocityUpdates(true)
                .setTrackingRange(96)
                .setUpdateInterval(1)
                .sized(0.4f, 0.4f)
                .build("recross_hook"));

    /**
     * Long range = 80 block (flyStep 4 × maxFlyTicks 20 → 1 sec flight).
     * Short range = 60 block (flyStep 3 × maxFlyTicks 20 → 1 sec flight).
     * 高速で飛びつつ vanilla の client interpolation で滑らかに見せる.
     */
    public static final RegistryObject<Item> RECROSS_HOOKSHOT_LONG =
        CUSTOM_ITEMS.register("recross_hookshot_long",
            () -> new minecraftarmorweapon.item.RecrossHookshotItem(4.0, 20));

    public static final RegistryObject<Item> RECROSS_HOOKSHOT_SHORT =
        CUSTOM_ITEMS.register("recross_hookshot_short",
            () -> new minecraftarmorweapon.item.RecrossHookshotItem(3.0, 20));

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(COMMON_SOLDIER.get(), CommonSoldierEntity.createAttributes().build());
        event.put(ELITE_SOLDIER.get(), EliteSoldierEntity.createAttributes().build());
        event.put(SINGULARITY.get(), SingularityEntity.createAttributes().build());
        event.put(HEROIC_TIER.get(), HeroicTierEntity.createAttributes().build());
        event.put(DEBUG_MOB.get(), DebugMobEntity.createAttributes().build());
        event.put(ANGEL_SERIOUS.get(), AngelTrioEntity.createAttributes().build());
        event.put(ANGEL_MOCKER1.get(), AngelTrioEntity.createAttributes().build());
        event.put(ANGEL_MOCKER2.get(), AngelTrioEntity.createAttributes().build());
        event.put(RECROSS_HOOK_ENTITY.get(),
            minecraftarmorweapon.entity.RecrossHookEntity.createAttributes().build());
    }

    // Modのコンストラクタやメインクラスで呼び出す必要があります
    public static void register() {
        // この登録メソッドは MinecraftArmorWeaponMod のコンストラクタで呼び出されます
    }
}