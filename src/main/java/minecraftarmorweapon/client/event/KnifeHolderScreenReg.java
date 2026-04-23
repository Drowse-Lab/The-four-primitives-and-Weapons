package minecraftarmorweapon.client.event;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.client.screens.KnifeHolderScreen;
import minecraftarmorweapon.init.MawExtraMenus;

import net.minecraft.client.gui.screens.MenuScreens;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * KnifeHolderMenu ↔ KnifeHolderScreen の紐付けをクライアント起動時に登録。
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID,
    bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KnifeHolderScreenReg {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(MawExtraMenus.KNIFE_HOLDER.get(), KnifeHolderScreen::new);
        });
    }
}
