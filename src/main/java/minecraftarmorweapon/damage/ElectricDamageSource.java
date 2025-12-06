package minecraftarmorweapon.damage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 電気/雷属性ダメージソース
 * 水中にいるとその周りにもダメージが入る
 */
public class ElectricDamageSource extends DamageSource {

    public ElectricDamageSource(String msgId) {
        super(msgId);
    }

    public static DamageSource electric(Entity source) {
        return new ElectricDamageSource("electric").setMagic();
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity entity) {
        String message = "death.attack.electric";
        return Component.translatable(message, entity.getDisplayName());
    }
}
