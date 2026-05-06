package minecraftarmorweapon.client;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.network.SkillSelectionPacket;
import minecraftarmorweapon.skill.PlayerSkillData;
import minecraftarmorweapon.skill.PlayerSkillData.AttackSlot;
import minecraftarmorweapon.skill.SkillRegistry.MotionInfo;
import minecraftarmorweapon.skill.SkillRegistry;
import minecraftarmorweapon.skill.WeaponTypeRegistry;

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

import java.util.Collections;
import java.util.List;

/**
 * 弓スキル選択ホイール (Shift+左クリック長押し中に表示、マウス位置で選択、離して確定)。
 *
 * R キー納刀ホイール (WeaponWheelState) と同じ放射状 UI を流用。
 * 選択結果は SkillSelectionPacket.setTypeMotion で武器タイプ別モーションとして保存される。
 */
@OnlyIn(Dist.CLIENT)
public class BowSkillWheelState {

    private static final int HOLD_THRESHOLD_TICKS = 5;
    private static final double DEAD_ZONE = 20.0;

    private static boolean attackHeld = false;
    private static long pressStartTick = -1;
    private static boolean wheelVisible = false;
    private static int selectedIndex = -1;
    private static List<MotionInfo> motions = Collections.emptyList();
    private static String typeId = null;

    public static boolean isWheelVisible() { return wheelVisible; }
    public static int getSelectedIndex() { return selectedIndex; }
    public static List<MotionInfo> getMotions() { return motions; }
    public static String getTypeId() { return typeId; }

    /** Shift+Attack 押下開始 */
    private static boolean onPressed(Player player, ItemStack bow) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        WeaponTypeRegistry.WeaponTypeData typeData = WeaponTypeRegistry.getTypeForItem(bow);
        if (typeData == null) return false;

        List<MotionInfo> avail = SkillRegistry.getAvailableMotionsForWeapon(AttackSlot.RIGHT_CLICK, bow);
        if (avail.size() < 2) return false;

        typeId = typeData.getId();
        motions = avail;
        attackHeld = true;
        pressStartTick = mc.level.getGameTime();
        selectedIndex = -1;
        return true;
    }

    /** Shift+Attack 解除 — R キー納刀ホイールと同じ挙動:
     *  ホイール表示済み + 有効選択 → そのモーションへ
     *  ホイール未表示 (短押し) → 次のモーションへ循環
     */
    private static void onReleased() {
        if (!attackHeld) return;

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
            // 短押し: 現在のモーションから次へ循環
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
        MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
            SkillSelectionPacket.setTypeMotion(typeId, AttackSlot.RIGHT_CLICK, motionId));
        Player p = Minecraft.getInstance().player;
        if (p != null) {
            p.getCapability(PlayerSkillData.SKILL_CAPABILITY).ifPresent(sd ->
                sd.setTypeMotion(typeId, AttackSlot.RIGHT_CLICK, motionId));
            // 短いフィードバック表示
            MotionInfo info = SkillRegistry.getById(motionId);
            if (info != null) {
                p.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(info.translationKey()),
                    true);
            }
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            if (attackHeld) reset();
            return;
        }
        if (mc.screen != null) {
            if (attackHeld) {
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

        boolean shiftDown = player.isShiftKeyDown();
        boolean attackDown = mc.options.keyAttack.isDown();
        boolean activate = isBow && shiftDown && attackDown;

        if (!attackHeld && activate) {
            onPressed(player, main);
        } else if (attackHeld && !activate) {
            onReleased();
            return;
        }

        if (!attackHeld) return;

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
        attackHeld = false;
        pressStartTick = -1;
        wheelVisible = false;
        selectedIndex = -1;
        motions = Collections.emptyList();
        typeId = null;
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
