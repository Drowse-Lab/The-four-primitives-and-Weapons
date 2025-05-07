// package minecraftarmorweapon.client.gui;

// import com.mojang.blaze3d.vertex.PoseStack;
// import net.minecraft.client.gui.screens.Screen;
// import net.minecraft.network.chat.Component;

// public class GuideBookScreen extends Screen {
//     private final String title;
//     private final String content;

//     public GuideBookScreen(String title, String content) {
//         super(Component.literal(title));
//         this.title = title;
//         this.content = content;
//     }

//     @Override
//     protected void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
//         super.render(poseStack, mouseX, mouseY, partialTicks);
//         this.font.draw(poseStack, this.title, this.width / 2 - this.font.width(this.title) / 2, 20, 0xFFFFFF);
//         this.font.drawWordWrap(Component.literal(this.content), this.width / 2 - 100, 40, 200, 0xCCCCCC);
//     }
// }
