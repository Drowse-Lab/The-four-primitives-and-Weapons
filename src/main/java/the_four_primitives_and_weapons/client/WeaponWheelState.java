package the_four_primitives_and_weapons.client;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.events.MagicalKatanaCrystalHandler;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import the_four_primitives_and_weapons.network.BattouFromSpecificSlotPacket;
import the_four_primitives_and_weapons.network.MagicalKatanaActionPacket;
import the_four_primitives_and_weapons.network.RMessage;
import the_four_primitives_and_weapons.network.SheathIntoSpecificSlotPacket;
import the_four_primitives_and_weapons.util.CuriosScabbardHelper;
import the_four_primitives_and_weapons.util.CuriosScabbardHelper.DrawableWeaponInfo;
import the_four_primitives_and_weapons.util.CuriosScabbardHelper.ScabbardLocation;
import the_four_primitives_and_weapons.events.DodgeAndBattouHandler;

import java.util.ArrayList;

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
 *   - R 押下 → 内部状態を記録 (ホイールはまだ表示しない)
 *   - 0.5秒以上長押し → ホイール表示 + マウス解放
 *   - マウスを動かして選択 → 離す → 選択された鞘を抜刀/納刀
 *   - 0.5秒未満で離す or 中央 (デッドゾーン) で離す → RMessage デフォルト動作にフォールバック
 *
 * tick() は TheFourPrimitivesAndWeaponsModKeyMappings.KeyEventListener.onClientTick から
 * 直接呼び出される (確実に subscribe される場所を経由)。
 */
@OnlyIn(Dist.CLIENT)
public class WeaponWheelState {

    public enum WheelMode { DRAW, SHEATH }

    /** ホイール上の特殊アクション ( 通常の抜刀 / 納刀候補とは別枠 )。 */
    public static final class SpecialAction {
        public final MagicalKatanaActionPacket.Action action;
        public final ItemStack icon;
        public final String label;
        public SpecialAction(MagicalKatanaActionPacket.Action action, ItemStack icon, String label) {
            this.action = action;
            this.icon = icon;
            this.label = label;
        }
    }

    private static final long WHEEL_DELAY_MS = 500L;

    private static boolean rKeyDown = false;
    private static boolean wheelVisible = false;
    private static int selectedIndex = -1;
    private static List<DrawableWeaponInfo> drawableWeapons = Collections.emptyList();
    private static List<SpecialAction> specialActions = Collections.emptyList();
    private static WheelMode currentMode = WheelMode.DRAW;
    private static long pressStartTime = 0L;

    public static boolean isWheelVisible() { return wheelVisible; }
    public static List<DrawableWeaponInfo> getDrawableWeapons() { return drawableWeapons; }
    public static List<SpecialAction> getSpecialActions() { return specialActions; }
    public static int getTotalEntries() { return drawableWeapons.size() + specialActions.size(); }
    public static int getSelectedIndex() { return selectedIndex; }
    public static WheelMode getCurrentMode() { return currentMode; }

    /**
     * R 押下時 (手に武器なし → 抜刀ホイール).
     * 状態のみ記録し、1秒長押しを待って tick() でホイール表示する。
     */
    public static boolean onRKeyPressed() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return false;

        List<DrawableWeaponInfo> weapons = CuriosScabbardHelper.findAllLoadedScabbards(player);

