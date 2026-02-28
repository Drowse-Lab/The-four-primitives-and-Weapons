package minecraftarmorweapon.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.network.SkillSelectionPacket;
import minecraftarmorweapon.skill.PlayerSkillData;
import minecraftarmorweapon.skill.PlayerSkillData.AttackSlot;
import minecraftarmorweapon.skill.SkillRegistry;
import minecraftarmorweapon.skill.SkillRegistry.MotionInfo;
import minecraftarmorweapon.skill.SkillRegistry.MotionCategory;

import java.util.List;
import java.util.Set;

public class SkillSelectionScreen extends Screen {

    private static final int GUI_WIDTH = 340;
    private static final int GUI_HEIGHT = 230;

    // 色定義
    private static final int COLOR_BG = 0xCC1A1A2E;
    private static final int COLOR_HEADER = 0xFF2D6A4F;
    private static final int COLOR_HEADER_TEXT = 0xFFFFFFFF;
    private static final int COLOR_BTN_NORMAL = 0xFF2A2A3D;
    private static final int COLOR_BTN_SELECTED = 0xFF1B5E20;
    private static final int COLOR_BTN_HOVER = 0xFF3A3A55;
    private static final int COLOR_BTN_SPECIAL = 0xFF2A2040;
    private static final int COLOR_BORDER = 0xFF4CAF50;
    private static final int COLOR_SLOT_LABEL = 0xFFFFD700;
    private static final int COLOR_FOOTER = 0xFF2D6A4F;
    private static final int COLOR_FOOTER_TEXT = 0xFFCCCCCC;
    private static final int COLOR_LOCK_BTN = 0xFF4A2040;
    private static final int COLOR_LOCK_BTN_HOVER = 0xFF6A3060;

    private PlayerSkillData.SkillStorage skillData;
    private String hoveredDescription = null;

    private int guiLeft;
    private int guiTop;
    private int weaponLockSectionY;

    public SkillSelectionScreen() {
        super(Component.literal("\u6280\u306E\u9078\u629E"));
    }

    @Override
    public void init() {
        super.init();

        guiLeft = (this.width - GUI_WIDTH) / 2;
        guiTop = (this.height - GUI_HEIGHT) / 2;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        skillData = PlayerSkillData.getSkillData(player);
        buildWidgets();
    }

