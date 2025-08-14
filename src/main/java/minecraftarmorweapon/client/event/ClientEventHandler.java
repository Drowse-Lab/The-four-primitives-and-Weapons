package minecraftarmorweapon.client.event;

import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientEventHandler {
    
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        // 既存の難易度ボタンをそのまま使用
        // カスタム難易度システムは削除
    }
}