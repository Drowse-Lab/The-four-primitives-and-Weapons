package minecraftarmorweapon.item;

import minecraftarmorweapon.entity.ThrowingKnifeEntity;
import minecraftarmorweapon.entity.ThrowingKnifeEntity.KnifeType;
import minecraftarmorweapon.init.CustomEntityInit;
import minecraftarmorweapon.mana.ManaHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 投げナイフランチャー — Touhou Luna Nights 風の多重投擲武器。
 *
 *   右クリック             : 現在モード/数でインベントリの投げナイフを扇形に飛ばす
 *                            (数が足りなければ手持ちを全て放つ)
 *   シフト + 右クリック    : モード切替 (通常 → スタン → スクリュー → 通常)
 *   シフト + マウスホイール: 一度に投げる本数 1〜10 で変更 (クライアント側で検出→packet)
 *
 * アイテム自体は消耗しない (stack=1 の耐久武器的扱い)。弾は別アイテムの
 *   THROWING_KNIFE / STUN_KNIFE / SCREW_KNIFE をインベントリから消費する。
 */
public class KnifeLauncherItem extends Item {

    public static final String TAG_MODE  = "Mode";
    public static final String TAG_COUNT = "Count";
    /** モード共通の絶対上限 (通常用)。モード別上限は maxCountFor() で取得。 */
    public static final int MAX_COUNT = 10;

    /**
     * スポーン位置オフセット (プレイヤー視線に対する垂直方向の最大ずれ、ブロック単位)。
     * 値は Lisp (data/minecraft_armor_weapon/knife_launcher/formula.lisp) から読む。
     */
    public static float spawnOffsetH() { return KnifeLauncherFormula.spawnOffsetH(); }
    public static float spawnOffsetV() { return KnifeLauncherFormula.spawnOffsetV(); }

    public KnifeLauncherItem() {
        super(new Item.Properties().stacksTo(1));
    }

    // --- NBT accessors -------------------------------------------------