        currentMode = WheelMode.DRAW;
        drawableWeapons = weapons;
        specialActions = buildSpecialActionsForDraw(player);
        rKeyDown = true;
        selectedIndex = -1;
        wheelVisible = false;
        pressStartTime = System.currentTimeMillis();
        return true;
    }

    /**
     * R 押下時 (手に武器あり → 納刀ホイール).
     * 状態のみ記録し、1秒長押しを待って tick() でホイール表示する。
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
        specialActions = buildSpecialActionsForSheath(player, weaponStack);
        rKeyDown = true;
        selectedIndex = -1;
        wheelVisible = false;
        pressStartTime = System.currentTimeMillis();
        return true;
    }

    /**
     * 抜刀ホイール用の SpecialAction:
     *   - メイン or オフハンドに 通常版 Magical Katana ( unlocked ) を持っていれば「結晶を出す」
     *     ( DRAW mode に来るのは手に武器がないケースだが、念のため off-hand を見ない仕様 )
     */
    private static List<SpecialAction> buildSpecialActionsForDraw(Player player) {
        // DRAW mode = 手に武器なし → Magical Katana を直接手に持ってないので special なし
        return Collections.emptyList();
    }

    /**
     * 納刀ホイール用の SpecialAction:
     *   - 持ってる武器が 通常版 Magical Katana ( unlocked ) → 「結晶を出す」
     *   - 持ってる武器が 具現化版 Magical Katana → 「破壊」
     */
    private static List<SpecialAction> buildSpecialActionsForSheath(Player player, ItemStack weaponStack) {
        List<SpecialAction> actions = new ArrayList<>();
        if (MagicalKatanaCrystalHandler.isMagicalKatana(weaponStack)) {
            if (MagicalKatanaCrystalHandler.isMaterialized(weaponStack)) {
                actions.add(new SpecialAction(
                        MagicalKatanaActionPacket.Action.SHATTER,
                        new ItemStack(TheFourPrimitivesAndWeaponsModItems.MAGICAL_KATANA.get()),
                        "§c破壊"));
            } else if (MagicalKatanaCrystalHandler.isUnlocked(weaponStack)) {
                actions.add(new SpecialAction(
                        MagicalKatanaActionPacket.Action.SPAWN_CRYSTAL,
                        new ItemStack(TheFourPrimitivesAndWeaponsModItems.MAGICAL_KATANA.get()),
                        "§d結晶を出す"));
            }
        }
        return actions;
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
        List<SpecialAction> savedSpecials = specialActions;
        WheelMode savedMode = currentMode;

        reset();

        int weaponCount = savedWeapons.size();
        int total = weaponCount + savedSpecials.size();
        if (savedSelectedIndex >= 0 && savedSelectedIndex < total) {
            if (savedSelectedIndex < weaponCount) {
                // 通常 weapon entry
                DrawableWeaponInfo info = savedWeapons.get(savedSelectedIndex);
                sendPacket(savedMode, info);
            } else {
                // SpecialAction
                SpecialAction sp = savedSpecials.get(savedSelectedIndex - weaponCount);
                TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(
                        new MagicalKatanaActionPacket(sp.action));
            }
        } else {
            // デッドゾーンで離した or 鞘無し → RMessage デフォルト動作にフォールバック (サーバーのみ)
            TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new RMessage(0, 0));
        }

        if (wasWheelVisible) {
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().mouseHandler.grabMouse();
            });
        }
    }

    private static void sendPacket(WheelMode mode, DrawableWeaponInfo info) {
        if (mode == WheelMode.DRAW) {
            TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(
                new BattouFromSpecificSlotPacket(
                    info.location, info.curioSlotId, info.slotIndex
                )
            );
        } else {
            Minecraft mc = Minecraft.getInstance();
            int weaponHandIndex = determineWeaponHand(mc.player);
            TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(
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
     * TheFourPrimitivesAndWeaponsModKeyMappings.KeyEventListener.onClientTick から直接呼び出される。
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
        int rKeyValue = the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModKeyMappings.R.getKey().getValue();
        boolean stillHeld = InputConstants.isKeyDown(mc.getWindow().getWindow(), rKeyValue);
        if (!stillHeld) {
            onRKeyReleased();
            return;
        }

        // 0.5秒以上長押しでホイール表示開始 (マウス解放もこのタイミング)
        if (!wheelVisible && System.currentTimeMillis() - pressStartTime >= WHEEL_DELAY_MS) {
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
        int itemCount = drawableWeapons.size() + specialActions.size();
        if (itemCount == 0) {
            selectedIndex = -1;
            return;
        }
        // 候補が1個しかない場合は dead zone でも自動選択する
        // (長押ししただけで確定できるようにするため)
        if (itemCount == 1) {
            selectedIndex = 0;
            return;
        }

        double centerX = screenWidth / 2.0;
        double centerY = screenHeight / 2.0;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < 20.0) {
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
        specialActions = Collections.emptyList();
        currentMode = WheelMode.DRAW;
        pressStartTime = 0L;
    }
}
