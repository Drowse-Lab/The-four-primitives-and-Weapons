package minecraftarmorweapon.skill;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.util.FakePlayer;

/**
 * スキルCapabilityの登録・アタッチ・クローン処理
 */
public class SkillCapabilityHandler {

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.register(PlayerSkillData.SkillStorage.class);
        }
    }

    @Mod.EventBusSubscriber
    public static class Attach {
        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player && !(event.getObject() instanceof FakePlayer)) {
                event.addCapability(
                    new ResourceLocation("minecraft_armor_weapon", "player_skills"),
                    new PlayerSkillData.SkillProvider()
                );
            }
        }

        @SubscribeEvent
        public static void onClonePlayer(PlayerEvent.Clone event) {
            event.getOriginal().revive();
            event.getOriginal().getCapability(PlayerSkillData.SKILL_CAPABILITY).ifPresent(original -> {
                event.getEntity().getCapability(PlayerSkillData.SKILL_CAPABILITY).ifPresent(clone -> {
                    clone.deserializeNBT(original.serializeNBT());
                });
            });
        }
    }
}
