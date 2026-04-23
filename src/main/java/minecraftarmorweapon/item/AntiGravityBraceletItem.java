package minecraftarmorweapon.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 反重力腕輪 — Curios の bracelet / charm スロット装備。
 *
 * 装備中、投擲したナイフが重力を無視して直進する。
 * 直進状態のナイフは 100 ブロック飛行すると空中分解する。
 * 実際の判定は {@link minecraftarmorweapon.entity.ThrowingKnifeEntity}
 * が毎 tick オーナーの装備を調べて行う。
 */
public class AntiGravityBraceletItem extends Item implements ICurioItem {

    public AntiGravityBraceletItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canEquip(SlotContext ctx, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canEquipFromUse(SlotContext ctx, ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§b装備中: 投げナイフが重力無視で直進"));
        tooltip.add(Component.literal("§7100ブロック飛ぶと空中分解する"));
    }
}
