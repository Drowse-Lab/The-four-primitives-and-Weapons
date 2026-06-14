package the_four_primitives_and_weapons.item;

import the_four_primitives_and_weapons.entity.RecrossHookEntity;
import the_four_primitives_and_weapons.event.RecrossPlayerHandler;
import the_four_primitives_and_weapons.item.rarity.WeaponRarity;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import net.minecraft.client.model.HumanoidModel;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Re:Cross Hookshot (slim 版).
 *
 * vanilla {@link CrossbowItem} のチャージ機構を流用:
 *   1. 右クリック (Charged=false) → チャージ開始 (CHARGE_TICKS で完了)
 *   2. チャージ完了 → Charged=true (装填済み)
 *   3. 右クリック (Charged=true) → 即発射 → cooldown
 *   4. cooldown 終了 → inventoryTick で自動再装填
 */
public class RecrossHookshotItem extends Item {

    public static final int RELOAD_TICKS = 10;
    public static final int CHARGE_TICKS = 3;
    private static final int FIRE_FALL_GUARD_TICKS = 60;

    /** Player UUID → 飛行中 hook の O(1) lookup. 重い 4000³ スキャン回避. */
    private static final Map<UUID, RecrossHookEntity> FLYING_BY_OWNER = new ConcurrentHashMap<>();

    public final double flyStep;
    public final int maxFlyTicks;
    public final double maxRange;

    public RecrossHookshotItem(double flyStep, int maxFlyTicks) {
        super(new Item.Properties().stacksTo(1).durability(0));
        this.flyStep = flyStep;
        this.maxFlyTicks = maxFlyTicks;
        this.maxRange = flyStep * maxFlyTicks;
    }

    public static void registerFlyingHook(Player owner, RecrossHookEntity hook) {
        FLYING_BY_OWNER.put(owner.getUUID(), hook);
    }

