package the_four_primitives_and_weapons;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import the_four_primitives_and_weapons.config.DodgeConfig;
import the_four_primitives_and_weapons.config.DebugConfig;

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
