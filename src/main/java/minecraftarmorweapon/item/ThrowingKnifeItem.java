package minecraftarmorweapon.item;

import minecraftarmorweapon.entity.ThrowingKnifeEntity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 投げナイフアイテム — 右クリックで飛翔体を投擲する。
 *  - サバイバル時は1個消費
 *  - 命中: エンティティにダメージを与えて消滅
 *  - ブロックにヒット: その位置にアイテムとしてドロップ（再拾得可能）
 */
public class ThrowingKnifeItem extends Item {

    public ThrowingKnifeItem() {
        super(new Item.Properties().stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.7f,
            0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));

        if (!level.isClientSide) {
            ThrowingKnifeEntity knife = new ThrowingKnifeEntity(level, player);
            knife.setItem(stack);
            knife.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.6f, 1.0f);
            level.addFreshEntity(knife);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, 8);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
