package minecraftarmorweapon.client.event;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.item.TestBowItem;
import minecraftarmorweapon.network.BowSkillPacket;
import minecraftarmorweapon.skill.BowSkill;
import minecraftarmorweapon.skill.BowSkillData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 弓スキル — クライアント側入力検出 & HUD。
 *
 *   スニーク + マウスホイール   → スロット切替パケット送信
 *   左クリック (押し始め)        → INSTANT即発動 / CHARGE開始
 *   左クリック (離し)            → CHARGE発動 (押下時間で威力)
 *   アクションバー               → 現在選択スキル名を表示
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID, value = Dist.CLIENT)
public class BowSkillClientHandler {

    private static final int CHARGE_MAX_TICKS = 40; // 2秒で最大
    private static boolean wasLeftPressed = false;
    private static boolean leftHeld = false;
    private static int holdTicks = 0;
    private static BowSkill heldSkill = BowSkill.NONE;

    /** Sneak+Wheelで装備スキル切替 */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isShiftKeyDown()) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof TestBowItem)) return;

        double dy = event.getScrollDelta();
        if (dy == 0) return;
        MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
            new BowSkillPacket(BowSkillPacket.ACTION_CYCLE, dy > 0 ? 1f : -1f));
        event.setCanceled(true);
    }

    /** 左クリックでスキル発動。バニラの近接攻撃モーションは弓の場合は元々無効。 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null) return;

        ItemStack stack = player.getMainHandItem();
        boolean isBow = stack.getItem() instanceof TestBowItem;
        boolean isLeftHeld = isBow && mc.options.keyAttack.isDown();

        if (!isBow) {
            wasLeftPressed = false;
            leftHeld = false;
            holdTicks = 0;
            return;
        }

        // 右クリックで弓を引いている時は左クリックスキルを無効化
        if (player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            wasLeftPressed = isLeftHeld;
            leftHeld = false;
            holdTicks = 0;
            return;
        }

        BowSkill skill = BowSkillData.getSelected(stack);

        // 押し始めの瞬間
        if (isLeftHeld && !wasLeftPressed) {
            heldSkill = skill;
            if (skill.type == BowSkill.Type.INSTANT) {
                MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                    new BowSkillPacket(BowSkillPacket.ACTION_USE, 1.0f));
            } else if (skill.type == BowSkill.Type.CHARGE) {
                leftHeld = true;
                holdTicks = 0;
            }
            // PASSIVEは何もしない (右クリック射撃に効果が乗る)
        }
        // チャージ継続
        else if (isLeftHeld && leftHeld) {
            holdTicks++;
        }
        // チャージリリース
        else if (!isLeftHeld && leftHeld) {
            float pct = Math.min(1f, (float) holdTicks / CHARGE_MAX_TICKS);
            MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                new BowSkillPacket(BowSkillPacket.ACTION_USE, pct));
            leftHeld = false;
            holdTicks = 0;
        }

        wasLeftPressed = isLeftHeld;
    }

    /** HUDに現在選択中スキルを表示 (ホットバー上) */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().getPath().equals("hotbar")) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof TestBowItem)) return;

        BowSkill skill = BowSkillData.getSelected(stack);
        int idx = BowSkillData.getIndex(stack);
        String text = "[" + (idx + 1) + "/" + BowSkillData.LOADOUT_SIZE + "] "
            + skill.coloredName() + " §7(" + skill.type + ")";
        if (leftHeld && heldSkill.type == BowSkill.Type.CHARGE) {
            int pct = (int) Math.min(100, holdTicks * 100f / CHARGE_MAX_TICKS);
            text += " §e[" + pct + "%]";
        }

        GuiGraphics g = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int x = sw / 2 - mc.font.width(text) / 2;
        int y = mc.getWindow().getGuiScaledHeight() - 60;
        g.drawString(mc.font, Component.literal(text), x, y, 0xFFFFFF, true);
    }
}
