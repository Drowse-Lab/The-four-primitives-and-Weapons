package minecraftarmorweapon.procedures; // 修正

import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.Rotations;
import com.mojang.math.Vector3f;
import net.minecraft.world.entity.Entity;


@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class YellowStainedGlassPaneGuardProcedureProcedure {
    public static void execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return; // プレイヤーでない場合は処理をしない
        }

        Level world = player.level;
        BlockPos playerPos = player.blockPosition();
      // Armor Stand をスポーン
      ArmorStand armorStand = new ArmorStand(world, playerPos.getX(), playerPos.getY(), playerPos.getZ());
      armorStand.setNoGravity(true);
      armorStand.setInvisible(true);
      armorStand.setCustomNameVisible(false);
      armorStand.addTag("minecraft_armor_weapon_guard_bind");

      // 腕のポーズを設定
      armorStand.setLeftArmPose(new Rotations(0.0f, 90.0f, -90.0f));
      armorStand.setRightArmPose(new Rotations(0.0f, -90.0f, 90.0f));

      // アイテムを持たせる
      armorStand.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.YELLOW_STAINED_GLASS_PANE, 1));
      armorStand.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.YELLOW_STAINED_GLASS_PANE, 1));

      world.addFreshEntity(armorStand);

      // 音を再生
      world.playSound(null, playerPos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 2.0f, 2.0f);
      world.playSound(null, playerPos, SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.PLAYERS, 1.0f, 1.0f);

      // パーティクルをスポーン
      Vector3f color = new Vector3f(1.0f, 1.0f, 0.0f); // RGB (255, 255, 0) を 0~1 の範囲に変換
      DustParticleOptions dust = new DustParticleOptions(color, 1.0f);
      for (int i = 0; i < 35; i++) {
          double offsetX = (world.random.nextDouble() - 0.5) * 0.5;
          double offsetY = world.random.nextDouble() * 1.0;
          double offsetZ = (world.random.nextDouble() - 0.5) * 0.5;
          world.addParticle(dust, playerPos.getX() + offsetX, playerPos.getY() + 1.0 + offsetY, playerPos.getZ() + offsetZ, 0.0, 0.0, 0.0);
      }
        // ここに元の処理を書く
    }

    // MCreatorから呼び出しやすいように、引数なしのバージョンを追加
    public static void execute() {
        System.out.println("エラー: プレイヤー情報が渡されていません！");
    }
}
   