package the_four_primitives_and_weapons.client.event;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.client.gui.SkillSelectionScreen;
import the_four_primitives_and_weapons.network.OpenSkillScreenPacket;

public class SkillScreenKeyHandler {

    public static final KeyMapping SKILL_SCREEN_KEY = new KeyMapping(
        "key.the_four_primitives_and_weapons.skill_screen",
        GLFW.GLFW_KEY_K,
        "key.categories.the_four_primitives_and_weapons"
    );

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class KeyRegister {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(SKILL_SCREEN_KEY);
        }
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static class KeyListener {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            if (SKILL_SCREEN_KEY.consumeClick()) {
                if (mc.screen instanceof SkillSelectionScreen) {
                    mc.player.closeContainer();
                } else if (mc.screen == null) {
                    TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new OpenSkillScreenPacket());
                }
            }
        }
    }
}
