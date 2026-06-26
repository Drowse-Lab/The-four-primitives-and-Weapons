package the_four_primitives_and_weapons.client.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.item.MaterializedPouchItem;
import the_four_primitives_and_weapons.network.PouchSlotClickMessage;
import the_four_primitives_and_weapons.network.PouchToggleAirMessage;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * インベントリ画面に「かぶさるように」浮かぶ結晶ポーチのフローティングパネル ( バンドル風 )。
 *
 *   - インベントリでポーチにカーソルを合わせて B → パネルを開く
 *   - タイトルバーをドラッグで移動、 右上の ✕ で閉じる
 *   - 4 スロットをクリックでカーソルと出し入れ ( サーバーへ {@link PouchSlotClickMessage} )
 *
 * 別画面ではなくオーバーレイなので、 元のインベントリ ( 防具・クラフト・プレイヤーモデル ) が
 * 背後にそのまま見える。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, value = Dist.CLIENT)
public class PouchOverlay {

    // --- 状態 ---
    private static boolean open = false;
    private static int pouchSlot = -1;
    private static int panelX = 0, panelY = 0;
    private static boolean posInit = false;
    private static boolean dragging = false;
    private static double dragOffX, dragOffY;
    /** 自分のポーチツールチップ描画中は背後抑制をスキップするためのフラグ */
    private static boolean renderingOwn = false;

    // --- レイアウト ---
    private static final int PAD = 6;
    private static final int TITLE_H = 14;
    private static final int SLOT = 18;
    private static final int PANEL_W = PAD + MaterializedPouchItem.SLOTS * SLOT + PAD;     // 84
    private static final int PANEL_H = TITLE_H + 2 + 16 + PAD;                              // 38

    // --- カラー ( バンドル風 濃紫 ) ---
    private static final int PANEL_FILL  = 0xF2241A33;
    private static final int PANEL_LIGHT = 0xFF6A4A8A;
    private static final int PANEL_DARK  = 0xFF120A1A;
    private static final int TITLE_BAR   = 0xFF3A2A55;
    private static final int SLOT_CELL   = 0xFF15101F;
    private static final int SLOT_EDGE   = 0xFF4A3A6A;
    private static final int SLOT_HOVER  = 0x80FFFFFF;
    private static final int CLOSE_BG    = 0xFF7A2A3A;
    private static final int CLOSE_HOV   = 0xFFB03A4E;

    /** バニラ防具インベントリと同じ空きスロットアイコン ( 兜/胴/脚/靴 ) */
    private static final ResourceLocation[] ARMOR_ICONS = {
        InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
        InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
        InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
        InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS
    };

    private static int slotX(int i) { return panelX + PAD + i * SLOT; }
    private static int slotY() { return panelY + TITLE_H + 2; }

    private static boolean inClose(double mx, double my) {
        int cx = panelX + PANEL_W - 12, cy = panelY + 2;
        return mx >= cx && mx <= cx + 10 && my >= cy && my <= cy + 10;
    }
    private static boolean inTitle(double mx, double my) {
        return mx >= panelX && mx <= panelX + PANEL_W && my >= panelY && my <= panelY + TITLE_H && !inClose(mx, my);
    }
    private static boolean inPanel(double mx, double my) {
        return mx >= panelX && mx <= panelX + PANEL_W && my >= panelY && my <= panelY + PANEL_H;
    }
    private static int slotAt(double mx, double my) {
        int sy = slotY();
        if (my < sy || my > sy + 16) return -1;
        for (int i = 0; i < MaterializedPouchItem.SLOTS; i++) {
            int sx = slotX(i);
            if (mx >= sx && mx <= sx + 16) return i;
        }
        return -1;
    }

    /** 現在対象のポーチ ( 無効なら EMPTY ) */
    private static ItemStack pouch(Player player) {
        if (pouchSlot >= 0 && pouchSlot < player.getInventory().getContainerSize()) {
            ItemStack s = player.getInventory().getItem(pouchSlot);
            if (s.getItem() instanceof MaterializedPouchItem) return s;
        }
        return MaterializedPouchItem.findFirst(player);
    }

