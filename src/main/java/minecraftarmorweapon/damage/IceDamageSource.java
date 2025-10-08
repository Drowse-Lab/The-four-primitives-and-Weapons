package minecraftarmorweapon.damage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 氷属性ダメージソース
 * 氷で固まっている時に攻撃すると多くダメージが入る
 */
public class IceDamageSource extends DamageSource {

    public IceDamageSource(String msgId) {
        super(msgId);
    }

    public static DamageSource ice(Entity source) {
        return new IceDamageSource("ice").setMagic();
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity entity) {
        String message = "death.attack.ice";
        return Component.translatable(message, entity.getDisplayName());
    }
}
