package the_four_primitives_and_weapons.mixin;

import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.IElementalDamageSource;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * DamageSourceにカスタム属性データを追加するMixin
 */
@Mixin(DamageSource.class)
public class DamageSourceMixin implements IElementalDamageSource {

    @Unique
    private ElementType the_four_primitives_and_weapons$elementType = ElementType.NONE;

    @Unique
    private int the_four_primitives_and_weapons$elementLevel = 0;

    @Override
    public ElementType getElementType() {
        return the_four_primitives_and_weapons$elementType;
    }

    @Override
    public void setElementType(ElementType type) {
        this.the_four_primitives_and_weapons$elementType = type;
    }

    @Override
    public int getElementLevel() {
        return the_four_primitives_and_weapons$elementLevel;
    }

    @Override
    public void setElementLevel(int level) {
        this.the_four_primitives_and_weapons$elementLevel = level;
    }
}
