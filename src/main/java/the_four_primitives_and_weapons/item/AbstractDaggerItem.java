package the_four_primitives_and_weapons.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;

import the_four_primitives_and_weapons.entity.ThrowingKnifeEntity;

/**
 * ダガー共通の基底。 近接武器としての振る舞いは {@link SwordItem} のまま、
 * <b>右クリックで投擲できる</b>点だけを足す。
 *
 * <p>投げナイフ ( {@link ThrowingKnifeItem} ) と違いダガーはスタックしないので、
 * 投げた個体をそのまま飛ばして回収できるようにしてある
 * ( {@code ThrowingKnifeEntity#recoveredStack} / {@code expireStuck} 側で対応 )。
 * 刺さったまま寿命が切れてもアイテム化して落ちるため、 投げて失うことはない。</p>
 *
 * <p>右クリックは技スロットと共用なので、 <b>右クリックの技を「なし」にしている時だけ</b>
 * ここまで到達する ( 回避やスペルを割り当てているとそちらが優先されてイベントが
 * キャンセルされる )。</p>
 */
public abstract class AbstractDaggerItem extends SwordItem {

    protected AbstractDaggerItem(Tier tier, int attackDamage, float attackSpeed, Item.Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    /** 投擲後のクールダウン ( tick )。 weapon_stats.json の "throw".cooldown があればそちらが優先。 */
    protected int throwCooldown() {
        return 20;
    }

    /** 投擲の初速。 weapon_stats.json の "throw".velocity があればそちらが優先。
     *  既定は投げナイフ ( 1.6 ) より少し遅い = 重い武器を投げている感じ。 */
    protected float throwVelocity() {
        return 1.35f;
    }

    /** JSON ( "throw" ) を当てた実効値。 未設定の項目は上の既定にフォールバックする。 */
    private int effCooldown(ItemStack stack) {
        var c = the_four_primitives_and_weapons.skill.WeaponStatsRegistry.throwConfig(stack);
        return (c != null && c.cooldown >= 0) ? c.cooldown : throwCooldown();
    }
    private float effVelocity(ItemStack stack) {
        var c = the_four_primitives_and_weapons.skill.WeaponStatsRegistry.throwConfig(stack);
        return (c != null && !Float.isNaN(c.velocity)) ? c.velocity : throwVelocity();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        level.playSound(player, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.7f,
                0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));

        // shrink で空になると getStats が引けなくなるので、 先に確定させる。
        int cd = effCooldown(stack);
        float vel = effVelocity(stack);

        if (!level.isClientSide) {
            ThrowingKnifeEntity thrown = new ThrowingKnifeEntity(level, player);
            // 投げた「実物」を持たせる。 回収時に同じダガー ( 属性/拵えのNBT込み ) が戻る。
            thrown.setItem(stack);
            thrown.setKnifeType(ThrowingKnifeEntity.KnifeType.NORMAL);
            thrown.shootFromRotation(player, player.getXRot(), player.getYRot(),
                    0.0f, vel, 1.0f);
            level.addFreshEntity(thrown);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1); // ダガーは最大スタック1なので手から消える
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(this, cd);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
