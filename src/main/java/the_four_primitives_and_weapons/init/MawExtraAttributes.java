package the_four_primitives_and_weapons.init;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

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

    @SubscribeEvent
    public static void onConstructMod(net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent event) {
        event.enqueueWork(() -> {
            ATTRIBUTES.register(
                net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        });
    }

    @SubscribeEvent
    public static void addAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, MANA_MAX.get());
        event.add(EntityType.PLAYER, MANA_REGEN.get());
    }
}
