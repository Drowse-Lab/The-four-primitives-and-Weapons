package the_four_primitives_and_weapons.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;

import the_four_primitives_and_weapons.damage.DummyDamageStats;
import the_four_primitives_and_weapons.damage.ElementDamageKind;
import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalDebugTrace;
import the_four_primitives_and_weapons.damage.ElementalDoTHandler;

/**
 * ターゲットダミー — 属性ダメージ計測用のサンドバッグ。
 *
 * <p>殴った分を集計して頭上に出し、被弾ごとにダメージ数値をポップアップ表示する。
 * 計測できるもの:</p>
 * <ul>
 *   <li>実ダメージの累計 / ヒット数 / 平均 / 最大・最小 / DPS</li>
 *   <li>属性が足した分の、与え方別 ( 物理 / 魔法 / 蓄積 ) の内訳</li>
 *   <li>属性ごとの累計</li>
 *   <li>DoT ( 蓄積・出血・闇の継続 ) の残り時間と累計</li>
 * </ul>
 *
 * <p>操作:</p>
 * <ul>
 *   <li>素手で右クリック … 計測レポートを表示</li>
 *   <li>スニーク + 素手で右クリック … 計測をリセット</li>
 *   <li>アイテムを持って右クリック … 装備させる ( 防具を着せて貫通の検証ができる )</li>
 *   <li>スニーク + アイテムを持って右クリック … 装備をすべて外す</li>
 * </ul>
 *
 * <p>ダメージクールダウン ( 無敵時間 ) を無視するので、連撃も DoT も 1 発残らず計測する。
 * バニラの無敵時間がある実戦より DoT は多めに入る点に注意。</p>
 */
public class TargetDummyEntity extends PathfinderMob {

    /** 頭上サマリを作り直す間隔 (tick)。 */
    private static final int SUMMARY_INTERVAL = 10;
    /** 「属性が乗っていない一撃」を表す ordinal。 */
    public static final int NO_KIND = -1;

    /** 直近の被弾の通し番号。 クライアントはこれが変わったらポップアップを 1 つ出す。 */
    private static final EntityDataAccessor<Integer> DATA_HIT_SEQ =
        SynchedEntityData.defineId(TargetDummyEntity.class, EntityDataSerializers.INT);
    /** 直近の被弾の実ダメージ。 */
    private static final EntityDataAccessor<Float> DATA_LAST_DAMAGE =
        SynchedEntityData.defineId(TargetDummyEntity.class, EntityDataSerializers.FLOAT);
    /** 直近の被弾に乗っていた属性 ({@link ElementType} の ordinal)。 */
    private static final EntityDataAccessor<Integer> DATA_LAST_ELEMENT =
        SynchedEntityData.defineId(TargetDummyEntity.class, EntityDataSerializers.INT);
    /** 直近の被弾の与え方 ({@link ElementDamageKind} の ordinal、属性なしは {@link #NO_KIND})。 */
    private static final EntityDataAccessor<Integer> DATA_LAST_KIND =
        SynchedEntityData.defineId(TargetDummyEntity.class, EntityDataSerializers.INT);
    /** 頭上に出す集計サマリ ( 整形済み文字列 )。 */
    private static final EntityDataAccessor<String> DATA_SUMMARY =
        SynchedEntityData.defineId(TargetDummyEntity.class, EntityDataSerializers.STRING);

    private final DummyDamageStats stats = new DummyDamageStats();

