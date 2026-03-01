package minecraftarmorweapon.client.event;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.client.gui.SkillSelectionScreen;
import minecraftarmorweapon.network.OpenSkillScreenPacket;

public class SkillScreenKeyHandler {

    public static final KeyMapping SKILL_SCREEN_KEY = new KeyMapping(
        "key.minecraft_armor_weapon.skill_screen",
        GLFW.GLFW_KEY_K,
        "key.categories.minecraft_armor_weapon"
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
                    MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(new OpenSkillScreenPacket());
                }
            }
        }
    }
}