    public static void unregisterFlyingHook(Player owner) {
        if (owner != null) FLYING_BY_OWNER.remove(owner.getUUID());
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.CROSSBOW; }
    @Override public int getUseDuration(ItemStack stack) { return CHARGE_TICKS + 3; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this) || hasFlyingHook(player)) {
            return InteractionResultHolder.fail(stack);
        }

        if (CrossbowItem.isCharged(stack)) {
            fireHook(level, player, hand, stack);
            CrossbowItem.setCharged(stack, false);
            player.getCooldowns().addCooldown(this, getEffectiveCooldown(stack));
            return InteractionResultHolder.consume(stack);
        }

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
        int held = getUseDuration(stack) - remainingUseTicks;
        if (held < CHARGE_TICKS || CrossbowItem.isCharged(stack)) return;
        CrossbowItem.setCharged(stack, true);
        if (!level.isClientSide) {
            level.playSound(null, p.getX(), p.getY(), p.getZ(),
                SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!CrossbowItem.isCharged(stack)) {
            CrossbowItem.setCharged(stack, true);
            if (!level.isClientSide && entity instanceof Player p) {
                level.playSound(null, p.getX(), p.getY(), p.getZ(),
                    SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }
        return stack;
    }

    /** 自動再装填: cooldown 終了後に Charged=true へ復帰. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player p)) return;
        if (CrossbowItem.isCharged(stack)) return;
        if (p.isUsingItem() && p.getUseItem() == stack) return;
        if (p.getCooldowns().isOnCooldown(this)) return;
        CrossbowItem.setCharged(stack, true);
        level.playSound(null, p.getX(), p.getY(), p.getZ(),
            SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 0.5f, 1.2f);
    }

    private void fireHook(Level level, Player player, InteractionHand hand, ItemStack stack) {
        if (level.isClientSide) return;

        // 古い hook (ANCHORED 残留 / light-pull / 旧 FLYING) を全部 discard.
        // これをしないと連射時に古い hook が生き残ったまま spawnChainParticles で線粒子を出し続け、
        // 画面上に複数の線が残留する。
        discardActiveHooks(player);

        double scale = getRangeScale(stack);
        int scaledMaxFlyTicks = (int) Math.max(1, Math.round(maxFlyTicks * scale));

        boolean offHand = hand == InteractionHand.OFF_HAND;
        RecrossHookEntity hook = new RecrossHookEntity(level, player, offHand, flyStep, scaledMaxFlyTicks);
        level.addFreshEntity(hook);
        registerFlyingHook(player, hook);

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            RecrossPlayerHandler.applyFallGuard(serverPlayer, FIRE_FALL_GUARD_TICKS);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.5f, 1.0f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.IRON_GOLEM_HURT, SoundSource.PLAYERS, 2.0f, 2.0f);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    /** 飛行中 (state==FLYING) の hook が存在するか — UUID マップ参照のみ (O(1)).
     *  注: state が ANCHORED 等に遷移したエントリは {@link #discardActiveHooks} のために
     *      レジストリには残しておく。死亡時のみ自動 cleanup. */
    private boolean hasFlyingHook(Player player) {
        RecrossHookEntity h = FLYING_BY_OWNER.get(player.getUUID());
        if (h == null) return false;
        if (!h.isAlive()) {
            FLYING_BY_OWNER.remove(player.getUUID());
            return false;
        }
        return h.getState() == RecrossHookEntity.State.FLYING;
    }

    /** 既存の hook (FLYING / ANCHORED / light-pull) を全て discard.
     *  連射時に古い hook が生き残ったまま spawnChainParticles で線粒子を出し続ける問題への対処. */
    private void discardActiveHooks(Player player) {
        if (player == null) return;
        RecrossHookEntity prev = FLYING_BY_OWNER.get(player.getUUID());
        if (prev != null && prev.isAlive()) {
            // discard() は RecrossHookEntity.remove() を呼び、両レジストリから自動で消える.
            prev.discard();
        }
    }

    private int getEffectiveCooldown(ItemStack stack) {
        WeaponRarity r = WeaponRarity.getFromStack(stack);
        double rarityScale = r != null ? r.getHookshotCooldownScale() : 1.0;
        int qc = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
        double qcScale = Math.max(0.25, 1.0 - 0.25 * qc);
        return (int) Math.max(1, Math.round(RELOAD_TICKS * rarityScale * qcScale));
    }

    private double getRangeScale(ItemStack stack) {
        WeaponRarity r = WeaponRarity.getFromStack(stack);
        return r != null ? r.getHookshotRangeScale() : 1.0;
    }

    public double getEffectiveRange(ItemStack stack) { return maxRange * getRangeScale(stack); }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (enchantment == Enchantments.QUICK_CHARGE) return true;
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        WeaponRarity r = WeaponRarity.getFromStack(stack);
        return r != null && r.ordinal() >= WeaponRarity.LEGENDARY.ordinal();
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        WeaponRarity r = WeaponRarity.getFromStack(stack);
        if (r == null) return base;
        return Component.literal(r.getColorCode() + base.getString());
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        WeaponRarity r = WeaponRarity.getFromStack(stack);
        if (r == null) return super.getRarity(stack);
        switch (r) {
            case LEGENDARY: case FORBIDDEN: return Rarity.EPIC;
            case RARE:                       return Rarity.RARE;
            case UNCOMMON:                   return Rarity.UNCOMMON;
            default:                         return Rarity.COMMON;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        WeaponRarity r = WeaponRarity.getFromStack(stack);
        if (r != null) tooltip.add(Component.literal(r.getColoredName()));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal(String.format("§f[Range: %.0f blocks]", getEffectiveRange(stack))));
        tooltip.add(Component.literal(String.format("§f[Cooldown: %.2fs]", getEffectiveCooldown(stack) / 20.0)));
        tooltip.add(Component.literal("§f[RClick (hold & release): Shoot]"));
        tooltip.add(Component.literal("§f[Sneak (mid-air): Float]"));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("§7\"謎の技術が用いられたフックショット。\""));
        tooltip.add(Component.literal("§7\"旧モデルの反省点を踏まえて作られた最新モデル。\""));
    }

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
