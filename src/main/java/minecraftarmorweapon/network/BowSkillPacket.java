package minecraftarmorweapon.network;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.item.TestBowItem;
import minecraftarmorweapon.skill.BowSkill;
import minecraftarmorweapon.skill.BowSkillData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 弓スキルのクライアント→サーバーパケット。
 *
 *   action 0: スロット切替 (param = +1 / -1)
 *   action 1: スキル発動 (チャージ率 0.0〜1.0)
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class BowSkillPacket {
    public static final int ACTION_CYCLE = 0;
    public static final int ACTION_USE   = 1;

    private final int action;
    private final float param;

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        MinecraftArmorWeaponMod.addNetworkMessage(BowSkillPacket.class,
            BowSkillPacket::encode, BowSkillPacket::new, BowSkillPacket::handle);
    }

    public BowSkillPacket(int action, float param) {
        this.action = action;
        this.param = param;
    }

    public BowSkillPacket(FriendlyByteBuf buf) {
        this.action = buf.readInt();
        this.param = buf.readFloat();
    }

    public static void encode(BowSkillPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.action);
        buf.writeFloat(msg.param);
    }

    public static void handle(BowSkillPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!(stack.getItem() instanceof TestBowItem)) return;

            if (msg.action == ACTION_CYCLE) {
                int delta = msg.param > 0 ? 1 : -1;
                BowSkillData.cycleIndex(stack, delta);
                BowSkill cur = BowSkillData.getSelected(stack);
                player.displayClientMessage(
                    Component.literal("§7▶ " + cur.coloredName() + " §7[" + (cur.type) + "]"),
                    true
                );
            } else if (msg.action == ACTION_USE) {
                BowSkill skill = BowSkillData.getSelected(stack);
                if (skill.type == BowSkill.Type.PASSIVE) return;
                if (player.getCooldowns().isOnCooldown(stack.getItem())) return;
                if (!consumeArrows(player, skill.arrowCost)) {
                    player.displayClientMessage(Component.literal("§c矢が足りない"), true);
                    return;
                }
                skill.execute(player, Math.max(0f, Math.min(1f, msg.param)));
                player.getCooldowns().addCooldown(stack.getItem(), skill.cooldownTicks);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /** 矢を必要数消費。クリエイティブ/Infinityなら消費せず成功。 */
    private static boolean consumeArrows(ServerPlayer player, int cost) {
        if (cost <= 0) return true;
        if (player.getAbilities().instabuild) return true;

        ItemStack bow = player.getItemInHand(InteractionHand.MAIN_HAND);
        int infinity = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
            net.minecraft.world.item.enchantment.Enchantments.INFINITY_ARROWS, bow);
        if (infinity > 0) return true;

        // 矢のあるスロットを順に消費
        int remaining = cost;
        var inv = player.getInventory();
        // まず必要数あるかチェック
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() == net.minecraft.world.item.Items.ARROW
             || s.getItem() == net.minecraft.world.item.Items.TIPPED_ARROW
             || s.getItem() == net.minecraft.world.item.Items.SPECTRAL_ARROW) {
                total += s.getCount();
                if (total >= cost) break;
            }
        }
        if (total < cost) return false;

        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() == net.minecraft.world.item.Items.ARROW
             || s.getItem() == net.minecraft.world.item.Items.TIPPED_ARROW
             || s.getItem() == net.minecraft.world.item.Items.SPECTRAL_ARROW) {
                int take = Math.min(s.getCount(), remaining);
                s.shrink(take);
                remaining -= take;
            }
        }
        return true;
    }
}
