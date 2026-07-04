package the_four_primitives_and_weapons.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.network.SkillSelectionPacket;
import the_four_primitives_and_weapons.skill.PlayerSkillData;
import the_four_primitives_and_weapons.skill.PlayerSkillData.AttackSlot;
import the_four_primitives_and_weapons.skill.PlayerSkillData.WeaponProficiency;
import the_four_primitives_and_weapons.skill.SkillRegistry;
import the_four_primitives_and_weapons.skill.SkillRegistry.MotionInfo;
import the_four_primitives_and_weapons.skill.SkillRegistry.MotionCategory;
import the_four_primitives_and_weapons.skill.WeaponTypeRegistry;
import the_four_primitives_and_weapons.world.inventory.SkillSelectionMenu;

import java.util.Collection;
import java.util.List;

public class SkillSelectionScreen extends AbstractContainerScreen<SkillSelectionMenu> {

    // 色定義 (RPG 風: 茶系を基調にしたファンタジー的トーン)
    private static final int COLOR_BG = 0xF22A1F12;            // 深い茶 (羊皮紙の裏)
    private static final int COLOR_OUTER_BORDER = 0xFF8B6F3C;  // ゴールド系の外枠
    private static final int COLOR_INNER_BORDER = 0xFF4A3820;  // 内枠 (影)
    private static final int COLOR_HEADER = 0xFF5C3A1A;
    private static final int COLOR_HEADER_TEXT = 0xFFE8C77A;   // 金色
    private static final int COLOR_BTN_NORMAL = 0xFF3D2E1C;    // 革のパネル風
    private static final int COLOR_BTN_SELECTED = 0xFF6B4A1F;
    private static final int COLOR_BTN_HOVER = 0xFF55401D;
    private static final int COLOR_BTN_SPECIAL = 0xFF4A2B40;
    private static final int COLOR_BORDER = 0xFFC9A24B;        // 金枠
    private static final int COLOR_SLOT_LABEL = 0xFFE8C77A;
    private static final int COLOR_FOOTER = 0xFF5C3A1A;
    private static final int COLOR_FOOTER_TEXT = 0xFFD8C490;
    private static final int COLOR_SLOT_BG = 0xFF2A1F12;
    private static final int COLOR_SLOT_BORDER = 0xFF4A3820;
    private static final int COLOR_SLOT_SELECTED_BORDER = 0xFFC9A24B;
    private static final int COLOR_RADIO_ON = 0xFFC9A24B;
    private static final int COLOR_RADIO_OFF = 0xFF6B5530;

    private String hoveredDescription = null;

    // -1 = デフォルト設定, 0以上 = 武器スロットインデックス
    private int selectedLoadoutIndex = -1;
    // null = デフォルト/武器スロット選択中, non-null = タイプ選択中
    private String selectedTypeId = null;
    // タイプモード用のクライアントローカル選択状態（サーバー同期とは別にUI表示用）
    private final java.util.Map<String, String> localTypeSelections = new java.util.HashMap<>();

    // タイプ列のスクロール ( 種類が多い時に横の欄をスクロールして全部選べる )
    private static final int TYPE_ROW_H = 11;
    private static final int TYPE_COL_W = 52;
    private int typeScrollOffset = 0;
    // スクロール当たり判定・▲▼表示・proficiency配置用に buildTypeButtons で保存
    private int typeColX, typeColW, typeColTop, typeRowH, typeMaxVisible, typeTotal;

    // レイアウト定数
    private static final int HEADER_HEIGHT = 14;
    // 実際のスロット (WeaponSlot) は y=22 に配置されるので、視覚的背景も同じ位置に揃える
    private static final int WEAPON_SLOT_SECTION_Y = 22;
    private static final int RADIO_ROW_Y = 42;
    private static final int DIVIDER_Y = 54;
    // editingLabel ("[ 武器名 ]") と最初のモーション行が縦に被らないよう余白を確保
    private static final int MOTION_ROW_Y = 68;

