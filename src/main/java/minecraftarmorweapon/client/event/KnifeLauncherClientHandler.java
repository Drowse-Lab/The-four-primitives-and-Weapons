package minecraftarmorweapon.client.event;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.client.screens.KnifeLauncherSkillScreen;
import minecraftarmorweapon.entity.ThrowingKnifeEntity.KnifeType;
import minecraftarmorweapon.item.KnifeLauncherItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * KnifeLauncher クライアント側入力 & HUD。
 *
 *   シフト + 左クリック : 技選択画面を開く
 *   HUD                 : ホットバー上部にモード/数を常時表示
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID, value = Dist.CLIENT)
public class KnifeLauncherClientHandler {

    private static boolean wasAttackPressed = false;

    /** 毎tickで Shift+左クリック押下を検出 → 技選択スクリーンを開く */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null) {
            wasAttackPressed = false;
            return;
        }
        ItemStack stack = player.getMainHandItem();
        boolean holdingLauncher = stack.getItem() instanceof KnifeLauncherItem;

        boolean attackDown = mc.options.keyAttack.isDown();
        boolean shiftDown  = player.isShiftKeyDown();

        if (holdingLauncher && shiftDown && attackDown && !wasAttackPressed) {
            mc.setScreen(new KnifeLauncherSkillScreen());
        }
        wasAttackPressed = attackDown;
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().getPath().equals("hotbar")) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof KnifeLauncherItem)) return;

        KnifeType mode = KnifeLauncherItem.getMode(stack);
        int count = KnifeLauncherItem.getCount(stack);
        String text = "§6▶ " + KnifeLauncherItem.modeLabel(mode) + " §7x §f" + count
            + "  §8[Shift+左クリックで設定]";

        GuiGraphics g = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int x = sw / 2 - mc.font.width(text) / 2;
        int y = mc.getWindow().getGuiScaledHeight() - 60;
        g.drawString(mc.font, Component.literal(text), x, y, 0xFFFFFF, true);
    }
}
