package the_four_primitives_and_weapons.client;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.network.SkillSelectionPacket;
import the_four_primitives_and_weapons.skill.PlayerSkillData;
import the_four_primitives_and_weapons.skill.PlayerSkillData.AttackSlot;
import the_four_primitives_and_weapons.skill.SkillRegistry.MotionInfo;
import the_four_primitives_and_weapons.skill.SkillRegistry;
import the_four_primitives_and_weapons.skill.WeaponTypeRegistry;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.List;

/**
 * 弓スキル選択ホイール。
 * 入力源は 2 通り:
 *  - SHIFT_ATTACK: 弓所持中に Shift+左クリック長押し
 *  - R_KEY: 弓所持中に R キー長押し (R キーホイールと同じ操作感)
 *
 * 操作: 短押し → 次のスキルへ循環 / 長押し → ホイール表示, マウス位置で選択, 離して確定。
 */
@OnlyIn(Dist.CLIENT)
public class BowSkillWheelState {

    public enum InputMode { SHIFT_ATTACK, R_KEY }

    private static final int HOLD_THRESHOLD_TICKS = 5;
    private static final double DEAD_ZONE = 20.0;

    private static boolean held = false;
    private static InputMode inputMode = InputMode.SHIFT_ATTACK;
    private static long pressStartTick = -1;
    private static boolean wheelVisible = false;
    private static int selectedIndex = -1;
    private static List<MotionInfo> motions = Collections.emptyList();
    private static String typeId = null;

    public static boolean isWheelVisible() { return wheelVisible; }
    public static int getSelectedIndex() { return selectedIndex; }
    public static List<MotionInfo> getMotions() { return motions; }
    public static String getTypeId() { return typeId; }
    public static boolean isHeld() { return held; }

    /**
     * R キーから明示的に開く (TheFourPrimitivesAndWeaponsModKeyMappings から呼び出す)。
     * @return true=処理した (R キー側はこれ以降の処理を行わない), false=弓未所持等で処理しなかった
     */
    public static boolean openOnRKey() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return false;
        ItemStack main = player.getMainHandItem();
        if (!(main.getItem() instanceof BowItem || main.getItem() instanceof CrossbowItem)) return false;
        if (held) return true; // 既に押下中ならそのまま継続

