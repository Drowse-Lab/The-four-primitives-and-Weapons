package minecraftarmorweapon.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Gate - 金の直刀ベースの特殊武器。
 * 右クリックでプレイヤーの周囲から3本の剣が出現し、
 * 視線方向のターゲットに向かって飛んでいく。
 * ヒット時にブロック破壊なしの爆発。
 * データパック「gate1-16」のForge MOD移植版。
 */
public class GateItem extends SwordItem {
    public GateItem() {
        super(new Tier() {
            public int getUses() { return 0; }
            public float getSpeed() { return 4f; }
            public float getAttackDamageBonus() { return 6f; }
            public int getLevel() { return 4; }
            public int getEnchantmentValue() { return 22; }
            public Ingredient getRepairIngredient() { return Ingredient.of(Items.GOLD_INGOT); }
        }, 3, -2.4f, new Properties().rarity(Rarity.EPIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Shift押下中は発射しない（ガード等を優先）
        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            Vec3 lookVec = player.getLookAngle();
            Vec3 eyePos = player.getEyePosition();

            // プレイヤーの右方向ベクトル
            double yawRad = Math.toRadians(player.getYRot());
            double rightX = -Math.cos(yawRad);
            double rightZ = Math.sin(yawRad); // 修正: sin→正しい右方向

            // 3本の剣: 右側、正面上、左側（データパック準拠）
            double[][] offsets = {
                { rightX * 3.5,  0.3, rightZ * 3.5 - 2 },  // 右
                { 0,             0.6, -2 },                  // 正面上
                { -rightX * 3.5, 0.7, -rightZ * 3.5 - 2 },  // 左
            };

            for (double[] offset : offsets) {
                minecraftarmorweapon.entity.GateProjectileEntity projectile =
                        new minecraftarmorweapon.entity.GateProjectileEntity(level, player);

                // プレイヤー周辺にスポーン
                double spawnX = eyePos.x + offset[0];
                double spawnY = eyePos.y + offset[1];
                double spawnZ = eyePos.z + offset[2];
                projectile.setPos(spawnX, spawnY, spawnZ);

                // 視線方向のターゲット地点に向かって飛ぶ
                double targetX = eyePos.x + lookVec.x * 50;
                double targetY = eyePos.y + lookVec.y * 50;
                double targetZ = eyePos.z + lookVec.z * 50;

                double dx = targetX - spawnX;
                double dy = targetY - spawnY;
                double dz = targetZ - spawnZ;
                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double speed = 2.0;

                projectile.setDeltaMovement(dx / len * speed, dy / len * speed, dz / len * speed);
                projectile.hasImpulse = true;

                level.addFreshEntity(projectile);
            }

            // 発射音（データパック準拠: wither.shoot × 4回 = 大音量）
            for (int i = 0; i < 4; i++) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 2.0f, 1.0f);
            }

            // 耐性付与（短時間、反動防止）
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 20, 4, true, false));

            player.getCooldowns().addCooldown(this, 20); // 1秒クールダウン
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6右クリック: 剣を3本射出する"));
        tooltip.add(Component.literal("§7Knockback X / Unbreakable"));
    }

    public static boolean isGateSword(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof GateItem;
    }
}
