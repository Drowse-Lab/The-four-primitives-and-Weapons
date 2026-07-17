package the_four_primitives_and_weapons.client.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.network.HandsSlotClickPacket;
import the_four_primitives_and_weapons.util.CuriosHandsHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * インベントリ画面の防具スロットの横に Curios hands ( 手袋 ) スロットを表示する
 * ( ElytraSlot / Trinkets 風 )。
 *
 * <p>チェストプレートスロットの右隣 ( 26, 26 ) に擬似スロットを描画し、
 * クリックで {@link HandsSlotClickPacket} をサーバーへ送って
 * カーソルの持ち物と hands スロットを入れ替える。 実際の在庫は Curios 側にあり、
 * ここは表示と入力の橋渡しだけを行う ( Curios の画面からも従来どおり操作可能 )。</p>
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, value = Dist.CLIENT)
public class HandsSlotOverlayHandler {

    /** スロット位置 ( GUI 左上基準 )。 チェストプレートスロット (8,26) の右隣。 */
    private static final int SLOT_X = 26;
    private static final int SLOT_Y = 26;

    /** 空スロットに薄く表示する手袋アイコン。 */
    private static final ResourceLocation EMPTY_ICON = new ResourceLocation(
            "the_four_primitives_and_weapons", "textures/item/gloves.png");

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        GuiGraphics g = event.getGuiGraphics();
        int x = screen.getGuiLeft() + SLOT_X;
        int y = screen.getGuiTop() + SLOT_Y;

        // スロット枠 ( バニラ風: 内側グレー + 左上暗/右下明の縁 )
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
        g.fill(x - 1, y - 1, x + 17, y, 0xFF373737);      // 上
        g.fill(x - 1, y, x, y + 16, 0xFF373737);          // 左
        g.fill(x - 1, y + 16, x + 17, y + 17, 0xFFFFFFFF); // 下
        g.fill(x + 16, y, x + 17, y + 16, 0xFFFFFFFF);     // 右

        ItemStack inSlot = CuriosHandsHelper.getSlotStack(player);
        if (!inSlot.isEmpty()) {
            g.renderItem(inSlot, x, y);
            g.renderItemDecorations(screen.getMinecraft().font, inSlot, x, y);
        } else {
            // 空: 手袋シルエットを薄く表示
            g.setColor(1.0f, 1.0f, 1.0f, 0.35f);
            g.blit(EMPTY_ICON, x, y, 0, 0f, 0f, 16, 16, 16, 16);
            g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        // ホバー: ハイライト + ツールチップ
        if (isHovering(screen, event.getMouseX(), event.getMouseY())) {
            AbstractContainerScreen.renderSlotHighlight(g, x, y, 0);
            ItemStack carried = player.containerMenu.getCarried();
            if (carried.isEmpty()) {
                if (!inSlot.isEmpty()) {
                    g.renderTooltip(screen.getMinecraft().font, inSlot, event.getMouseX(), event.getMouseY());
                } else {
                    g.renderTooltip(screen.getMinecraft().font,
                            Component.translatable("curios.identifier.hands"),
                            event.getMouseX(), event.getMouseY());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        if (event.getButton() != 0) return; // 左クリックのみ
        if (!isHovering(screen, event.getMouseX(), event.getMouseY())) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // 何もない状態で空スロットをクリックしても通信しない
        ItemStack carried = player.containerMenu.getCarried();
        ItemStack inSlot = CuriosHandsHelper.getSlotStack(player);
        if (carried.isEmpty() && inSlot.isEmpty()) {
            event.setCanceled(true);
            return;
        }
        // 装備できないアイテムを持っているときは何もしない ( ドロップ暴発防止にキャンセルはする )
        if (!carried.isEmpty() && !CuriosHandsHelper.isHandsEquippable(carried)) {
            event.setCanceled(true);
            return;
        }

        TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new HandsSlotClickPacket());
        event.setCanceled(true);
    }

    private static boolean isHovering(InventoryScreen screen, double mouseX, double mouseY) {
        int x = screen.getGuiLeft() + SLOT_X;
        int y = screen.getGuiTop() + SLOT_Y;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }
}
