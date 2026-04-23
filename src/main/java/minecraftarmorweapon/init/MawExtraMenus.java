package minecraftarmorweapon.init;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.world.inventory.KnifeHolderMenu;

import net.minecraft.world.inventory.MenuType;

import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * MCreator 再生成対象の MinecraftArmorWeaponModMenus に追加できないメニューをここに登録する。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MawExtraMenus {

    public static final DeferredRegister<MenuType<?>> REGISTRY =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, MinecraftArmorWeaponMod.MODID);

    public static final RegistryObject<MenuType<KnifeHolderMenu>> KNIFE_HOLDER =
        REGISTRY.register("knife_holder",
            () -> IForgeMenuType.create(KnifeHolderMenu::new));

    @SubscribeEvent
    public static void onConstructMod(FMLConstructModEvent event) {
        event.enqueueWork(() -> {
            REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
        });
    }
}
