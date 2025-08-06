package minecraftarmorweapon.client;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Register item properties for all items
            for (RegistryObject<Item> itemObj : MinecraftArmorWeaponModItems.REGISTRY.getEntries()) {
                ItemPropertyInit.registerItemProperties(itemObj.get());
            }
        });
    }
}