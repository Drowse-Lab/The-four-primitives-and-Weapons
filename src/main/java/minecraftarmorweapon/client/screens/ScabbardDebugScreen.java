package minecraftarmorweapon.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import org.lwjgl.glfw.GLFW;

import minecraftarmorweapon.client.renderer.ScabbardCurioRenderer;

/**
 * 鞘のリアルタイム調整GUI
 *
 * 操作方法:
 *   左ドラッグ: 回転 (rotX / rotZ)
 *   右ドラッグ: 位置 (x / y)
 *   スクロール: 奥行き (z)
 *   Shift+左ドラッグ: rotY 調整
 *   Shift+スクロール: スケール (均一)
 *   Tab: back / belt 切り替え
 *   R: 値をリセット
 *   Enter: 現在の値をチャットにコピー用出力
 *   Esc: 閉じる
 */
public class ScabbardDebugScreen extends Screen {

    private boolean editingBack = true; // true=back, false=belt
    private double lastMouseX, lastMouseY;
    private boolean draggingLeft = false;
    private boolean draggingRight = false;

    public ScabbardDebugScreen() {
        super(Component.literal("鞘デバッグ"));
    }

    @Override
    public boolean isPauseScreen() {
        return false; // ゲームを止めない
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 半透明の背景
        graphics.fill(5, 5, 260, editingBack ? 175 : 175, 0x88000000);

        String slot = editingBack ? "§e背中 (back)" : "§e腰 (belt)";
        graphics.drawString(this.font, slot, 10, 10, 0xFFFFFF);
        graphics.drawString(this.font, "§7Tab: 切替  Esc: 閉じる  R: リセット  Enter: コピー", 10, 22, 0xAAAAAA);

        double px, py, pz;
        float rx, ry, rz, sx, sy, sz;
        if (editingBack) {
            px = ScabbardCurioRenderer.backX; py = ScabbardCurioRenderer.backY; pz = ScabbardCurioRenderer.backZ;
            rx = ScabbardCurioRenderer.backRotX; ry = ScabbardCurioRenderer.backRotY; rz = ScabbardCurioRenderer.backRotZ;
            sx = ScabbardCurioRenderer.backScaleX; sy = ScabbardCurioRenderer.backScaleY; sz = ScabbardCurioRenderer.backScaleZ;
        } else {
            px = ScabbardCurioRenderer.beltX; py = ScabbardCurioRenderer.beltY; pz = ScabbardCurioRenderer.beltZ;
            rx = ScabbardCurioRenderer.beltRotX; ry = ScabbardCurioRenderer.beltRotY; rz = ScabbardCurioRenderer.beltRotZ;
            sx = ScabbardCurioRenderer.beltScaleX; sy = ScabbardCurioRenderer.beltScaleY; sz = ScabbardCurioRenderer.beltScaleZ;
        }

        int y = 38;
        graphics.drawString(this.font, "§b--- 位置 (右ドラッグ) ---", 10, y, 0xFFFFFF); y += 12;
        graphics.drawString(this.font, String.format("  X: §f%.3f", px), 10, y, 0xAAAAAA); y += 11;
        graphics.drawString(this.font, String.format("  Y: §f%.3f", py), 10, y, 0xAAAAAA); y += 11;
        graphics.drawString(this.font, String.format("  Z: §f%.3f §7(スクロール)", pz), 10, y, 0xAAAAAA); y += 14;

        graphics.drawString(this.font, "§b--- 回転 (左ドラッグ) ---", 10, y, 0xFFFFFF); y += 12;
        graphics.drawString(this.font, String.format("  rotX: §f%.1f°", rx), 10, y, 0xAAAAAA); y += 11;
        graphics.drawString(this.font, String.format("  rotY: §f%.1f° §7(Shift+左ドラッグ)", ry), 10, y, 0xAAAAAA); y += 11;
        graphics.drawString(this.font, String.format("  rotZ: §f%.1f°", rz), 10, y, 0xAAAAAA); y += 14;

        graphics.drawString(this.font, "§b--- スケール (Shift+スクロール) ---", 10, y, 0xFFFFFF); y += 12;
        graphics.drawString(this.font, String.format("  scale: §f%.2f / %.2f / %.2f", sx, sy, sz), 10, y, 0xAAAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) draggingLeft = true;
        if (button == 1) draggingRight = true;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) draggingLeft = false;
        if (button == 1) draggingRight = false;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        double dx = mouseX - lastMouseX;
        double dy = mouseY - lastMouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        boolean shift = hasShiftDown();

        if (draggingLeft) {
            // 左ドラッグ: 回転
            if (shift) {
                // Shift: rotY
                addRotY((float) (dx * 0.5));
            } else {
                // 通常: rotX (上下), rotZ (左右)
                addRotX((float) (dy * 0.5));
                addRotZ((float) (-dx * 0.5));
            }
        }

