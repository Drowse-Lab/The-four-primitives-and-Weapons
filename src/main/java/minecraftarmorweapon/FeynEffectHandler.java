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

        // 最大体力の AttributeModifier を作成
        AttributeModifier healthModifier = new AttributeModifier(
            HEALTH_MODIFIER_UUID,
            "Cursed Health Down",
            -4.0,
            AttributeModifier.Operation.ADDITION
        );

        // 攻撃力の AttributeModifier を作成
        AttributeModifier attackModifier = new AttributeModifier(
            ATTACK_MODIFIER_UUID,
            "Cursed Attack Up",
            6.0,
            AttributeModifier.Operation.ADDITION
        );

        // 最大体力の変更
        if (!player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getModifiers().contains(healthModifier)) {
            player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                  .addTransientModifier(healthModifier);
        }

        // 攻撃力の変更
        if (!player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getModifiers().contains(attackModifier)) {
            player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                  .addTransientModifier(attackModifier);
        }
    }

    private static void removeModifiers(Player player) {
        // 最大体力の効果を削除
        if (player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getModifiers().stream()
                  .anyMatch(modifier -> modifier.getId().equals(HEALTH_MODIFIER_UUID))) {
            player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).removeModifier(HEALTH_MODIFIER_UUID);
        }

        // 攻撃力の効果を削除
        if (player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getModifiers().stream()
                  .anyMatch(modifier -> modifier.getId().equals(ATTACK_MODIFIER_UUID))) {
            player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).removeModifier(ATTACK_MODIFIER_UUID);
        }
    }
}