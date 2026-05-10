package minecraftarmorweapon.client;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.network.BattouFromSpecificSlotPacket;
import minecraftarmorweapon.network.RMessage;
import minecraftarmorweapon.network.SheathIntoSpecificSlotPacket;
import minecraftarmorweapon.util.CuriosScabbardHelper;
import minecraftarmorweapon.util.CuriosScabbardHelper.DrawableWeaponInfo;
import minecraftarmorweapon.util.CuriosScabbardHelper.ScabbardLocation;
import minecraftarmorweapon.events.DodgeAndBattouHandler;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.Collections;
import java.util.List;

/**
 * 武器ホイールのクライアント側ステートマシン（抜刀・納刀デュアルモード）。
 *
 * 動作:
 *   - R 押下 → 即座にホイール表示 (鞘の数に関わらず)
 *   - マウスを動かして選択 → 離す → 選択された鞘を抜刀/納刀
 *   - 中央 (デッドゾーン) で離す → RMessage デフォルト動作にフォールバック
 *
 * tick() は MinecraftArmorWeaponModKeyMappings.KeyEventListener.onClientTick から
 * 直接呼び出される (確実に subscribe される場所を経由)。
 */
@OnlyIn(Dist.CLIENT)
public class WeaponWheelState {

    public enum WheelMode { DRAW, SHEATH }

    private static boolean rKeyDown = false;
    private static boolean wheelVisible = false;
    private static int selectedIndex = -1;
    private static List<DrawableWeaponInfo> drawableWeapons = Collections.emptyList();
    private static WheelMode currentMode = WheelMode.DRAW;

    public static boolean isWheelVisible() { return wheelVisible; }
    public static List<DrawableWeaponInfo> getDrawableWeapons() { return drawableWeapons; }
    public static int getSelectedIndex() { return selectedIndex; }
    public static WheelMode getCurrentMode() { return currentMode; }

    /**
     * R 押下時 (手に武器なし → 抜刀ホイール).
     * 鞘の数に関わらずホイールを表示する。
     */
    public static boolean onRKeyPressed() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return false;

        List<DrawableWeaponInfo> weapons = CuriosScabbardHelper.findAllLoadedScabbards(player);

        currentMode = WheelMode.DRAW;
        drawableWeapons = weapons;
        rKeyDown = true;
        selectedIndex = -1;
        wheelVisible = true;
        mc.mouseHandler.releaseMouse();
        return true;
    }

    /**
     * R 押下時 (手に武器あり → 納刀ホイール).
     * 互換のある空鞘の数に関わらずホイールを表示する。
     */
    public static boolean onRKeyPressedForSheathing(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (player == null || mc.level == null) return false;

        ItemStack weaponStack = getWeaponInHand(player);
        if (weaponStack.isEmpty()) return false;

        List<DrawableWeaponInfo> emptyScabbards = CuriosScabbardHelper.findAllEmptyScabbards(player);
        emptyScabbards.removeIf(info -> !CuriosScabbardHelper.isCompatible(weaponStack, info.scabbardStack));

        currentMode = WheelMode.SHEATH;
        drawableWeapons = emptyScabbards;
        rKeyDown = true;
        selectedIndex = -1;
        wheelVisible = true;
        mc.mouseHandler.releaseMouse();
        return true;
    }

    /**
     * R リリース時.
     */
    public static void onRKeyReleased() {
        if (!rKeyDown) return;

        // grabMouse() の cascade による再帰を防ぐため、状態保存→reset()→処理 の順
        boolean wasWheelVisible = wheelVisible;
        int savedSelectedIndex = selectedIndex;
        List<DrawableWeaponInfo> savedWeapons = drawableWeapons;
        WheelMode savedMode = currentMode;

        reset();

        if (savedSelectedIndex >= 0 && savedSelectedIndex < savedWeapons.size()) {
            // 選択あり → 該当パケット送信
            DrawableWeaponInfo info = savedWeapons.get(savedSelectedIndex);
            sendPacket(savedMode, info);
        } else {
            // デッドゾーンで離した or 鞘無し → RMessage デフォルト動作にフォールバック (サーバーのみ)
            MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(new RMessage(0, 0));
        }

        if (wasWheelVisible) {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().mouseHandler.grabMouse();
            });
        }
    }

    private static void sendPacket(WheelMode mode, DrawableWeaponInfo info) {
        if (mode == WheelMode.DRAW) {
            MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                new BattouFromSpecificSlotPacket(
                    info.location, info.curioSlotId, info.slotIndex
                )
            );
        } else {
            Minecraft mc = Minecraft.getInstance();
            int weaponHandIndex = determineWeaponHand(mc.player);
            MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                new SheathIntoSpecificSlotPacket(
                    info.location, info.curioSlotId, info.slotIndex, weaponHandIndex
                )
            );
        }
    }

    private static int determineWeaponHand(Player player) {
        if (player == null) return 0;
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (DodgeAndBattouHandler.isWeapon(mainHand) && !DodgeAndBattouHandler.isSaya(mainHand)) {
            return 0;
        }
        return 1;
    }

    private static ItemStack getWeaponInHand(Player player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (DodgeAndBattouHandler.isWeapon(mainHand) && !DodgeAndBattouHandler.isSaya(mainHand)) {
            return mainHand;
        }
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        if (DodgeAndBattouHandler.isWeapon(offHand) && !DodgeAndBattouHandler.isSaya(offHand)) {
            return offHand;
        }
        return ItemStack.EMPTY;
    }

    /**
     * クライアント tick 毎に呼ばれる。
     * MinecraftArmorWeaponModKeyMappings.KeyEventListener.onClientTick から直接呼び出される。
     */
    public static void tick() {
        if (!rKeyDown) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            reset();
            return;
        }

        if (mc.screen != null) {
            boolean wasWheelVisible = wheelVisible;
            reset();
            if (wasWheelVisible) {
                Minecraft.getInstance().execute(() -> {
                    Minecraft.getInstance().mouseHandler.grabMouse();
                });
            }
            return;
        }

        // R が物理的に押されているか GLFW で確認
        int rKeyValue = minecraftarmorweapon.init.MinecraftArmorWeaponModKeyMappings.R.getKey().getValue();
        boolean stillHeld = InputConstants.isKeyDown(mc.getWindow().getWindow(), rKeyValue);
        if (!stillHeld) {
            onRKeyReleased();
            return;
        }

        // ホイール表示中はマウス位置で選択を更新
        if (wheelVisible) {
            double mouseX = mc.mouseHandler.xpos() *
                (double) mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
            double mouseY = mc.mouseHandler.ypos() *
                (double) mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
            updateSelection(mouseX, mouseY,
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight());
        }
    }

    private static void updateSelection(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        double centerX = screenWidth / 2.0;
        double centerY = screenHeight / 2.0;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        int itemCount = drawableWeapons.size();
        if (distance < 20.0 || itemCount == 0) {
            selectedIndex = -1;
            return;
        }

        double angle = Math.atan2(dx, -dy);
        if (angle < 0) angle += 2 * Math.PI;

        double segmentSize = 2.0 * Math.PI / itemCount;
        double shifted = angle + segmentSize / 2.0;
        if (shifted >= 2 * Math.PI) shifted -= 2 * Math.PI;

        selectedIndex = (int) (shifted / segmentSize);
        if (selectedIndex >= itemCount) selectedIndex = itemCount - 1;
    }

    private static void reset() {
        rKeyDown = false;
        wheelVisible = false;
        selectedIndex = -1;
        drawableWeapons = Collections.emptyList();
        currentMode = WheelMode.DRAW;
    }
}
