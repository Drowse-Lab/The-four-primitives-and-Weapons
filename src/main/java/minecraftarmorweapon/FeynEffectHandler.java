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

    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ATTACK_MODIFIER_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (player == null || player.level.isClientSide) return;

        boolean hasCursedItem = hasCursedFeyn(player.getMainHandItem()) || hasCursedFeyn(player.getOffhandItem());

        if (hasCursedItem) {
            // 最大体力を常に「元の値 −4」にする
            AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttr != null) {
                removeModifier(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER_UUID);
                applyModifier(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER_UUID, "Cursed Health Down", -4.0);
            }

            // 攻撃力 +6（重複を防ぐ）
            if (!hasModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER_UUID)) {
                applyModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER_UUID, "Cursed Attack Up", 6.0);
            }
        } else {
            // 効果の除去
            removeModifier(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER_UUID);
            removeModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER_UUID);
        }

        // クライアントへ同期
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundUpdateAttributesPacket(
                player.getId(), player.getAttributes().getSyncableAttributes()
            ));
        }
    }

    // NBTが "Feyn":"cursed" であるか確認
    private static boolean hasCursedFeyn(ItemStack stack) {
        return stack.hasTag() && "cursed".equals(stack.getTag().getString("Feyn"));
    }

    // UUIDによってmodifierを持っているか確認
    private static boolean hasModifier(Player player, Attribute attribute, UUID uuid) {
        AttributeInstance instance = player.getAttribute(attribute);
        return instance != null && instance.getModifier(uuid) != null;
    }

    // modifier を追加
    private static void applyModifier(Player player, Attribute attribute, UUID uuid, String name, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        if (instance.getModifier(uuid) == null) {
            AttributeModifier modifier = new AttributeModifier(uuid, name, value, AttributeModifier.Operation.ADDITION);
            instance.addPermanentModifier(modifier);
        }
    }

    // modifier を削除
    private static void removeModifier(Player player, Attribute attribute, UUID uuid) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        AttributeModifier modifier = instance.getModifier(uuid);
        if (modifier != null) {
            instance.removeModifier(modifier);
        }
    }
}