    public TargetDummyEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
        this.setPersistenceRequired();
        this.setNoAi(true);          // 動かない・向きも変えない
        this.setInvulnerable(false); // 殴れる ( 死なないのは hurt 側で担保 )
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
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_HIT_SEQ, 0);
        this.entityData.define(DATA_LAST_DAMAGE, 0.0f);
        this.entityData.define(DATA_LAST_ELEMENT, ElementType.NONE.ordinal());
        this.entityData.define(DATA_LAST_KIND, NO_KIND);
        this.entityData.define(DATA_SUMMARY, "");
    }

    @Override
    protected void registerGoals() {
        // AI なし ( サンドバッグ )
    }

    // ────────────────────────────────────────────────────────────
    // 計測
    // ────────────────────────────────────────────────────────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // /kill や奈落など「無敵を貫通する」ダメージだけは通常どおり死なせる
        boolean forcedKill = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);

        if (!forcedKill) {
            // ダメージクールダウンを無視して 1 発残らず計測する
            this.invulnerableTime = 0;
            // 1 発で削り切るダメージでも死なないようクランプする
            amount = Math.min(amount, Math.max(0.0f, this.getHealth() - 1.0f));
        }

        float hpBefore = this.getHealth();
        boolean result = super.hurt(source, amount);

        if (!forcedKill && !this.level().isClientSide) {
            // 実際に HP が減った量 = 防具軽減も魔法貫通も反映した後の実ダメージ
            recordDamage(source, Math.max(0.0f, hpBefore - this.getHealth()));
            // サンドバッグなので常に満タンに戻す
            this.setHealth(this.getMaxHealth());
            this.entityData.set(DATA_SUMMARY, buildSummary());
        }

        return result;
    }

    private void recordDamage(DamageSource source, float actualDamage) {
        if (actualDamage <= 0.0f) return;
        long now = this.level().getGameTime();

        // 属性処理が「属性で何ダメージ足したか」を記録していれば取り出す
        ElementalDebugTrace.Entry trace = ElementalDebugTrace.consume(this);

        // 攻撃者のいないダメージで DoT がかかっていれば、継続ダメージの 1tick 分とみなす
        boolean isDot = source.getEntity() == null && ElementalDoTHandler.isActive(this);

        if (isDot) {
            ElementType dotElement = ElementalDoTHandler.getActiveElement(this);
            stats.recordDot(now, dotElement, actualDamage);
            pushPopup(actualDamage, dotElement, ElementDamageKind.BUILDUP);
        } else {
            stats.recordHit(now, actualDamage, trace);
            pushPopup(actualDamage,
                    trace != null ? trace.type : ElementType.NONE,
                    trace != null ? trace.kind : null);
        }
    }

    /** クライアントにダメージ数値ポップアップを 1 つ出させる。 */
    private void pushPopup(float damage, ElementType element, ElementDamageKind kind) {
        this.entityData.set(DATA_LAST_DAMAGE, damage);
        this.entityData.set(DATA_LAST_ELEMENT,
                element != null ? element.ordinal() : ElementType.NONE.ordinal());
        this.entityData.set(DATA_LAST_KIND, kind != null ? kind.ordinal() : NO_KIND);
        this.entityData.set(DATA_HIT_SEQ, this.entityData.get(DATA_HIT_SEQ) + 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        // DoT の残り時間などが動くので、被弾していなくても定期的に作り直す
        if (this.tickCount % SUMMARY_INTERVAL == 0) {
            this.entityData.set(DATA_SUMMARY, buildSummary());
        }
    }

    /** 頭上に出す 1 行サマリを組み立てる。 */
    private String buildSummary() {
        StringBuilder sb = new StringBuilder();

        if (stats.isEmpty()) {
            sb.append("§7殴ると計測開始");
        } else {
            sb.append(String.format("§f総 §c%.1f §7| §f%dhit §7| §f平均 §e%.1f",
                    stats.getTotal(), stats.getHits(), stats.getAverageHit()));
            float dps = stats.getDps();
            if (dps >= 0.0f) {
                sb.append(String.format(" §7| §bDPS %.1f", dps));
            }
        }

        int remainTick = ElementalDoTHandler.getRemainingTick(this);
        if (remainTick > 0) {
            sb.append(String.format(" §7| §2DoT %s %.2f/t 残り%.1fs",
                    ElementalDoTHandler.getActiveElement(this).getName().toUpperCase(),
                    ElementalDoTHandler.getDamagePerTick(this), remainTick / 20.0f));
        }
        return sb.toString();
    }

    public String getSummary() {
        return this.entityData.get(DATA_SUMMARY);
    }

    public int getHitSeq() {
        return this.entityData.get(DATA_HIT_SEQ);
    }

    public float getLastDamage() {
        return this.entityData.get(DATA_LAST_DAMAGE);
    }

    public ElementType getLastElement() {
        int ordinal = this.entityData.get(DATA_LAST_ELEMENT);
        ElementType[] values = ElementType.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ElementType.NONE;
    }

    /** 直近の被弾の与え方。 属性が乗っていなければ null。 */
    public ElementDamageKind getLastKind() {
        int ordinal = this.entityData.get(DATA_LAST_KIND);
        ElementDamageKind[] values = ElementDamageKind.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    // ────────────────────────────────────────────────────────────
    // レポート
    // ────────────────────────────────────────────────────────────

    private void sendReport(Player player) {
        player.displayClientMessage(Component.literal("§6=== ターゲットダミー 計測 ==="), false);

        if (stats.isEmpty()) {
            player.displayClientMessage(Component.literal("§7まだ何も当たっていません"), false);
            return;
        }

        player.displayClientMessage(Component.literal(String.format(
                "§f実ダメージ: 総 §c%.1f §7/ §f%dhit §7/ §f平均 §e%.1f §7/ §f最大 §e%.1f §7/ §f最小 §e%.1f",
                stats.getTotal(), stats.getHits(), stats.getAverageHit(),
                stats.getMaxHit(), stats.getMinHit())), false);

        float dps = stats.getDps();
        player.displayClientMessage(Component.literal(dps >= 0.0f
                ? String.format("§f計測時間: §e%.1f秒 §7/ §bDPS %.1f", stats.getElapsedSeconds(), dps)
                : "§f計測時間: §7— (2発以上当てるとDPSが出ます)"), false);

        // 与え方別の内訳 ( 属性が足した分 )
        StringBuilder kinds = new StringBuilder("§f属性が足した分: ");
        boolean anyKind = false;
        for (ElementDamageKind kind : ElementDamageKind.values()) {
            float value = stats.getByKind(kind);
            if (value == 0.0f) continue;
            if (anyKind) kinds.append(" §7/ ");
            kinds.append(String.format("%s%s §c%+.1f", kindColor(kind), kindLabel(kind), value));
            anyKind = true;
        }
        player.displayClientMessage(Component.literal(
                anyKind ? kinds.toString() : "§f属性が足した分: §7なし"), false);
        if (stats.getByKind(ElementDamageKind.BUILDUP) != 0.0f) {
            player.displayClientMessage(Component.literal(
                "§8  ※蓄積は一発では入らない予定値。実際に入った分は下の DoT に出ます"), false);
        }

        // 属性ごとの累計
        StringBuilder elements = new StringBuilder("§f属性別: ");
        boolean anyElement = false;
        for (ElementType element : ElementType.values()) {
            float value = stats.getByElement(element);
            if (value == 0.0f) continue;
            if (anyElement) elements.append(" §7/ ");
            elements.append(String.format("%s%s §c%+.1f",
                    elementColor(element), element.getName().toUpperCase(), value));
            anyElement = true;
        }
        if (anyElement) {
            player.displayClientMessage(Component.literal(elements.toString()), false);
        }

        // DoT
        if (stats.getDotTotal() > 0.0f) {
            StringBuilder dot = new StringBuilder(String.format(
                    "§2DoT: 累計 §c%.1f §7/ §f%dtick", stats.getDotTotal(), stats.getDotTicks()));
            for (ElementType element : ElementType.values()) {
                float value = stats.getDotByElement(element);
                if (value == 0.0f) continue;
                dot.append(String.format(" §7/ %s%s §c%.1f",
                        elementColor(element), element.getName().toUpperCase(), value));
            }
            player.displayClientMessage(Component.literal(dot.toString()), false);
        }
        int remainTick = ElementalDoTHandler.getRemainingTick(this);
        if (remainTick > 0) {
            player.displayClientMessage(Component.literal(String.format(
                    "§2DoT 継続中: %s%s §7/ §f%.2f/tick §7/ 残り §e%.1f秒",
                    elementColor(ElementalDoTHandler.getActiveElement(this)),
                    ElementalDoTHandler.getActiveElement(this).getName().toUpperCase(),
                    ElementalDoTHandler.getDamagePerTick(this), remainTick / 20.0f)), false);
        }

        // 防具を着せているなら貫通の見え方の参考として出す
        double armor = this.getAttributeValue(Attributes.ARMOR);
        if (armor > 0.0) {
            player.displayClientMessage(Component.literal(String.format(
                    "§7防具値: §f%.0f §8(魔法属性ダメージはこれを貫通します)", armor)), false);
        }
    }

    // ────────────────────────────────────────────────────────────
    // 操作
    // ────────────────────────────────────────────────────────────

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (held.isEmpty()) {
            if (player.isShiftKeyDown()) {
                stats.reset();
                this.entityData.set(DATA_SUMMARY, buildSummary());
                player.displayClientMessage(Component.literal("§a[ダミー] §f計測をリセットしました"), false);
            } else {
                sendReport(player);
            }
            return InteractionResult.SUCCESS;
        }

        // スニーク + アイテム → 装備を全部外す
        if (player.isShiftKeyDown()) {
            boolean removed = false;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack equipped = this.getItemBySlot(slot);
                if (!equipped.isEmpty()) {
                    this.spawnAtLocation(equipped.copy());
                    this.setItemSlot(slot, ItemStack.EMPTY);
                    removed = true;
                }
            }
            player.displayClientMessage(Component.literal(removed
                    ? "§a[ダミー] §f装備をすべて外しました"
                    : "§a[ダミー] §7装備していません"), false);
            return InteractionResult.SUCCESS;
        }

        // アイテムを持たせる / 着せる
        EquipmentSlot targetSlot = getSlotForItem(held);
        ItemStack previous = this.getItemBySlot(targetSlot);
        if (!previous.isEmpty()) {
            player.addItem(previous.copy());
        }
        this.setItemSlot(targetSlot, held.split(1));
        this.setDropChance(targetSlot, 1.0f);

        player.displayClientMessage(Component.literal(String.format(
                "§a[ダミー] §f%s §7→ §e%s",
                targetSlot.getName(), this.getItemBySlot(targetSlot).getHoverName().getString())), false);
        return InteractionResult.SUCCESS;
    }

    private EquipmentSlot getSlotForItem(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem armorItem) {
            return armorItem.getEquipmentSlot();
        }
        if (stack.getItem() instanceof ShieldItem) {
            return EquipmentSlot.OFFHAND;
        }
        return EquipmentSlot.MAINHAND;
    }

    // ────────────────────────────────────────────────────────────
    // サンドバッグとしての振る舞い
    // ────────────────────────────────────────────────────────────

    @Override
    public boolean canAttack(LivingEntity target) {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
        // ノックバック無効 ( 殴っても位置がずれない )
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.NOTE_BLOCK_PLING.value();
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

    // ────────────────────────────────────────────────────────────

    private static String kindLabel(ElementDamageKind kind) {
        switch (kind) {
            case MAGIC:   return "魔法";
            case BUILDUP: return "蓄積";
            default:      return "物理";
        }
    }

    private static String kindColor(ElementDamageKind kind) {
        switch (kind) {
            case MAGIC:   return "§d";
            case BUILDUP: return "§2";
            default:      return "§7";
        }
    }

    /** 属性ごとの表示色 ( デバッグMobの配色に合わせる )。 */
    public static String elementColor(ElementType type) {
        switch (type) {
            case ICE:       return "§b";
            case ELECTRIC:  return "§e";
            case THUNDER:   return "§b";
            case CORROSION: return "§d";
            case ERASURE:   return "§5";
            case HOLY:      return "§6";
            case DARK:      return "§8";
            case FIRE:      return "§c";
            case WIND:      return "§a";
            case WATER:     return "§9";
            case MIASMA:    return "§5";
            case BLOOD:     return "§4";
            case SOUL:      return "§b";
            case SOUL_FIRE: return "§b";
            default:        return "§7";
        }
    }
}
