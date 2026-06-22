package the_four_primitives_and_weapons.client.screens;

import the_four_primitives_and_weapons.client.WeaponWheelState;
import the_four_primitives_and_weapons.client.WeaponWheelState.WheelMode;
import the_four_primitives_and_weapons.util.CuriosScabbardHelper.DrawableWeaponInfo;
import the_four_primitives_and_weapons.util.CuriosScabbardHelper.ScabbardLocation;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;

import org.joml.Matrix4f;

import java.util.List;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class WeaponWheelOverlay {

    private static final float WHEEL_RADIUS = 60.0f;
    private static final float ICON_SIZE = 16.0f;
    private static final int BG_COLOR = 0xAA1A1A2E;
    private static final int SELECTED_COLOR = 0x884CAF50;
    private static final int CIRCLE_SEGMENTS = 64;

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (!WeaponWheelState.isWheelVisible()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics g = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();
        int centerX = w / 2;
        int centerY = h / 2;

        List<DrawableWeaponInfo> weapons = WeaponWheelState.getDrawableWeapons();
        List<WeaponWheelState.SpecialAction> specials = WeaponWheelState.getSpecialActions();
        int weaponCount = weapons.size();
        int specialCount = specials.size();
        int count = weaponCount + specialCount;
        int selected = WeaponWheelState.getSelectedIndex();

        PoseStack poseStack = g.pose();

        // 半透明の円背景を描画
        renderCircle(poseStack, centerX, centerY, WHEEL_RADIUS + 20, BG_COLOR);

        // 選択中のセグメントをハイライト
        if (selected >= 0 && selected < count) {
            double segStart = (2.0 * Math.PI * selected / count) - Math.PI / 2.0 - Math.PI / count;
            double segEnd = segStart + 2.0 * Math.PI / count;
            renderArc(poseStack, centerX, centerY, WHEEL_RADIUS + 20, segStart, segEnd, SELECTED_COLOR);
        }

        WheelMode mode = WeaponWheelState.getCurrentMode();

        // 各アイコンを円周上に配置 ( 通常 weapon + special action 順 )
        for (int i = 0; i < count; i++) {
            double angle = (2.0 * Math.PI * i / count) - Math.PI / 2.0;
            int iconX = (int) (centerX + Math.cos(angle) * WHEEL_RADIUS - ICON_SIZE / 2);
            int iconY = (int) (centerY + Math.sin(angle) * WHEEL_RADIUS - ICON_SIZE / 2);

            ItemStack displayIcon;
            Component itemName;
            String slotLabel;

            if (i < weaponCount) {
                DrawableWeaponInfo info = weapons.get(i);
                displayIcon = (mode == WheelMode.SHEATH) ? info.scabbardStack : info.weaponStack;
                itemName = displayIcon.getHoverName();
                slotLabel = getSlotLabel(info);
            } else {
                WeaponWheelState.SpecialAction sp = specials.get(i - weaponCount);
                displayIcon = sp.icon;
                itemName = Component.literal(sp.label);
                slotLabel = "特殊";
            }

            if (i == selected) {
                poseStack.pushPose();
                poseStack.translate(iconX + ICON_SIZE / 2, iconY + ICON_SIZE / 2, 0);
                poseStack.scale(1.5f, 1.5f, 1.0f);
                poseStack.translate(-(ICON_SIZE / 2), -(ICON_SIZE / 2), 0);
                g.renderItem(displayIcon, 0, 0);
                poseStack.popPose();
            } else {
                g.renderItem(displayIcon, iconX, iconY);
            }

            int nameColor = (i == selected) ? 0xFFFFFF : 0xAAAAAAA;
            int nameY = iconY + (int) ICON_SIZE + 2;
            g.drawCenteredString(mc.font, itemName, iconX + (int)(ICON_SIZE / 2), nameY, nameColor);

            int labelY = nameY + 10;
            g.drawCenteredString(mc.font, Component.literal("§7[" + slotLabel + "]"),
                iconX + (int)(ICON_SIZE / 2), labelY, 0x888888);
        }

        // 中央の十字線
        g.fill(centerX - 1, centerY - 5, centerX + 1, centerY + 5, 0xAAFFFFFF);
        g.fill(centerX - 5, centerY - 1, centerX + 5, centerY + 1, 0xAAFFFFFF);
    }

    private static String getSlotLabel(DrawableWeaponInfo info) {
        switch (info.location) {
            case CURIOS:
                if ("belt".equals(info.curioSlotId)) return "ベルト";
                if ("back".equals(info.curioSlotId)) return "背中";
                return info.curioSlotId != null ? info.curioSlotId : "Curios";
            case HAND:
                return info.slotIndex == 0 ? "メインハンド" : "オフハンド";
            case INVENTORY:
                return "インベントリ";
            case COVERUP:
                return "CoverUp";
            default:
                return "";
        }
    }

    private static void renderCircle(PoseStack poseStack, int cx, int cy, float radius, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float green = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f matrix = poseStack.last().pose();
        builder.vertex(matrix, cx, cy, 0).color(r, green, b, a).endVertex();

        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            double angle = 2.0 * Math.PI * i / CIRCLE_SEGMENTS;
            float px = cx + (float) (Math.cos(angle) * radius);
            float py = cy + (float) (Math.sin(angle) * radius);
            builder.vertex(matrix, px, py, 0).color(r, green, b, a).endVertex();
        }

        BufferUploader.drawWithShader(builder.end());
        RenderSystem.disableBlend();
    }

    private static void renderArc(PoseStack poseStack, int cx, int cy, float radius,
                                   double startAngle, double endAngle, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float green = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f matrix = poseStack.last().pose();
        builder.vertex(matrix, cx, cy, 0).color(r, green, b, a).endVertex();

        int arcSegments = 32;
        for (int i = 0; i <= arcSegments; i++) {
            double angle = startAngle + (endAngle - startAngle) * i / arcSegments;
            float px = cx + (float) (Math.cos(angle) * radius);
            float py = cy + (float) (Math.sin(angle) * radius);
            builder.vertex(matrix, px, py, 0).color(r, green, b, a).endVertex();
        }

        BufferUploader.drawWithShader(builder.end());
        RenderSystem.disableBlend();
    }
}
