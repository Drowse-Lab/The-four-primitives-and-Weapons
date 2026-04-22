package minecraftarmorweapon.client.event;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.compat.SpellbooksCompat;
import minecraftarmorweapon.mana.ManaHelper;

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
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID, value = Dist.CLIENT)
public class ManaHudOverlay {

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
        // 数値
        String txt = "MP " + (int)cur + "/" + (int)max;
        g.drawString(mc.font, txt, x, y - 10, 0xAACCFF, true);
    }
}
