package the_four_primitives_and_weapons.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * パリィシールド
 *
 * ＜操作体系＞
 *
 *   【メインハンド — シールドバッシュ（チャージ式）】
 *     右クリック長押しでチャージ → 離した瞬間バッシュ発動。
 *     チャージ量でダメージ・ノックバック・射程がすべてスケール。
 *     チャージ状況はクロスヘア下のバーで確認（ShieldChargeHudRenderer）。
 *
 *       ダメージ    : 2.0 〜 8.0
 *       ノックバック: 0.5 〜 3.0
 *       射程範囲    : 2.0 〜 4.5 ブロック
 *       最低チャージ: MIN_CHARGE_TICKS tick（未満で離すと不発）
 *       フルチャージ: MAX_CHARGE_TICKS tick
 *
 *   【オフハンド — パリィ】
 *     右クリック直後 PARRY_WINDOW_TICKS tick 以内に攻撃を受けるとパリィ発動。
 *
 *   【Fキー（持ちかえ）— スワップパリィ】
 *     メインハンドに来た瞬間から PARRY_WINDOW_TICKS tick 以内にパリィ発動。
 */
public class ParryShieldItem extends ShieldItem {

    public static final String NBT_BLOCK_START = "ParryShieldBlockStart";
    public static final String NBT_SWAP_START  = "ParryShieldSwapStart";

    public static final int PARRY_WINDOW_TICKS = 10;
    public static final int MIN_CHARGE_TICKS   = 5;
    public static final int MAX_CHARGE_TICKS   = 40;
    public static final int USE_DURATION       = 72000;

    public ParryShieldItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    // =========================================================
    // 右クリック押し込み
    // =========================================================
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack shield = player.getItemInHand(hand);

        if (!level.isClientSide && hand == InteractionHand.OFF_HAND) {
            // オフハンド: パリィウィンドウ開始
            player.getPersistentData().putLong(NBT_BLOCK_START, level.getGameTime());
        }
        // メインハンド: バッシュは releaseUsing() で実行

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(shield);
    }

    // =========================================================
    // チャージ中のサウンドフィードバック（メッセージなし）
    // =========================================================
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide) return;
        if (!(user instanceof Player player)) return;
        if (player.getUsedItemHand() != InteractionHand.MAIN_HAND) return;

        int held = getUseDuration(stack) - remainingUseDuration;

        if (held == MIN_CHARGE_TICKS) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.4f, 1.5f);

        } else if (held == MAX_CHARGE_TICKS / 2) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.6f, 1.1f);

        } else if (held == MAX_CHARGE_TICKS) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4f, 1.8f);
        }
    }

    // =========================================================
    // 右クリック離し — バッシュ発動
    // =========================================================
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeCharged) {
        if (level.isClientSide) return;
        if (!(user instanceof Player player)) return;
        if (player.getUsedItemHand() != InteractionHand.MAIN_HAND) return;

        int held = getUseDuration(stack) - timeCharged;
        if (held < MIN_CHARGE_TICKS) return; // 不発

        float charge = Math.min(
            (float)(held - MIN_CHARGE_TICKS) / (MAX_CHARGE_TICKS - MIN_CHARGE_TICKS),
            1.0f
        );
        performShieldBash(level, player, charge);
    }

    // =========================================================
    // シールドバッシュ本体
    // =========================================================
    private void performShieldBash(Level level, Player player, float charge) {
        float damage    = 2.0f + charge * 6.0f;
        float knockback = 0.5f + charge * 2.5f;
        double range    = 2.0  + charge * 2.5;

        Vec3 look = player.getLookAngle();
        AABB area = player.getBoundingBox()
            .inflate(range, 0.5, range)
            .move(look.x * (range * 0.5), 0, look.z * (range * 0.5));

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
            e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 0) target.knockback(knockback, -dx / len, -dz / len);
            target.hurt(player.damageSources().playerAttack(player), damage);
        }

        float pitch  = 0.6f + charge * 0.6f;
        float volume = 0.8f + charge * 0.7f;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, volume, pitch);
    }

    // =========================================================
    // ShieldParryHandler / ShieldChargeHudRenderer から参照
    // =========================================================

    public static long getBlockStartTime(Player player) {
        CompoundTag d = player.getPersistentData();
        return d.contains(NBT_BLOCK_START) ? d.getLong(NBT_BLOCK_START) : -1L;
    }

    public static long getSwapStartTime(Player player) {
        CompoundTag d = player.getPersistentData();
        return d.contains(NBT_SWAP_START) ? d.getLong(NBT_SWAP_START) : -1L;
    }

    public static void recordSwapParry(Player player, long gameTick) {
        player.getPersistentData().putLong(NBT_SWAP_START, gameTick);
    }
}
