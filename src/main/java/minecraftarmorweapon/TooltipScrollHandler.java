package minecraftarmorweapon.event;

import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.gui.screens.Screen;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TooltipScrollHandler {
    
    public TooltipScrollHandler() {
        MinecraftForge.EVENT_BUS.register(this); // イベント登録
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        // Shiftキーが押されているときのみスクロール可能
        if (Screen.hasShiftDown()) {
            double scrollDelta = event.getScrollDelta();
            System.out.println("MouseScrolled event fired: " + scrollDelta); // デバッグ出力
            
            // スクロール位置を更新
            TooltipEventHandler.adjustScrollIndex(scrollDelta > 0 ? -1 : 1);
            System.out.println("Scroll Index Updated: " + TooltipEventHandler.getScrollIndex()); // デバッグ出力

            event.setCanceled(true); // 通常のスクロール動作を無効化
        }
    }
}
