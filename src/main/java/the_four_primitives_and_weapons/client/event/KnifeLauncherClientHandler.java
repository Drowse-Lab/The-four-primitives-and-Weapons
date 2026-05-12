package the_four_primitives_and_weapons.client.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.client.screens.KnifeLauncherSkillScreen;
import the_four_primitives_and_weapons.entity.ThrowingKnifeEntity.KnifeType;
import the_four_primitives_and_weapons.item.KnifeLauncherItem;

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
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, value = Dist.CLIENT)
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

    // HUD テキストはモード/本数が変わった時のみ再構築。毎フレーム文字列連結するとGC圧発生。
    private static KnifeType cachedMode;
    private static int cachedCount = -1;
    private static String cachedText = "";
    private static Component cachedComponent = Component.empty();

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
        if (mode != cachedMode || count != cachedCount) {
            cachedMode = mode;
            cachedCount = count;
            cachedText = "§6▶ " + KnifeLauncherItem.modeLabel(mode) + " §7x §f" + count
                + "  §8[Shift+左クリックで設定]";
            cachedComponent = Component.literal(cachedText);
        }

        GuiGraphics g = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int x = sw / 2 - mc.font.width(cachedText) / 2;
        int y = mc.getWindow().getGuiScaledHeight() - 60;
        g.drawString(mc.font, cachedComponent, x, y, 0xFFFFFF, true);
    }
}