    private void buildWidgets() {
        this.clearWidgets();

        Set<String> unlockedSpecials = skillData.getUnlockedSpecials();

        int rowY = guiTop + 30;
        int labelWidth = 50;
        int btnHeight = 18;
        int btnGap = 2;
        int rowGap = 5;
        int btnAreaLeft = guiLeft + 8 + labelWidth;

        // === 5つの攻撃スロット行 ===
        for (AttackSlot slot : AttackSlot.values()) {
            List<MotionInfo> motions = SkillRegistry.getAvailableMotions(slot, unlockedSpecials);

            int btnX = btnAreaLeft;
            for (MotionInfo motion : motions) {
                int btnWidth = Math.max(36, font.width(motion.getDisplayName()) + 10);
                final AttackSlot finalSlot = slot;
                final MotionInfo finalMotion = motion;

                addRenderableWidget(new AbstractButton(btnX, rowY, btnWidth, btnHeight,
                        Component.literal(motion.getDisplayName())) {
                    @Override
                    public void onPress() {
                        skillData.setMotion(finalSlot, finalMotion.getId());
                        MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                            SkillSelectionPacket.selectMotion(finalSlot, finalMotion.getId())
                        );
                        buildWidgets();
                    }

                    @Override
                    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float pt) {
                        boolean isSelected = finalMotion.getId().equals(skillData.getMotion(finalSlot));
                        boolean isHover = this.isHoveredOrFocused();
                        boolean isSpecial = finalMotion.getCategory() == MotionCategory.SPECIAL;

                        int bgColor;
                        if (isSelected) bgColor = COLOR_BTN_SELECTED;
                        else if (isHover) bgColor = COLOR_BTN_HOVER;
                        else bgColor = isSpecial ? COLOR_BTN_SPECIAL : COLOR_BTN_NORMAL;

                        g.fill(this.getX(), this.getY(),
                            this.getX() + this.width, this.getY() + this.height, bgColor);

                        if (isSelected) {
                            drawBorder(g, this.getX(), this.getY(), this.width, this.height, COLOR_BORDER);
                        }

                        g.drawCenteredString(font, this.getMessage(),
                            this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2,
                            isSelected ? 0x7FFF7F : 0xFFFFFF);

                        if (isHover) {
                            hoveredDescription = finalMotion.getDescription();
                        }
                    }

                    @Override
                    protected void updateWidgetNarration(NarrationElementOutput n) {
                        this.defaultButtonNarrationText(n);
                    }
                });
                btnX += btnWidth + btnGap;
            }
            rowY += btnHeight + rowGap;
        }

        weaponLockSectionY = rowY + 4;

        // === 武器ロックボタン ===
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            ItemStack mainHand = player.getMainHandItem();
            if (!mainHand.isEmpty()) {
                String weaponClass = mainHand.getItem().getClass().getSimpleName();
                List<String> specialIds = SkillRegistry.getSpecialIdsForWeapon(weaponClass);

                // 未解放の特殊スキルがあるか確認
                boolean hasNewSpecials = false;
                for (String sid : specialIds) {
                    if (!skillData.isSpecialUnlocked(sid)) {
                        hasNewSpecials = true;
                        break;
                    }
                }

                if (hasNewSpecials) {
                    String lockText = mainHand.getHoverName().getString() + " \u3092\u30ED\u30C3\u30AF";
                    int lockBtnWidth = Math.max(120, font.width(lockText) + 16);
                    int lockBtnX = guiLeft + (GUI_WIDTH - lockBtnWidth) / 2;

                    addRenderableWidget(new AbstractButton(lockBtnX, weaponLockSectionY + 14, lockBtnWidth, 20,
                            Component.literal(lockText)) {
                        @Override
                        public void onPress() {
                            MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                                SkillSelectionPacket.lockWeapon(weaponClass)
                            );
                            buildWidgets();
                        }

                        @Override
                        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float pt) {
                            boolean isHover = this.isHoveredOrFocused();
                            int bgColor = isHover ? COLOR_LOCK_BTN_HOVER : COLOR_LOCK_BTN;

                            g.fill(this.getX(), this.getY(),
                                this.getX() + this.width, this.getY() + this.height, bgColor);
                            drawBorder(g, this.getX(), this.getY(), this.width, this.height, 0xFFFF4444);

                            g.drawCenteredString(font, this.getMessage(),
                                this.getX() + this.width / 2, this.getY() + 6, 0xFFFF4444);

                            if (isHover) {
                                hoveredDescription = "\u30E1\u30A4\u30F3\u30CF\u30F3\u30C9\u306E\u6B66\u5668\u3092\u6D88\u8CBB\u3057\u3066\u7279\u6B8A\u6280\u3092\u89E3\u653E\u3057\u307E\u3059\uFF08\u5143\u306B\u623B\u305B\u307E\u305B\u3093\uFF09";
                            }
                        }

                        @Override
                        protected void updateWidgetNarration(NarrationElementOutput n) {
                            this.defaultButtonNarrationText(n);
                        }
                    });
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hoveredDescription = null;

        // 背景の暗転
        this.renderBackground(g);

        // メインの背景パネル
        g.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, COLOR_BG);
        drawBorder(g, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, 0xFF555577);

        // ヘッダー
        g.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + 24, COLOR_HEADER);
        g.drawCenteredString(font, Component.literal("\u2694 \u6280\u306E\u9078\u629E \u2694"),
            guiLeft + GUI_WIDTH / 2, guiTop + 8, COLOR_HEADER_TEXT);

        // スロットラベル（5行）
        int rowY = guiTop + 30;
        int btnHeight = 18;
        int rowGap = 5;
        for (AttackSlot slot : AttackSlot.values()) {
            g.drawString(font, Component.literal(slot.getDisplayName()),
                guiLeft + 8, rowY + (btnHeight - 8) / 2, COLOR_SLOT_LABEL, false);
            rowY += btnHeight + rowGap;
        }

        // 特殊技解放セクション
        g.drawString(font, Component.literal("\u25BC \u7279\u6B8A\u6280\u89E3\u653E"),
            guiLeft + 8, weaponLockSectionY + 2, COLOR_SLOT_LABEL, false);

        // 解放済みスキル表示
        Set<String> unlocked = skillData.getUnlockedSpecials();
        if (!unlocked.isEmpty()) {
            StringBuilder sb = new StringBuilder("\u89E3\u653E\u6E08: ");
            boolean first = true;
            for (String sid : unlocked) {
                MotionInfo info = SkillRegistry.getById(sid);
                if (info != null) {
                    if (!first) sb.append(", ");
                    sb.append(info.getDisplayName());
                    first = false;
                }
            }
            g.drawString(font, Component.literal(sb.toString()),
                guiLeft + 80, weaponLockSectionY + 2, 0xFF88FF88, false);
        }

        // ロック済み武器一覧
        List<ItemStack> lockedWeapons = skillData.getLockedWeapons();
        if (!lockedWeapons.isEmpty()) {
            int weaponX = guiLeft + 8;
            int weaponY = weaponLockSectionY + 38;
            g.drawString(font, Component.literal("\u30ED\u30C3\u30AF\u6E08: "),
                weaponX, weaponY, 0xFFAA6666, false);
            int textX = weaponX + font.width("\u30ED\u30C3\u30AF\u6E08: ");
            for (int i = 0; i < lockedWeapons.size(); i++) {
                ItemStack w = lockedWeapons.get(i);
                String name = w.getHoverName().getString();
                if (i > 0) {
                    g.drawString(font, Component.literal(", "), textX, weaponY, 0xFFAA6666, false);
                    textX += font.width(", ");
                }
                g.drawString(font, Component.literal(name), textX, weaponY, 0xFFFF8888, false);
                textX += font.width(name);
            }
        }

        // ボタン描画
        super.render(g, mouseX, mouseY, partialTick);

        // フッター
        int footerY = guiTop + GUI_HEIGHT - 26;
        g.fill(guiLeft, footerY, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, COLOR_FOOTER);

        if (hoveredDescription != null) {
            g.drawCenteredString(font, Component.literal(hoveredDescription),
                guiLeft + GUI_WIDTH / 2, footerY + 4, 0xFFFFFF);
            g.drawCenteredString(font, Component.literal("\u30AF\u30EA\u30C3\u30AF\u3067\u9078\u629E"),
                guiLeft + GUI_WIDTH / 2, footerY + 14, COLOR_FOOTER_TEXT);
        } else {
            g.drawCenteredString(font, Component.literal(
                "\u30E2\u30FC\u30B7\u30E7\u30F3\u3092\u30AF\u30EA\u30C3\u30AF\u3057\u3066\u9078\u629E | K\u30AD\u30FC\u3067\u9589\u3058\u308B"),
                guiLeft + GUI_WIDTH / 2, footerY + 9, COLOR_FOOTER_TEXT);
        }
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_K) {
            this.onClose();
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_E) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
