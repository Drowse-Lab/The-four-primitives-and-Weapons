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

    private static final CustomDifficulty[] DIFFICULTIES = CustomDifficulty.values();
    private static int currentDifficultyIndex = 2; // デフォルト: NORMAL

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof ControlsScreen controlsScreen) {
            Button dodgeConfigButton = Button.builder(
                    Component.literal("回避設定..."),
                    btn -> Minecraft.getInstance().setScreen(new DodgeConfigScreen(controlsScreen)))
                    .bounds(controlsScreen.width / 2 + 5, controlsScreen.height / 6 + 72 + 24 * 2, 150, 20)
                    .build();
            event.addListener(dodgeConfigButton);
        }

        if (event.getScreen() instanceof OptionsScreen optionsScreen) {
            syncCurrentDifficultyIndex();

            // バニラの「Difficulty:」ボタンを探す
            AbstractWidget difficultyWidget = null;

            for (var listener : event.getListenersList()) {
                if (listener instanceof AbstractWidget widget) {
                    String msg = widget.getMessage().getString();
                    if (msg.contains("Difficulty") || msg.contains("難易度")) {
                        difficultyWidget = widget;
                        break;
                    }
                }
            }

            if (difficultyWidget != null) {
                int x = difficultyWidget.getX();
                int y = difficultyWidget.getY();
                int h = difficultyWidget.getHeight();

                // 難易度ボタン自体を非表示
                difficultyWidget.visible = false;
                difficultyWidget.active = false;

                // 難易度ボタンのX以降（右側）にある同じY座標のウィジェットも非表示（ロックボタン）
                for (var listener : event.getListenersList()) {
                    if (listener instanceof AbstractWidget widget && widget != difficultyWidget) {
                        if (widget.getY() == y && widget.getX() >= x) {
                            widget.visible = false;
                            widget.active = false;
                        }
                    }
                }

                // 難易度ボタンと同じ位置、右端までの幅でカスタムボタンを配置
                int rightEdge = optionsScreen.width / 2 + 155;
                int w = rightEdge - x;

                Button customDiffButton = Button.builder(
                        Component.literal(getDifficultyDisplayName()),
                        btn -> {
                            currentDifficultyIndex = (currentDifficultyIndex + 1) % DIFFICULTIES.length;
                            CustomDifficulty newDiff = DIFFICULTIES[currentDifficultyIndex];
                            btn.setMessage(Component.literal(getDifficultyDisplayName()));

                            MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                                    new CustomDifficultyPacket(newDiff.getName())
                            );

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
        return "Difficulty: " + diff.getName();
    }
}
