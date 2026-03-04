package minecraftarmorweapon.client;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.network.BattouFromSpecificSlotPacket;
import minecraftarmorweapon.network.SheathIntoSpecificSlotPacket;
import minecraftarmorweapon.util.CuriosScabbardHelper;
import minecraftarmorweapon.util.CuriosScabbardHelper.DrawableWeaponInfo;
import minecraftarmorweapon.util.CuriosScabbardHelper.ScabbardLocation;
import minecraftarmorweapon.events.DodgeAndBattouHandler;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.Collections;
import java.util.List;

/**
 * 武器ホイールのクライアント側ステートマシン（抜刀・納刀デュアルモード）
 */
@OnlyIn(Dist.CLIENT)
public class WeaponWheelState {

    public enum WheelMode { DRAW, SHEATH }

    private static boolean rKeyDown = false;
    private static long rKeyPressStartTick = -1;
    private static boolean wheelVisible = false;
    private static int selectedIndex = -1;
    private static List<DrawableWeaponInfo> drawableWeapons = Collections.emptyList();
    private static WheelMode currentMode = WheelMode.DRAW;

    private static final int HOLD_THRESHOLD_TICKS = 5;

    public static boolean isWheelVisible() { return wheelVisible; }
    public static List<DrawableWeaponInfo> getDrawableWeapons() { return drawableWeapons; }
    public static int getSelectedIndex() { return selectedIndex; }
    public static WheelMode getCurrentMode() { return currentMode; }

    /**
     * Rキー押下時に呼ばれる（手に武器がない場合＝抜刀モード）
     * @return true = 抜刀モードに入った（RMessageを送るべきでない）
     */
    public static boolean onRKeyPressed() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return false;

        // 全ての武器入り鞘を列挙（CoverUpも含む）
        List<DrawableWeaponInfo> weapons = CuriosScabbardHelper.findAllLoadedScabbards(player);

        if (weapons.isEmpty()) {
            return false;
        }

        if (weapons.size() == 1) {
            // 1個だけなら即座に抜刀
            DrawableWeaponInfo info = weapons.get(0);
            MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                new BattouFromSpecificSlotPacket(
                    info.location,
                    info.curioSlotId,
                    info.slotIndex
                )
            );
            return true;
        }

        // 2個以上: ホイールモード開始
        currentMode = WheelMode.DRAW;
        drawableWeapons = weapons;
        rKeyDown = true;
        rKeyPressStartTick = mc.level.getGameTime();
        selectedIndex = -1;
        return true;
    }

    /**
     * Rキー押下時に呼ばれる（手に武器がある場合＝納刀モード）
     * @return true = 納刀ホイールモードに入った（RMessageを送るべきでない）
     */
    public static boolean onRKeyPressedForSheathing(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (player == null || mc.level == null) return false;

        // 空の鞘を全て列挙
        List<DrawableWeaponInfo> emptyScabbards = CuriosScabbardHelper.findAllEmptyScabbards(player);

        // 互換性フィルタ: 手に持っている武器と互換の鞘のみ
        ItemStack weaponStack = getWeaponInHand(player);
        if (weaponStack.isEmpty()) return false;

        emptyScabbards.removeIf(info -> !CuriosScabbardHelper.isCompatible(weaponStack, info.scabbardStack));

        if (emptyScabbards.size() < 2) {
            return false; // 0-1個 → RMessageの既存処理に任せる
        }

        // 2個以上: 納刀ホイールモード開始
        currentMode = WheelMode.SHEATH;
        drawableWeapons = emptyScabbards;
        rKeyDown = true;
        rKeyPressStartTick = mc.level.getGameTime();
        selectedIndex = -1;
        return true;
    }

    /**
     * Rキーリリース時に呼ばれる
     */
    public static void onRKeyReleased() {
        if (!rKeyDown) return;

        // grabMouse() → KeyMapping.setAll() → setDown(false) → onRKeyReleased() の
        // 無限再帰を防ぐため、状態を保存してからreset()を先に呼ぶ
        boolean wasWheelVisible = wheelVisible;
        int savedSelectedIndex = selectedIndex;
        List<DrawableWeaponInfo> savedWeapons = drawableWeapons;
        WheelMode savedMode = currentMode;

        reset();

        if (wasWheelVisible && savedSelectedIndex >= 0 && savedSelectedIndex < savedWeapons.size()) {
            DrawableWeaponInfo info = savedWeapons.get(savedSelectedIndex);
            sendPacket(savedMode, info);
        } else if (!wasWheelVisible && !savedWeapons.isEmpty()) {
            // ホイールが表示される前にリリースされた（クイックタップ）→ 最初のアイテム
            DrawableWeaponInfo info = savedWeapons.get(0);
            sendPacket(savedMode, info);
        }

        // ホイールが表示されていたらマウスを再取得（次フレームに遅延）
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
            // SHEATH: 武器を持っている手を特定
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
            return 0; // MAIN_HAND
        }
        return 1; // OFF_HAND
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
     * 毎クライアントtickで呼ばれる
     */
    public static void tick() {
        if (!rKeyDown) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            reset();
            return;
        }

        // GUI画面が開いたらキャンセル
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

        // Rキーがまだ物理的に押されているか確認（GLFW）
        int rKeyValue = minecraftarmorweapon.init.MinecraftArmorWeaponModKeyMappings.R.getKey().getValue();
        boolean stillHeld = InputConstants.isKeyDown(mc.getWindow().getWindow(), rKeyValue);

        if (!stillHeld) {
            onRKeyReleased();
            return;
        }

        // ホールド閾値を超えたらホイール表示
        long currentTick = mc.level.getGameTime();
        long ticksHeld = currentTick - rKeyPressStartTick;

        if (ticksHeld >= HOLD_THRESHOLD_TICKS && !wheelVisible && drawableWeapons.size() >= 2) {
            wheelVisible = true;
            mc.mouseHandler.releaseMouse();
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

        // 中心のデッドゾーン
        if (distance < 20.0 || itemCount == 0) {
            selectedIndex = -1;
            return;
        }

        // 角度計算: 上が0、時計回り
        double angle = Math.atan2(dx, -dy);
        if (angle < 0) angle += 2 * Math.PI;

        // 半セグメント分オフセットして、選択ゾーンをアイテム位置の中心に合わせる
        double segmentSize = 2.0 * Math.PI / itemCount;
        double shifted = angle + segmentSize / 2.0;
        if (shifted >= 2 * Math.PI) shifted -= 2 * Math.PI;

        selectedIndex = (int) (shifted / segmentSize);
        if (selectedIndex >= itemCount) selectedIndex = itemCount - 1;
    }

    private static void reset() {
        rKeyDown = false;
        rKeyPressStartTick = -1;
        wheelVisible = false;
        selectedIndex = -1;
        drawableWeapons = Collections.emptyList();
        currentMode = WheelMode.DRAW;
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class WheelTicker {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            WeaponWheelState.tick();
        }
    }
}
