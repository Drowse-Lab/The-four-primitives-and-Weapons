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
 * 収束型Gate - 剣が広がった位置から発射され、前方の一点に収束する。
 */
public class ConvergentGateItem extends SwordItem {

    /** 収束地点までの距離（ブロック） */
    private static final double CONVERGE_DISTANCE = 20.0;
    /** 発射時の横方向の広がり */
    private static final double SPREAD = 5.0;
    /** 発射数 */
    private static final int PROJECTILE_COUNT = 5;

    public ConvergentGateItem() {
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

        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            Vec3 lookVec = player.getLookAngle();
            Vec3 eyePos = player.getEyePosition();

            // プレイヤーの向きベクトル（水平面）
            double yawRad = Math.toRadians(player.getYRot());
            double rightX = -Math.cos(yawRad);
            double rightZ = -Math.sin(yawRad);

            // 収束地点 = プレイヤーの視線方向 CONVERGE_DISTANCE ブロック先
            Vec3 convergePoint = eyePos.add(lookVec.scale(CONVERGE_DISTANCE));

            // PROJECTILE_COUNT本の剣を横に広げて配置し、全て収束地点を狙う
            for (int i = 0; i < PROJECTILE_COUNT; i++) {
                // -SPREAD ~ +SPREAD の等間隔
                double lateralOffset = -SPREAD + (2.0 * SPREAD * i / (PROJECTILE_COUNT - 1));

                minecraftarmorweapon.entity.GateProjectileEntity projectile =
                        new minecraftarmorweapon.entity.GateProjectileEntity(level, player);

                // スポーン位置: プレイヤーの横方向にオフセット + 少し後ろ
                double spawnX = eyePos.x + rightX * lateralOffset + lookVec.x * (-2);
                double spawnY = eyePos.y + 0.5;
                double spawnZ = eyePos.z + rightZ * lateralOffset + lookVec.z * (-2);
                projectile.setPos(spawnX, spawnY, spawnZ);

                // 収束地点に向かって飛ぶ
                double dx = convergePoint.x - spawnX;
                double dy = convergePoint.y - spawnY;
                double dz = convergePoint.z - spawnZ;
                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double speed = 2.0;

                projectile.setDeltaMovement(dx / len * speed, dy / len * speed, dz / len * speed);
                projectile.hasImpulse = true;

                level.addFreshEntity(projectile);
            }

            // 発射音
            for (int i = 0; i < 4; i++) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 2.0f, 1.0f);
            }

            // 耐性付与
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 20, 4, true, false));

            player.getCooldowns().addCooldown(this, 20);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6右クリック: 剣を" + PROJECTILE_COUNT + "本収束射出する"));
        tooltip.add(Component.literal("§7Knockback X / Unbreakable"));
    }
}
