package the_four_primitives_and_weapons.client.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.compat.SpellbooksCompat;
import the_four_primitives_and_weapons.mana.ManaHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 画面下にMana残量バーを表示。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, value = Dist.CLIENT)
public class ManaHudOverlay {

    // 表示値が変化した時だけテキスト再構築
    private static int cachedCur = -1;
    private static int cachedMax = -1;
    private static String cachedTxt = "MP 0/0";

    @SubscribeEvent
    public static void onRender(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().getPath().equals("hotbar")) return;
        // Iron's Spellbooks が入っている時は向こうが自前の Mana HUD を描画するので
        // 本 MOD 側のバーは表示しない (二重バー防止)。
        if (SpellbooksCompat.isLoaded()) return;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || p.isSpectator()) return;

        double cur = ManaHelper.getMana(p);
        double max = ManaHelper.maxMana(p);
        if (max <= 0) return;
        float pct = (float)(cur / max);
        int curI = (int) cur;
        int maxI = (int) max;
        if (curI != cachedCur || maxI != cachedMax) {
            cachedCur = curI;
            cachedMax = maxI;
            cachedTxt = "MP " + curI + "/" + maxI;
        }

        GuiGraphics g = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int barW = 80;
        int barH = 4;
        int x = sw / 2 + 92; // ホットバーの右側
        int y = sh - 20;

        // 背景
        g.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, 0xFF000000);
        g.fill(x, y, x + barW, y + barH, 0xFF1A1A40);
        // 残量
        int fill = (int)(barW * pct);
        g.fill(x, y, x + fill, y + barH, 0xFF3060FF);
        // 数値 (キャッシュ済み文字列)
        g.drawString(mc.font, cachedTxt, x, y - 10, 0xAACCFF, true);
    }
}
