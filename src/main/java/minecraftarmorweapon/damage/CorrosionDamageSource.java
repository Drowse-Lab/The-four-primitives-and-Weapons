package minecraftarmorweapon.damage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 侵食/闇属性ダメージソース
 * 防御力を一時的に落とす
 */
public class CorrosionDamageSource extends DamageSource {

    public CorrosionDamageSource(String msgId) {
        super(msgId);
    }

    public static DamageSource corrosion(Entity source) {
        return new CorrosionDamageSource("corrosion").setMagic().bypassArmor();
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity entity) {
        String message = "death.attack.corrosion";
        return Component.translatable(message, entity.getDisplayName());
    }
}
