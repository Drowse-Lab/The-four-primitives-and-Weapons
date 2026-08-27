package the_four_primitives_and_weapons.potion;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

/** 召喚されたLunaが漂っている間だけ付与する、表示を持たない専用暗視。 */
public class LunaVisionMobEffect extends MobEffect {
    public LunaVisionMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7868D8);
    }

    @Override
    public String getDescriptionId() {
        return "effect.the_four_primitives_and_weapons.luna_vision";
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(new IClientMobEffectExtensions() {
            @Override public boolean isVisibleInInventory(MobEffectInstance effect) { return false; }
            @Override public boolean isVisibleInGui(MobEffectInstance effect) { return false; }
            @Override
            public boolean renderInventoryText(MobEffectInstance instance,
                    EffectRenderingInventoryScreen<?> screen, GuiGraphics graphics,
                    int x, int y, int blitOffset) {
                return false;
            }
        });
    }
}
