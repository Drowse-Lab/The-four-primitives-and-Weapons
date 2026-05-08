package minecraftarmorweapon.item;

import minecraftarmorweapon.entity.RecrossHookEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import net.minecraft.client.model.HumanoidModel;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

/**
 * Re:Cross Hookshot.
 *
 * vanilla {@link CrossbowItem} のチャージ機構を流用する形で実装:
 *   1. 右クリック (Charged=false) → {@code startUsingItem} でチャージ開始 (pulling 表示)
 *   2. {@code CHARGE_TICKS} 経過後 {@link #finishUsingItem} → Charged=true (charged モデル)
 *   3. 右クリック (Charged=true) → 即フック発射 → Charged=false に戻して cooldown
 *
 * vanilla CrossbowItem を直接 extend はしない (ammo 要求や CrossbowItem 内部ロジックが
 * 競合するため). 代わりに {@link CrossbowItem#isCharged} / {@link CrossbowItem#setCharged}
 * という static helper だけ流用する → Charged NBT のフォーマットが vanilla 互換になり、
 * model predicate {@code charged}, {@code pulling}, {@code pull} がそのまま動く.
 */
public class RecrossHookshotItem extends Item {

    /** 発射後 reload に入る時間 (tick). */
    public static final int RELOAD_TICKS = 10;
    /** チャージにかかる tick. vanilla crossbow は 25 だが、元データパックは replaceitem で
     *  実質瞬間チャージ → mod では 3 tick (= 0.15 秒) で "ほぼ瞬間" 感を再現. */
    public static final int CHARGE_TICKS = 3;

    public final double flyStep;
    public final int maxFlyTicks;
    public final double maxRange;

    public RecrossHookshotItem(double flyStep, int maxFlyTicks) {
        super(new Item.Properties().stacksTo(1).durability(0));
        this.flyStep = flyStep;
        this.maxFlyTicks = maxFlyTicks;
        this.maxRange = flyStep * maxFlyTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.CROSSBOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return CHARGE_TICKS + 3;   // +3 buffer (vanilla crossbow と同じ. release→チャージ完了の余裕)
    }

    /**
     * 動作 (vanilla クロスボウ風 + 自動再装填):
     *   - Charged=true で右クリック     → 即発射 → Charged=false → cooldown 開始
     *   - Charged=false で右クリック    → チャージ開始 (releaseUsing で Charged=true)
     *   - cooldown 解除 (= フックの動作終了相当) → inventoryTick が自動で Charged=true へ復帰
     *
     * Charged=true の状態は NBT に保存されるので、別スロット切替やインベントリ保管を跨いでも維持される。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (hasFlyingHook(level, player)) {
            return InteractionResultHolder.fail(stack);
        }

        // 装填済み → 即発射 (cooldown 解除されるまで Charged=false で "true" モデル表示)
        if (CrossbowItem.isCharged(stack)) {
            fireHook(level, player, hand, stack);
            CrossbowItem.setCharged(stack, false);
            player.getCooldowns().addCooldown(this, getEffectiveCooldown(stack));
            return InteractionResultHolder.consume(stack);
        }

        // 未装填 → チャージ開始 (押しっぱなしで pulling アニメ → release/finish で Charged=true)
        player.startUsingItem(hand);
        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_LOADING_START, SoundSource.PLAYERS, 0.6f, 1.0f);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingUseTicks) {
        if (!(entity instanceof Player p)) return;
        int ticksHeld = getUseDuration(stack) - remainingUseTicks;
        // 早すぎる release はキャンセル (タップ誤射防止)
        if (ticksHeld < CHARGE_TICKS) return;
        if (CrossbowItem.isCharged(stack)) return;

        // チャージ完了 → 装填済みで待機 (true_charged モデル)
        CrossbowItem.setCharged(stack, true);
        if (!level.isClientSide) {
            level.playSound(null, p.getX(), p.getY(), p.getZ(),
                SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // 押しっぱなしで getUseDuration 到達 → チャージ完了扱い (release と同じ)
        if (!CrossbowItem.isCharged(stack)) {
            CrossbowItem.setCharged(stack, true);
            if (!level.isClientSide && entity instanceof Player p) {
                level.playSound(null, p.getX(), p.getY(), p.getZ(),
                    SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }
        return stack;
    }

    /**
     * 自動再装填: 発射 → cooldown 終了 → インベントリ tick で Charged=true に戻す。
     * これにより「打っている (cooldown 中) 間は true モデル、戻ったタイミングで true_charged モデル」
     * というユーザー指定の見た目切り替えが実現される。
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) return;
        if (!(entity instanceof Player p)) return;
        if (CrossbowItem.isCharged(stack)) return;
        // 現在チャージアニメ中ならそちらに任せる (releaseUsing で Charged=true 化される)
        if (p.isUsingItem() && p.getUseItem() == stack) return;
        if (p.getCooldowns().isOnCooldown(this)) return;

        // cooldown 解除直後 → 再装填
        CrossbowItem.setCharged(stack, true);
        level.playSound(null, p.getX(), p.getY(), p.getZ(),
            SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 0.5f, 1.2f);
    }

    private void fireHook(Level level, Player player, InteractionHand hand, ItemStack stack) {
        if (level.isClientSide) return;

        // ★ rarity による飛距離スケーリング
        double scale = getRangeScale(stack);
        int scaledMaxFlyTicks = (int) Math.max(1, Math.round(maxFlyTicks * scale));

        boolean offHand = (hand == InteractionHand.OFF_HAND);
        RecrossHookEntity hook = new RecrossHookEntity(level, player, offHand, flyStep, scaledMaxFlyTicks);
        level.addFreshEntity(hook);

        // 発射音
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.5f, 1.0f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.IRON_GOLEM_HURT, SoundSource.PLAYERS, 2.0f, 2.0f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 2.0f, 1.0f);

        player.awardStat(Stats.ITEM_USED.get(this));
    }

    /**
     * rarity と Quick Charge エンチャを考慮した実効クールダウン (tick). 最低 1 tick.
     * Quick Charge 1 レベルあたり 25% 短縮 (max level 3 で 75% 短縮、最低 25%)。
     */
    private int getEffectiveCooldown(ItemStack stack) {
        var rarity = minecraftarmorweapon.item.rarity.WeaponRarity.getFromStack(stack);
        double rarityScale = (rarity != null) ? rarity.getHookshotCooldownScale() : 1.0;
        int quickCharge = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
        double quickChargeScale = Math.max(0.25, 1.0 - 0.25 * quickCharge);
        return (int) Math.max(1, Math.round(RELOAD_TICKS * rarityScale * quickChargeScale));
    }

