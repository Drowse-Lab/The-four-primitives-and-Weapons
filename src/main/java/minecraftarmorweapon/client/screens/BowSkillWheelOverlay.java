package minecraftarmorweapon.client.screens;

import minecraftarmorweapon.client.BowSkillWheelState;
import minecraftarmorweapon.skill.SkillRegistry.MotionInfo;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
public class BowSkillWheelOverlay {

    private static final float WHEEL_RADIUS = 60.0f;
    private static final float ICON_SIZE = 16.0f;
    private static final int BG_COLOR = 0xAA1A1A2E;
    private static final int SELECTED_COLOR = 0x884CAF50;
    private static final int CIRCLE_SEGMENTS = 64;

    private static ItemStack iconFor(String motionId) {
        return switch (motionId) {
            case "bow_power_shot" -> new ItemStack(Items.SPECTRAL_ARROW);
            case "bow_explosive"  -> new ItemStack(Items.TNT);
            case "bow_pierce"     -> new ItemStack(Items.ARROW);
            case "bow_heavy_blow" -> new ItemStack(Items.IRON_INGOT);
            case "bow_homing"     -> new ItemStack(Items.COMPASS);
            case "bow_rapid_fire" -> new ItemStack(Items.FEATHER);
            case "bow_arrow_rain" -> new ItemStack(Items.WATER_BUCKET);
            case "bow_quick_draw" -> new ItemStack(Items.SUGAR);
            case "bow_wind"       -> new ItemStack(Items.PHANTOM_MEMBRANE);
            case "dodge"          -> new ItemStack(Items.LEATHER_BOOTS);
            case "none_right"     -> new ItemStack(Items.BOW);
            default               -> new ItemStack(Items.BOW);
        };
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (!BowSkillWheelState.isWheelVisible()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        List<MotionInfo> motions = BowSkillWheelState.getMotions();
        int count = motions.size();
        if (count == 0) return;

        int selected = BowSkillWheelState.getSelectedIndex();

        GuiGraphics g = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();
        int cx = w / 2;
        int cy = h / 2;

        PoseStack pose = g.pose();

        renderCircle(pose, cx, cy, WHEEL_RADIUS + 20, BG_COLOR);

        if (selected >= 0 && selected < count) {
            double segStart = (2.0 * Math.PI * selected / count) - Math.PI / 2.0 - Math.PI / count;
            double segEnd = segStart + 2.0 * Math.PI / count;
            renderArc(pose, cx, cy, WHEEL_RADIUS + 20, segStart, segEnd, SELECTED_COLOR);
        }

        for (int i = 0; i < count; i++) {
            MotionInfo info = motions.get(i);
            double angle = (2.0 * Math.PI * i / count) - Math.PI / 2.0;
            int iconX = (int) (cx + Math.cos(angle) * WHEEL_RADIUS - ICON_SIZE / 2);
            int iconY = (int) (cy + Math.sin(angle) * WHEEL_RADIUS - ICON_SIZE / 2);

            ItemStack displayIcon = iconFor(info.getId());

            if (i == selected) {
                pose.pushPose();
                pose.translate(iconX + ICON_SIZE / 2, iconY + ICON_SIZE / 2, 0);
                pose.scale(1.5f, 1.5f, 1.0f);
                pose.translate(-(ICON_SIZE / 2), -(ICON_SIZE / 2), 0);
                g.renderItem(displayIcon, 0, 0);
                pose.popPose();
            } else {
                g.renderItem(displayIcon, iconX, iconY);
            }

            String name = Component.translatable(info.translationKey()).getString();
            int nameColor = (i == selected) ? 0xFFFFFF : 0xAAAAAA;
            int nameY = iconY + (int) ICON_SIZE + 2;
            g.drawCenteredString(mc.font, name, iconX + (int)(ICON_SIZE / 2), nameY, nameColor);
        }

        if (selected >= 0 && selected < count) {
            String desc = Component.translatable(motions.get(selected).descriptionTranslationKey()).getString();
            int dy = cy + (int) WHEEL_RADIUS + 40;
            g.drawCenteredString(mc.font, desc, cx, dy, 0xAAAAAA);
        }

        g.fill(cx - 1, cy - 5, cx + 1, cy + 5, 0xAAFFFFFF);
        g.fill(cx - 5, cy - 1, cx + 5, cy + 1, 0xAAFFFFFF);
    }

    private static void renderCircle(PoseStack pose, int cx, int cy, float radius, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float gn = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = pose.last().pose();
        bb.vertex(m, cx, cy, 0).color(r, gn, b, a).endVertex();
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            double angle = 2.0 * Math.PI * i / CIRCLE_SEGMENTS;
            float px = cx + (float) (Math.cos(angle) * radius);
            float py = cy + (float) (Math.sin(angle) * radius);
            bb.vertex(m, px, py, 0).color(r, gn, b, a).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.disableBlend();
    }

    private static void renderArc(PoseStack pose, int cx, int cy, float radius,
                                   double startAngle, double endAngle, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float gn = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = pose.last().pose();
        bb.vertex(m, cx, cy, 0).color(r, gn, b, a).endVertex();
        int arcSegments = 32;
        for (int i = 0; i <= arcSegments; i++) {
            double angle = startAngle + (endAngle - startAngle) * i / arcSegments;
            float px = cx + (float) (Math.cos(angle) * radius);
            float py = cy + (float) (Math.sin(angle) * radius);
            bb.vertex(m, px, py, 0).color(r, gn, b, a).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.disableBlend();
    }
}
