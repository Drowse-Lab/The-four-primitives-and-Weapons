package minecraftarmorweapon.client.screens;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.entity.ThrowingKnifeEntity.KnifeType;
import minecraftarmorweapon.item.KnifeLauncherItem;
import minecraftarmorweapon.network.KnifeLauncherPacket;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * ナイフランチャーの技選択 GUI。
 * モードを 3 ボタンから選択、本数を +/- で 1〜10 に設定する。
 * 変更は即座にサーバーへパケット送信し NBT を同期。
 */
public class KnifeLauncherSkillScreen extends Screen {

    private static final int PANEL_W = 240;
    private static final int PANEL_H = 160;

    private int panelX;
    private int panelY;
    private Button[] modeButtons = new Button[3];
    private Button minusBtn;
    private Button plusBtn;

    public KnifeLauncherSkillScreen() {
        super(Component.literal("Knife Launcher Skill"));
    }

    private ItemStack holdingStack() {
        if (minecraft == null || minecraft.player == null) return ItemStack.EMPTY;
        ItemStack s = minecraft.player.getItemInHand(InteractionHand.MAIN_HAND);
        return s.getItem() instanceof KnifeLauncherItem ? s : ItemStack.EMPTY;
    }

    @Override
    protected void init() {
        super.init();
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;

        KnifeType[] modes = { KnifeType.NORMAL, KnifeType.STUN, KnifeType.SCREW };
        String[] labels   = { "通常", "スタン", "スクリュー" };

        int btnW = 70;
        int gap = 4;
        int totalW = btnW * 3 + gap * 2;
        int startX = panelX + (PANEL_W - totalW) / 2;
        int y = panelY + 40;
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            modeButtons[i] = Button.builder(
                Component.literal(labels[i]),
                b -> {
                    MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                        new KnifeLauncherPacket(KnifeLauncherPacket.ACTION_SET_MODE, idx));
                    // 即座にクライアント側の NBT も同期 (見た目反映)
                    ItemStack s = holdingStack();
                    if (!s.isEmpty()) KnifeLauncherItem.setMode(s, modes[idx]);
                })
                .bounds(startX + i * (btnW + gap), y, btnW, 20)
                .build();
            this.addRenderableWidget(modeButtons[i]);
        }

        // 本数 +/-
        int countY = panelY + 90;
        this.minusBtn = Button.builder(Component.literal("-"), b -> adjustCount(-1))
            .bounds(panelX + 60, countY, 24, 20).build();
        this.plusBtn = Button.builder(Component.literal("+"), b -> adjustCount(+1))
            .bounds(panelX + PANEL_W - 60 - 24, countY, 24, 20).build();
        this.addRenderableWidget(this.minusBtn);
        this.addRenderableWidget(this.plusBtn);

        // 閉じる
        this.addRenderableWidget(Button.builder(
            Component.literal("閉じる"),
            b -> this.onClose())
            .bounds(panelX + (PANEL_W - 80) / 2, panelY + PANEL_H - 26, 80, 20)
            .build());
    }

    private void adjustCount(int delta) {
        ItemStack s = holdingStack();
        if (s.isEmpty()) return;
        int cur = KnifeLauncherItem.getCount(s);
        int next = Math.max(1, Math.min(KnifeLauncherItem.MAX_COUNT, cur + delta));
        KnifeLauncherItem.setCount(s, next);
        MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
            new KnifeLauncherPacket(KnifeLauncherPacket.ACTION_SET_COUNT, next));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        // 背景を暗く
        this.renderBackground(g);
        // パネル本体
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xE0202020);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 1, 0xFFFFD700);
        g.fill(panelX, panelY + PANEL_H - 1, panelX + PANEL_W, panelY + PANEL_H, 0xFFFFD700);
        g.fill(panelX, panelY, panelX + 1, panelY + PANEL_H, 0xFFFFD700);
        g.fill(panelX + PANEL_W - 1, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xFFFFD700);

        Minecraft mc = Minecraft.getInstance();
        String title = "ナイフ技選択";
        int tx = panelX + (PANEL_W - mc.font.width(title)) / 2;
        g.drawString(mc.font, title, tx, panelY + 12, 0xFFFFD700, false);

        // モードラベル
        g.drawString(mc.font, "モード", panelX + 16, panelY + 30, 0xFFFFFFFF, false);

        // 本数表示
        g.drawString(mc.font, "一度に投げる数", panelX + 16, panelY + 78, 0xFFFFFFFF, false);
        ItemStack s = holdingStack();
        String countText = s.isEmpty() ? "-" : String.valueOf(KnifeLauncherItem.getCount(s));
        int nx = panelX + (PANEL_W - mc.font.width(countText) * 2) / 2;
        // 数字を大きめに描画 (2倍スケール)
        g.pose().pushPose();
        g.pose().scale(2.0f, 2.0f, 1.0f);
        g.drawString(mc.font, countText,
            (int)(nx / 2.0f),
            (int)((panelY + 92) / 2.0f),
            0xFFFFD700, true);
        g.pose().popPose();

        // 現在モードの強調
        if (!s.isEmpty()) {
            KnifeType cur = KnifeLauncherItem.getMode(s);
            KnifeType[] modes = { KnifeType.NORMAL, KnifeType.STUN, KnifeType.SCREW };
            for (int i = 0; i < 3; i++) {
                if (modes[i] == cur && modeButtons[i] != null) {
                    int bx = modeButtons[i].getX();
                    int by = modeButtons[i].getY();
                    int bw = modeButtons[i].getWidth();
                    int bh = modeButtons[i].getHeight();
                    g.fill(bx - 2, by - 2, bx + bw + 2, by - 1, 0xFFFFD700);
                    g.fill(bx - 2, by + bh + 1, bx + bw + 2, by + bh + 2, 0xFFFFD700);
                    g.fill(bx - 2, by - 2, bx - 1, by + bh + 2, 0xFFFFD700);
                    g.fill(bx + bw + 1, by - 2, bx + bw + 2, by + bh + 2, 0xFFFFD700);
                }
            }
        }

        // ヒント
        String hint = "§7本数が多いほどブレが大きい";
        int hx = panelX + (PANEL_W - mc.font.width(hint)) / 2;
        g.drawString(mc.font, Component.literal(hint), hx, panelY + PANEL_H - 44, 0xFFFFFFFF, false);

        super.render(g, mouseX, mouseY, partialTicks);
        RenderSystem.disableBlend();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
