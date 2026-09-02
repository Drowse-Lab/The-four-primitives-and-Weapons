package the_four_primitives_and_weapons.init;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.damage.ElementType;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * MCreator 再生成の影響を受けない追加アトリビュート。
 *   - mana_max    : MP の最大値 (既存 MANA attribute の上限を担当)
 *   - mana_regen  : 1 tick あたりの MP 自動回復量
 *   - evasion     : 回避率(%) 敵の攻撃を自動で回避する確率 (100 超過で反射)
 *   - *_aptitude  : 属性武器の持ち歩きデバフへの適性
 *
 * 装備・効果・/attribute コマンドで調整可能。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MawExtraAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
        DeferredRegister.create(ForgeRegistries.ATTRIBUTES, TheFourPrimitivesAndWeaponsMod.MODID);

    public static final RegistryObject<Attribute> MANA_MAX =
        ATTRIBUTES.register("mana_max",
            () -> new RangedAttribute(
                "attribute." + TheFourPrimitivesAndWeaponsMod.MODID + ".mana_max",
                /* default */ 100.0,
                /* min     */ 0.0,
                /* max     */ 2_000_000_000.0
            ).setSyncable(true));

    public static final RegistryObject<Attribute> MANA_REGEN =
        ATTRIBUTES.register("mana_regen",
            () -> new RangedAttribute(
                "attribute." + TheFourPrimitivesAndWeaponsMod.MODID + ".mana_regen",
                /* default */ 0.1,
                /* min     */ 0.0,
                /* max     */ 1000.0
            ).setSyncable(true));

    /**
     * 近接攻撃範囲(ブロック)。デフォルト 3.0 からの差分が実際のリーチへ加算される。
     * プレイヤーは Forge の entity reach へ、MOB は近接攻撃判定の距離へ反映される。
     */
    public static final RegistryObject<Attribute> ENTITY_REACH =
        ATTRIBUTES.register("entity_reach",
            () -> new RangedAttribute(
                "attribute." + TheFourPrimitivesAndWeaponsMod.MODID + ".entity_reach",
                /* default */ 3.0,
                /* min     */ 0.0,
                /* max     */ 100_000.0
            ).setSyncable(true));

    /**
     * 回避率(%)。敵の攻撃を確率で自動的に流す。
     * 0 = 回避しない、100 = 必ず回避。
     * 100 を超えていると、回避した攻撃を攻撃者へ跳ね返す。
     * 跳ね返す量は 100 ごとの段階式で、101〜200 は敵ダメージの 5%、201〜300 は 10%（+5%ずつ）。
     * 上限は十分に大きく取ってあり、反射 100%（2001 以上）を超える設定もできる。
     */
    public static final RegistryObject<Attribute> EVASION =
        ATTRIBUTES.register("evasion",
            () -> new RangedAttribute(
                "attribute." + TheFourPrimitivesAndWeaponsMod.MODID + ".evasion",
                /* default */ 0.0,
                /* min     */ 0.0,
                /* max     */ 100_000.0
            ).setSyncable(true));

    public static final RegistryObject<Attribute> FIRE_APTITUDE = aptitude("fire");
    public static final RegistryObject<Attribute> WATER_APTITUDE = aptitude("water");
    public static final RegistryObject<Attribute> WIND_APTITUDE = aptitude("wind");
    public static final RegistryObject<Attribute> ICE_APTITUDE = aptitude("ice");
    public static final RegistryObject<Attribute> THUNDER_APTITUDE = aptitude("thunder");
    public static final RegistryObject<Attribute> ELECTRIC_APTITUDE = aptitude("electric");
    public static final RegistryObject<Attribute> CORROSION_APTITUDE = aptitude("corrosion");
    public static final RegistryObject<Attribute> HOLY_APTITUDE = aptitude("holy");
    public static final RegistryObject<Attribute> DARK_APTITUDE = aptitude("dark");
    public static final RegistryObject<Attribute> MIASMA_APTITUDE = aptitude("miasma");
    public static final RegistryObject<Attribute> BLOOD_APTITUDE = aptitude("blood");
    public static final RegistryObject<Attribute> ERASURE_APTITUDE = aptitude("erasure");
    public static final RegistryObject<Attribute> SOUL_APTITUDE = aptitude("soul");
    public static final RegistryObject<Attribute> SOUL_FIRE_APTITUDE = aptitude("soul_fire");
    public static final RegistryObject<Attribute> CURSE_APTITUDE = aptitude("curse");

    private static RegistryObject<Attribute> aptitude(String name) {
        return ATTRIBUTES.register(name + "_aptitude",
            () -> new RangedAttribute(
                "attribute." + TheFourPrimitivesAndWeaponsMod.MODID + "." + name + "_aptitude",
                /* default */ 0.0,
                /* min     */ 0.0,
                /* max     */ 1024.0
            ).setSyncable(true));
    }

    public static Attribute getAptitudeAttribute(ElementType type) {
        if (type == null) return null;
        switch (type) {
            case FIRE: return FIRE_APTITUDE.get();
            case WATER: return WATER_APTITUDE.get();
            case WIND: return WIND_APTITUDE.get();
            case ICE: return ICE_APTITUDE.get();
            case THUNDER: return THUNDER_APTITUDE.get();
            case ELECTRIC: return ELECTRIC_APTITUDE.get();
            case CORROSION: return CORROSION_APTITUDE.get();
            case HOLY: return HOLY_APTITUDE.get();
            case DARK: return DARK_APTITUDE.get();
            case MIASMA: return MIASMA_APTITUDE.get();
            case BLOOD: return BLOOD_APTITUDE.get();
            case ERASURE: return ERASURE_APTITUDE.get();
            case SOUL: return SOUL_APTITUDE.get();
            case SOUL_FIRE: return SOUL_FIRE_APTITUDE.get();
            default: return null;
        }
    }

    @SubscribeEvent
    public static void onConstructMod(net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent event) {
        event.enqueueWork(() -> {
            ATTRIBUTES.register(
                net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        });
    }

    @SubscribeEvent
    public static void addAttributes(EntityAttributeModificationEvent event) {
        // 回避率・攻撃範囲はプレイヤー・MOB を問わず /attribute や防具の修飾子で設定できるようにする
        for (EntityType<? extends net.minecraft.world.entity.LivingEntity> type : event.getTypes()) {
            if (!event.has(type, EVASION.get())) {
                event.add(type, EVASION.get());
            }
            if (!event.has(type, ENTITY_REACH.get())) {
                event.add(type, ENTITY_REACH.get());
            }
        }
        event.add(EntityType.PLAYER, MANA_MAX.get());
        event.add(EntityType.PLAYER, MANA_REGEN.get());
        event.add(EntityType.PLAYER, FIRE_APTITUDE.get());
        event.add(EntityType.PLAYER, WATER_APTITUDE.get());
        event.add(EntityType.PLAYER, WIND_APTITUDE.get());
        event.add(EntityType.PLAYER, ICE_APTITUDE.get());
        event.add(EntityType.PLAYER, THUNDER_APTITUDE.get());
        event.add(EntityType.PLAYER, ELECTRIC_APTITUDE.get());
        event.add(EntityType.PLAYER, CORROSION_APTITUDE.get());
        event.add(EntityType.PLAYER, HOLY_APTITUDE.get());
        event.add(EntityType.PLAYER, DARK_APTITUDE.get());
        event.add(EntityType.PLAYER, MIASMA_APTITUDE.get());
        event.add(EntityType.PLAYER, BLOOD_APTITUDE.get());
        event.add(EntityType.PLAYER, ERASURE_APTITUDE.get());
        event.add(EntityType.PLAYER, SOUL_APTITUDE.get());
        event.add(EntityType.PLAYER, SOUL_FIRE_APTITUDE.get());
        event.add(EntityType.PLAYER, CURSE_APTITUDE.get());
    }
}
