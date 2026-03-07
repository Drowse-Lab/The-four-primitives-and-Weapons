package minecraftarmorweapon.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import minecraftarmorweapon.client.screens.DodgeConfigScreen;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof ControlsScreen controlsScreen) {
            // Controls画面に「回避設定...」ボタンを追加
            Button dodgeConfigButton = Button.builder(
                    Component.literal("回避設定..."),
                    btn -> Minecraft.getInstance().setScreen(new DodgeConfigScreen(controlsScreen)))
                    .bounds(controlsScreen.width / 2 + 5, controlsScreen.height / 6 + 72 + 24 * 2, 150, 20)
                    .build();
            event.addListener(dodgeConfigButton);
        }
    }
}