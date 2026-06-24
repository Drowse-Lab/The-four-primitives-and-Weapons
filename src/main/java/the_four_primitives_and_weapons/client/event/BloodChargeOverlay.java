package the_four_primitives_and_weapons.client.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import the_four_primitives_and_weapons.item.RiversOfBloodItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Rivers of Blood の「ため」 進行度をホットバー上に表示する HUD オーバーレイ。
 *
 * 表示条件:
 *   - メイン/オフハンドの どちらかで Rivers of Blood を使用中 ( player.isUsingItem() )
 *
 * 表示内容:
 *   - ホットバーの中央上に細いバー ( 暗赤 背景 + 鮮赤 充填 )
 *   - 閾値到達 ( = TP モード ) なら 黄色っぽい色 + "HOLD: TP" ラベル
 *   - 未達 ( = 斬撃モード ) なら 赤色 + "TAP: SLASH" ラベル
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, value = Dist.CLIENT)
public class BloodChargeOverlay {

    @SubscribeEvent
    public static void onRender(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().getPath().equals("hotbar")) return;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || p.isSpectator()) return;
        if (!p.isUsingItem()) return;

        ItemStack using = p.getUseItem();
        if (using.isEmpty()
                || using.getItem() != TheFourPrimitivesAndWeaponsModItems.RIVERS_OF_BLOOD.get()) return;

        int held = RiversOfBloodItem.USE_DURATION_TICKS - p.getUseItemRemainingTicks();
        if (held <= 0) return;
        float progress = Math.min(1.0f, (float) held / (float) RiversOfBloodItem.HOLD_THRESHOLD_TICKS);
        boolean overThreshold = held >= RiversOfBloodItem.HOLD_THRESHOLD_TICKS;

        GuiGraphics g = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        int barW = 80;
        int barH = 5;
        int x = sw / 2 - barW / 2;
        int y = sh - 38;            // ホットバーの真上

        // ラベル
        String label = overThreshold ? "§e[HOLD] §6Hemorrhagic Eclipse"
                                      : "§c[TAP] §7Blood Cleaver";
        Component labelText = Component.literal(label);
        int labelW = mc.font.width(labelText);
        g.drawString(mc.font, labelText, sw / 2 - labelW / 2, y - 10, 0xFFFFFFFF, true);

        // バー背景
        g.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, 0xFF000000);
        g.fill(x, y, x + barW, y + barH, 0xFF2A0808);

        // 充填部
        int fill = (int) (barW * progress);
        int color = overThreshold ? 0xFFE8B850 : 0xFFD42030;
        g.fill(x, y, x + fill, y + barH, color);

        // 閾値マーカー ( 縦線 )
        if (!overThreshold) {
            // 閾値ぴったりに細い線を表示してプレイヤーに「ここで TP」 と教える
            int markX = x + barW; // progress 1.0 = 閾値ぴったり
            g.fill(markX - 1, y - 2, markX, y + barH + 2, 0xFFFFFF80);
        }
    }
}
