package minecraftarmorweapon.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Entity;

public class AttackDamageProcedure {
    public static void execute(Entity entity) {
        if (entity instanceof Player player) {
            // 利き手にあるアイテムを取得
            ItemStack mainHandItem = player.getMainHandItem();

            // アイテムの攻撃ダメージ属性を取得
            double attackDamage = mainHandItem.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE)
                .stream()
                .mapToDouble(attribute -> attribute.getAmount())
                .sum();

            // デバッグログに出力
            System.out.println("Attack Damage: " + attackDamage);
        }
    }
}
