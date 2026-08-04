package the_four_primitives_and_weapons.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import the_four_primitives_and_weapons.entity.ThrowingKnifeEntity;
import the_four_primitives_and_weapons.entity.ThrowingKnifeEntity.KnifeType;
import the_four_primitives_and_weapons.compat.FarmersDelightCompat;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * 投げナイフアイテム — 右クリック投擲 / 左クリック近接攻撃 (トライデント風)
 *
 *  右クリック: 飛翔体を投擲 (1個消費 / cooldown / 食料ゲージ消費)
 *  左クリック: 通常の近接攻撃 (攻撃力 +3.0 / 攻撃速度 やや早め)
 *
 * サブクラスは getKnifeType() / hungerCost() / cooldown() / meleeBonus() をオーバーライド。
 *
 * 消費は MP ではなく **食料ゲージ**。ただし Farmer's Delight の「満腹 (Nourishment)」
 * エフェクト中は消費しない。
 */
public class ThrowingKnifeItem extends Item {

    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    public ThrowingKnifeItem() {
        this(16, 3.0);
    }

    public ThrowingKnifeItem(int stackSize, double meleeBonus) {
        super(new Item.Properties().stacksTo(stackSize));
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
            BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", meleeBonus, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.ATTACK_SPEED, new AttributeModifier(
            // ダガー ( AbstractDaggerItem = SwordItem の -1.8 ) と同値。
            // 実際には weapon_stats.json の types.throwing.attack_speed が上書きするが、
            // JSON を外した時の素の値もダガーと揃えておく。
            BASE_ATTACK_SPEED_UUID, "Weapon modifier", -1.8, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = b.build();
    }

    /** サブクラスが上書き — 飛翔体に渡すKnifeType */
    public KnifeType getKnifeType() { return KnifeType.NORMAL; }
    /** 1投あたりの食料ゲージ消費 (肉アイコン半分=1.0 / >0なら不足時投げない)。
     *  weapon_stats.json の "throw".hunger があればそちらが優先。 */
    public float hungerCost() { return 0; }
    /** クールダウン(tick)。 weapon_stats.json の "throw".cooldown があればそちらが優先。 */
    public int cooldown() { return 8; }
    /** 射出初速。 weapon_stats.json の "throw".velocity があればそちらが優先。 */
    public float throwVelocity() { return 1.6f; }

    /** JSON ( "throw" ) を当てた実効値。 未設定の項目は Java 側の既定にフォールバックする。 */
    private the_four_primitives_and_weapons.skill.WeaponStatsRegistry.ThrowConfig cfgOf(ItemStack stack) {
        return the_four_primitives_and_weapons.skill.WeaponStatsRegistry.throwConfig(stack);
    }
    private float effHunger(ItemStack stack) {
        var c = cfgOf(stack);
        return (c != null && !Float.isNaN(c.hunger)) ? c.hunger : hungerCost();
    }
    private int effCooldown(ItemStack stack) {
        var c = cfgOf(stack);
        return (c != null && c.cooldown >= 0) ? c.cooldown : cooldown();
    }
    private float effVelocity(ItemStack stack) {
        var c = cfgOf(stack);
        return (c != null && !Float.isNaN(c.velocity)) ? c.velocity : throwVelocity();
    }
    /** スタック消費 (false = 弾切れ無視 = 永続。STUN等は1消費) */
    public boolean consumesItem() { return true; }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        float cost = effHunger(stack);
        // Farmer's Delight の「満腹」中は食料ゲージを消費しない (未導入なら常に false)
        boolean needsFood = cost > 0 && !player.getAbilities().instabuild
            && !FarmersDelightCompat.hasNourishment(player);

        // 空腹チェックは両サイドで実施 (UI の即時フィードバックのため)。ただし
        // 消費はサーバー側のみで行う。両サイドで消費すると同期のタイムラグで
        // 片側だけ fail して片側成功する "不発" が発生する。
        if (needsFood && foodTotal(player) < cost) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("§6🍖 空腹 (必要: " + fmt(cost) + ")"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        // player 引数を渡すとサーバー側では当該プレイヤー以外にブロードキャスト、
        // クライアント側ではローカル再生だけ → 投擲者に二重に聞こえない。
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.7f,
            0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));

        if (!level.isClientSide) {
            // サーバーのみで食料ゲージ消費 (権威あり)。
            // 満腹度4.0 = 肉アイコン半分1.0 分。まず隠し満腹度(saturation)から削られる。
            if (needsFood) {
                player.causeFoodExhaustion(cost * 4.0f);
            }

            ThrowingKnifeEntity knife = new ThrowingKnifeEntity(level, player);
            knife.setItem(stack);
            knife.setKnifeType(getKnifeType());
            knife.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, effVelocity(stack), 1.0f);

            // HOMING: ロック対象があれば優先追尾、無ければ視線先コーン内の最近接 Mob へ
            // 初期方向を補正しつつ UUID も転写して entity 側の強力追尾を有効化。
            if (getKnifeType() == KnifeType.HOMING) {
                java.util.UUID lock = the_four_primitives_and_weapons.event.HomingLockTracker.getLockedTargetUuid(player);
                LivingEntity tgt;
                if (lock != null
                        && level instanceof net.minecraft.server.level.ServerLevel sl
                        && sl.getEntity(lock) instanceof LivingEntity locked && locked.isAlive()) {
                    tgt = locked;
                    knife.setLockedTarget(lock);
                } else {
                    tgt = pickHomingTarget(player);
                }
                if (tgt != null) {
                    Vec3 to = tgt.getEyePosition().subtract(knife.position()).normalize();
                    Vec3 mv = knife.getDeltaMovement();
                    knife.setDeltaMovement(mv.normalize().scale(1.0 - 0.4)
                        .add(to.scale(0.4)).normalize().scale(mv.length()));
                }
            }
            level.addFreshEntity(knife);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        // shrink で空になると getStats が引けなくなるので、 先に確定させる。
        int cd = effCooldown(stack);
        if (consumesItem() && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, cd);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** 食料ゲージ + 隠し満腹度 の合計 (肉アイコン半分=1.0 換算) */
    private static float foodTotal(Player player) {
        return player.getFoodData().getFoodLevel() + player.getFoodData().getSaturationLevel();
    }

    private static String fmt(float v) {
        return v == (int) v ? String.valueOf((int) v) : String.valueOf(v);
    }

    private LivingEntity pickHomingTarget(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        return player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(24)).stream()
            .filter(e -> e != player && e.isAlive() && !e.isSpectator())
            .filter(e -> {
                Vec3 to = e.getEyePosition().subtract(eye).normalize();
                return to.dot(look) > 0.85; // 30°コーン
            })
            .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
            .orElse(null);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (hungerCost() > 0) {
            tooltip.add(Component.literal("§6🍖 満腹度消費: §f" + fmt(hungerCost())));
            tooltip.add(Component.literal("§8(満腹エフェクト中は消費なし)"));
        }
    }
}
