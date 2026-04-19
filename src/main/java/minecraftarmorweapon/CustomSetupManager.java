package minecraftarmorweapon;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;
import minecraftarmorweapon.TestBowItemsProperties;
import minecraftarmorweapon.config.DodgeConfig;
import minecraftarmorweapon.config.DebugConfig;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CustomSetupManager {
	public CustomSetupManager() {
		// コンストラクタ
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		DodgeConfig.load();
		DebugConfig.load();
		new CustomSetupManager();
	}

	@Mod.EventBusSubscriber
	private static class ForgeBusEvents {
		@SubscribeEvent
		public static void serverLoad(ServerStartingEvent event) {
			TestBowItemsProperties.addCustomItemProperties();
		}
	}
}
