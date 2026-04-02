package minecraftarmorweapon.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.network.SkillSelectionPacket;
import minecraftarmorweapon.skill.PlayerSkillData;
import minecraftarmorweapon.skill.PlayerSkillData.AttackSlot;
import minecraftarmorweapon.skill.PlayerSkillData.WeaponProficiency;
import minecraftarmorweapon.skill.SkillRegistry;
import minecraftarmorweapon.skill.SkillRegistry.MotionInfo;
import minecraftarmorweapon.skill.SkillRegistry.MotionCategory;
import minecraftarmorweapon.world.inventory.SkillSelectionMenu;

import java.util.List;

public class SkillSelectionScreen extends AbstractContainerScreen<SkillSelectionMenu> {

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
    private static final int COLOR_SLOT_BG = 0xFF1A1A30;
    private static final int COLOR_SLOT_BORDER = 0xFF333355;
    private static final int COLOR_SLOT_SELECTED_BORDER = 0xFF4CAF50;
    private static final int COLOR_RADIO_ON = 0xFF4CAF50;
    private static final int COLOR_RADIO_OFF = 0xFF555577;

    private String hoveredDescription = null;

    // -1 = デフォルト設定, 0以上 = 武器スロットインデックス
    private int selectedLoadoutIndex = -1;

    // レイアウト定数
    private static final int HEADER_HEIGHT = 24;
    private static final int WEAPON_SLOT_SECTION_Y = 28;
    private static final int RADIO_ROW_Y = 50;
    private static final int DIVIDER_Y = 64;
    private static final int MOTION_ROW_Y = 72;

    public SkillSelectionScreen(SkillSelectionMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 340;
        this.imageHeight = 312;
        // デフォルトのラベル描画を無効化
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();

        // メインハンドの武器に一致するスロットを自動選択
        autoSelectHeldWeapon();

        buildWidgets();
    }

