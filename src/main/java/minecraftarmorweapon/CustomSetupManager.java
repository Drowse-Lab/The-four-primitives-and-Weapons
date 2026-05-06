package minecraftarmorweapon;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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

}
