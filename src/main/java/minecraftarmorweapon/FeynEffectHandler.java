package minecraftarmorweapon;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@SubscribeEvent(priority = EventPriority.HIGHEST)
public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
    Player player = event.player;

    if (player == null || player.level.isClientSide) return;

    System.out.println("PlayerTickEvent is triggered for player: " + player.getName().getString());

    // 最大体力の変更
    if (hasModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, HEALTH_MODIFIER_UUID)) {
        removeModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, HEALTH_MODIFIER_UUID);
    } else {
        applyModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, HEALTH_MODIFIER_UUID, "Cursed Health Down", -4.0);
        System.out.println("Health modifier applied. New health: " + player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getValue());
    }

    // 攻撃力の変更
    if (hasModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER_UUID)) {
        removeModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER_UUID);
    } else {
        applyModifier(player, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER_UUID, "Cursed Attack Up", 6.0);
        System.out.println("Attack modifier applied. New attack damage: " + player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue());
    }

    // クライアント同期
    if (!player.level.isClientSide && player instanceof ServerPlayer) {
        ((ServerPlayer) player).connection.send(new ClientboundUpdateAttributesPacket(player.getId(), player.getAttributes().getSyncableAttributes()));
    }
}