    /** Quick Charge をエンチャテーブルでもフックショットに付与可能にする。 */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (enchantment == Enchantments.QUICK_CHARGE) return true;
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }

    /** rarity を考慮した飛距離倍率. */
    private double getRangeScale(ItemStack stack) {
        var rarity = minecraftarmorweapon.item.rarity.WeaponRarity.getFromStack(stack);
        return (rarity != null) ? rarity.getHookshotRangeScale() : 1.0;
    }

    /** Tooltip 等で使う rarity 適用後の最大射程 (block). */
    public double getEffectiveRange(ItemStack stack) {
        return maxRange * getRangeScale(stack);
    }

    private boolean hasFlyingHook(Level level, Player player) {
        for (RecrossHookEntity h : level.getEntitiesOfClass(
                RecrossHookEntity.class, player.getBoundingBox().inflate(160.0))) {
            if (h.getOwnerPlayer() == player && h.getState() == RecrossHookEntity.State.FLYING) {
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
        tooltip.add(Component.literal(" "));
        double effRange = getEffectiveRange(stack);
        int effCooldown = getEffectiveCooldown(stack);
        tooltip.add(Component.literal(String.format("§f[Range: %.0f blocks]", effRange)));
        tooltip.add(Component.literal(String.format("§f[Cooldown: %.2fs]", effCooldown / 20.0)));
        tooltip.add(Component.literal("§f[RClick (hold & release): Shoot]"));
        tooltip.add(Component.literal("§f[Sneak (mid-air): Float]"));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("§7\"謎の技術が用いられたフックショット。\""));
        tooltip.add(Component.literal("§7\"旧モデルの反省点を踏まえて作られた最新モデル。\""));
    }

    /**
     * クロスボウ構えポーズ (常時):
     *   - 使用中 (チャージ中) → CROSSBOW_CHARGE
     *   - その他 (Charged=true でも false でも) → CROSSBOW_HOLD
     *
     * Charged=false (= 撃った直後 / cooldown 中) でも構え視点を維持したい、というユーザー指定の挙動。
     * vanilla {@code PlayerRenderer.getArmPose} は {@code Items.CROSSBOW} のみ CROSSBOW_HOLD を
     * 返すので、custom item では IClientItemExtensions で上書き必須.
     */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                if (entity.getUsedItemHand() == hand && entity.getUseItemRemainingTicks() > 0) {
                    return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                }
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
        });
    }
}