    private static void close() {
        open = false;
        dragging = false;
        posInit = false;
    }

    // --- B: ポーチにカーソルを合わせて開く / 閉じる ---
    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> acs)) return;
        if (!CrystalPouchKeybind.OPEN_POUCH.matches(event.getKeyCode(), event.getScanCode())) return;
        if (open) { close(); event.setCanceled(true); return; }

        Slot hovered = acs.getSlotUnderMouse();
        boolean isPouch = hovered != null && hovered.hasItem()
            && hovered.getItem().getItem() instanceof MaterializedPouchItem;
        if (!isPouch) {
            // 案内 ( = イベントが発火している確認も兼ねる )
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal("§7結晶ポーチにカーソルを合わせて B"), true);
            }
            return;
        }
        open = true;
        // スロット→インベントリ番号のマッピングは画面種別で不確実なので findFirst に委ねる
        pouchSlot = -1;
        posInit = false;
        event.setCanceled(true);
    }

    // --- 描画 ---
    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (!open) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack pouch = pouch(mc.player);
        if (pouch.isEmpty()) { close(); return; }

        GuiGraphics g = event.getGuiGraphics();
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();

        // 初期位置 ( 元インベントリのメイングリッド上あたり ) / ドラッグ追従
        if (!posInit) {
            AbstractContainerScreen<?> acs = (AbstractContainerScreen<?>) event.getScreen();
            panelX = acs.getGuiLeft() + 46;
            panelY = acs.getGuiTop() + 84;
            posInit = true;
        }
        if (dragging) {
            panelX = (int) (mouseX - dragOffX);
            panelY = (int) (mouseY - dragOffY);
        }

        ItemStack[] lo = MaterializedPouchItem.getLoadout(pouch);
        int hovered = slotAt(mouseX, mouseY);

        // インベントリのアイテム ( z≈150 ) より前面に描く ( でないとアイテムがパネルを突き抜ける )
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);

        // パネル本体
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, PANEL_FILL);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 1, PANEL_LIGHT);
        g.fill(panelX, panelY, panelX + 1, panelY + PANEL_H, PANEL_LIGHT);
        g.fill(panelX, panelY + PANEL_H - 1, panelX + PANEL_W, panelY + PANEL_H, PANEL_DARK);
        g.fill(panelX + PANEL_W - 1, panelY, panelX + PANEL_W, panelY + PANEL_H, PANEL_DARK);
        g.fill(panelX + 1, panelY + 1, panelX + PANEL_W - 1, panelY + TITLE_H, TITLE_BAR);
        g.drawString(mc.font, "結晶ポーチ", panelX + 4, panelY + 3, 0xFFFFFFFF, false);

        // ✕ ボタン
        int cx = panelX + PANEL_W - 12, cy = panelY + 2;
        g.fill(cx, cy, cx + 10, cy + 10, inClose(mouseX, mouseY) ? CLOSE_HOV : CLOSE_BG);
        g.drawCenteredString(mc.font, "x", cx + 5, cy + 1, 0xFFFFFFFF);

        // スロット + アイテム
        for (int i = 0; i < MaterializedPouchItem.SLOTS; i++) {
            int sx = slotX(i), sy = slotY();
            boolean airOn = i < MaterializedPouchItem.SLOTS && MaterializedPouchItem.getReplaceAir(pouch, i);
            // 空気置換 ON は枠も中身も白く ( = 結晶化時に着用防具を空気に置換 )
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, airOn ? 0xFFFFFFFF : SLOT_EDGE);
            g.fill(sx, sy, sx + 16, sy + 16, airOn ? 0xFFE8E8E8 : SLOT_CELL);
            ItemStack s = lo[i];
            if (!s.isEmpty()) {
                g.renderItem(s, sx, sy);
                g.renderItemDecorations(mc.font, s, sx, sy);
            } else if (i < ARMOR_ICONS.length) {
                // 空きスロットはバニラ防具インベントリと同じアイコンを表示 ( 何用か分かりやすく )
                TextureAtlasSprite sprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ARMOR_ICONS[i]);
                g.blit(sx, sy, 0, 16, 16, sprite);
            }
            if (hovered == i) {
                g.fill(sx, sy, sx + 16, sy + 16, SLOT_HOVER);
            }
        }

        g.pose().popPose();

        // ホバー中スロットのツールチップ ( renderTooltip 自身が最前面 z を扱う )
        if (hovered >= 0 && !lo[hovered].isEmpty()) {
            renderingOwn = true;
            g.renderTooltip(mc.font, lo[hovered], mouseX, mouseY);
            renderingOwn = false;
        }
    }

    /**
     * パネルの上にカーソルがある間は、 背後のインベントリアイテムのツールチップを抑制する
     * ( 自分のポーチツールチップ {@link #renderingOwn} は通す )。
     */
    @SubscribeEvent
    public static void onTooltipPre(RenderTooltipEvent.Pre event) {
        if (!open || renderingOwn) return;
        if (inPanel(event.getX(), event.getY())) {
            event.setCanceled(true);
        }
    }

    // --- クリック ( 閉じる / ドラッグ開始 / スロット操作 ) ---
    @SubscribeEvent
    public static void onMousePress(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!open) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen)) return;
        double mx = event.getMouseX(), my = event.getMouseY();
        if (!inPanel(mx, my)) return; // パネル外は通常のインベントリ操作

        if (event.getButton() == 0 && inClose(mx, my)) {
            close();
            event.setCanceled(true);
            return;
        }
        if (event.getButton() == 0 && inTitle(mx, my)) {
            dragging = true;
            dragOffX = mx - panelX;
            dragOffY = my - panelY;
            event.setCanceled(true);
            return;
        }
        int i = slotAt(mx, my);
        if (i >= 0) {
            handleSlotClick((AbstractContainerScreen<?>) event.getScreen(), i);
        }
        event.setCanceled(true); // パネル内クリックはインベントリへ通さない
    }

    /**
     * スロットクリック: カーソル ↔ ポーチスロット のスワップをクライアントで計算し、
     * クライアントのカーソルを更新 ＆ 結果をサーバーへ送って永続化する ( 両モード対応 )。
     */
    private static void handleSlotClick(AbstractContainerScreen<?> screen, int i) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        ItemStack pouch = pouch(player);
        if (pouch.isEmpty()) return;

        AbstractContainerMenu menu = screen.getMenu();
        ItemStack cursor = menu.getCarried();
        ItemStack[] lo = MaterializedPouchItem.getLoadout(pouch);
        ItemStack current = lo[i];

        ItemStack newSlot, newCursor;
        if (cursor.isEmpty()) {
            if (current.isEmpty()) {
                // 空きスロット + 空カーソル → 「空気置換」 フラグをトグル
                TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(
                    new PouchToggleAirMessage(pouchSlot, i));
                return;
            }
            // 取り出し: ポーチ由来の刻印を付けておく ( Magical Katana 破壊で確実に回収できる )
            newSlot = ItemStack.EMPTY;
            newCursor = current.copy();
            MaterializedPouchItem.stampFromPouch(newCursor, player.getUUID());
        } else {
            if (MaterializedPouchItem.isInsertLocked(player)) return;     // 結晶化中は収納不可
            if (!MaterializedPouchItem.canStore(cursor)) return;
            if (current.isEmpty()) {
                newSlot = cursor.copy();
                newSlot.setCount(1);
                ItemStack c = cursor.copy();
                c.shrink(1);
                newCursor = c.isEmpty() ? ItemStack.EMPTY : c;
            } else {
                if (cursor.getCount() != 1) return; // スタックの入れ替えは扱わない
                newSlot = cursor.copy();
                newCursor = current.copy();
            }
        }

        menu.setCarried(newCursor); // クライアント側カーソル ( クリエイティブで権威 )
        TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(
            new PouchSlotClickMessage(pouchSlot, i, newSlot, newCursor));
    }

    @SubscribeEvent
    public static void onMouseRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        if (dragging && event.getButton() == 0) {
            dragging = false;
            event.setCanceled(true);
        }
    }
}
