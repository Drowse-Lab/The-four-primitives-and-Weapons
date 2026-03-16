package minecraftarmorweapon.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.client.screens.DodgeConfigScreen;
import minecraftarmorweapon.command.CustomDifficultyCommand;
import minecraftarmorweapon.command.CustomDifficultyCommand.CustomDifficulty;
import minecraftarmorweapon.network.CustomDifficultyPacket;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientEventHandler {

    // カスタム難易度のリスト
    private static final CustomDifficulty[] DIFFICULTIES = CustomDifficulty.values();
    private static int currentDifficultyIndex = 2; // デフォルト: NORMAL

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

        if (event.getScreen() instanceof OptionsScreen optionsScreen) {
            // 現在のカスタム難易度のインデックスを同期
            syncCurrentDifficultyIndex();

            // 「Difficulty:」ボタンを探して差し替え
            List<AbstractWidget> toRemove = new ArrayList<>();
            for (var listener : event.getListenersList()) {
                if (listener instanceof AbstractWidget widget) {
                    String msg = widget.getMessage().getString();
                    if (msg.contains("Difficulty") || msg.contains("難易度")) {
                        toRemove.add(widget);
                    }
                }
            }

            for (AbstractWidget widget : toRemove) {
                // 元のボタンの位置を使う
                int x = widget.getX();
                int y = widget.getY();
                int w = widget.getWidth();
                int h = widget.getHeight();

                event.removeListener(widget);

                // カスタム難易度ボタンを追加
                Button customDiffButton = Button.builder(
                        Component.literal(getDifficultyDisplayName()),
                        btn -> {
                            // 次の難易度に切り替え
                            currentDifficultyIndex = (currentDifficultyIndex + 1) % DIFFICULTIES.length;
                            CustomDifficulty newDiff = DIFFICULTIES[currentDifficultyIndex];
                            btn.setMessage(Component.literal(getDifficultyDisplayName()));

                            // サーバーにパケットを送信
                            MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                                    new CustomDifficultyPacket(newDiff.getName())
                            );

                            // クライアント側も同期（シングルプレイ対応）
                            CustomDifficultyCommand.setCurrentDifficulty(newDiff);
                        })
                        .bounds(x, y, w, h)
                        .build();
                event.addListener(customDiffButton);
            }
        }
    }

    private static void syncCurrentDifficultyIndex() {
        CustomDifficulty current = CustomDifficultyCommand.getCurrentDifficulty();
        for (int i = 0; i < DIFFICULTIES.length; i++) {
            if (DIFFICULTIES[i] == current) {
                currentDifficultyIndex = i;
                return;
            }
        }
    }

    private static String getDifficultyDisplayName() {
        CustomDifficulty diff = DIFFICULTIES[currentDifficultyIndex];
        return "難易度: " + diff.getName() + " (x" + String.format("%.1f", diff.getDamageMultiplier()) + ")";
    }
}
