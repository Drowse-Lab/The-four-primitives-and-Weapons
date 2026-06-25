package the_four_primitives_and_weapons.client.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

/**
 * チャージ進行率を 演出するパーティクルの共通エミッタ。
 *
 *   - 手元 ( 視線方向 0.7m 前、 目線 -0.6m ) に 1〜3 個 ( 進行率で増加 )
 *   - 足元の薄い回転リング ( 6〜10 セグメント )
 *   - 閾値ぴったりの 1 frame: 自分視点に FLASH 1 個 + 他プレイヤー向け burst + 音
 *
 * Rivers of Blood の チャージ ( 右クリック保持 ) と、 通常武器の attack cooldown を
 * 共通の見た目で揃えるために 集約。
 */
public final class ChargeParticleEmitter {

    private ChargeParticleEmitter() {}

    /**
     * @param progress      0.0 〜 1.0 ( 1.0 で 閾値到達 )
     * @param baseColor     0% 時の dust 色 ( RGB float )
     * @param peakColor     100% 時の dust 色 ( RGB float )
     * @param tickPhase     回転リングの位相 ( ティック数の単調増加値 )
     * @param hitThreshold  true なら この frame で 閾値到達演出 ( 音 + FLASH ) を出す
     */
    public static void emit(Level world, Player player, float progress,
                            Vector3f baseColor, Vector3f peakColor,
                            int tickPhase, boolean hitThreshold) {
        // ため中の dust は出さない ( 視界の邪魔 )。 閾値到達 frame の合図のみ。
        if (!hitThreshold) return;
        if (!(world instanceof ServerLevel sl)) return;

        Vec3 look = player.getLookAngle().normalize();

        // 「ピン」 とした 軽い 鈴音 ( マックスチャージの合図 )
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.7f, 1.8f);

        // owner ( 自分 ) 視点だけ ピンポイントで FLASH 1 個 ( ガッツリ赤くしない )
        if (player instanceof ServerPlayer sp) {
            sl.sendParticles(sp, ParticleTypes.FLASH, true,
                    sp.getX() + look.x * 1.0,
                    sp.getEyeY() - 0.2,
                    sp.getZ() + look.z * 1.0,
                    1, 0, 0, 0, 0);
        }
    }
}