    /**
     * プレイヤーのメインハンドの武器と一致するスロットがあれば自動選択する。
     * これにより、ユーザーが手動でスロットを選ばなくてもすぐにロードアウトを編集できる。
     */
    private void autoSelectHeldWeapon() {
        if (minecraft == null || minecraft.player == null) return;
        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (mainHand.isEmpty()) return;

        String heldClass = mainHand.getItem().getClass().getSimpleName();
        for (int i = 0; i < SkillSelectionMenu.WEAPON_SLOTS; i++) {
            ItemStack slotItem = menu.getSlot(i).getItem();
            if (!slotItem.isEmpty() && slotItem.getItem().getClass().getSimpleName().equals(heldClass)) {
                selectedLoadoutIndex = i;
                return;
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // スロット内容が変わった場合にウィジェットを再構築
        rebuildIfNeeded();
    }

    private String[] lastWeaponClasses = new String[SkillSelectionMenu.WEAPON_SLOTS];

    private void rebuildIfNeeded() {
        boolean changed = false;
        for (int i = 0; i < SkillSelectionMenu.WEAPON_SLOTS; i++) {
            ItemStack item = menu.getSlot(i).getItem();
            String cls = item.isEmpty() ? "" : item.getItem().getClass().getSimpleName();
            if (!cls.equals(lastWeaponClasses[i])) {
                lastWeaponClasses[i] = cls;
                changed = true;
            }
        }
        if (changed) {
            // 選択中のスロットが空になったらデフォルトに戻す
            if (selectedLoadoutIndex >= 0) {
                ItemStack sel = menu.getSlot(selectedLoadoutIndex).getItem();
                if (sel.isEmpty()) {
                    selectedLoadoutIndex = -1;
                }
            }
            // まだデフォルトが選択されている場合、持っている武器の自動選択を試みる
            if (selectedLoadoutIndex == -1) {
                autoSelectHeldWeapon();
            }
            buildWidgets();
        }
    }

    private void buildWidgets() {
        this.clearWidgets();
        buildRadioButtons();
        buildMotionRows();
        buildProficiencyButton();

        // lastWeaponClasses を現在の状態で初期化
        for (int i = 0; i < SkillSelectionMenu.WEAPON_SLOTS; i++) {
            ItemStack item = menu.getSlot(i).getItem();
            lastWeaponClasses[i] = item.isEmpty() ? "" : item.getItem().getClass().getSimpleName();
        }
    }

    /**
     * 武器スロットの下にラジオボタンを配置（どのロードアウトを編集するか選ぶ）
     */
    private void buildRadioButtons() {
        int startX = leftPos + SkillSelectionMenu.WEAPON_SLOT_START_X;
        int y = topPos + RADIO_ROW_Y;
        int gap = SkillSelectionMenu.WEAPON_SLOT_GAP;

        // デフォルトボタン（左端）
        int defaultBtnX = leftPos + 4;
        addRenderableWidget(new AbstractButton(defaultBtnX, y, 50, 12,
                Component.literal("\u30C7\u30D5\u30A9\u30EB\u30C8")) {
            @Override
            public void onPress() {
                selectedLoadoutIndex = -1;
                buildWidgets();
            }

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                boolean selected = selectedLoadoutIndex == -1;
                boolean hover = this.isHoveredOrFocused();
                int bg = selected ? COLOR_BTN_SELECTED : (hover ? COLOR_BTN_HOVER : COLOR_BTN_NORMAL);
                g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
                if (selected) {
                    drawBorder(g, this.getX(), this.getY(), this.width, this.height, COLOR_BORDER);
                }
                g.drawCenteredString(font, this.getMessage(),
                    this.getX() + this.width / 2, this.getY() + 2,
                    selected ? 0x7FFF7F : 0xCCCCCC);
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput n) {
                this.defaultButtonNarrationText(n);
            }
        });

        // 各武器スロット用の選択ボタン
        for (int i = 0; i < SkillSelectionMenu.WEAPON_SLOTS; i++) {
            final int slotIdx = i;
            int btnX = startX + i * gap + 1;

            addRenderableWidget(new AbstractButton(btnX, y, 16, 12,
                    Component.literal("")) {
                @Override
                public void onPress() {
                    ItemStack weapon = menu.getSlot(slotIdx).getItem();
                    if (!weapon.isEmpty()) {
                        selectedLoadoutIndex = (selectedLoadoutIndex == slotIdx) ? -1 : slotIdx;
                        buildWidgets();
                    }
                }

                @Override
                public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    ItemStack weapon = menu.getSlot(slotIdx).getItem();
                    boolean hasWeapon = !weapon.isEmpty();
                    boolean selected = selectedLoadoutIndex == slotIdx;
                    boolean hover = this.isHoveredOrFocused() && hasWeapon;

                    // ラジオボタン風の丸（塗りつぶし四角で代用）
                    int cx = this.getX() + this.width / 2;
                    int cy = this.getY() + this.height / 2;
                    int size = 4;

                    if (hasWeapon) {
                        int color = selected ? COLOR_RADIO_ON : (hover ? COLOR_BTN_HOVER : COLOR_RADIO_OFF);
                        g.fill(cx - size, cy - size, cx + size, cy + size, color);
                        if (selected) {
                            // 内側を明るく
                            g.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFFFFFFFF);
                        }
                    } else {
                        // 空スロット：薄い四角
                        g.fill(cx - size, cy - size, cx + size, cy + size, 0xFF333344);
                    }

                    if (hover && hasWeapon) {
                        hoveredDescription = weapon.getHoverName().getString() + " \u306E\u6280\u8A2D\u5B9A";
                    }
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput n) {
                    this.defaultButtonNarrationText(n);
                }
            });
        }
    }

    /**
     * モーション選択ボタンを構築
     */
    /**
     * 得意武器タイプ選択ボタン
     */
    private void buildProficiencyButton() {
        if (minecraft == null || minecraft.player == null) return;

        PlayerSkillData.SkillStorage skillData = PlayerSkillData.getSkillData(minecraft.player);
        if (skillData == null) return;

        WeaponProficiency current = skillData.getWeaponProficiency();
        int btnX = leftPos + imageWidth - 110;
        int btnY = topPos + imageHeight - 18;

        addRenderableWidget(new AbstractButton(btnX, btnY, 105, 14,
                Component.literal("\u5F97\u610F: " + current.getDisplayName())) {
            @Override
            public void onPress() {
                WeaponProficiency next = current.next();
                skillData.setWeaponProficiency(next);
                // サーバーに同期
                MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                        SkillSelectionPacket.setProficiency(next.getId()));
                buildWidgets();
            }

            @Override
            public void updateWidgetNarration(NarrationElementOutput output) {}
        });
    }

    private void buildMotionRows() {
        String weaponClass = getSelectedWeaponClass();

        int rowY = topPos + MOTION_ROW_Y;
        int labelWidth = 52;
        int btnHeight = 18;
        int btnGap = 2;
        int rowGap = 5;
        int btnAreaLeft = leftPos + 4 + labelWidth;

        for (AttackSlot slot : AttackSlot.values()) {
            List<MotionInfo> motions = SkillRegistry.getAvailableMotions(slot, weaponClass);

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
                            menu.setDefaultMotion(finalSlot, finalMotion.getId());
                        } else {
                            menu.setLoadoutMotion(selectedLoadoutIndex, finalSlot, finalMotion.getId());
                        }
                        MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                            SkillSelectionPacket.selectLoadoutMotion(selectedLoadoutIndex, finalSlot, finalMotion.getId())
                        );
                        buildWidgets();
                    }

                    @Override
                    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float pt) {
                        String currentMotion = getCurrentMotion(finalSlot);

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

    private String getSelectedWeaponClass() {
        // 武器スロット選択中はそのスロットの武器クラスを返す
        if (selectedLoadoutIndex >= 0 && selectedLoadoutIndex < SkillSelectionMenu.WEAPON_SLOTS) {
            ItemStack weapon = menu.getSlot(selectedLoadoutIndex).getItem();
            if (!weapon.isEmpty()) return weapon.getItem().getClass().getSimpleName();
        }
        // デフォルト選択時はメインハンドの武器クラスを返す（特殊技を表示するため）
        if (minecraft != null && minecraft.player != null) {
            ItemStack mainHand = minecraft.player.getMainHandItem();
            if (!mainHand.isEmpty()) return mainHand.getItem().getClass().getSimpleName();
        }
        return null;
    }

    private String getCurrentMotion(AttackSlot slot) {
        if (selectedLoadoutIndex == -1) {
            return menu.getDefaultMotion(slot);
        } else {
            return menu.getLoadoutMotion(selectedLoadoutIndex, slot);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // メインの背景パネル
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, COLOR_BG);
        drawBorder(g, leftPos, topPos, imageWidth, imageHeight, 0xFF555577);

        // ヘッダー
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + HEADER_HEIGHT, COLOR_HEADER);
        g.drawCenteredString(font, Component.literal("\u2694 \u6280\u306E\u9078\u629E \u2694"),
            leftPos + imageWidth / 2, topPos + 8, COLOR_HEADER_TEXT);

        // 武器スロットセクションラベル
        g.drawString(font, Component.literal("\u25BC \u6B66\u5668\u30B9\u30ED\u30C3\u30C8"),
            leftPos + 4, topPos + 18, COLOR_SLOT_LABEL, false);

        // 武器スロットの背景と枠線
        renderWeaponSlotBackgrounds(g);

        // 区切り線
        int divY = topPos + DIVIDER_Y;
        g.fill(leftPos + 4, divY, leftPos + imageWidth - 4, divY + 1, 0xFF444466);

        // 選択中ロードアウトの表示
        String editingLabel;
        if (selectedLoadoutIndex == -1) {
            editingLabel = "[ \u30C7\u30D5\u30A9\u30EB\u30C8\u8A2D\u5B9A ]";
        } else {
            ItemStack weapon = menu.getSlot(selectedLoadoutIndex).getItem();
            if (!weapon.isEmpty()) {
                editingLabel = "[ " + weapon.getHoverName().getString() + " ]";
            } else {
                editingLabel = "[ \u30C7\u30D5\u30A9\u30EB\u30C8\u8A2D\u5B9A ]";
            }
        }
        g.drawCenteredString(font, Component.literal(editingLabel),
            leftPos + imageWidth / 2, divY + 2, 0xFFAACCFF);

        // スロットラベル（5行：一撃目、二撃目、...）
        int rowY = topPos + MOTION_ROW_Y;
        int btnHeight = 18;
        int rowGap = 5;
        for (AttackSlot slot : AttackSlot.values()) {
            g.drawString(font, Component.literal(slot.getDisplayName()),
                leftPos + 4, rowY + (btnHeight - 8) / 2, COLOR_SLOT_LABEL, false);
            rowY += btnHeight + rowGap;
        }

        // プレイヤーインベントリ背景
        int invY = topPos + SkillSelectionMenu.INV_START_Y - 12;
        g.drawString(font, Component.literal("\u30A4\u30F3\u30D9\u30F3\u30C8\u30EA"),
            leftPos + SkillSelectionMenu.INV_START_X, invY, 0xFF999999, false);
    }

    private void renderWeaponSlotBackgrounds(GuiGraphics g) {
        for (int i = 0; i < SkillSelectionMenu.WEAPON_SLOTS; i++) {
            int slotX = leftPos + SkillSelectionMenu.WEAPON_SLOT_START_X + i * SkillSelectionMenu.WEAPON_SLOT_GAP;
            int slotY = topPos + WEAPON_SLOT_SECTION_Y;

            // 18x18 スロット背景
            g.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, COLOR_SLOT_BG);

            // 選択中のスロットは緑枠、それ以外は薄い枠
            int borderColor = (i == selectedLoadoutIndex) ? COLOR_SLOT_SELECTED_BORDER : COLOR_SLOT_BORDER;
            drawBorder(g, slotX - 1, slotY - 1, 18, 18, borderColor);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hoveredDescription = null;

        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        // フッター
        int footerY = topPos + imageHeight - 26;
        g.fill(leftPos, footerY, leftPos + imageWidth, topPos + imageHeight, COLOR_FOOTER);

        if (hoveredDescription != null) {
            g.drawCenteredString(font, Component.literal(hoveredDescription),
                leftPos + imageWidth / 2, footerY + 4, 0xFFFFFF);
            g.drawCenteredString(font, Component.literal("\u30AF\u30EA\u30C3\u30AF\u3067\u9078\u629E"),
                leftPos + imageWidth / 2, footerY + 14, COLOR_FOOTER_TEXT);
        } else {
            g.drawCenteredString(font, Component.literal(
                "\u6B66\u5668\u3092D&D\u3067\u30B9\u30ED\u30C3\u30C8\u306B | K\u30AD\u30FC\u3067\u9589\u3058\u308B"),
                leftPos + imageWidth / 2, footerY + 9, COLOR_FOOTER_TEXT);
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
