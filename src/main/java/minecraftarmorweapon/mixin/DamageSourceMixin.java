package minecraftarmorweapon.mixin;

import minecraftarmorweapon.damage.ElementType;
import minecraftarmorweapon.damage.IElementalDamageSource;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * DamageSourceにカスタム属性データを追加するMixin
 */
@Mixin(DamageSource.class)
public class DamageSourceMixin implements IElementalDamageSource {

    @Unique
    private ElementType minecraft_armor_weapon$elementType = ElementType.NONE;

    @Unique
    private int minecraft_armor_weapon$elementLevel = 0;

    @Override
    public ElementType getElementType() {
        return minecraft_armor_weapon$elementType;
    }

    @Override
    public void setElementType(ElementType type) {
        this.minecraft_armor_weapon$elementType = type;
    }

    @Override
    public int getElementLevel() {
        return minecraft_armor_weapon$elementLevel;
    }

    @Override
    public void setElementLevel(int level) {
        this.minecraft_armor_weapon$elementLevel = level;
    }
}
