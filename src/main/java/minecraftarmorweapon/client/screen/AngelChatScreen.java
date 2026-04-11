package minecraftarmorweapon.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.network.AngelChatC2SPacket;
import minecraftarmorweapon.network.AngelChatS2CPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * 天使の三人組との会話GUI。
 * チャット風の画面にメッセージ履歴が表示され、下にテキスト入力欄がある。
 */
public class AngelChatScreen extends Screen {

    private EditBox inputField;
    private String entityName;
    private int personality;
    private String entityUUID;

    // メッセージ履歴
    private static final List<ChatLine> chatHistory = new ArrayList<>();
    private int scrollOffset = 0;

    // 待機中フラグ
    private boolean waitingForResponse = false;

    private static class ChatLine {
        final String speaker; // 名前
        final String message;
        final boolean isPlayer;
        final int color;

        ChatLine(String speaker, String message, boolean isPlayer, int color) {
            this.speaker = speaker;
            this.message = message;
            this.isPlayer = isPlayer;
            this.color = color;
        }
    }

    public AngelChatScreen(String entityName, int personality, String entityUUID, String firstMessage) {
        super(Component.literal("会話"));
        this.entityName = entityName;
        this.personality = personality;
        this.entityUUID = entityUUID;
        chatHistory.clear();
        if (!firstMessage.isEmpty()) {
            addEntityMessage(firstMessage);
        }
    }

    @Override
    protected void init() {
        // テキスト入力欄（画面下部）
        int inputWidth = this.width - 80;
        inputField = new EditBox(this.font, 10, this.height - 30, inputWidth, 20, Component.literal(""));
        inputField.setMaxLength(200);
        inputField.setFocused(true);
        this.addRenderableWidget(inputField);
        this.setFocused(inputField);

        // 送信ボタン
        this.addRenderableWidget(Button.builder(Component.literal("送信"), btn -> sendMessage())
            .bounds(this.width - 65, this.height - 30, 55, 20).build());
    }

    private void sendMessage() {
        String text = inputField.getValue().trim();
        if (text.isEmpty() || waitingForResponse) return;

        // プレイヤーのメッセージを表示
        addPlayerMessage(text);
        inputField.setValue("");

        // byeで終了
        if ("bye".equalsIgnoreCase(text)) {
            addSystemMessage("会話を終了しました");
            MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                new AngelChatC2SPacket("bye", entityUUID));
            // 少し待ってから閉じる
            Minecraft.getInstance().tell(() -> {
                try { Thread.sleep(500); } catch (Exception e) {}
                Minecraft.getInstance().setScreen(null);
            });
            return;
        }

        // サーバーに送信
        waitingForResponse = true;
        MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
            new AngelChatC2SPacket(text, entityUUID));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // Escape
            // 終了パケットを送って閉じる
            MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                new AngelChatC2SPacket("bye", entityUUID));
            this.onClose();
            return true;
        }
        if (keyCode == 257) { // Enter
            sendMessage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // ヘッダー背景
        graphics.fill(0, 0, this.width, 25, 0xCC000000);
        // タイトル
        String titleColor = personality == 0 ? "\u00A7a" : "\u00A7f";
        graphics.drawCenteredString(this.font,
            titleColor + entityName + "\u00A77 との会話  \u00A78(byeで終了)", this.width / 2, 8, 0xFFFFFF);

        // チャット領域背景
        int chatTop = 28;
        int chatBottom = this.height - 38;
        graphics.fill(5, chatTop, this.width - 5, chatBottom, 0x90000000);

        // メッセージ描画
        int lineHeight = 12;
        int maxLines = (chatBottom - chatTop - 8) / lineHeight;
        int startIdx = Math.max(0, chatHistory.size() - maxLines - scrollOffset);
        int endIdx = Math.min(chatHistory.size(), startIdx + maxLines);

        int y = chatTop + 4;
        for (int i = startIdx; i < endIdx; i++) {
            ChatLine line = chatHistory.get(i);
            String prefix = line.isPlayer ? "\u00A7e[あなた] \u00A7f" : "\u00A77[" + getNameColor() + line.speaker + "\u00A77] \u00A7f";
            graphics.drawString(this.font, prefix + line.message, 12, y, line.color, false);
            y += lineHeight;
        }

        // 待機中表示
        if (waitingForResponse) {
            graphics.drawString(this.font, "\u00A78考え中...", 12, chatBottom - lineHeight, 0x888888, false);
        }

        // 入力欄の背景
        graphics.fill(5, this.height - 35, this.width - 5, this.height - 5, 0x80000000);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollOffset = Math.max(0, Math.min(chatHistory.size() - 5, scrollOffset - (int) delta));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String getNameColor() {
        return switch (personality) {
            case 0 -> "\u00A7a"; // 緑
            case 1 -> "\u00A7f"; // 白
            case 2 -> "\u00A7f"; // 白
            default -> "\u00A7f";
        };
    }

    // === メッセージ追加 ===

    private void addPlayerMessage(String message) {
        chatHistory.add(new ChatLine("あなた", message, true, 0xFFFFFF));
        scrollOffset = 0;
    }

    private void addEntityMessage(String message) {
        chatHistory.add(new ChatLine(entityName, message, false, 0xFFFFFF));
        scrollOffset = 0;
    }

    private void addSystemMessage(String message) {
        chatHistory.add(new ChatLine("", "\u00A77" + message, false, 0x888888));
        scrollOffset = 0;
    }

    // === S2Cパケット受信ハンドラ（クライアント側） ===

    public static void handlePacket(AngelChatS2CPacket packet) {
        Minecraft mc = Minecraft.getInstance();

        if (packet.openGui) {
            // GUI を開く
            mc.tell(() -> mc.setScreen(new AngelChatScreen(
                packet.entityName, packet.personality, packet.entityUUID, packet.message)));
            return;
        }

        if (packet.closeGui) {
            // GUI を閉じる
            mc.tell(() -> {
                if (mc.screen instanceof AngelChatScreen) {
                    mc.setScreen(null);
                }
            });
            return;
        }

        // 応答メッセージ
        mc.tell(() -> {
            if (mc.screen instanceof AngelChatScreen chatScreen) {
                chatScreen.addEntityMessage(packet.message);
                chatScreen.waitingForResponse = false;
            }
        });
    }
}
