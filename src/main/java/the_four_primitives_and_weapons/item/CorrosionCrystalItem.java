package the_four_primitives_and_weapons.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;
import the_four_primitives_and_weapons.events.DodgeAndBattouHandler;

/** オフハンドの武器へ侵食属性を付与する、剣界の侵食地帯由来の結晶。 */
public class CorrosionCrystalItem extends Item {
    private static final int MAX_LEVEL = 3;

    public CorrosionCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack crystal = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(crystal);
        ItemStack weapon = player.getOffhandItem();
        if (!DodgeAndBattouHandler.isWeapon(weapon)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable(
                    "message.the_four_primitives_and_weapons.corrosion_crystal.weapon_required"), true);
            return InteractionResultHolder.fail(crystal);
        }

        ElementType current = ElementalDamageUtils.getElementType(weapon);
        int currentLevel = current == ElementType.CORROSION ? ElementalDamageUtils.getElementLevel(weapon) : 0;
        if (currentLevel >= MAX_LEVEL) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable(
                    "message.the_four_primitives_and_weapons.corrosion_crystal.maximum"), true);
            return InteractionResultHolder.fail(crystal);
        }

        if (!level.isClientSide) {
            int nextLevel = current == ElementType.CORROSION ? currentLevel + 1 : 1;
            ElementalDamageUtils.setElement(weapon, ElementType.CORROSION, nextLevel);
            if (!player.getAbilities().instabuild) crystal.shrink(1);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.9F, 0.7F + nextLevel * 0.12F);
            player.displayClientMessage(Component.translatable(
                    "message.the_four_primitives_and_weapons.corrosion_crystal.applied", nextLevel), true);
        }
        return InteractionResultHolder.sidedSuccess(crystal, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.the_four_primitives_and_weapons.corrosion_crystal")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
