package minecraftarmorweapon.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.network.AngelChatC2SPacket;
import minecraftarmorweapon.network.AngelChatS2CPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * RPG風の選択肢ベース会話GUI。
 * NPCの台詞と、プレイヤーが選ぶ選択肢ボタンを表示。
 */
public class AngelChatScreen extends Screen {

    private String entityName;
    private int personality;
    private String entityUUID;
    private String npcText = "";
    private List<String> choiceTexts = new ArrayList<>();
    private List<String> choiceNexts = new ArrayList<>();
    private boolean isEnd = false;

    public AngelChatScreen(String entityName, int personality, String entityUUID,
                           String npcText, List<String> choiceTexts, List<String> choiceNexts, boolean isEnd) {
        super(Component.literal("会話"));
        this.entityName = entityName;
        this.personality = personality;
        this.entityUUID = entityUUID;
        this.npcText = npcText;
        this.choiceTexts = choiceTexts;
        this.choiceNexts = choiceNexts;
        this.isEnd = isEnd;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int buttonWidth = 280;
        int buttonHeight = 22;
        int buttonSpacing = 4;

        // 選択肢ボタンを画面下部に並べる
        int totalButtons = isEnd ? 1 : choiceTexts.size();
        int totalHeight = totalButtons * (buttonHeight + buttonSpacing);
        int startY = this.height - totalHeight - 20;

        if (isEnd) {
            // 会話終了 → 閉じるボタンのみ
            this.addRenderableWidget(Button.builder(
                Component.literal("§f閉じる"),
                btn -> this.onClose())
                .bounds(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight).build());
        } else {
            for (int i = 0; i < choiceTexts.size(); i++) {
                String choiceText = choiceTexts.get(i);
                String nextId = choiceNexts.get(i);
                int y = startY + i * (buttonHeight + buttonSpacing);
                this.addRenderableWidget(Button.builder(
                    Component.literal("§f▶ " + choiceText),
                    btn -> selectChoice(nextId))
                    .bounds(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight).build());
            }
        }
    }

    private void selectChoice(String nextNodeId) {
        // サーバーに選択を送信
        MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
            new AngelChatC2SPacket(nextNodeId, entityUUID));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // Escape
            this.onClose();
            return true;
        }
        // 数字キーで選択
        if (keyCode >= 49 && keyCode <= 57 && !isEnd) { // 1-9
            int index = keyCode - 49;
            if (index < choiceNexts.size()) {
                selectChoice(choiceNexts.get(index));
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // ヘッダー
        graphics.fill(0, 0, this.width, 30, 0xCC000000);
        String nameColor = personality == 0 ? "§a" : "§f";
        graphics.drawCenteredString(this.font,
            nameColor + "§l" + entityName + "§r§7 との会話", this.width / 2, 11, 0xFFFFFF);

        // NPC台詞エリア（画面中央上部）
        int textTop = 50;
        int textBottom = this.height - (isEnd ? 60 : (choiceTexts.size() * 26 + 40));
        int textLeft = this.width / 2 - 200;
        int textRight = this.width / 2 + 200;

        graphics.fill(textLeft - 10, textTop - 10, textRight + 10, textBottom, 0xA0000000);
        graphics.fill(textLeft - 10, textTop - 10, textRight + 10, textTop - 9, 0xFF888888); // 上ボーダー
        graphics.fill(textLeft - 10, textBottom - 1, textRight + 10, textBottom, 0xFF888888); // 下ボーダー

        // 話者名
        graphics.drawString(this.font, nameColor + "§l" + entityName, textLeft, textTop, 0xFFFFFF, false);

        // 台詞本文（自動改行）
        List<String> wrappedLines = wrapText(npcText, 400);
        int y = textTop + 15;
        for (String line : wrappedLines) {
            graphics.drawString(this.font, "§f" + line, textLeft, y, 0xFFFFFF, false);
            y += 11;
        }

        // 選択肢エリアの背景
        if (!isEnd && !choiceTexts.isEmpty()) {
            int choicesTop = this.height - (choiceTexts.size() * 26) - 30;
            graphics.fill(0, choicesTop - 10, this.width, this.height, 0x80000000);
            graphics.drawCenteredString(this.font,
                "§7クリック or 数字キー(1-9)で選択", this.width / 2, choicesTop - 8, 0x888888);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * テキストを指定幅で改行する
     */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            current.append(c);
            if (this.font.width(current.toString()) > maxWidth || c == '\n') {
                if (c == '\n') current.setLength(current.length() - 1);
                result.add(current.toString());
                current.setLength(0);
            }
        }
        if (current.length() > 0) result.add(current.toString());
        return result;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // === S2Cパケット受信 ===

    public static void handlePacket(AngelChatS2CPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.tell(() -> {
            // 既に会話画面なら内容を更新、違うなら新規オープン
            if (mc.screen instanceof AngelChatScreen existing) {
                existing.npcText = packet.npcText;
                existing.choiceTexts = packet.choiceTexts;
                existing.choiceNexts = packet.choiceNexts;
                existing.isEnd = packet.isEnd;
                existing.rebuild();
            } else {
                mc.setScreen(new AngelChatScreen(
                    packet.entityName, packet.personality, "unused",
                    packet.npcText, packet.choiceTexts, packet.choiceNexts, packet.isEnd));
            }
        });
    }
}
