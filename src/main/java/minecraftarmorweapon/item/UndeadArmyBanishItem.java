package minecraftarmorweapon.item;

import minecraftarmorweapon.event.UndeadArmyEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * アンデットアーミーを中断させる「聖なる鐘」アイテム。
 * 右クリック時、使用プレイヤーに進行中のアンデットアーミーがあれば中断し、
 * 残存Mobを全て消去する。使用後はアイテムを消費し、長いクールダウンが付く。
 */
public class UndeadArmyBanishItem extends Item {

    public UndeadArmyBanishItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }

        if (!UndeadArmyEvent.hasActiveRaid(sp)) {
            sp.displayClientMessage(
                Component.literal("§7進行中のアンデットアーミーはない。"),
                true
            );
            return InteractionResultHolder.fail(stack);
        }

        boolean ok = UndeadArmyEvent.cancelRaid(sp);
        if (!ok) {
            return InteractionResultHolder.fail(stack);
        }

        level.playSound(null, sp.blockPosition(),
            SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 2.0f, 1.0f);
        level.playSound(null, sp.blockPosition(),
            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.2f);

        sp.displayClientMessage(
            Component.literal("§6§l聖なる鐘が鳴り響き、アンデットの軍団を祓った！"),
            false
        );

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, 12000);

        return InteractionResultHolder.success(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7右クリックで §5§lアンデットアーミー §7を中断").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8残存するアンデット軍団も祓われる"));
        tooltip.add(Component.literal("§c使用後10分間クールダウン"));
    }
}
