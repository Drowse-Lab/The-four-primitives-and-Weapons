package the_four_primitives_and_weapons.item;

import the_four_primitives_and_weapons.compat.SpellbooksCompat;
import the_four_primitives_and_weapons.mana.ManaHelper;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * マナ回復ポーション。
 *
 * 飲むと以下のルールで MP を回復する:
 *   - Iron's Spells 'n Spellbooks が導入されている → 向こうの Mana を回復
 *     (SpellbooksCompat 経由で PlayerMagicData.setMana を加算)
 *   - 導入されていない → 本 MOD の Mana attribute を回復
 *
 * 飲み干したあと空のガラス瓶が手元に返る (vanilla ポーションと同挙動)。
 */
public class ManaPotionItem extends Item {

    /** 1 本あたり回復する MP 量 */
    public static final double RESTORE_AMOUNT = 50.0;
    /** 飲む時間 (tick) — vanilla ポーションと同じ 32 */
    private static final int DRINK_DURATION = 32;

    public ManaPotionItem() {
        super(new Item.Properties().stacksTo(16));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return DRINK_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof Player p && !level.isClientSide) {
            // MP 回復: ManaHelper が Iron's Spellbooks が入っている時は向こうに routed する
            double cur = ManaHelper.getMana(p);
            double next = cur + RESTORE_AMOUNT;
            if (SpellbooksCompat.isLoaded()) {
                // Iron's 側には max 制限があるので setMana で上書き (内部で clamp)
                SpellbooksCompat.setMana(p, next);
            } else {
                ManaHelper.setMana(p, next);
            }

            // 飲んだ SE
            level.playSound(null, p.getX(), p.getY(), p.getZ(),
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        // クリエイティブ以外はスタック減らす + 空き瓶を返す
        if (user instanceof Player p && !p.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (stack.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        if (user instanceof Player p && !p.getAbilities().instabuild) {
            if (!p.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
                p.drop(new ItemStack(Items.GLASS_BOTTLE), false);
            }
        }
        return stack;
    }
}
