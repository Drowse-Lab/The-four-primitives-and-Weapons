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

            // プレイヤーの前方・右方向ベクトル（水平面）
            double yawRad = Math.toRadians(player.getYRot());
            double forwardX = -Math.sin(yawRad);
            double forwardZ = Math.cos(yawRad);
            double rightX = -Math.cos(yawRad);
            double rightZ = -Math.sin(yawRad);

            // 本数と展開幅は gate/formula.lisp から読み取る。
            // 横オフセットは本数に対して等間隔に -side〜+side で展開し、
            // 縦/前後は lisp の単一値を全本に適用 (シンプル化)。
            int count = GateFormula.gateProjectileCount();
            double side = GateFormula.gateSideSpread();
            double fwd  = GateFormula.gateForwardOffset();
            double vy   = GateFormula.gateVerticalOffset();
            double speed = GateFormula.gateShootVelocity();

            for (int i = 0; i < count; i++) {
                // 本数 1 なら中央、2 以上なら -side〜+side で等分
                double lateral = count <= 1 ? 0.0
                    : -side + (2.0 * side * i / (count - 1));

                minecraftarmorweapon.entity.GateProjectileEntity projectile =
                        new minecraftarmorweapon.entity.GateProjectileEntity(level, player);

                double spawnX = eyePos.x + rightX * lateral + forwardX * fwd;
                double spawnY = eyePos.y + vy;
                double spawnZ = eyePos.z + rightZ * lateral + forwardZ * fwd;
                projectile.setPos(spawnX, spawnY, spawnZ);

                projectile.setDeltaMovement(
                        lookVec.x * speed,
                        lookVec.y * speed,
                        lookVec.z * speed);
                projectile.hasImpulse = true;

                level.addFreshEntity(projectile);
            }

            // 発射音 (wither.shoot × N)
            int soundReps = GateFormula.gateSoundReps();
            for (int i = 0; i < soundReps; i++) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 2.0f, 1.0f);
            }

            // 耐性付与 (反動防止)
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,
                    GateFormula.gateResistDur(), GateFormula.gateResistAmp(), true, false));

            player.getCooldowns().addCooldown(this, GateFormula.gateCooldown());
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
