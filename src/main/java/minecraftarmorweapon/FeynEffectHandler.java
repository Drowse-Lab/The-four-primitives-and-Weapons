package minecraftarmorweapon;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber
public class FeynEffectHandler {

    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ATTACK_MODIFIER_UUID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (player == null || player.level.isClientSide) return;

        boolean hasCursedItem = hasCursedFeyn(player.getMainHandItem()) || hasCursedFeyn(player.getOffhandItem());

        if (hasCursedItem) {
            // 最大体力：-3
            if (!hasModifier(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER_UUID)) {
                applyModifier(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER_UUID, "Cursed Health Down", -3.0);
            }
            // 攻撃力：+6
            if (!hasModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER_UUID)) {
                applyModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER_UUID, "Cursed Attack Up", 6.0);
            }
        } else {
            // 持ってなければ解除
            removeModifier(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER_UUID);
            removeModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER_UUID);
        }

        // クライアント同期
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundUpdateAttributesPacket(
                    player.getId(), player.getAttributes().getSyncableAttributes()
            ));
        }
    }

    private static boolean hasCursedFeyn(ItemStack stack) {
        return stack.hasTag() && "cursed".equals(stack.getTag().getString("Feyn"));
    }

    private static boolean hasModifier(Player player, Attribute attribute, UUID uuid) {
        AttributeInstance instance = player.getAttribute(attribute);
        return instance != null && instance.getModifier(uuid) != null;
    }

    private static void applyModifier(Player player, Attribute attribute, UUID uuid, String name, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.getModifier(uuid) == null) {
            AttributeModifier modifier = new AttributeModifier(uuid, name, value, AttributeModifier.Operation.ADDITION);
            instance.addTransientModifier(modifier); // Transient にして、セッション限り
        }
    }

    private static void removeModifier(Player player, Attribute attribute, UUID uuid) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.getModifier(uuid) != null) {
            instance.removeModifier(uuid);
        }
    }
}
