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

            // Re:Cross Hookshot 用の vanilla crossbow predicate を登録 (pull / pulling / charged).
            // これで model JSON の overrides が動作 → 3D モデル切替が機能する.
            for (var hookshot : new net.minecraft.world.item.Item[] {
                    CustomEntityInit.RECROSS_HOOKSHOT_LONG.get(),
                    CustomEntityInit.RECROSS_HOOKSHOT_SHORT.get() }) {
                ItemProperties.register(hookshot, new ResourceLocation("pull"),
                    (stack, lvl, entity, seed) -> {
                        if (entity == null) return 0f;
                        if (net.minecraft.world.item.CrossbowItem.isCharged(stack)) return 0f;
                        return (stack.getUseDuration() - entity.getUseItemRemainingTicks())
                            / (float) minecraftarmorweapon.item.RecrossHookshotItem.CHARGE_TICKS;
                    });
                ItemProperties.register(hookshot, new ResourceLocation("pulling"),
                    (stack, lvl, entity, seed) ->
                        entity != null
                            && entity.isUsingItem()
                            && entity.getUseItem() == stack
                            && !net.minecraft.world.item.CrossbowItem.isCharged(stack)
                        ? 1f : 0f);
                ItemProperties.register(hookshot, new ResourceLocation("charged"),
                    (stack, lvl, entity, seed) ->
                        net.minecraft.world.item.CrossbowItem.isCharged(stack) ? 1f : 0f);
            }
        });
    }
}