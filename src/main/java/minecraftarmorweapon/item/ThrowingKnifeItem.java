package minecraftarmorweapon.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import minecraftarmorweapon.entity.ThrowingKnifeEntity;
import minecraftarmorweapon.entity.ThrowingKnifeEntity.KnifeType;
import minecraftarmorweapon.mana.ManaHelper;

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
 *  右クリック: 飛翔体を投擲 (1個消費 / cooldown / MP消費)
 *  左クリック: 通常の近接攻撃 (攻撃力 +3.0 / 攻撃速度 やや早め)
 *
 * サブクラスは getKnifeType() / manaCost() / cooldown() / meleeBonus() をオーバーライド。
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
            BASE_ATTACK_SPEED_UUID, "Weapon modifier", -1.5, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = b.build();
    }

    /** サブクラスが上書き — 飛翔体に渡すKnifeType */
    public KnifeType getKnifeType() { return KnifeType.NORMAL; }
    /** 1投あたりMP消費 (>0なら不足時投げない) */
    public double manaCost() { return 0; }
    /** クールダウン(tick) */
    public int cooldown() { return 8; }
    /** スタック消費 (false = 弾切れ無視 = 永続。STUN等は1消費) */
    public boolean consumesItem() { return true; }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        double cost = manaCost();
        boolean needsMp = cost > 0 && !player.getAbilities().instabuild;

        // MP チェックは両サイドで実施 (UI の即時フィードバックのため)。ただし
        // 消費はサーバー側のみで行う。両サイドで tryConsume すると Attribute 同期の
        // タイムラグで片側だけ failして片側成功する "不発" が発生する。
        if (needsMp && ManaHelper.getMana(player) < cost) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("§b✦ MP不足 (" + (int)cost + ")"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        // player 引数を渡すとサーバー側では当該プレイヤー以外にブロードキャスト、
        // クライアント側ではローカル再生だけ → 投擲者に二重に聞こえない。
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.7f,
            0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));

        if (!level.isClientSide) {
            // サーバーのみで MP 消費 (権威あり)
            if (needsMp) {
                ManaHelper.setMana(player, ManaHelper.getMana(player) - cost);
            }

            ThrowingKnifeEntity knife = new ThrowingKnifeEntity(level, player);
            knife.setItem(stack);
            knife.setKnifeType(getKnifeType());
            knife.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.6f, 1.0f);

            // HOMING: ロック対象があれば優先追尾、無ければ視線先コーン内の最近接 Mob へ
            // 初期方向を補正しつつ UUID も転写して entity 側の強力追尾を有効化。
            if (getKnifeType() == KnifeType.HOMING) {
                java.util.UUID lock = minecraftarmorweapon.event.HomingLockTracker.getLockedTargetUuid(player);
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
        if (consumesItem() && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, cooldown());
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
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
        if (manaCost() > 0) {
            tooltip.add(Component.literal("§b✦ MP消費: §f" + (int)manaCost()));
        }
    }
}
