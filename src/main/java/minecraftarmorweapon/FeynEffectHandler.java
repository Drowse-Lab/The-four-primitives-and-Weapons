package minecraftarmorweapon;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber
public class FeynEffectHandler {

    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ATTACK_MODIFIER_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (player == null || player.level.isClientSide) return;

        boolean hasCursedItem = hasCursedFeyn(player.getMainHandItem()) || hasCursedFeyn(player.getOffhandItem());

        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);

        if (hasCursedItem) {
            if (healthAttr != null) {
                removeModifier(healthAttr, HEALTH_MODIFIER_UUID);
                AttributeModifier healthMod = new AttributeModifier(
                        HEALTH_MODIFIER_UUID, "Cursed Health Down", -3.0, AttributeModifier.Operation.ADDITION);
                healthAttr.addPermanentModifier(healthMod);
            }

            if (attackAttr != null) {
                removeModifier(attackAttr, ATTACK_MODIFIER_UUID);
                AttributeModifier attackMod = new AttributeModifier(
                        ATTACK_MODIFIER_UUID, "Cursed Attack Boost", 6.0, AttributeModifier.Operation.ADDITION);
                attackAttr.addPermanentModifier(attackMod);
            }
        } else {
            if (healthAttr != null) removeModifier(healthAttr, HEALTH_MODIFIER_UUID);
            if (attackAttr != null) removeModifier(attackAttr, ATTACK_MODIFIER_UUID);
        }

        // クライアントと同期
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundUpdateAttributesPacket(
                    player.getId(), player.getAttributes().getSyncableAttributes()
            ));
        }
    }

    private static boolean hasCursedFeyn(ItemStack stack) {
        if (stack == null || !stack.hasTag()) return false;
        CompoundTag tag = stack.getTag();
        return "cursed".equals(tag.getString("Feyn"));
    }

    private static void removeModifier(AttributeInstance attr, UUID uuid) {
        AttributeModifier mod = attr.getModifier(uuid);
        if (mod != null) attr.removeModifier(mod);
    }
}
