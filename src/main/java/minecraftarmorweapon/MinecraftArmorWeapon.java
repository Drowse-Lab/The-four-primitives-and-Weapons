
package minecraftarmorweapon;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.util.ResourceLocation;

@Mod("minecraftarmorweapon") // ModのIDを指定
public class MinecraftArmorWeapon {
    public MinecraftArmorWeapon() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // 必要に応じてセットアップコードを追加
    }

    private void onServerStarting(ServerStartingEvent event) {
        // サーバー開始時にデータパックを適用
        event.getServer().getDataPackRegistries().load(new ResourceLocation("minecraftarmorweapon", "my_datapack"));
    }
}
