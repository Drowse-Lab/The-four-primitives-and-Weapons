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

    // 数値パラメータは gate/formula.lisp から取得 (converge-distance / converge-spread /
    // converge-projectile-count / converge-shoot-velocity / converge-cooldown)。
    // Tooltip 用に定数が必要なので lazy に getter 経由で参照する。

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

            // 数値は gate/formula.lisp から取得
            double convergeDist = GateFormula.convergeDistance();
            double spread       = GateFormula.convergeSpread();
            int    count        = GateFormula.convergeProjectileCount();
            double speed        = GateFormula.convergeShootVelocity();

            // 収束地点 = プレイヤーの視線方向 convergeDist ブロック先
            Vec3 convergePoint = eyePos.add(lookVec.scale(convergeDist));

            // count 本の剣を横に広げて配置し、全て収束地点を狙う
            for (int i = 0; i < count; i++) {
                // -spread 〜 +spread の等間隔
                double lateralOffset = -spread + (2.0 * spread * i / (count - 1));

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

                projectile.setDeltaMovement(dx / len * speed, dy / len * speed, dz / len * speed);
                projectile.hasImpulse = true;

                level.addFreshEntity(projectile);
            }

            // 発射音 (GateItem と同じ sound-reps 設定を共有)
            int soundReps = GateFormula.gateSoundReps();
            for (int i = 0; i < soundReps; i++) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 2.0f, 1.0f);
            }

            // 耐性付与 (GateItem と同じ resist 設定を共有)
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,
                    GateFormula.gateResistDur(), GateFormula.gateResistAmp(), true, false));

            player.getCooldowns().addCooldown(this, GateFormula.convergeCooldown());
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6右クリック: 剣を" + GateFormula.convergeProjectileCount() + "本収束射出する"));
        tooltip.add(Component.literal("§7Knockback X / Unbreakable"));
    }
}
