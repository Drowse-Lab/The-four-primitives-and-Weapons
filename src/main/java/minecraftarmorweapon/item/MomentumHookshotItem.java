package minecraftarmorweapon.item;

import minecraftarmorweapon.entity.MomentumHookEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Momentum Hookshot.
 *
 * 元データパックは vanilla {@code minecraft:crossbow} を Charged:1b 状態で持たせ、
 * 発射時に {@code minecraft.used:minecraft.crossbow} スコアを使って検出する仕組み。
 * → クロスボウの「チャージ → 発射」フローを利用しているが、replaceitem で常に
 * Charged:1b に保つため**実質的に瞬間チャージ**で連射可能。
 *
 * Java 移植では:
 *   - {@link UseAnim#CROSSBOW} で「クロスボウを構えるポーズ」を再現
 *   - {@link #getUseDuration} を 4 tick に設定 → 構え始めから 0.2 秒で auto-fire
 *   - {@link #finishUsingItem} で実際の発射処理
 *   - 発射後は cooldown で次の発射までの間隔を持たせる (元データパック reload/main 相当)
 */
public class MomentumHookshotItem extends Item {

    /** 発射後のリロード時間 (tick). 元データパック: 20 tick (ヒット時は 15). */
    public static final int RELOAD_TICKS = 10;
    /** チャージ時間 (tick) — "一瞬で終わる" 感覚にするため短く設定. */
    public static final int CHARGE_TICKS = 4;

    public MomentumHookshotItem() {
        super(new Item.Properties().stacksTo(1).durability(0));
    }

    /** プレイヤーがクロスボウを構えるポーズになる. */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.CROSSBOW;
    }

    /** チャージ時間 — 短くして "一瞬でチャージ完了" 感を演出. */
    @Override
    public int getUseDuration(ItemStack stack) {
        return CHARGE_TICKS + 1;  // +1: finishUsingItem が確実に呼ばれるように
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // リロード中 → fail (両側同期される cooldown を使用)
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        // 既存フックが飛行中 → fail
        if (!level.isClientSide && hasFlyingHook(level, player)) {
            return InteractionResultHolder.fail(stack);
        }

        // クロスボウ構えポーズ + 4 tick チャージ開始
        // 完了で finishUsingItem が呼ばれて実際の発射処理が走る
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /** チャージ完了 (4 tick 後) で呼ばれる — フック発射の本処理. */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            fireHook(level, player);
        }
        return stack;
    }

    /** 早期リリース (4 tick 経過前にクリック離した) で呼ばれる. 1 tick 以上 charge していたら fire. */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingUseTicks) {
        if (!(entity instanceof Player player)) return;
        int chargeUsed = getUseDuration(stack) - remainingUseTicks;
        if (chargeUsed >= 1) {
            // 早期リリースでも発射 (チャージは一瞬で終わる仕様なのでリリース感度を寛容に)
            fireHook(level, player);
        }
    }

    private void fireHook(Level level, Player player) {
        if (level.isClientSide) return;
        if (player.getCooldowns().isOnCooldown(this)) return;
        if (hasFlyingHook(level, player)) return;

        // 発射元のhandを取得 (off-hand 対応)
        InteractionHand usedHand = player.getUsedItemHand();
        boolean offHand = (usedHand == InteractionHand.OFF_HAND);
        MomentumHookEntity hook = new MomentumHookEntity(level, player, offHand);
        level.addFreshEntity(hook);
        // 元データパックの `data modify entity @s Leash.UUID` 相当 — vanilla リード機構で繋ぐ
        hook.setLeashedTo(player, true);

        // 発射音 (元データパック shot_anchor: fishing_bobber.throw + iron_golem.hurt + iron_door.open + wooden_button.click_on)
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.5f, 0.7f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.IRON_GOLEM_HURT, SoundSource.PLAYERS, 1.5f, 2.0f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 1.5f, 1.0f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.PLAYERS, 1.5f, 0.8f);

        // クロスボウのチャージ完了音 (リロード視覚効果)
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 0.8f, 1.2f);

        player.getCooldowns().addCooldown(this, RELOAD_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    /** FLYING 中の自分のフックがあるかチェック (ANCHORED 中はチェイン射出許可). */
    private boolean hasFlyingHook(Level level, Player player) {
        for (MomentumHookEntity h : level.getEntitiesOfClass(
                MomentumHookEntity.class, player.getBoundingBox().inflate(160.0))) {
            if (h.getOwnerPlayer() == player && h.getState() == MomentumHookEntity.State.FLYING) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // 元データパック loot table と同じ lore
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("§f[RClick: Shoot Hook]"));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("§7\"物理法則を完全に無視している\""));
    }
}
