package the_four_primitives_and_weapons.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;
import the_four_primitives_and_weapons.item.BubbleshotItem;
import the_four_primitives_and_weapons.item.MagicalKatanaItem;
import the_four_primitives_and_weapons.skill.PlayerSkillData;
import top.theillusivec4.curios.api.CuriosApi;

/** Water movement unlocked by a water-attuned Magical Katana and an equipped water book. */
public final class MagicalKatanaWaterMovementHandler {
    private static final int REQUIRED_WATER_LEVEL = 5;
    private static final String DOLPHIN_TICKS = "MagicalKatanaDolphinTicks";
    private static final String DOLPHIN_SURFACE_Y = "MagicalKatanaDolphinSurfaceY";

    private MagicalKatanaWaterMovementHandler() {}

    public static boolean isEnabled(Player player, ItemStack katana) {
        if (!(katana.getItem() instanceof MagicalKatanaItem)
                || ElementalDamageUtils.getElementType(katana) != ElementType.WATER
                || ElementalDamageUtils.getElementLevel(katana) < REQUIRED_WATER_LEVEL) {
            return false;
        }

        final boolean[] found = {false};
        try {
            CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler ->
                    handler.getStacksHandler("book").ifPresent(bookSlot -> {
                        for (int i = 0; i < bookSlot.getStacks().getSlots(); i++) {
                            if (bookSlot.getStacks().getStackInSlot(i).getItem() instanceof BubbleshotItem) {
                                found[0] = true;
                                break;
                            }
                        }
                    }));
        } catch (RuntimeException ignored) {
            return false;
        }
        return found[0];
    }

    public static InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                           InteractionHand hand, ItemStack katana) {
        if (!isEnabled(player, katana) || player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(katana);
        }

        Vec3 look = player.getLookAngle().normalize();
        boolean inWater = player.isInWaterOrBubble();
        String dodgeStyle = getDodgeStyle(player, katana);

        if (inWater) {
            boolean canBreach = look.y > 0.28 && distanceToSurface(player, 7) >= 0;
            boolean leapSpecialist = "leap_slash".equals(dodgeStyle);
            boolean riptideSpecialist = "dash_rush".equals(dodgeStyle);
            boolean shadowSpecialist = "shadow_step".equals(dodgeStyle);
            double speed = canBreach
                    ? (leapSpecialist ? 3.05 : 2.35)
                    : (riptideSpecialist ? 3.6 : shadowSpecialist ? 3.25 : 2.8);
            Vec3 movement = look.scale(speed);
            if (canBreach) {
                // Minecraft の重力・空気抵抗下でも、水面からおよそ3ブロックを越えない上限。
                // 視線が真上でも look.scale(speed) の大きなY速度をそのまま使わない。
                double breachY = leapSpecialist ? 0.66 : 0.58;
                movement = new Vec3(movement.x, breachY, movement.z);
                player.getPersistentData().putInt(DOLPHIN_TICKS, leapSpecialist ? 34 : 18);
                player.getPersistentData().putDouble(DOLPHIN_SURFACE_Y, findSurfaceY(player, 7));
            }
            launch(level, player, movement, !canBreach, riptideSpecialist ? 28 : 20);
            return InteractionResultHolder.sidedSuccess(katana, level.isClientSide);
        }

        // 浮上直後の追加入力は水面ダイブより先に、息継ぎジャンプとして扱う。
        int dolphinTicks = player.getPersistentData().getInt(DOLPHIN_TICKS);
        if (dolphinTicks > 0 && !player.onGround()) {
            boolean leapSpecialist = "leap_slash".equals(dodgeStyle);
            Vec3 horizontal = new Vec3(look.x, 0, look.z);
            if (horizontal.lengthSqr() > 1.0E-4) {
                horizontal = horizontal.normalize().scale(leapSpecialist ? 2.35 : 1.75);
            }
            Vec3 old = player.getDeltaMovement();
            launch(level, player, new Vec3(horizontal.x,
                    Math.min(0.62, Math.max(old.y, leapSpecialist ? 0.48 : 0.32)), horizontal.z), false, 0);
            return InteractionResultHolder.sidedSuccess(katana, level.isClientSide);
        }

        // 通常の水面上では視線が水平/上向きでも、必ず足元の水へ前下方ダイブする。
        if (isOnWaterSurface(level, player)) {
            Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
            if (horizontal.lengthSqr() > 1.0E-4) horizontal = horizontal.normalize().scale(1.8);
            stopRiptide(player);
            player.setOnGround(false);
            launch(level, player, new Vec3(horizontal.x, -1.35, horizontal.z), false, 0);
            return InteractionResultHolder.sidedSuccess(katana, level.isClientSide);
        }

        BlockHitResult waterHit = traceWater(level, player, 16.0);
        if (waterHit.getType() != HitResult.Type.MISS
                && level.getFluidState(waterHit.getBlockPos()).is(Fluids.WATER)) {
            // Diving toward water deliberately keeps the vertical look component.
            launch(level, player, look.scale(2.65), false, 0);
            return InteractionResultHolder.sidedSuccess(katana, level.isClientSide);
        }

        // On the water surface the click belongs to this ability even when it causes no launch.
        // Consuming it here prevents the normal ground dodge from leaking through.
        if (isOnWaterSurface(level, player)) {
            return InteractionResultHolder.sidedSuccess(katana, level.isClientSide);
        }
        return InteractionResultHolder.pass(katana);
    }

    /** Called from the selected katana's inventory tick on both logical sides. */
    public static void tickSelected(Level level, Player player, ItemStack katana) {
        if (!isEnabled(player, katana)) return;

        int dolphinTicks = player.getPersistentData().getInt(DOLPHIN_TICKS);
        if (dolphinTicks > 0) player.getPersistentData().putInt(DOLPHIN_TICKS, dolphinTicks - 1);
        if (!player.isInWaterOrBubble()) stopRiptide(player);

        // クライアント/サーバーの速度差が出ても、水面から3ブロックで上昇を強制終了する。
        if (dolphinTicks > 0) {
            double surfaceY = player.getPersistentData().getDouble(DOLPHIN_SURFACE_Y);
            if (surfaceY != 0.0 && player.getY() >= surfaceY + 3.0 && player.getDeltaMovement().y > 0.0) {
                Vec3 velocity = player.getDeltaMovement();
                player.setDeltaMovement(velocity.x, 0.0, velocity.z);
                player.hurtMarked = true;
            }
        }

        String dodgeStyle = getDodgeStyle(player, katana);
        if (player.isInWaterOrBubble() && "shadow_step".equals(dodgeStyle)) {
            Vec3 waterVelocity = player.getDeltaMovement();
            double horizontal = Math.sqrt(waterVelocity.x * waterVelocity.x + waterVelocity.z * waterVelocity.z);
            boolean swimmingInput = Math.abs(player.zza) > 0.01F || Math.abs(player.xxa) > 0.01F;
            if (swimmingInput && horizontal > 0.025 && horizontal < 0.48) {
                double boost = 0.48 / horizontal;
                player.setDeltaMovement(waterVelocity.x * boost, waterVelocity.y, waterVelocity.z * boost);
                player.hurtMarked = true;
            }
        }

        // Treat source/flowing water directly below the feet as a runnable surface.
        BlockPos below = BlockPos.containing(player.getX(), player.getBoundingBox().minY - 0.08, player.getZ());
        boolean waterBelow = !level.getFluidState(below).isEmpty()
                && level.getFluidState(below).is(Fluids.WATER);
        boolean bodyDry = !player.isInWaterOrBubble();
        if (!waterBelow || !bodyDry || player.isShiftKeyDown() || player.isFallFlying()) return;

        stopRiptide(player);

        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        boolean hasMovementInput = Math.abs(player.zza) > 0.01F || Math.abs(player.xxa) > 0.01F;
        if (hasMovementInput && horizontalSpeed > 0.05) {
            double minimum = "shadow_step".equals(dodgeStyle)
                    ? (player.isSprinting() ? 1.05 : 0.72)
                    : (player.isSprinting() ? 0.72 : 0.46);
            double multiplier = horizontalSpeed < minimum ? minimum / horizontalSpeed : 1.0;
            player.setDeltaMovement(velocity.x * multiplier, Math.max(0.0, velocity.y), velocity.z * multiplier);
        } else {
            // 入力が無い時は残留速度を増幅せず、素早く減衰させて自動歩行を防ぐ。
            double coast = hasMovementInput ? 1.0 : 0.35;
            player.setDeltaMovement(velocity.x * coast, Math.max(0.0, velocity.y), velocity.z * coast);
        }
        player.fallDistance = 0.0F;
        player.setOnGround(true);

        if (level.isClientSide && player.tickCount % 3 == 0) {
            level.addParticle(ParticleTypes.SPLASH, player.getX(), player.getBoundingBox().minY,
                    player.getZ(), -velocity.x * 0.2, 0.08, -velocity.z * 0.2);
        }
    }

    private static int distanceToSurface(Player player, int maxBlocks) {
        BlockPos origin = player.blockPosition();
        for (int dy = 0; dy <= maxBlocks; dy++) {
            if (!player.level().getFluidState(origin.above(dy)).is(Fluids.WATER)) return dy;
        }
        return -1;
    }

    private static double findSurfaceY(Player player, int maxBlocks) {
        BlockPos origin = player.blockPosition();
        for (int dy = 0; dy <= maxBlocks; dy++) {
            BlockPos pos = origin.above(dy);
            if (!player.level().getFluidState(pos).is(Fluids.WATER)) return pos.getY();
        }
        return player.getY();
    }

    public static boolean blocksOrdinaryDodge(Player player, ItemStack katana) {
        return isEnabled(player, katana)
                && (player.isInWaterOrBubble() || isOnWaterSurface(player.level(), player));
    }

    private static boolean isOnWaterSurface(Level level, Player player) {
        BlockPos below = BlockPos.containing(player.getX(), player.getBoundingBox().minY - 0.08, player.getZ());
        return !player.isInWaterOrBubble() && level.getFluidState(below).is(Fluids.WATER);
    }

    private static String getDodgeStyle(Player player, ItemStack katana) {
        PlayerSkillData.SkillStorage skills = PlayerSkillData.getSkillData(player);
        return skills == null ? "dash_rush"
                : skills.getMotionForWeapon(PlayerSkillData.AttackSlot.DASH, katana);
    }

    private static BlockHitResult traceWater(Level level, Player player, double reach) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(reach));
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY, player));
    }

    private static void launch(Level level, Player player, Vec3 movement,
                               boolean riptideAnimation, int spinTicks) {
        player.setDeltaMovement(movement);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        if (riptideAnimation) player.startAutoSpinAttack(spinTicks);
        else stopRiptide(player);
        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    riptideAnimation ? SoundEvents.TRIDENT_RIPTIDE_3 : SoundEvents.DOLPHIN_JUMP,
                    SoundSource.PLAYERS, 1.0F, riptideAnimation ? 1.0F : 1.15F);
        }
    }

    private static void stopRiptide(Player player) {
        if (!player.isAutoSpinAttack()) return;
        the_four_primitives_and_weapons.mixin.LivingEntityAutoSpinAccessor accessor =
                (the_four_primitives_and_weapons.mixin.LivingEntityAutoSpinAccessor) player;
        accessor.maw_setAutoSpinAttackTicks(0);
        if (!player.level().isClientSide) accessor.maw_invokeSetLivingEntityFlag(4, false);
    }
}
