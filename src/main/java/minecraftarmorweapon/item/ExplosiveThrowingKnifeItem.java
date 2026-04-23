package minecraftarmorweapon.item;

import minecraftarmorweapon.entity.ThrowingKnifeEntity.KnifeType;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 爆発属性付き投げナイフ。
 * 火薬込みレシピで作成される。
 * ブロックに命中すると 3×3×3 の範囲で爆発を起こし、
 * ブロックは破壊せずエンティティのみにダメージを与える。
 * 爆発後、ナイフは消滅する (拾得不可)。
 */
public class ExplosiveThrowingKnifeItem extends ThrowingKnifeItem {

    public ExplosiveThrowingKnifeItem() {
        super(16, 3.0);
    }

    @Override
    public KnifeType getKnifeType() {
        return KnifeType.NORMAL;
    }

    @Override
    public int cooldown() {
        return 10;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§c着弾時に小爆発"));
        tooltip.add(Component.literal("§7ブロックは壊さないが敵にダメージ"));
    }
}