        if (draggingRight) {
            // 右ドラッグ: 位置
            addPosX(dx * 0.002);
            addPosY(-dy * 0.002);
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (hasShiftDown()) {
            // Shift+スクロール: スケール（均一）
            float d = (float) (delta * 0.05);
            addScale(d);
        } else {
            // スクロール: Z位置
            addPosZ(delta * 0.01);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            editingBack = !editingBack;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R) {
            resetValues();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            outputCode();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // === 位置変更 ===
    private void addPosX(double d) {
        if (editingBack) ScabbardCurioRenderer.backX += d;
        else ScabbardCurioRenderer.beltX += d;
    }
    private void addPosY(double d) {
        if (editingBack) ScabbardCurioRenderer.backY += d;
        else ScabbardCurioRenderer.beltY += d;
    }
    private void addPosZ(double d) {
        if (editingBack) ScabbardCurioRenderer.backZ += d;
        else ScabbardCurioRenderer.beltZ += d;
    }

    // === 回転変更 ===
    private void addRotX(float d) {
        if (editingBack) ScabbardCurioRenderer.backRotX += d;
        else ScabbardCurioRenderer.beltRotX += d;
    }
    private void addRotY(float d) {
        if (editingBack) ScabbardCurioRenderer.backRotY += d;
        else ScabbardCurioRenderer.beltRotY += d;
    }
    private void addRotZ(float d) {
        if (editingBack) ScabbardCurioRenderer.backRotZ += d;
        else ScabbardCurioRenderer.beltRotZ += d;
    }

    // === スケール変更 ===
    private void addScale(float d) {
        if (editingBack) {
            ScabbardCurioRenderer.backScaleX += d;
            ScabbardCurioRenderer.backScaleY += d;
            ScabbardCurioRenderer.backScaleZ += d;
        } else {
            ScabbardCurioRenderer.beltScaleX += d;
            ScabbardCurioRenderer.beltScaleY += d;
            ScabbardCurioRenderer.beltScaleZ += d;
        }
    }

    // === リセット ===
    private void resetValues() {
        if (editingBack) {
            ScabbardCurioRenderer.backX = -0.330; ScabbardCurioRenderer.backY = 0.040; ScabbardCurioRenderer.backZ = 0.130;
            ScabbardCurioRenderer.backRotX = 0f; ScabbardCurioRenderer.backRotY = 270f; ScabbardCurioRenderer.backRotZ = 140f;
            ScabbardCurioRenderer.backScaleX = 1.15f; ScabbardCurioRenderer.backScaleY = 1.3f; ScabbardCurioRenderer.backScaleZ = 1.15f;
        } else {
            ScabbardCurioRenderer.beltX = 0.280; ScabbardCurioRenderer.beltY = 0.660; ScabbardCurioRenderer.beltZ = -0.240;
            ScabbardCurioRenderer.beltRotX = -100f; ScabbardCurioRenderer.beltRotY = 180f; ScabbardCurioRenderer.beltRotZ = 0f;
            ScabbardCurioRenderer.beltScaleX = 1.15f; ScabbardCurioRenderer.beltScaleY = 1.3f; ScabbardCurioRenderer.beltScaleZ = 1.15f;
        }
    }

    // === コード出力 ===
    private void outputCode() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String slot = editingBack ? "back" : "belt";
        double px, py, pz;
        float rx, ry, rz, sx, sy, sz;
        if (editingBack) {
            px = ScabbardCurioRenderer.backX; py = ScabbardCurioRenderer.backY; pz = ScabbardCurioRenderer.backZ;
            rx = ScabbardCurioRenderer.backRotX; ry = ScabbardCurioRenderer.backRotY; rz = ScabbardCurioRenderer.backRotZ;
            sx = ScabbardCurioRenderer.backScaleX; sy = ScabbardCurioRenderer.backScaleY; sz = ScabbardCurioRenderer.backScaleZ;
        } else {
            px = ScabbardCurioRenderer.beltX; py = ScabbardCurioRenderer.beltY; pz = ScabbardCurioRenderer.beltZ;
            rx = ScabbardCurioRenderer.beltRotX; ry = ScabbardCurioRenderer.beltRotY; rz = ScabbardCurioRenderer.beltRotZ;
            sx = ScabbardCurioRenderer.beltScaleX; sy = ScabbardCurioRenderer.beltScaleY; sz = ScabbardCurioRenderer.beltScaleZ;
        }

        // 全コードを1つの文字列にまとめる
        String code = String.format(
                "poseStack.translate(%.3f, %.3f, %.3f);", px, py, pz)
                + "\n" + String.format("poseStack.mulPose(Axis.XP.rotationDegrees(%.1ff));", rx)
                + "\n" + String.format("poseStack.mulPose(Axis.ZP.rotationDegrees(%.1ff));", rz)
                + "\n" + String.format("poseStack.mulPose(Axis.YP.rotationDegrees(%.1ff));", ry)
                + "\n" + String.format("poseStack.scale(%.2ff, %.2ff, %.2ff);", sx, sy, sz);

        // クリックでクリップボードにコピーできるメッセージ
        mc.player.sendSystemMessage(Component.literal("§a=== " + slot + " §e[クリックでコピー] ==="));

        // 各行を表示 + 全体をクリックでコピー
        MutableComponent copyable = Component.literal("§f" + code.replace("\n", "\n§f"))
                .withStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code))
                        .withUnderlined(true));
        mc.player.sendSystemMessage(copyable);
        mc.player.sendSystemMessage(Component.literal("§7↑ クリックでクリップボードにコピー"));
    }

    // === キーバインドでGUIを開く ===
    public static KeyMapping DEBUG_KEY;

    @Mod.EventBusSubscriber(modid = "minecraft_armor_weapon", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class KeyRegister {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            DEBUG_KEY = new KeyMapping(
                    "key.minecraft_armor_weapon.scabbard_debug",
                    GLFW.GLFW_KEY_F8,
                    "key.categories.minecraft_armor_weapon"
            );
            event.register(DEBUG_KEY);
        }
    }

    @Mod.EventBusSubscriber(modid = "minecraft_armor_weapon", value = Dist.CLIENT)
    public static class KeyHandler {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (DEBUG_KEY != null && DEBUG_KEY.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen == null) {
                    mc.setScreen(new ScabbardDebugScreen());
                }
            }
        }
    }
}
