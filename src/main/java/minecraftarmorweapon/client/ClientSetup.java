package minecraftarmorweapon.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.init.CustomEntityInit;
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

            // Momentum Hookshot の状態切替 (cooldown は vanilla 同期される):
            //   0 = 待機 (standby.json)
            //   1 = リロード中/発射直後 (reload.json)
            ItemProperties.register(CustomEntityInit.MOMENTUM_HOOKSHOT.get(),
                new ResourceLocation(MinecraftArmorWeaponMod.MODID, "momentum_state"),
                (stack, level, entity, seed) -> {
                    if (!(entity instanceof net.minecraft.world.entity.player.Player p)) return 0f;
                    if (p.getCooldowns().isOnCooldown(stack.getItem())) return 1f;
                    return 0f;
                });
        });
    }
}