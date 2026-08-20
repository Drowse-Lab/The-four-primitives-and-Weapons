package the_four_primitives_and_weapons.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.MouseSettingsScreen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.client.screens.DodgeConfigScreen;
import the_four_primitives_and_weapons.client.screens.DebugConfigScreen;
import the_four_primitives_and_weapons.command.CustomDifficultyCommand;
import the_four_primitives_and_weapons.command.CustomDifficultyCommand.CustomDifficulty;
import the_four_primitives_and_weapons.config.TooltipConfig;
import the_four_primitives_and_weapons.network.CustomDifficultyPacket;

import net.minecraft.client.gui.components.LockIconButton;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientEventHandler {

    private static final CustomDifficulty[] DIFFICULTIES = CustomDifficulty.values();
    private static int currentDifficultyIndex = 2; // デフォルト: NORMAL
    private static boolean difficultyLocked = false;

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof MouseSettingsScreen mouseSettingsScreen) {
            AbstractWidget touchscreenWidget = null;
            for (var listener : event.getListenersList()) {
                if (listener instanceof AbstractWidget widget) {
                    String message = widget.getMessage().getString();
                    if (message.contains("Touchscreen") || message.contains("タッチスクリーン")) {
                        touchscreenWidget = widget;
                        break;
                    }
                }
            }

            int x = mouseSettingsScreen.width / 2 + 5;
            int y = touchscreenWidget != null ? touchscreenWidget.getY() : 132;
            Button directionButton = Button.builder(
                    tooltipScrollDirectionMessage(),
                    button -> {
                        TooltipConfig.reverseScrollDirection = !TooltipConfig.reverseScrollDirection;
                        TooltipConfig.save();
                        button.setMessage(tooltipScrollDirectionMessage());
                    })
                    .bounds(x, y, 150, 20)
                    .build();
            event.addListener(directionButton);
        }

        if (event.getScreen() instanceof ControlsScreen controlsScreen) {
            // vanilla の Done ボタン (width/2 - 100, width 200) を探す
            AbstractWidget doneButton = null;
            for (var listener : event.getListenersList()) {
                if (listener instanceof AbstractWidget widget) {
                    String msg = widget.getMessage().getString();
                    if (msg.equals("Done") || msg.equals("完了")) {
                        doneButton = widget;
                        break;
                    }
                }
            }

            // vanilla 行 1-3 と同じ X 座標 (左列 width/2-155、右列 width/2+5、150x20)
            int leftX = controlsScreen.width / 2 - 155;
            int rightX = controlsScreen.width / 2 + 5;
            // 行 4 の Y = 元の Done 位置 (vanilla では height/6 - 12 + 72 = height/6 + 60)
            int rowY = doneButton != null ? doneButton.getY() : controlsScreen.height / 6 + 60;

            Button dodgeConfigButton = Button.builder(
                    Component.translatable("screen.the_four_primitives_and_weapons.dodge_config"),
                    btn -> Minecraft.getInstance().setScreen(new DodgeConfigScreen(controlsScreen)))
                    .bounds(leftX, rowY, 150, 20)
                    .build();
            event.addListener(dodgeConfigButton);

            Button debugConfigButton = Button.builder(
                    Component.translatable("screen.the_four_primitives_and_weapons.debug_config"),
                    btn -> Minecraft.getInstance().setScreen(new DebugConfigScreen(controlsScreen)))
                    .bounds(rightX, rowY, 150, 20)
                    .build();
            event.addListener(debugConfigButton);

            // 既存の Done ボタンを 1 行下にずらして衝突回避
            if (doneButton != null) {
                doneButton.setY(doneButton.getY() + 24);
            }
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

                // バニラのロックボタンを探す（難易度ボタンの右隣）
                AbstractWidget vanillaLockWidget = null;
                for (var listener : event.getListenersList()) {
                    if (listener instanceof AbstractWidget widget && widget != difficultyWidget) {
                        if (widget.getY() == y && widget.getX() >= x) {
                            vanillaLockWidget = widget;
                            break;
                        }
                    }
                }

                // バニラの難易度ボタンとロックボタンを非表示
                difficultyWidget.visible = false;
                difficultyWidget.active = false;
                if (vanillaLockWidget != null) {
                    vanillaLockWidget.visible = false;
                    vanillaLockWidget.active = false;
                }

                // カスタム難易度ボタン（ロックボタンの幅20を引く）
                int rightEdge = optionsScreen.width / 2 + 155;
                int lockSize = 20;
                int w = rightEdge - x - lockSize;

                Button customDiffButton = Button.builder(
                        Component.literal(getDifficultyDisplayName()),
                        btn -> {
                            if (difficultyLocked) return; // ロック中は変更不可
                            currentDifficultyIndex = (currentDifficultyIndex + 1) % DIFFICULTIES.length;
                            CustomDifficulty newDiff = DIFFICULTIES[currentDifficultyIndex];
                            btn.setMessage(Component.literal(getDifficultyDisplayName()));

                            TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(
                                    new CustomDifficultyPacket(newDiff.getName())
                            );

                            CustomDifficultyCommand.setCurrentDifficulty(newDiff);
                        })
                        .bounds(x, y, w, h)
                        .build();
                event.addListener(customDiffButton);

                // ロックボタン（難易度ボタンの右隣）
                LockIconButton lockButton = new LockIconButton(
                        x + w, y,
                        btn -> {
                            difficultyLocked = !difficultyLocked;
                            ((LockIconButton) btn).setLocked(difficultyLocked);
                            customDiffButton.active = !difficultyLocked;
                            // サーバーにロック状態を送信
                            if (Minecraft.getInstance().player != null) {
                                String cmd = "gamerule difficulty_lock " + (difficultyLocked ? "true" : "false");
                                Minecraft.getInstance().player.connection.sendUnsignedCommand(cmd);
                            }
                        }
                );
                lockButton.setLocked(difficultyLocked);
                customDiffButton.active = !difficultyLocked;
                event.addListener(lockButton);
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

    private static Component tooltipScrollDirectionMessage() {
        return Component.translatable(
                "options.the_four_primitives_and_weapons.tooltip_scroll_direction",
                Component.translatable(TooltipConfig.reverseScrollDirection
                        ? "options.the_four_primitives_and_weapons.direction.reversed"
                        : "options.the_four_primitives_and_weapons.direction.normal"));
    }
}
