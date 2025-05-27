package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.server.level.ServerLevel;

import minecraftarmorweapon.entity.NiseGenEiKenEntity;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEntities;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

public class NiseGenEiKenSpawnProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (world instanceof ServerLevel _level) {
            NiseGenEiKenEntity swordEntity = new NiseGenEiKenEntity(MinecraftArmorWeaponModEntities.NISE_GEN_EI_KEN.get(), _level);
            // プレイヤーの向き・横・上方向にスポーン位置を調整
            double yaw = entity.getYRot();
            double rad = Math.toRadians(yaw);
            double offsetX = -Math.sin(rad) * 1.0;
            double offsetZ = Math.cos(rad) * 1.0;
            double spawnX = entity.getX() + offsetX;
            double spawnY = entity.getY() + 1.2;
            double spawnZ = entity.getZ() + offsetZ;
            swordEntity.moveTo(spawnX, spawnY, spawnZ, (float)yaw, entity.getXRot());
            // 刀剣アイテムを持たせる（半透明テクスチャのカスタム剣）
            ItemStack sword = new ItemStack(MinecraftArmorWeaponModItems.NISE_GEN_EI_KEN_SWORD.get());
            sword.enchant(Enchantments.KILL, 1); // _kill_エンチャント
            swordEntity.setItemSlot(EquipmentSlot.MAINHAND, sword);
            // 水の影響を受けない
            swordEntity.setNoGravity(true);
            // 速度をプレイヤーの向きに与える
            double speed = 1.2;
            swordEntity.setDeltaMovement(-Math.sin(rad) * speed, 0, Math.cos(rad) * speed);
            _level.addFreshEntity(swordEntity);
        }
    }
}
