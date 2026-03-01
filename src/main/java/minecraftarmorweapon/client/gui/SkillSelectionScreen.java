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
import minecraftarmorweapon.skill.PlayerSkillData.WeaponLoadout;
import minecraftarmorweapon.skill.SkillRegistry;
import minecraftarmorweapon.skill.SkillRegistry.MotionInfo;
import minecraftarmorweapon.skill.SkillRegistry.MotionCategory;

import java.util.List;

public class SkillSelectionScreen extends Screen {

    private static final int GUI_WIDTH = 360;
    private static final int GUI_HEIGHT = 260;

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
    private static final int COLOR_LOADOUT_EMPTY = 0xFF252535;
    private static final int COLOR_LOADOUT_FILLED = 0xFF2A3A55;
    private static final int COLOR_LOADOUT_SELECTED = 0xFF1A4A6F;
    private static final int COLOR_LOADOUT_BORDER = 0xFF4488AA;
    private static final int COLOR_REMOVE_BTN = 0xFF5A1A1A;
    private static final int COLOR_REMOVE_BTN_HOVER = 0xFF8A2A2A;

    private PlayerSkillData.SkillStorage skillData;
    private String hoveredDescription = null;

    // -1 = デフォルト設定, 0以上 = ロードアウトインデックス
    private int selectedLoadoutIndex = -1;

    private int guiLeft;
    private int guiTop;

    // 武器スロット行のY座標
    private static final int LOADOUT_ROW_Y_OFFSET = 28;
    // 技設定行の開始Y
    private static final int MOTION_ROW_Y_OFFSET = 70;

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

        // 選択インデックスが範囲外になった場合はデフォルトに戻す
        if (selectedLoadoutIndex >= skillData.getWeaponLoadouts().size()) {
            selectedLoadoutIndex = -1;
        }