    // モーション行のメトリクス（ラベル描画とボタン構築で共通化、ズレ防止）
    private static final int MOTION_BTN_HEIGHT = 10;
    private static final int MOTION_ROW_GAP = 1;

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
                        hoveredDescription = Component.translatable(
                            "screen.the_four_primitives_and_weapons.skill_selection.weapon_skill_settings",
                            weapon.getHoverName().getString()).getString();
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
    /**
     * タイプ列の行ピッチ。 タイプ数が多くても ウィンドウ高さに収まるよう動的に詰める ( 溢れ防止 )。
     * addon で種類が増えても全部表示されるようにする。
     */
    /** タイプ列に表示できる最大行数 ( ウィンドウ高さから算出 )。 */
    private int typeVisibleRows() {
        int top = topPos + RADIO_ROW_Y;
        int bottom = Math.min(topPos + imageHeight, this.height) - 24; // proficiencyボタン分の余白
        return Math.max(1, (bottom - top) / TYPE_ROW_H);
    }

    private void buildTypeButtons() {
        java.util.List<WeaponTypeRegistry.WeaponTypeData> types =
                new java.util.ArrayList<>(WeaponTypeRegistry.getAllTypes());
        if (types.isEmpty()) return;

        final float TYPE_SCALE = 0.7f;
        int bx = leftPos + imageWidth - 4 - TYPE_COL_W;
        int top = topPos + RADIO_ROW_Y;
        int maxVisible = typeVisibleRows();
        int total = types.size();
        int maxOffset = Math.max(0, total - maxVisible);
        if (typeScrollOffset > maxOffset) typeScrollOffset = maxOffset;
        if (typeScrollOffset < 0) typeScrollOffset = 0;
        // スクロール当たり判定 / ▲▼ / proficiency 用に保存
        typeColX = bx; typeColW = TYPE_COL_W; typeColTop = top; typeRowH = TYPE_ROW_H;
        typeMaxVisible = maxVisible; typeTotal = total;

        int end = Math.min(total, typeScrollOffset + maxVisible);
        int btnY = top;
        // スクロール窓内のタイプだけボタン化 ( 種類が多くても収まる )
        for (int idx = typeScrollOffset; idx < end; idx++) {
            WeaponTypeRegistry.WeaponTypeData type = types.get(idx);
            final String typeId = type.getId();
            Component labelComp = Component.translatableWithFallback(type.translationKey(), type.getDisplayName());

            addRenderableWidget(new AbstractButton(bx, btnY, TYPE_COL_W, 9, labelComp) {
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
                    int padding = 4;
                    int rawWidth = font.width(getMessage());
                    float fitScale = TYPE_SCALE;
                    if (rawWidth * TYPE_SCALE > width - padding) {
                        fitScale = (float)(width - padding) / Math.max(1, rawWidth);
                        if (fitScale < 0.45f) fitScale = 0.45f;
                    }
                    drawScaledCenteredString(g, getMessage(),
                        getX() + width / 2, getY() + height / 2, 0xFFFFFF, fitScale);
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput n) {
                    this.defaultButtonNarrationText(n);
                }
            });
            btnY += TYPE_ROW_H;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        // タイプ列の上でホイール → スクロールして隠れているタイプを表示・選択できる
        if (typeTotal > typeMaxVisible
                && mx >= typeColX && mx <= typeColX + typeColW
                && my >= typeColTop && my <= typeColTop + typeMaxVisible * typeRowH) {
            int maxOffset = Math.max(0, typeTotal - typeMaxVisible);
            int old = typeScrollOffset;
            typeScrollOffset -= (int) Math.signum(delta);   // 上スクロールで前のタイプへ
            typeScrollOffset = Math.max(0, Math.min(maxOffset, typeScrollOffset));
            if (typeScrollOffset != old) { buildWidgets(); return true; }
        }
        return super.mouseScrolled(mx, my, delta);
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
        // 右側の type 列の下に配置 (Trident の下の余ったスペース)
        int btnW = 80;
        int btnH = 11;
        int btnX = leftPos + imageWidth - btnW - 4;
        // type スクロール窓の下に配置 ( 種類が増えて窓が満杯でも被らない )。
        int visible = Math.min(typeVisibleRows(), WeaponTypeRegistry.getAllTypes().size());
        int btnY = topPos + RADIO_ROW_Y + visible * TYPE_ROW_H + 6;

        Component currentName = Component.translatable(current.translationKey());
        addRenderableWidget(new AbstractButton(btnX, btnY, btnW, btnH,
                Component.translatable("screen.the_four_primitives_and_weapons.skill_selection.proficiency", currentName)) {
            @Override
            public void onPress() {
                WeaponProficiency next = current.next();
                skillData.setWeaponProficiency(next);
                // サーバーに同期
                TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(
                        SkillSelectionPacket.setProficiency(next.getId()));
                buildWidgets();
            }

            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                int bg = isHoveredOrFocused() ? COLOR_BTN_HOVER : COLOR_BTN_SPECIAL;
                g.fill(getX(), getY(), getX() + width, getY() + height, bg);
                drawBorder(g, getX(), getY(), width, height, COLOR_BORDER);
                // 文字幅がボタンに収まらない時は自動縮小
                int padding = 4;
                int rawWidth = font.width(getMessage());
                float fitScale = 0.7f;
                if (rawWidth * fitScale > width - padding) {
                    fitScale = (float)(width - padding) / Math.max(1, rawWidth);
                    if (fitScale < 0.45f) fitScale = 0.45f;
                }
                drawScaledCenteredString(g, getMessage(),
                    getX() + width / 2, getY() + height / 2, COLOR_SLOT_LABEL, fitScale);
                if (isHoveredOrFocused()) {
                    hoveredDescription = Component.translatable(
                        "screen.the_four_primitives_and_weapons.skill_selection.proficiency_hover").getString();
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
        int btnHeight = MOTION_BTN_HEIGHT;
        int btnGap = 1;
        int rowGap = MOTION_ROW_GAP;
        int btnAreaLeft = leftPos + 3 + labelWidth;
        // モーションボタン領域の右端 (タイプ列ボタンと衝突しないように左に余白を確保)
        int motionAreaRight = leftPos + imageWidth - 60;
        final float TEXT_SCALE = 0.7f;

        for (AttackSlot slot : AttackSlot.values()) {
            List<MotionInfo> motions;
            if (selectedTypeId != null) {
                // タイプ選択中: JSONのタイプ定義から取得
                motions = getMotionsForType(selectedTypeId, slot);
            } else {
                motions = SkillRegistry.getAvailableMotionsForWeapon(slot, selectedWeapon);
            }

            int btnX = btnAreaLeft;
            // 1 行に収まるようボタン幅を均等配分 (折り返しなし、インベントリとの被り防止)
            int availWidth = motionAreaRight - btnAreaLeft;
            int totalBtnGap = btnGap * Math.max(0, motions.size() - 1);
            int evenBtnWidth = motions.isEmpty() ? 0 : Math.max(18, (availWidth - totalBtnGap) / motions.size());
            for (MotionInfo motion : motions) {
                Component motionLabel = Component.translatableWithFallback(motion.translationKey(), motion.getDisplayName());
                int btnWidth = evenBtnWidth;
                final AttackSlot finalSlot = slot;
                final MotionInfo finalMotion = motion;

                addRenderableWidget(new AbstractButton(btnX, rowY, btnWidth, btnHeight, motionLabel) {
                    @Override
                    public void onPress() {
                        // Shift+クリック → 該当 motion の ON/OFF をトグル ( 選択は変えない )
                        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                            boolean currentlyEnabled = minecraft != null && minecraft.player != null
                                    ? PlayerSkillData.isMotionEnabled(minecraft.player, finalMotion.getId())
                                    : true;
                            boolean nextEnabled = !currentlyEnabled;
                            TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(
                                SkillSelectionPacket.toggleMotion(finalMotion.getId(), nextEnabled)
                            );
                            // クライアント側でも即時反映 ( renderWidget が即座に変化を表示 )
                            if (minecraft != null && minecraft.player != null) {
                                PlayerSkillData.setMotionEnabled(minecraft.player, finalMotion.getId(), nextEnabled);
                            }
                            return;
                        }

                        if (selectedTypeId != null) {
                            // タイプ別設定: サーバーに送信 + ローカル状態 + クライアントcapability更新
                            TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(
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
                            TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(
                                SkillSelectionPacket.selectLoadoutMotion(selectedLoadoutIndex, finalSlot, finalMotion.getId())
                            );
                            // クライアント側capabilityも更新
                            if (minecraft != null && minecraft.player != null) {
                                PlayerSkillData.SkillStorage sd = PlayerSkillData.getSkillData(minecraft.player);
                                if (sd != null) sd.setMotion(finalSlot, finalMotion.getId());
                            }
                        } else {
                            menu.setLoadoutMotion(selectedLoadoutIndex, finalSlot, finalMotion.getId());
                            TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(
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
                        boolean isDisabled = minecraft != null && minecraft.player != null
                                && !PlayerSkillData.isMotionEnabled(minecraft.player, finalMotion.getId());

                        int bgColor;
                        if (isSelected) bgColor = COLOR_BTN_SELECTED;
                        else if (isHover) bgColor = COLOR_BTN_HOVER;
                        else bgColor = isSpecial ? COLOR_BTN_SPECIAL : COLOR_BTN_NORMAL;

                        g.fill(this.getX(), this.getY(),
                            this.getX() + this.width, this.getY() + this.height, bgColor);

                        if (isSelected) {
                            drawBorder(g, this.getX(), this.getY(), this.width, this.height, COLOR_BORDER);
                        }

                        // 無効化中はボタン全体を半透明の赤で塗りつぶし + 右上に "OFF" マーカー
                        if (isDisabled) {
                            g.fill(this.getX(), this.getY(),
                                this.getX() + this.width, this.getY() + this.height,
                                0xB0660000);
                            g.fill(this.getX() + this.width - 11, this.getY() + 1,
                                this.getX() + this.width - 1, this.getY() + 7,
                                0xFFAA0000);
                        }

                        // ボタン幅に応じてスケール自動縮小 (ラベルが収まるよう)
                        int padding = 4;
                        int rawWidth = font.width(this.getMessage());
                        float fitScale = TEXT_SCALE;
                        if (rawWidth * TEXT_SCALE > this.width - padding) {
                            fitScale = (float)(this.width - padding) / Math.max(1, rawWidth);
                            // 読めなくならないよう下限を設ける
                            if (fitScale < 0.45f) fitScale = 0.45f;
                        }
                        drawScaledCenteredString(g, this.getMessage(),
                            this.getX() + this.width / 2, this.getY() + this.height / 2,
                            isSelected ? 0x7FFF7F : 0xFFFFFF, fitScale);

                        if (isHover) {
                            String desc = Component.translatableWithFallback(
                                finalMotion.descriptionTranslationKey(),
                                finalMotion.getDescription()).getString();
                            String hint = isDisabled
                                ? " §c[OFF — Shift+Clickで有効化]"
                                : " §7[Shift+Clickで無効化]";
                            hoveredDescription = desc + hint;
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
                String nbt = the_four_primitives_and_weapons.skill.WeaponSkillNBT.getMotion(weapon, slot);
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
        // メインの背景パネル (RPG 風: 茶背景 + 金内枠 + 暗外枠)
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, COLOR_BG);
        // 外枠 (暗い影)
        drawBorder(g, leftPos, topPos, imageWidth, imageHeight, COLOR_INNER_BORDER);
        // 内側ゴールド枠 (1 ピクセル内側)
        drawBorder(g, leftPos + 2, topPos + 2, imageWidth - 4, imageHeight - 4, COLOR_OUTER_BORDER);
        // ヘッダー帯
        g.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + 11, COLOR_HEADER);

        // ヘッダー（バーなし、テキストのみ）
        g.drawCenteredString(font, Component.translatable("screen.the_four_primitives_and_weapons.skill_selection.header"),
            leftPos + imageWidth / 2, topPos + 3, COLOR_HEADER_TEXT);

        // 武器スロットセクションラベル (上に持ち上げ)
        g.drawString(font, Component.translatable("screen.the_four_primitives_and_weapons.skill_selection.weapon_slots"),
            leftPos + 4, topPos + 13, COLOR_SLOT_LABEL, false);

        // 武器スロットの背景と枠線
        renderWeaponSlotBackgrounds(g);

        // 区切り線
        int divY = topPos + DIVIDER_Y;
        g.fill(leftPos + 4, divY, leftPos + imageWidth - 4, divY + 1, 0xFF444466);

        // 選択中ロードアウトの表示（武器名が長い場合は省略記号で切り詰める）
        String rawName;
        if (selectedTypeId != null) {
            WeaponTypeRegistry.WeaponTypeData typeData = WeaponTypeRegistry.getType(selectedTypeId);
            rawName = typeData != null
                ? Component.translatableWithFallback(typeData.translationKey(), typeData.getDisplayName()).getString()
                : selectedTypeId;
        } else if (selectedLoadoutIndex >= 0) {
            ItemStack weapon = menu.getSlot(selectedLoadoutIndex).getItem();
            rawName = !weapon.isEmpty() ? weapon.getHoverName().getString() : null;
        } else {
            rawName = null;
        }
        String editingLabel = "";
        if (rawName != null) {
            // 左右のタイプボタン列(右側)/左マージンを避けるため、中央寄せで両側に
            // それぞれ ~70px の余白を確保する。"[ ... ]" の囲み込み分も差し引く。
            int maxNameWidth = imageWidth - 2 * 70 - font.width("[  ]");
            if (font.width(rawName) > maxNameWidth) {
                String ellipsis = "…";
                rawName = font.plainSubstrByWidth(rawName, maxNameWidth - font.width(ellipsis)) + ellipsis;
            }
            editingLabel = "[ " + rawName + " ]";
        }
        g.drawCenteredString(font, Component.literal(editingLabel),
            leftPos + imageWidth / 2, divY + 2, 0xFFAACCFF);

        // スロットラベル（各行：一撃目、二撃目、...）
        // buildMotionRows と同じピッチ(MOTION_BTN_HEIGHT + MOTION_ROW_GAP)で並べる。
        // 以前は label=16px/btn=11px ピッチでズレて他のUIと被っていたバグを修正。
        int rowY = topPos + MOTION_ROW_Y;
        for (AttackSlot slot : AttackSlot.values()) {
            g.drawString(font, Component.translatable(slot.translationKey()),
                leftPos + 3, rowY + (MOTION_BTN_HEIGHT - 8) / 2, COLOR_SLOT_LABEL, false);
            rowY += MOTION_BTN_HEIGHT + MOTION_ROW_GAP;
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
        g.drawString(font, Component.translatable("container.inventory"),
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

        // タイプ列がスクロール可能なら ▲▼ を表示 ( 上下に隠れているタイプがある合図 )
        if (typeTotal > typeMaxVisible) {
            int cx = typeColX + typeColW / 2;
            if (typeScrollOffset > 0)
                g.drawCenteredString(font, "▲", cx, typeColTop - 8, 0xFFC9A24B);
            if (typeScrollOffset + typeMaxVisible < typeTotal)
                g.drawCenteredString(font, "▼", cx, typeColTop + typeMaxVisible * typeRowH, 0xFFC9A24B);
        }

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

    /** スケールを掛けたテキストを中央寄せで描画 (重なり防止用、文字を小さく)。 */
    private void drawScaledCenteredString(GuiGraphics g, Component text, int cx, int cy, int color, float scale) {
        com.mojang.blaze3d.vertex.PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 0);
        pose.scale(scale, scale, 1.0f);
        int width = font.width(text);
        g.drawString(font, text, -width / 2, -font.lineHeight / 2, color, false);
        pose.popPose();
    }

    /** スケールを掛けたテキストを左寄せで描画。 */
    private void drawScaledString(GuiGraphics g, Component text, int x, int y, int color, float scale) {
        com.mojang.blaze3d.vertex.PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1.0f);
        g.drawString(font, text, 0, 0, color, false);
        pose.popPose();
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
