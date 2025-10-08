package minecraftarmorweapon.damage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 聖属性ダメージソース
 * アンデットに多くダメージが入る
 */
public class HolyDamageSource extends DamageSource {

    public HolyDamageSource(String msgId) {
        super(msgId);
    }

    public static DamageSource holy(Entity source) {
        return new HolyDamageSource("holy").setMagic();
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity entity) {
        String message = "death.attack.holy";
        return Component.translatable(message, entity.getDisplayName());
    }
}
