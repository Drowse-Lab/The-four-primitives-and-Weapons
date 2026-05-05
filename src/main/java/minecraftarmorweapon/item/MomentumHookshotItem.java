package minecraftarmorweapon.item;

import minecraftarmorweapon.entity.MomentumHookEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Momentum Hookshot — 着弾点へ慣性で吹っ飛ばすスイング型フックショット.
 *
 *   - 右クリック: フック発射 (cooldown 中以外)
 *   - 着弾 (block / heavy entity): プレイヤーに impulse + 短時間 boost で着弾点を目掛けて飛ぶ
 *   - 着弾 (light entity): 対象をプレイヤーへ引き寄せる
 *   - リロード = 14 tick (clear: 12 tick の射程 + 着弾後 anchor visual)
 */
public class MomentumHookshotItem extends Item {

    public static final int RELOAD_TICKS = 10;  // 元 20 tick より短くしてチェイン射出可能に

    public MomentumHookshotItem() {
        super(new Item.Properties().stacksTo(1).durability(0));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // cooldown は両側同期される → 即応答
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            if (hasOwnHook(level, player)) {
                return InteractionResultHolder.fail(stack);
            }
            MomentumHookEntity hook = new MomentumHookEntity(level, player);
            level.addFreshEntity(hook);
        }

        // 元データパック準拠の発射音 (両側で再生 — 自分は client local、他は server broadcast)
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
            SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.5f, 0.7f);
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
            SoundEvents.IRON_GOLEM_HURT, SoundSource.PLAYERS, 1.5f, 2.0f);
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
            SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 1.5f, 1.0f);
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
            SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.PLAYERS, 1.5f, 0.8f);

        player.getCooldowns().addCooldown(this, RELOAD_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** FLYING 中のみブロック (ANCHORED 中はチェイン射出を許可)。 */
    private boolean hasOwnHook(Level level, Player player) {
        for (MomentumHookEntity h : level.getEntitiesOfClass(
                MomentumHookEntity.class, player.getBoundingBox().inflate(160.0))) {
            if (h.getOwner() == player && h.getState() == MomentumHookEntity.State.FLYING) {
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
        // 元データパックの loot table と同じ lore
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("§f[RClick: Shoot Hook]"));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("§7\"物理法則を完全に無視している\""));
    }
}
