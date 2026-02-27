package minecraftarmorweapon.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.network.SkillSelectionPacket;
import minecraftarmorweapon.skill.PlayerSkillData;
import minecraftarmorweapon.skill.PlayerSkillData.SkillType;
import minecraftarmorweapon.skill.PlayerSkillData.WeaponType;
import minecraftarmorweapon.skill.SkillRegistry;
import minecraftarmorweapon.skill.SkillRegistry.SkillInfo;

import java.util.List;

public class SkillSelectionScreen extends Screen {

    // GUI全体のサイズ
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 200;

    // 色定義
    private static final int COLOR_BG = 0xCC1A1A2E;           // 背景（暗い紫）
    private static final int COLOR_HEADER = 0xFF2D6A4F;        // ヘッダー（緑）
    private static final int COLOR_HEADER_TEXT = 0xFFFFFFFF;    // ヘッダー文字
    private static final int COLOR_TAB_SELECTED = 0xFF2D6A4F;  // 選択中タブ（緑）
    private static final int COLOR_TAB_NORMAL = 0xFF333344;     // 通常タブ
    private static final int COLOR_TAB_HOVER = 0xFF444466;      // ホバータブ
    private static final int COLOR_CARD_NORMAL = 0xFF2A2A3D;    // スキルカード通常
    private static final int COLOR_CARD_SELECTED = 0xFF1B5E20;  // スキルカード選択中（深緑）
    private static final int COLOR_CARD_HOVER = 0xFF3A3A55;     // スキルカードホバー
    private static final int COLOR_BORDER = 0xFF4CAF50;         // 選択中ボーダー（緑）
    private static final int COLOR_SECTION_TITLE = 0xFFFFD700;  // セクションタイトル（金）
    private static final int COLOR_SKILL_NAME = 0xFFFFFFFF;     // スキル名
    private static final int COLOR_DESCRIPTION = 0xFFAAAAAA;    // 説明文
    private static final int COLOR_FOOTER = 0xFF2D6A4F;        // フッター（緑）
    private static final int COLOR_FOOTER_TEXT = 0xFFCCCCCC;    // フッター文字

    private WeaponType currentWeaponType;
    private PlayerSkillData.SkillStorage skillData;
    private String hoveredSkillDescription = null;

    private int guiLeft;
    private int guiTop;

    public SkillSelectionScreen() {
        super(Component.literal("技の選択"));
    }

    @Override
    public void init() {
        super.init();

        guiLeft = (this.width - GUI_WIDTH) / 2;
        guiTop = (this.height - GUI_HEIGHT) / 2;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        skillData = PlayerSkillData.getSkillData(player);
        currentWeaponType = skillData.getSelectedWeaponType();

        buildSkillWidgets();
    }

