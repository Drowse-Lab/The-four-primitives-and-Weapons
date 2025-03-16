package minecraftarmorweapon.item;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("minecraft_armor_weapon")
public class MyMod {
    public MyMod() {
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ItemTooltipEventHandler());
    }

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void doClientStuff(FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        SetFeynCommand.register(event.getServer().getCommands().getDispatcher());
    }
}