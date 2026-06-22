package the_four_primitives_and_weapons.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import the_four_primitives_and_weapons.damage.CorrosionElementDamageHandler;
import the_four_primitives_and_weapons.damage.ElementalDoTHandler;
import the_four_primitives_and_weapons.damage.IceElementDamageHandler;
import the_four_primitives_and_weapons.damage.MiasmaElementDamageHandler;

/**
 * 属性デバフ消去ポーション
 * - 飲むと mod 独自の属性デバフ ( Ice / Corrosion / Miasma / 各属性 DoT ) だけを除去
 * - vanilla の HARMFUL effect (Poison / Wither / 等) は除去しない (ResetMax の上位互換ではない)
 * - 空き瓶を返す
 */
public class ElementCleansePotionItem extends Item {

    public ElementCleansePotionItem() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // 属性デバフを除去 (mod 独自の attribute modifier / 内部 state のみ)
        if (!level.isClientSide()) {
            try { IceElementDamageHandler.clear(entity); } catch (Throwable ignored) {}
            try { CorrosionElementDamageHandler.clear(entity); } catch (Throwable ignored) {}
            try { MiasmaElementDamageHandler.clear(entity); } catch (Throwable ignored) {}
            try { ElementalDoTHandler.clear(entity); } catch (Throwable ignored) {}

            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 1.2f);
        }

        // 空き瓶を返す (creative なら数も維持)
        if (entity instanceof Player player) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
                if (stack.isEmpty()) {
                    return emptyBottle;
                }
                if (!player.getInventory().add(emptyBottle)) {
                    player.drop(emptyBottle, false);
                }
            }
        } else {
            stack.shrink(1);
        }

        return stack;
    }
}