    private void buildSkillWidgets() {
        this.clearWidgets();

        int tabX = guiLeft + 4;
        int tabY = guiTop + 28;
        int tabWidth = 50;
        int tabHeight = 18;

        // 武器タイプ選択タブ
        for (WeaponType type : WeaponType.values()) {
            final WeaponType weaponType = type;
            final int btnY = tabY;
            addRenderableWidget(new AbstractButton(tabX, btnY, tabWidth, tabHeight, Component.literal(type.getDisplayName())) {
                @Override
                public void onPress() {
                    currentWeaponType = weaponType;
                    skillData.setSelectedWeaponType(weaponType);
                    MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                        new SkillSelectionPacket(weaponType, null, null, null)
                    );
                    buildSkillWidgets();
                }

                @Override
                public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float pt) {
                    boolean isSelected = currentWeaponType == weaponType;
                    int bgColor = isSelected ? COLOR_TAB_SELECTED : (this.isHoveredOrFocused() ? COLOR_TAB_HOVER : COLOR_TAB_NORMAL);
                    g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);
                    if (isSelected) {
                        drawBorder(g, this.getX(), this.getY(), this.width, this.height, COLOR_BORDER);
                    }
                    g.drawCenteredString(font, this.getMessage(),
                        this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2,
                        isSelected ? 0xFFFFFF : 0xAAAAAA);
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput n) {
                    this.defaultButtonNarrationText(n);
                }
            });
            tabY += tabHeight + 3;
        }

        // スキル選択カード
        int cardAreaX = guiLeft + 60;
        int cardAreaY = guiTop + 28;
        int cardWidth = 66;
        int cardHeight = 36;
        int cardGap = 4;

        for (SkillType skillType : SkillType.values()) {
            List<SkillInfo> skills = SkillRegistry.getSkills(currentWeaponType, skillType);

            int cx = cardAreaX;
            for (SkillInfo skill : skills) {
                final SkillInfo currentSkill = skill;
                final SkillType currentSkillType = skillType;
                addRenderableWidget(new AbstractButton(cx, cardAreaY, cardWidth, cardHeight, Component.literal(skill.getDisplayName())) {
                    @Override
                    public void onPress() {
                        skillData.setSelectedSkill(currentSkillType, currentSkill.getId());
                        MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                            new SkillSelectionPacket(null, currentSkillType, currentSkill.getId(), null)
                        );
                        buildSkillWidgets();
                    }

                    @Override
                    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float pt) {
                        boolean isSelected = currentSkill.getId().equals(skillData.getSelectedSkill(currentSkillType));
                        boolean isHover = this.isHoveredOrFocused();

                        int bgColor = isSelected ? COLOR_CARD_SELECTED : (isHover ? COLOR_CARD_HOVER : COLOR_CARD_NORMAL);
                        g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);

                        if (isSelected) {
                            drawBorder(g, this.getX(), this.getY(), this.width, this.height, COLOR_BORDER);
                        }

                        // スキル名
                        g.drawCenteredString(font, currentSkill.getDisplayName(),
                            this.getX() + this.width / 2, this.getY() + 6,
                            isSelected ? 0x7FFF7F : COLOR_SKILL_NAME);

                        // 選択マーク
                        if (isSelected) {
                            g.drawCenteredString(font, "\u2714",
                                this.getX() + this.width / 2, this.getY() + 22, 0x00FF00);
                        }

                        // ホバー時に説明を更新
                        if (isHover) {
                            hoveredSkillDescription = currentSkill.getDescription();
                        }
                    }

                    @Override
                    protected void updateWidgetNarration(NarrationElementOutput n) {
                        this.defaultButtonNarrationText(n);
                    }
                });
                cx += cardWidth + cardGap;
            }
            cardAreaY += cardHeight + 16;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        hoveredSkillDescription = null;

        // 背景の暗転
        this.renderBackground(guiGraphics);

        // メインの背景パネル
        guiGraphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, COLOR_BG);
        drawBorder(guiGraphics, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, 0xFF555577);

        // ヘッダー（緑のバー）
        guiGraphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + 24, COLOR_HEADER);
        guiGraphics.drawCenteredString(font, Component.literal("\u2694 \u6280\u306E\u9078\u629E \u2694"),
            guiLeft + GUI_WIDTH / 2, guiTop + 8, COLOR_HEADER_TEXT);

        // セクションタイトル（技タイプ名）
        int sectionY = guiTop + 28;
        int cardHeight = 36;
        for (SkillType skillType : SkillType.values()) {
            guiGraphics.drawString(font, Component.literal(skillType.getDisplayName()),
                guiLeft + 62, sectionY - 1, COLOR_SECTION_TITLE, false);
            sectionY += cardHeight + 16;
        }

        // ボタン描画
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // フッター（緑のバー）- システム情報
        int footerY = guiTop + GUI_HEIGHT - 30;
        guiGraphics.fill(guiLeft, footerY, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, COLOR_FOOTER);

        // ホバー中のスキル説明を表示
        if (hoveredSkillDescription != null) {
            guiGraphics.drawCenteredString(font, Component.literal(hoveredSkillDescription),
                guiLeft + GUI_WIDTH / 2, footerY + 5, 0xFFFFFF);
            guiGraphics.drawCenteredString(font, Component.literal("\u30AF\u30EA\u30C3\u30AF\u3067\u9078\u629E"),
                guiLeft + GUI_WIDTH / 2, footerY + 17, COLOR_FOOTER_TEXT);
        } else {
            // 現在の装備情報
            guiGraphics.drawCenteredString(font, Component.literal(
                "\u6B66\u5668\u30BF\u30A4\u30D7: " + currentWeaponType.getDisplayName() + " | K\u30AD\u30FC\u3067\u958B\u9589"),
                guiLeft + GUI_WIDTH / 2, footerY + 5, COLOR_FOOTER_TEXT);
            guiGraphics.drawCenteredString(font, Component.literal(
                "\u6280\u3092\u30AF\u30EA\u30C3\u30AF\u3057\u3066\u9078\u629E\u3057\u3066\u304F\u3060\u3055\u3044"),
                guiLeft + GUI_WIDTH / 2, footerY + 17, COLOR_FOOTER_TEXT);
        }
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);             // top
        g.fill(x, y + h - 1, x + w, y + h, color);     // bottom
        g.fill(x, y, x + 1, y + h, color);              // left
        g.fill(x + w - 1, y, x + w, y + h, color);      // right
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Kキーで閉じる
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_K) {
            this.onClose();
            return true;
        }
        // Eキーでも閉じる（インベントリキー）
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_E) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
