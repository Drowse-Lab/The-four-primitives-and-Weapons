package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.core.particles.DustParticleOptions;

import org.joml.Vector3f;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

public class SwordOfNightChargingGlowProcedure {

    // 紫色 (0.7, 0.17, 1.0)
    private static final DustParticleOptions PURPLE_DOT =
            new DustParticleOptions(new Vector3f(0.7f, 0.17f, 1.0f), 0.4f);

    /**
     * Sword of Nightのチャージ中に自分の目の前に紫のdust particleを1つ表示する（自分だけに見える）
     */
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, boolean isCharging) {
        if (entity == null || world == null) return;
        if (!(entity instanceof ServerPlayer player)) return;

        // Sword of Nightを持っているかチェック
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() != TheFourPrimitivesAndWeaponsModItems.SWORD_OF_NIGHT.get()) {
            return;
        }

        if (isCharging) {
            // プレイヤーの視線方向2ブロック先に紫パーティクルを1つ表示（自分だけ）
            Vec3 lookVec = player.getLookAngle();
            Vec3 eyePos = player.getEyePosition();
            Vec3 particlePos = eyePos.add(lookVec.scale(2.0));

            // ClientboundLevelParticlesPacketで自分だけに送信
            ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                    PURPLE_DOT,
                    true, // longDistance
                    particlePos.x, particlePos.y, particlePos.z,
                    0f, 0f, 0f, // offset (0 = 点)
                    0f,          // speed
                    1            // count = 1
            );
            player.connection.send(packet);
        }
        // チャージ解除時は何もしない（発光を使わないのでクリーンアップ不要）
    }

    /**
     * 発光管理は不要になったため空実装
     */
    public static void managGlowTicks(net.minecraft.world.entity.LivingEntity entity) {
        // 何もしない
    }
}
