package minecraftarmorweapon;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FeynEffectHandler {

    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("a1b2c3d4-5678-90ab-cdef-1234567890ab");
    private static final UUID ATTACK_MODIFIER_UUID = UUID.fromString("abcd1234-5678-90ab-cdef-1234567890ef");

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;

        if (player == null || player.level.isClientSide) return;

        // 最大体力の変更
        if (hasModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, HEALTH_MODIFIER_UUID)) {
            removeModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, HEALTH_MODIFIER_UUID);
        } else {
            applyModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, HEALTH_MODIFIER_UUID, "Cursed Health Down", -4.0);
        }

        // 攻撃力の変更
        if (hasModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER_UUID)) {
            removeModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER_UUID);
        } else {
            applyModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER_UUID, "Cursed Attack Up", 6.0);
        }
    }

    private static boolean hasModifier(Player player, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID modifierUUID) {
        return player.getAttribute(attribute).getModifiers().stream()
                .anyMatch(modifier -> modifier.getId().equals(modifierUUID));
    }

    private static void applyModifier(Player player, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID modifierUUID, String name, double amount) {
        AttributeModifier modifier = new AttributeModifier(modifierUUID, name, amount, AttributeModifier.Operation.ADDITION);
        player.getAttribute(attribute).addTransientModifier(modifier);
    }

    private static void removeModifier(Player player, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID modifierUUID) {
        player.getAttribute(attribute).removeModifier(modifierUUID);
    }
}