    public static KnifeType getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        int id = tag != null ? tag.getInt(TAG_MODE) : 0;
        return KnifeType.byId(id);
    }

    public static void setMode(ItemStack stack, KnifeType mode) {
        stack.getOrCreateTag().putInt(TAG_MODE, mode.ordinal());
    }

    /** モード別の最大投擲数上限。値は Lisp (formula.lisp) から。 */
    public static int maxCountFor(KnifeType mode) {
        return KnifeLauncherFormula.maxCountFor(mode);
    }

    public static int getCount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        int c = tag != null ? tag.getInt(TAG_COUNT) : 0;
        if (c <= 0) c = 1;
        int max = maxCountFor(getMode(stack));
        if (c > max) c = max;
        return c;
    }

    public static void setCount(ItemStack stack, int count) {
        int max = maxCountFor(getMode(stack));
        stack.getOrCreateTag().putInt(TAG_COUNT, Math.max(1, Math.min(max, count)));
    }

    /** 通常 → スタン → スクリュー → 通常 ... 新モードの上限より大きな数は clamp する。 */
    public static void cycleMode(ItemStack stack) {
        KnifeType[] modes = { KnifeType.NORMAL, KnifeType.STUN, KnifeType.SCREW };
        KnifeType cur = getMode(stack);
        int idx = 0;
        for (int i = 0; i < modes.length; i++) if (modes[i] == cur) { idx = i; break; }
        KnifeType next = modes[(idx + 1) % modes.length];
        setMode(stack, next);
        // モード変更で上限が下がる場合はその場で clamp (setMode が既に tag を確保している)
        int max = maxCountFor(next);
        if (stack.getOrCreateTag().getInt(TAG_COUNT) > max) setCount(stack, max);
    }

    // --- Mode-dependent values ----------------------------------------

    public static Item ammoFor(KnifeType mode) {
        return switch (mode) {
            case STUN  -> CustomEntityInit.STUN_KNIFE.get();
            case SCREW -> CustomEntityInit.SCREW_KNIFE.get();
            default    -> CustomEntityInit.THROWING_KNIFE.get();
        };
    }

    /** 1本あたりのMP消費 (formula.lisp の mana-xxx で調整可能) */
    public static double manaCostPer(KnifeType mode) {
        return KnifeLauncherFormula.manaCostFor(mode);
    }

    /** クールダウン (tick) — formula.lisp の cooldown-xxx で調整可能 */
    public static int cooldownFor(KnifeType mode) {
        return KnifeLauncherFormula.cooldownFor(mode);
    }

    public static String modeLabel(KnifeType m) {
        return switch (m) {
            case STUN  -> "§eスタン";
            case SCREW -> "§bスクリュー";
            case GRIP  -> "§7グリップ";
            case HOMING-> "§dホーミング";
            default    -> "§f通常";
        };
    }

    // --- Interaction ---------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        KnifeType mode = getMode(stack);
        int requested = getCount(stack);
        boolean creative = player.getAbilities().instabuild;

        // 弾の在庫確認: 不足なら持っている分だけ放つ
        Item ammoItem = ammoFor(mode);
        int available = creative ? requested : countItem(player, ammoItem);
        int toThrow = Math.min(requested, available);
        if (toThrow <= 0) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                    Component.literal("§c✦ " + ammoItem.getDescription().getString() + " が足りない"),
                    true);
            }
            return InteractionResultHolder.fail(stack);
        }

        // MP チェック (数分まとめて) — サーバー消費のみ
        double totalMp = manaCostPer(mode) * toThrow;
        boolean needsMp = totalMp > 0 && !creative;
        if (needsMp && ManaHelper.getMana(player) < totalMp) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                    Component.literal("§b✦ MP不足 (" + (int)totalMp + ")"),
                    true);
            }
            return InteractionResultHolder.fail(stack);
        }

        // サウンド (投擲者は client 側のみ, 他者には server broadcast)
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8f,
            0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));

        if (!level.isClientSide) {
            if (needsMp) {
                ManaHelper.setMana(player, ManaHelper.getMana(player) - totalMp);
            }

            // 全員同じ方向 (プレイヤーの視線) に飛ぶが、スポーン位置を視線に対して
            // 横/縦にランダムにずらす。扇形ではない "同方向別位置から同時発射" 挙動。
            Vec3 look = player.getLookAngle().normalize();
            // 視線に直交する basis を作る
            Vec3 worldUp = new Vec3(0, 1, 0);
            Vec3 right = look.cross(worldUp);
            if (right.lengthSqr() < 1e-6) {
                // 真上/真下を向いている時のフォールバック
                right = new Vec3(1, 0, 0);
            } else {
                right = right.normalize();
            }
            Vec3 up = right.cross(look).normalize();

            double eyeX = player.getX();
            double eyeY = player.getEyeY() - 0.1;
            double eyeZ = player.getZ();

            // 本数に応じて散布幅をスケール: 1本=0 (ピンポイント) / 上限=1.0 (最大広がり)。
            // モード別の最大本数を分母にするので、スタン 3本/スクリュー 5本でも "上限本数=最大広がり" の
            // 体感になり、本数を減らせば素直に狙いが絞れる。
            int maxForMode = maxCountFor(mode);
            float spreadScale = maxForMode <= 1 ? 0f
                : (toThrow - 1) / (float)(maxForMode - 1);

            var rng = level.getRandom();
            for (int i = 0; i < toThrow; i++) {
                // -1.0〜+1.0 の uniform ランダム × 最大オフセット × 本数スケール
                double randH = (rng.nextDouble() - 0.5) * 2.0 * KnifeLauncherFormula.spawnOffsetH() * spreadScale;
                double randV = (rng.nextDouble() - 0.5) * 2.0 * KnifeLauncherFormula.spawnOffsetV() * spreadScale;
                double offX = right.x * randH + up.x * randV;
                double offY = right.y * randH + up.y * randV;
                double offZ = right.z * randH + up.z * randV;

                ThrowingKnifeEntity knife = new ThrowingKnifeEntity(level, player);
                knife.setItem(new ItemStack(ammoItem));
                knife.setKnifeType(mode);
                // 同じ向きで発射 (ブレなし、全弾同方向)。初速は formula.lisp の shoot-velocity。
                knife.shootFromRotation(player,
                    player.getXRot(), player.getYRot(),
                    0.0f, KnifeLauncherFormula.shootVelocity(), 1.0f);
                // スポーン位置を視線垂直方向にランダムにオフセット
                knife.setPos(eyeX + offX, eyeY + offY, eyeZ + offZ);
                level.addFreshEntity(knife);
            }

            // 弾を消費 (クリエイティブは消費しない)
            if (!creative) consumeItems(player, ammoItem, toThrow);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(this, cooldownFor(mode));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // --- Inventory helpers --------------------------------------------

    private static int countItem(Player p, Item item) {
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() == item) n += s.getCount();
        }
        return n;
    }

    private static void consumeItems(Player p, Item item, int amount) {
        int remaining = amount;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() == item) {
                int take = Math.min(s.getCount(), remaining);
                s.shrink(take);
                remaining -= take;
            }
        }
    }

    // --- Tooltip -------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        KnifeType mode = getMode(stack);
        tooltip.add(Component.literal("§7▶ モード: " + modeLabel(mode)));
        int max = maxCountFor(mode);
        tooltip.add(Component.literal("§7▶ 一度に投げる数: §f" + getCount(stack)
            + "§8 / " + max));
        tooltip.add(Component.literal("§8シフト+左クリック: 技選択画面を開く"));
        tooltip.add(Component.literal("§8右クリック: 設定された技で投擲"));
        tooltip.add(Component.literal("§8※同方向に飛ぶが位置がランダムにずれる"));
        double mp = manaCostPer(mode);
        if (mp > 0) {
            tooltip.add(Component.literal("§b✦ MP消費: §f" + (int)mp + " §8/本"));
        }
    }
}
