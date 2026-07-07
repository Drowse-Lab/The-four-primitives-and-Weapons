package the_four_primitives_and_weapons.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;
import the_four_primitives_and_weapons.damage.IElementalDamageSource;

/**
 * デバッグ用Mob（サンドバッグ）
 *
 * 特徴:
 * - 攻撃しない、動かない
 * - HP 1024 でほぼ不死身
 * - 受けたダメージをチャットに表示
 * - 現在のHPをネームタグに表示
 */
public class DebugMobEntity extends PathfinderMob {

    /** true の間、被弾/死亡サウンドを鳴らさない (kill時の静音消滅・再スポーン用)。 */
    public boolean silentSound = false;

    public DebugMobEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1024.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.ATTACK_DAMAGE, 0.0)
            .add(Attributes.ARMOR, 0.0)
            .add(Attributes.ARMOR_TOUGHNESS, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 0.0);
    }

    @Override
    protected void registerGoals() {
        // プレイヤーを見るだけ
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 16.0f));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);

        if (!this.level().isClientSide && source.getEntity() instanceof Player player) {
            String damageType = source.type().msgId();
            float currentHP = this.getHealth();
            float maxHP = this.getMaxHealth();

            // 基本ダメージ表示
            player.displayClientMessage(Component.literal(
                String.format("§a[Debug] §fダメージ: §c%.1f §7(種類: %s) §fHP: §e%.1f§7/§e%.0f",
                    amount, damageType, currentHP, maxHP)
            ), false);

            // 属性ダメージ表示: 攻撃者の武器の属性をチェック
            ItemStack weapon = player.getMainHandItem();
            if (ElementalDamageUtils.hasElement(weapon)) {
                ElementType elemType = ElementalDamageUtils.getElementType(weapon);
                int elemLevel = ElementalDamageUtils.getElementLevel(weapon);
                String elemColor = getElementColor(elemType);
                player.displayClientMessage(Component.literal(
                    String.format("§a[Debug] §f属性: %s%s Lv.%d §7(武器NBT)",
                        elemColor, elemType.getName().toUpperCase(), elemLevel)
                ), false);
            }

            // DamageSource自体に属性が付いている場合（属性追加ダメージ等）
            if (source instanceof IElementalDamageSource elemSource) {
                ElementType srcType = elemSource.getElementType();
                if (srcType != null && srcType != ElementType.NONE) {
                    int srcLevel = elemSource.getElementLevel();
                    String srcColor = getElementColor(srcType);
                    player.displayClientMessage(Component.literal(
                        String.format("§a[Debug] §f属性DmgSrc: %s%s Lv.%d §c%.1fダメージ",
                            srcColor, srcType.getName().toUpperCase(), srcLevel, amount)
                    ), false);
                }
            }

            // HPをリセット（サンドバッグなので常に満タン）
            this.setHealth(this.getMaxHealth());
        }

        return result;
    }

    @Override
    public Component getName() {
        float hp = this.getHealth();
        float maxHp = this.getMaxHealth();
        return Component.literal(String.format("§eDebug Mob §7[HP: %.0f/%.0f]", hp, maxHp));
    }

    /**
     * 右クリックでアイテムを持たせる
     * - 防具 → 対応する防具スロット
     * - 盾 → オフハンド
     * - その他 → メインハンド
     * スニーク+右クリック → 全装備をドロップしてリセット
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // スニーク+右クリック → 装備リセット
        if (player.isShiftKeyDown()) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack equipped = this.getItemBySlot(slot);
                if (!equipped.isEmpty()) {
                    this.spawnAtLocation(equipped.copy());
                    this.setItemSlot(slot, ItemStack.EMPTY);
                }
            }
            player.displayClientMessage(Component.literal("§a[Debug] §f装備をリセットしました"), false);
            return InteractionResult.SUCCESS;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.isEmpty()) {
            // 空の手で右クリック → 現在の装備を表示
            player.displayClientMessage(Component.literal("§a[Debug] §f--- 装備一覧 ---"), false);
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack equipped = this.getItemBySlot(slot);
                if (!equipped.isEmpty()) {
                    player.displayClientMessage(Component.literal(
                        String.format("§7  %s: §f%s", slot.getName(), equipped.getHoverName().getString())
                    ), false);
                }
            }
            return InteractionResult.SUCCESS;
        }

        // アイテムを持たせる
        EquipmentSlot targetSlot = getSlotForItem(heldItem);
        ItemStack previous = this.getItemBySlot(targetSlot);

        // 既に装備があればプレイヤーに返す
        if (!previous.isEmpty()) {
            player.addItem(previous.copy());
        }

        this.setItemSlot(targetSlot, heldItem.split(1));
        this.setDropChance(targetSlot, 1.0f);

        player.displayClientMessage(Component.literal(
            String.format("§a[Debug] §f%s §7→ §e%s", targetSlot.getName(), this.getItemBySlot(targetSlot).getHoverName().getString())
        ), false);

        return InteractionResult.SUCCESS;
    }

    /**
     * アイテムに対応する装備スロットを判定
     */
    private EquipmentSlot getSlotForItem(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem armorItem) {
            return armorItem.getEquipmentSlot();
        }
        if (stack.getItem() instanceof ShieldItem) {
            return EquipmentSlot.OFFHAND;
        }
        return EquipmentSlot.MAINHAND;
    }

    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
        // ノックバック無効
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return silentSound ? null : SoundEvents.NOTE_BLOCK_PLING.value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return silentSound ? null : SoundEvents.NOTE_BLOCK_BASS.value();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    private static String getElementColor(ElementType type) {
        switch (type) {
            case ICE:       return "§b";  // 水色
            case ELECTRIC:  return "§e";  // 黄色
            case THUNDER:   return "§e";  // 黄色
            case CORROSION: return "§5";  // 紫
            case HOLY:      return "§6";  // 金
            case DARK:      return "§8";  // 灰
            case FIRE:      return "§c";  // 赤
            case WIND:      return "§a";  // 緑
            case WATER:     return "§9";  // 青
            case MIASMA:    return "§5";  // 暗紫
            case SOUL:      return "§b";  // 青白
            case SOUL_FIRE: return "§b";  // 燐火
            default:        return "§7";  // グレー
        }
    }

    public static void init() {
    }
}
