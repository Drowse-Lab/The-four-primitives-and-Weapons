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
import minecraftarmorweapon.skill.WeaponTypeRegistry;
import minecraftarmorweapon.world.inventory.SkillSelectionMenu;

import java.util.Collection;
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
    // null = デフォルト/武器スロット選択中, non-null = タイプ選択中
    private String selectedTypeId = null;
    // タイプモード用のクライアントローカル選択状態（サーバー同期とは別にUI表示用）
    private final java.util.Map<String, String> localTypeSelections = new java.util.HashMap<>();

    // レイアウト定数
    private static final int HEADER_HEIGHT = 18;
    private static final int WEAPON_SLOT_SECTION_Y = 20;
    private static final int RADIO_ROW_Y = 40;
    private static final int DIVIDER_Y = 52;
    private static final int MOTION_ROW_Y = 58;

    public SkillSelectionScreen(SkillSelectionMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 400;
        this.imageHeight = 268;
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
        buildTypeButtons();
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

        // 各武器スロット用の選択ボタン
        for (int i = 0; i < SkillSelectionMenu.WEAPON_SLOTS; i++) {
            final int slotIdx = i;
            int btnX = startX + i * gap + 1;

            addRenderableWidget(new AbstractButton(btnX, y, 14, 10,
                    Component.literal("")) {
                @Override
                public void onPress() {
                    ItemStack weapon = menu.getSlot(slotIdx).getItem();
                    if (!weapon.isEmpty()) {
                        selectedLoadoutIndex = slotIdx;
                    selectedTypeId = null;
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
     * 武器タイプ選択ボタン（刀、剣、直刀…）
     */
    private void buildTypeButtons() {
        Collection<WeaponTypeRegistry.WeaponTypeData> types = WeaponTypeRegistry.getAllTypes();
        if (types.isEmpty()) return;

        int btnX = leftPos + imageWidth - 4;
        int btnY = topPos + RADIO_ROW_Y;

        // 右側に縦並びでタイプボタンを配置
        for (WeaponTypeRegistry.WeaponTypeData type : types) {
            final String typeId = type.getId();
            String label = type.getDisplayName();
            int btnW = Math.max(20, font.width(label) + 6);
            int bx = btnX - btnW;

            addRenderableWidget(new AbstractButton(bx, btnY, btnW, 10,
                    Component.literal(label)) {
                @Override
                public void onPress() {
                    selectedLoadoutIndex = -1;
                    selectedTypeId = typeId.equals(selectedTypeId) ? null : typeId;
                    buildWidgets();
                }

                @Override
                public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    boolean sel = typeId.equals(selectedTypeId);
                    int bg = sel ? COLOR_BTN_SELECTED : (isHoveredOrFocused() ? COLOR_BTN_HOVER : COLOR_BTN_NORMAL);
                    g.fill(getX(), getY(), getX() + width, getY() + height, bg);
                    if (sel) drawBorder(g, getX(), getY(), width, height, COLOR_BORDER);
                    g.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + 1, 0xFFFFFF);
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput n) {
                    this.defaultButtonNarrationText(n);
                }
            });
            btnY += 12;
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
        int btnW = 90;
        int btnH = 13;
        // タイトルと重ならないよう、パネル左下（インベントリ上部）に配置
        int btnX = leftPos + 3;
        int btnY = topPos + SkillSelectionMenu.INV_START_Y - 16;

        addRenderableWidget(new AbstractButton(btnX, btnY, btnW, btnH,
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
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                int bg = isHoveredOrFocused() ? COLOR_BTN_HOVER : COLOR_BTN_SPECIAL;
                g.fill(getX(), getY(), getX() + width, getY() + height, bg);
                drawBorder(g, getX(), getY(), width, height, COLOR_BORDER);
                g.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, COLOR_SLOT_LABEL);
                if (isHoveredOrFocused()) {
                    hoveredDescription = "\u30AF\u30EA\u30C3\u30AF\u3067\u5F97\u610F\u6B66\u5668\u30BF\u30A4\u30D7\u3092\u5207\u308A\u66FF\u3048";
                }
            }

            @Override
            public void updateWidgetNarration(NarrationElementOutput output) {}
        });
    }

    private void buildMotionRows() {
        // タイプ選択中はそのタイプのモーション、それ以外は武器スロット/デフォルト
        ItemStack selectedWeapon = getSelectedWeapon();

        int rowY = topPos + MOTION_ROW_Y;
        int labelWidth = 46;
        int btnHeight = 14;
        int btnGap = 1;
        int rowGap = 2;
        int btnAreaLeft = leftPos + 3 + labelWidth;

        for (AttackSlot slot : AttackSlot.values()) {
            List<MotionInfo> motions;
            if (selectedTypeId != null) {
                // タイプ選択中: JSONのタイプ定義から取得
                motions = getMotionsForType(selectedTypeId, slot);
            } else {
                motions = SkillRegistry.getAvailableMotionsForWeapon(slot, selectedWeapon);
            }

            int btnX = btnAreaLeft;
            for (MotionInfo motion : motions) {
                int btnWidth = Math.max(28, font.width(motion.getDisplayName()) + 6);
                final AttackSlot finalSlot = slot;
                final MotionInfo finalMotion = motion;

                addRenderableWidget(new AbstractButton(btnX, rowY, btnWidth, btnHeight,
                        Component.literal(motion.getDisplayName())) {
                    @Override
                    public void onPress() {
                        if (selectedTypeId != null) {
                            // タイプ別設定: サーバーに送信 + ローカル状態 + クライアントcapability更新
                            MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                                SkillSelectionPacket.setTypeMotion(selectedTypeId, finalSlot, finalMotion.getId())
                            );
                            localTypeSelections.put(selectedTypeId + ":" + finalSlot.getId(), finalMotion.getId());
                            // クライアント側capabilityも更新（performDodgeのチェック用）
                            if (minecraft != null && minecraft.player != null) {
                                PlayerSkillData.SkillStorage sd = PlayerSkillData.getSkillData(minecraft.player);
                                if (sd != null) sd.setTypeMotion(selectedTypeId, finalSlot, finalMotion.getId());
                            }
                        } else if (selectedLoadoutIndex == -1) {
                            // グローバルデフォルト設定
                            menu.setDefaultMotion(finalSlot, finalMotion.getId());
                            MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                                SkillSelectionPacket.selectLoadoutMotion(selectedLoadoutIndex, finalSlot, finalMotion.getId())
                            );
                            // クライアント側capabilityも更新
                            if (minecraft != null && minecraft.player != null) {
                                PlayerSkillData.SkillStorage sd = PlayerSkillData.getSkillData(minecraft.player);
                                if (sd != null) sd.setMotion(finalSlot, finalMotion.getId());
                            }
                        } else {
                            menu.setLoadoutMotion(selectedLoadoutIndex, finalSlot, finalMotion.getId());
                            MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                                SkillSelectionPacket.selectLoadoutMotion(selectedLoadoutIndex, finalSlot, finalMotion.getId())
                            );
                            // クライアント側capabilityのloadoutも更新（performDodge/Guardチェック用）
                            if (minecraft != null && minecraft.player != null) {
                                PlayerSkillData.SkillStorage sd = PlayerSkillData.getSkillData(minecraft.player);
                                if (sd != null) {
                                    sd.setLoadoutMotion(selectedLoadoutIndex, finalSlot, finalMotion.getId());
                                }
                            }
                        }
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

    private ItemStack getSelectedWeapon() {
        // 武器スロット選択中のみそのスロットの武器を返す
        if (selectedLoadoutIndex >= 0 && selectedLoadoutIndex < SkillSelectionMenu.WEAPON_SLOTS) {
            ItemStack weapon = menu.getSlot(selectedLoadoutIndex).getItem();
            if (!weapon.isEmpty()) return weapon;
        }
        return ItemStack.EMPTY;
    }

    private String getCurrentMotion(AttackSlot slot) {
        if (selectedTypeId != null) {
            // タイプ選択中: ローカル → サーバーデータ → グローバルデフォルト
            String key = selectedTypeId + ":" + slot.getId();
            String local = localTypeSelections.get(key);
            if (local != null) return local;
            // サーバーで保存済みのtypeMotionを参照
            if (minecraft != null && minecraft.player != null) {
                PlayerSkillData.SkillStorage sd = PlayerSkillData.getSkillData(minecraft.player);
                if (sd != null) {
                    String saved = sd.getTypeMotion(selectedTypeId, slot);
                    if (saved != null) return saved;
                }
            }
            return menu.getDefaultMotion(slot);
        } else if (selectedLoadoutIndex == -1) {
            return menu.getDefaultMotion(slot);
        } else {
            // 武器スロットモード: 武器NBTを最優先で参照
            ItemStack weapon = menu.getSlot(selectedLoadoutIndex).getItem();
            if (!weapon.isEmpty()) {
                String nbt = minecraftarmorweapon.skill.WeaponSkillNBT.getMotion(weapon, slot);
                if (nbt != null) return nbt;
            }
            return menu.getLoadoutMotion(selectedLoadoutIndex, slot);
        }
    }

    private List<MotionInfo> getMotionsForType(String typeId, AttackSlot slot) {
        java.util.List<MotionInfo> result = new java.util.ArrayList<>();
        WeaponTypeRegistry.WeaponTypeData typeData = WeaponTypeRegistry.getType(typeId);
        if (typeData != null) {
            for (String motionId : typeData.getMotionsForSlot(slot)) {
                MotionInfo info = SkillRegistry.getById(motionId);
                if (info != null) result.add(info);
            }
        }
        return result;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // メインの背景パネル
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, COLOR_BG);
        drawBorder(g, leftPos, topPos, imageWidth, imageHeight, 0xFF555577);

        // ヘッダー（バーなし、テキストのみ）
        g.drawCenteredString(font, Component.literal("\u2694 \u6280\u306E\u9078\u629E \u2694"),
            leftPos + imageWidth / 2, topPos + 3, COLOR_HEADER_TEXT);

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
        if (selectedTypeId != null) {
            WeaponTypeRegistry.WeaponTypeData typeData = WeaponTypeRegistry.getType(selectedTypeId);
            String typeName = typeData != null ? typeData.getDisplayName() : selectedTypeId;
            editingLabel = "[ " + typeName + " ]";
        } else if (selectedLoadoutIndex >= 0) {
            ItemStack weapon = menu.getSlot(selectedLoadoutIndex).getItem();
            editingLabel = !weapon.isEmpty() ? "[ " + weapon.getHoverName().getString() + " ]" : "";
        } else {
            editingLabel = "";
        }
        g.drawCenteredString(font, Component.literal(editingLabel),
            leftPos + imageWidth / 2, divY + 2, 0xFFAACCFF);

        // スロットラベル（各行：一撃目、二撃目、...）
        int rowY = topPos + MOTION_ROW_Y;
        int btnHeight = 14;
        int rowGap = 2;
        for (AttackSlot slot : AttackSlot.values()) {
            g.drawString(font, Component.literal(slot.getDisplayName()),
                leftPos + 3, rowY + (btnHeight - 8) / 2, COLOR_SLOT_LABEL, false);
            rowY += btnHeight + rowGap;
        }

        // プレイヤーインベントリ背景（バニラ風の不透明パネル）
        int invSlotX = leftPos + SkillSelectionMenu.INV_START_X;
        int invSlotY = topPos + SkillSelectionMenu.INV_START_Y;
        int hotbarY = topPos + SkillSelectionMenu.HOTBAR_Y;
        int invPanelX = invSlotX - 8;
        int invPanelY = invSlotY - 14;
        int invPanelW = 9 * 18 + 14;
        int invPanelH = hotbarY + 18 + 6 - invPanelY;

        // 不透明パネル背景
        g.fill(invPanelX, invPanelY, invPanelX + invPanelW, invPanelY + invPanelH, 0xFFC6C6C6);
        // 外枠（バニラ風: 上と左が白、下と右が暗い）
        g.fill(invPanelX, invPanelY, invPanelX + invPanelW, invPanelY + 2, 0xFFFFFFFF);
        g.fill(invPanelX, invPanelY, invPanelX + 2, invPanelY + invPanelH, 0xFFFFFFFF);
        g.fill(invPanelX, invPanelY + invPanelH - 2, invPanelX + invPanelW, invPanelY + invPanelH, 0xFF555555);
        g.fill(invPanelX + invPanelW - 2, invPanelY, invPanelX + invPanelW, invPanelY + invPanelH, 0xFF555555);

        // 各スロットの背景（暗い凹み）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = invSlotX + col * 18 - 1;
                int sy = invSlotY + row * 18 - 1;
                g.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
                g.fill(sx + 1, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
                g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFFC6C6C6);
            }
        }
        // ホットバーのスロット背景
        for (int col = 0; col < 9; col++) {
            int sx = invSlotX + col * 18 - 1;
            int sy = hotbarY - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
            g.fill(sx + 1, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFFC6C6C6);
        }

        // "インベントリ" ラベル
        g.drawString(font, Component.literal("Inventory"),
            invSlotX, invPanelY + 4, 0xFF404040, false);
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

        // ホバー説明文（フッターバーなし、テキストのみ）
        if (hoveredDescription != null) {
            int descY = topPos + SkillSelectionMenu.HOTBAR_Y + 24;
            g.drawCenteredString(font, Component.literal(hoveredDescription),
                leftPos + imageWidth / 2, descY, 0xFFCCCCCC);
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