        buildWidgets();
    }

    private void buildWidgets() {
        this.clearWidgets();

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // === 武器ロードアウトスロット ===
        buildLoadoutSlots(player);

        // === 技設定行 ===
        buildMotionRows();
    }

    private void buildLoadoutSlots(Player player) {
        List<WeaponLoadout> loadouts = skillData.getWeaponLoadouts();
        int slotWidth = 80;
        int slotHeight = 20;
        int slotGap = 4;
        int totalSlotWidth = loadouts.size() * (slotWidth + slotGap);

        // 武器スロットを左端から並べる
        int startX = guiLeft + 8;
        int slotY = guiTop + LOADOUT_ROW_Y_OFFSET;

        for (int i = 0; i < loadouts.size(); i++) {
            final int loadoutIdx = i;
            final WeaponLoadout loadout = loadouts.get(i);
            String weaponName = loadout.getWeapon().getHoverName().getString();
            if (weaponName.length() > 10) weaponName = weaponName.substring(0, 9) + "…";
            final String displayName = weaponName;

            int btnX = startX + i * (slotWidth + slotGap);

            // ロードアウト選択ボタン
            addRenderableWidget(new AbstractButton(btnX, slotY, slotWidth, slotHeight,
                    Component.literal(displayName)) {
                @Override
                public void onPress() {
                    selectedLoadoutIndex = (selectedLoadoutIndex == loadoutIdx) ? -1 : loadoutIdx;
                    buildWidgets();
                }

                @Override
                public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float pt) {
                    boolean isSelected = selectedLoadoutIndex == loadoutIdx;
                    boolean isHover = this.isHoveredOrFocused();

                    int bgColor = isSelected ? COLOR_LOADOUT_SELECTED : (isHover ? COLOR_BTN_HOVER : COLOR_LOADOUT_FILLED);
                    g.fill(this.getX(), this.getY(),
                        this.getX() + this.width, this.getY() + this.height, bgColor);
                    drawBorder(g, this.getX(), this.getY(), this.width, this.height,
                        isSelected ? COLOR_BORDER : COLOR_LOADOUT_BORDER);

                    g.drawCenteredString(font, this.getMessage(),
                        this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2,
                        isSelected ? 0x7FD7FF : 0xCCCCCC);

                    if (isHover) {
                        hoveredDescription = loadout.getWeapon().getHoverName().getString() + " の技設定";
                    }
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput n) {
                    this.defaultButtonNarrationText(n);
                }
            });

            // 取り出しボタン（×）
            int removeBtnX = btnX + slotWidth - 10;
            addRenderableWidget(new AbstractButton(removeBtnX, slotY, 10, slotHeight,
                    Component.literal("\u00D7")) {
                @Override
                public void onPress() {
                    MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                        SkillSelectionPacket.removeWeaponLoadout(loadoutIdx)
                    );
                    if (selectedLoadoutIndex >= loadoutIdx) {
                        selectedLoadoutIndex = -1;
                    }
                    // サーバー反映前に楽観的更新
                    skillData.removeWeaponLoadout(loadoutIdx);
                    buildWidgets();
                }

                @Override
                public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float pt) {
                    boolean isHover = this.isHoveredOrFocused();
                    g.fill(this.getX(), this.getY(),
                        this.getX() + this.width, this.getY() + this.height,
                        isHover ? COLOR_REMOVE_BTN_HOVER : COLOR_REMOVE_BTN);
                    g.drawCenteredString(font, this.getMessage(),
                        this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2,
                        0xFF4444);

                    if (isHover) {
                        hoveredDescription = "武器を取り出す";
                    }
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput n) {
                    this.defaultButtonNarrationText(n);
                }
            });
        }

        // === 武器追加ボタン ===
        ItemStack mainHand = player.getMainHandItem();
        boolean canAdd = !mainHand.isEmpty()
            && !SkillRegistry.getSpecialIdsForWeapon(mainHand.getItem().getClass().getSimpleName()).isEmpty()
            && !skillData.hasLoadoutForWeapon(mainHand.getItem().getClass().getSimpleName());

        int addBtnX = startX + loadouts.size() * (slotWidth + slotGap);
        int addBtnColor = canAdd ? 0xFF2D6A4F : 0xFF3A3A3A;
        int addBtnTextColor = canAdd ? 0xFF88FF88 : 0xFF666666;

        addRenderableWidget(new AbstractButton(addBtnX, slotY, 60, slotHeight,
                Component.literal("+ 追加")) {
            @Override
            public void onPress() {
                if (!canAdd) return;
                MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                    SkillSelectionPacket.addWeaponLoadout()
                );
                // 楽観的更新
                ItemStack stored = mainHand.copy();
                stored.setCount(1);
                int newIdx = skillData.addWeaponLoadout(stored);
                selectedLoadoutIndex = newIdx;
                buildWidgets();
            }

            @Override
            public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float pt) {
                boolean isHover = this.isHoveredOrFocused() && canAdd;
                int bgColor = isHover ? 0xFF3D8A6F : addBtnColor;
                g.fill(this.getX(), this.getY(),
                    this.getX() + this.width, this.getY() + this.height, bgColor);
                drawBorder(g, this.getX(), this.getY(), this.width, this.height,
                    canAdd ? COLOR_BORDER : 0xFF444444);
                g.drawCenteredString(font, this.getMessage(),
                    this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2,
                    addBtnTextColor);

                if (this.isHoveredOrFocused()) {
                    if (canAdd) {
                        hoveredDescription = mainHand.getHoverName().getString() + " をスロットに登録";
                    } else if (mainHand.isEmpty()) {
                        hoveredDescription = "主手に特殊技を持つ武器を持ってください";
                    } else if (skillData.hasLoadoutForWeapon(mainHand.getItem().getClass().getSimpleName())) {
                        hoveredDescription = "この武器は既に登録済みです";
                    } else {
                        hoveredDescription = "この武器には特殊技がありません";
                    }
                }
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput n) {
                this.defaultButtonNarrationText(n);
            }
        });
    }

    private void buildMotionRows() {
        // 現在の選択に応じて技の選択肢を構築
        String weaponClass = null;
        if (selectedLoadoutIndex >= 0) {
            List<WeaponLoadout> loadouts = skillData.getWeaponLoadouts();
            if (selectedLoadoutIndex < loadouts.size()) {
                weaponClass = loadouts.get(selectedLoadoutIndex).getWeaponClass();
            } else {
                selectedLoadoutIndex = -1;
            }
        }
        final String finalWeaponClass = weaponClass;

        int rowY = guiTop + MOTION_ROW_Y_OFFSET;
        int labelWidth = 52;
        int btnHeight = 18;
        int btnGap = 2;
        int rowGap = 5;
        int btnAreaLeft = guiLeft + 8 + labelWidth;

        for (AttackSlot slot : AttackSlot.values()) {
            List<MotionInfo> motions = SkillRegistry.getAvailableMotions(slot, finalWeaponClass);

            int btnX = btnAreaLeft;
            for (MotionInfo motion : motions) {
                int btnWidth = Math.max(36, font.width(motion.getDisplayName()) + 10);
                final AttackSlot finalSlot = slot;
                final MotionInfo finalMotion = motion;

                addRenderableWidget(new AbstractButton(btnX, rowY, btnWidth, btnHeight,
                        Component.literal(motion.getDisplayName())) {
                    @Override
                    public void onPress() {
                        if (selectedLoadoutIndex == -1) {
                            skillData.setMotion(finalSlot, finalMotion.getId());
                        } else {
                            skillData.setLoadoutMotion(selectedLoadoutIndex, finalSlot, finalMotion.getId());
                        }
                        MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                            SkillSelectionPacket.selectLoadoutMotion(selectedLoadoutIndex, finalSlot, finalMotion.getId())
                        );
                        buildWidgets();
                    }

                    @Override
                    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float pt) {
                        String currentMotion;
                        if (selectedLoadoutIndex == -1) {
                            currentMotion = skillData.getMotion(finalSlot);
                        } else {
                            List<WeaponLoadout> loadouts = skillData.getWeaponLoadouts();
                            if (selectedLoadoutIndex < loadouts.size()) {
                                currentMotion = loadouts.get(selectedLoadoutIndex).getMotion(finalSlot);
                            } else {
                                currentMotion = skillData.getMotion(finalSlot);
                            }
                        }

                        boolean isSelected = finalMotion.getId().equals(currentMotion);
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

        // 武器スロットセクションラベル
        g.drawString(font, Component.literal("\u25BC \u6B66\u5668\u30ED\u30FC\u30C9\u30A2\u30A6\u30C8"),
            guiLeft + 8, guiTop + 18, COLOR_SLOT_LABEL, false);

        // 武器スロット行（空きスロットを視覚的に示す）
        drawEmptyLoadoutSlotBackground(g);

        // 区切り線
        int dividerY = guiTop + MOTION_ROW_Y_OFFSET - 6;
        g.fill(guiLeft + 4, dividerY, guiLeft + GUI_WIDTH - 4, dividerY + 1, 0xFF444466);

        // 選択中ロードアウトの表示
        String editingLabel;
        if (selectedLoadoutIndex == -1) {
            editingLabel = "[ デフォルト設定 ]";
        } else {
            List<WeaponLoadout> loadouts = skillData.getWeaponLoadouts();
            if (selectedLoadoutIndex < loadouts.size()) {
                editingLabel = "[ " + loadouts.get(selectedLoadoutIndex).getWeapon().getHoverName().getString() + " ]";
            } else {
                editingLabel = "[ デフォルト設定 ]";
            }
        }
        g.drawCenteredString(font, Component.literal(editingLabel),
            guiLeft + GUI_WIDTH / 2, dividerY + 3, 0xFFAACCFF);

        // スロットラベル（5行）
        int rowY = guiTop + MOTION_ROW_Y_OFFSET;
        int btnHeight = 18;
        int rowGap = 5;
        for (AttackSlot slot : AttackSlot.values()) {
            g.drawString(font, Component.literal(slot.getDisplayName()),
                guiLeft + 8, rowY + (btnHeight - 8) / 2, COLOR_SLOT_LABEL, false);
            rowY += btnHeight + rowGap;
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
                "\u30B9\u30ED\u30C3\u30C8\u3092\u9078\u629E\u3057\u3066\u6280\u3092\u8A2D\u5B9A | K\u30AD\u30FC\u3067\u9589\u3058\u308B"),
                guiLeft + GUI_WIDTH / 2, footerY + 9, COLOR_FOOTER_TEXT);
        }
    }

    private void drawEmptyLoadoutSlotBackground(GuiGraphics g) {
        // 武器スロット行の背景として薄い枠を描く
        int slotY = guiTop + LOADOUT_ROW_Y_OFFSET;
        int slotHeight = 20;
        int panelWidth = GUI_WIDTH - 16;
        g.fill(guiLeft + 8, slotY - 1, guiLeft + 8 + panelWidth, slotY + slotHeight + 1, 0xFF1A1A30);
        drawBorder(g, guiLeft + 8, slotY - 1, panelWidth, slotHeight + 2, 0xFF333355);
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
