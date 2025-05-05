package minecraftarmorweapon;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon", value = Dist.CLIENT)
public class FeynTooltipRenderer {
//画面の位置からずれたsanctedという文字
    // @SubscribeEvent
    // public static void onTooltipRender(RenderTooltipEvent.Pre event) {
    //     ItemStack stack = event.getItemStack();
    //     if (stack == null || !stack.hasTag()) return;

    //     String feyn = stack.getTag().getString("Feyn");
    //     if (!"sancted".equals(feyn)) return;

    //     PoseStack poseStack = event.getPoseStack();
    //     Font font = Minecraft.getInstance().font;

    //     // アニメーション用の色（時間で変化）
    //     long time = System.currentTimeMillis();
    //     float phase = (time % 1000) / 1000.0f * 2 * (float)Math.PI;
    //     int r = 220 + (int)(35 * Math.sin(phase));
    //     int g = 220 + (int)(35 * Math.cos(phase));
    //     int b = 180 + (int)(55 * Math.sin(phase + 1));

    //     int color = (r << 16) | (g << 8) | b;

    //     // 描画位置（背景の少し上に描画）
    //     int x = event.getX() + 0;
    //     int y = event.getY() - 0;

    //     font.draw(poseStack, Component.literal("sancted"), x, y, color);
    // }
}