        if (onPressed(player, main)) {
            inputMode = InputMode.R_KEY;
            return true;
        }
        return false;
    }

    /** R キー解除 (TheFourPrimitivesAndWeaponsModKeyMappings から呼び出す) */
    public static void releaseOnRKey() {
        if (held && inputMode == InputMode.R_KEY) {
            onReleased();
        }
    }

    private static boolean onPressed(Player player, ItemStack bow) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        WeaponTypeRegistry.WeaponTypeData typeData = WeaponTypeRegistry.getTypeForItem(bow);
        if (typeData == null) return false;

        List<MotionInfo> avail = SkillRegistry.getAvailableMotionsForWeapon(AttackSlot.RIGHT_CLICK, bow);
        if (avail.size() < 2) return false;

        typeId = typeData.getId();
        motions = avail;
        held = true;
        pressStartTick = mc.level.getGameTime();
        selectedIndex = -1;
        return true;
    }

    private static void onReleased() {
        if (!held) return;

        boolean wasVisible = wheelVisible;
        int savedIdx = selectedIndex;
        List<MotionInfo> savedMotions = motions;
        String savedType = typeId;

        reset();

        if (savedType == null || savedMotions.isEmpty()) {
            if (wasVisible) Minecraft.getInstance().execute(() -> Minecraft.getInstance().mouseHandler.grabMouse());
            return;
        }

        String chosen = null;
        if (wasVisible && savedIdx >= 0 && savedIdx < savedMotions.size()) {
            chosen = savedMotions.get(savedIdx).getId();
        } else if (!wasVisible) {
            chosen = nextMotionId(savedType, savedMotions);
        }

        if (chosen != null) sendSelection(savedType, chosen);

        if (wasVisible) {
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().mouseHandler.grabMouse());
        }
    }

    private static String nextMotionId(String typeId, List<MotionInfo> avail) {
        Player p = Minecraft.getInstance().player;
        if (p == null || avail.isEmpty()) return avail.isEmpty() ? null : avail.get(0).getId();

        ItemStack held = p.getMainHandItem();
        String current = p.getCapability(PlayerSkillData.SKILL_CAPABILITY)
                .map(sd -> sd.getMotionForWeapon(AttackSlot.RIGHT_CLICK, held))
                .orElse(null);

        int curIdx = -1;
        if (current != null) {
            for (int i = 0; i < avail.size(); i++) {
                if (current.equals(avail.get(i).getId())) { curIdx = i; break; }
            }
        }
        return avail.get((curIdx + 1) % avail.size()).getId();
    }

    private static void sendSelection(String typeId, String motionId) {
        TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(
            SkillSelectionPacket.setTypeMotion(typeId, AttackSlot.RIGHT_CLICK, motionId));
        Player p = Minecraft.getInstance().player;
        if (p != null) {
            p.getCapability(PlayerSkillData.SKILL_CAPABILITY).ifPresent(sd ->
                sd.setTypeMotion(typeId, AttackSlot.RIGHT_CLICK, motionId));
            MotionInfo info = SkillRegistry.getById(motionId);
            if (info != null) {
                p.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(info.translationKey()),
                    true);
            }
        }
    }

    /** GLFW で keyAttack の物理状態を確実に検出 (KeyMapping.isDown は不安定なため) */
    private static boolean isAttackKeyHeldGlfw(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        InputConstants.Key key = mc.options.keyAttack.getKey();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(window, key.getValue());
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            if (held) reset();
            return;
        }
        if (mc.screen != null) {
            if (held) {
                boolean wasVisible = wheelVisible;
                reset();
                if (wasVisible) {
                    Minecraft.getInstance().execute(() -> Minecraft.getInstance().mouseHandler.grabMouse());
                }
            }
            return;
        }

        ItemStack main = player.getMainHandItem();
        boolean isBow = main.getItem() instanceof BowItem || main.getItem() instanceof CrossbowItem;

        // SHIFT_ATTACK モードの場合、毎 tick で押下状態を判定 (R_KEY モードはキー側で開閉)
        if (!held || inputMode == InputMode.SHIFT_ATTACK) {
            boolean shiftDown = player.isShiftKeyDown();
            boolean attackDown = isAttackKeyHeldGlfw(mc);
            boolean activate = isBow && shiftDown && attackDown;

            if (!held && activate) {
                inputMode = InputMode.SHIFT_ATTACK;
                onPressed(player, main);
            } else if (held && inputMode == InputMode.SHIFT_ATTACK && !activate) {
                onReleased();
                return;
            }
        }

        if (!held) return;

        // R キー押下中も継続的にキー状態を確認 (フォーカス外などで取り逃しがあるため)
        if (inputMode == InputMode.R_KEY) {
            int rKey = the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModKeyMappings.R.getKey().getValue();
            boolean stillHeld = InputConstants.isKeyDown(mc.getWindow().getWindow(), rKey);
            if (!stillHeld) {
                onReleased();
                return;
            }
        }

        long ticksHeld = mc.level.getGameTime() - pressStartTick;
        if (ticksHeld >= HOLD_THRESHOLD_TICKS && !wheelVisible) {
            wheelVisible = true;
            mc.mouseHandler.releaseMouse();
        }

        if (wheelVisible) {
            double mx = mc.mouseHandler.xpos() *
                (double) mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
            double my = mc.mouseHandler.ypos() *
                (double) mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
            updateSelection(mx, my,
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight());
        }
    }

    private static void updateSelection(double mouseX, double mouseY, int sw, int sh) {
        double cx = sw / 2.0;
        double cy = sh / 2.0;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);

        int n = motions.size();
        if (dist < DEAD_ZONE || n == 0) {
            selectedIndex = -1;
            return;
        }

        double angle = Math.atan2(dx, -dy);
        if (angle < 0) angle += 2 * Math.PI;
        double seg = 2.0 * Math.PI / n;
        double shifted = angle + seg / 2.0;
        if (shifted >= 2 * Math.PI) shifted -= 2 * Math.PI;
        selectedIndex = (int) (shifted / seg);
        if (selectedIndex >= n) selectedIndex = n - 1;
    }

    private static void reset() {
        held = false;
        pressStartTick = -1;
        wheelVisible = false;
        selectedIndex = -1;
        motions = Collections.emptyList();
        typeId = null;
        inputMode = InputMode.SHIFT_ATTACK;
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class WheelTicker {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            BowSkillWheelState.tick();
        }
    }
}
