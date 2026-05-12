package the_four_primitives_and_weapons.mixin;

import the_four_primitives_and_weapons.status.BlessingSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 加護保持者の視界制限Mixin（クライアントサイド）
 *
 * 効果:
 *   - 通常の視界を暗くする（インベントリ画面のみ操作可能な状態を演出）
 *   - ブロックのエッジラインが薄く見える演出は
 *     RenderLevelStageMixin / GameRendererMixin で対応予定
 *
 * 登録先: mixins.the_four_primitives_and_weapons.json の "client" 配列
 *   "the_four_primitives_and_weapons.mixin.BlessingVisionMixin"
 */
@Mixin(Minecraft.class)
public abstract class BlessingVisionMixin {

    /**
     * getNightVisionScale() に介入し、加護保持者の視界を制限する。
     * 戻り値を 0.0f にすると暗視が無効化される。
     * ブロックのラインを薄く見せる演出は別途 RenderLevelStageMixin で実装する。
     */
    @Inject(
            method = "getNightVisionScale",
            at = @At("HEAD"),
            cancellable = true
    )
    private void blessing_suppressNightVision(
            net.minecraft.world.entity.LivingEntity entity,
            float partialTick,
            CallbackInfoReturnable<Float> cir) {

        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;

        if (BlessingSystem.hasBlessing(localPlayer)) {
            // 通常の暗視を無効化 → 霊視用の暗闇演出を優先
            cir.setReturnValue(0.0f);
        }
    }
}
