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

    private static final float WHEEL_RADIUS = 70.0f;
    private static final int BG_COLOR = 0xAA1A1A2E;
    private static final int SELECTED_COLOR = 0x884CAF50;
    private static final int CIRCLE_SEGMENTS = 64;

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

        renderCircle(pose, cx, cy, WHEEL_RADIUS + 25, BG_COLOR);

        if (selected >= 0 && selected < count) {
            double segStart = (2.0 * Math.PI * selected / count) - Math.PI / 2.0 - Math.PI / count;
            double segEnd = segStart + 2.0 * Math.PI / count;
            renderArc(pose, cx, cy, WHEEL_RADIUS + 25, segStart, segEnd, SELECTED_COLOR);
        }

        for (int i = 0; i < count; i++) {
            MotionInfo info = motions.get(i);
            double angle = (2.0 * Math.PI * i / count) - Math.PI / 2.0;
            int ix = (int) (cx + Math.cos(angle) * WHEEL_RADIUS);
            int iy = (int) (cy + Math.sin(angle) * WHEEL_RADIUS);

            String name = Component.translatable(info.translationKey()).getString();
            int color = (i == selected) ? 0xFFFFFF : 0xCCCCCC;
            int prefix = (i == selected) ? 0xFFFFD700 : 0x888888;

            g.fill(ix - 4, iy - 4, ix + 4, iy + 4, 0xFF000000 | prefix);

            int nameY = iy + 8;
            g.drawCenteredString(mc.font, name, ix, nameY, color);
        }

        if (selected >= 0 && selected < count) {
            String desc = Component.translatable(motions.get(selected).descriptionTranslationKey()).getString();
            int dy = cy + (int) WHEEL_RADIUS + 50;
            g.drawCenteredString(mc.font, desc, cx, dy, 0xAAAAAA);
        }

        String header = Component.translatable("motion.minecraft_armor_weapon.bow_wheel.title").getString();
        g.drawCenteredString(mc.font, header, cx, cy - (int) WHEEL_RADIUS - 35, 0xFFFFD700);

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
