package minecraftarmorweapon.mixin;

import net.minecraft.world.item.enchantment.Enchantment;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 全エンチャント (vanilla / mod 独自 問わず) の max level を {@link Integer#MAX_VALUE} (= 2147483647)
 * に拡張 — 実質 "レベル無限"。
 *
 * 効果:
 *   - {@code /enchant <player> <id> <level>} で Integer.MAX_VALUE まで指定可能。
 *     vanilla の Infinity / Mending / Sharpness、mod 独自の Multi Jump / Kill 等すべて対象。
 *
 * 注意 (NBT 保存上の制限):
 *   - vanilla の {@code EnchantmentHelper.storeAllFromMap} は lvl を short で putShort する。
 *     ↳ 32768 以上を指定すると NBT 上 overflow し負値になる ⇒ そのレベルは事実上無効。
 *     ↳ 安全に保存できる "実用上の上限" は 32767。
 *     ↳ 32767 を超える指定はコマンド通るが、保存後に効果が消える / 異常動作する可能性あり。
 *   - エンチャテーブル / 書のランダム付与は getMinCost/getMaxCost が level に依存するため
 *     経験値レベル 30 帯の自然付与では高 level はまず出ない (バランス影響小)。
 *   - Tooltip のローマ数字表記は 10 まで。Lv11 以上は半角数字。
 */
@Mixin(Enchantment.class)
public class EnchantmentMaxLevelMixin {

    @Inject(method = "getMaxLevel", at = @At("HEAD"), cancellable = true)
    private void msw_overrideMaxLevel(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Integer.MAX_VALUE);
    }
}
