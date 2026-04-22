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
    public static final int MAX_COUNT = 10;

    /** 扇形の総角度上限 (度)。数が多いほど広がるが MAX_ARC_DEG を超えない。 */
    private static final float MAX_ARC_DEG = 50f;
    private static final float ARC_PER_KNIFE = 7f;

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

    public static int getCount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        int c = tag != null ? tag.getInt(TAG_COUNT) : 0;
        if (c <= 0) c = 1;
        if (c > MAX_COUNT) c = MAX_COUNT;
        return c;
    }

    public static void setCount(ItemStack stack, int count) {
        stack.getOrCreateTag().putInt(TAG_COUNT, Math.max(1, Math.min(MAX_COUNT, count)));
    }

    /** 通常 → スタン → スクリュー → 通常 ... */
    public static void cycleMode(ItemStack stack) {
        KnifeType[] modes = { KnifeType.NORMAL, KnifeType.STUN, KnifeType.SCREW };
        KnifeType cur = getMode(stack);
        int idx = 0;
        for (int i = 0; i < modes.length; i++) if (modes[i] == cur) { idx = i; break; }
        setMode(stack, modes[(idx + 1) % modes.length]);
    }

    // --- Mode-dependent values ----------------------------------------

    public static Item ammoFor(KnifeType mode) {
        return switch (mode) {
            case STUN  -> CustomEntityInit.STUN_KNIFE.get();
            case SCREW -> CustomEntityInit.SCREW_KNIFE.get();
            default    -> CustomEntityInit.THROWING_KNIFE.get();
        };
    }

    /** 1本あたりのMP消費 (個別アイテムの manaCost に合わせる) */
    public static double manaCostPer(KnifeType mode) {
        return switch (mode) {
            case STUN  -> 25.0;
            case SCREW -> 10.0;
            default    -> 0.0;
        };
    }

    public static int cooldownFor(KnifeType mode) {
        return switch (mode) {
            case STUN  -> 16;
            case SCREW -> 10;
            default    -> 8;
        };
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

            // 扇形スプレッド + 本数依存のブレ (多いほど精度低下)
            // fan (対称配置) + noise (ランダム散り) の合成
            float arcDeg = Math.min(MAX_ARC_DEG, toThrow * ARC_PER_KNIFE);
            // 1本=0°, 10本=~13.5° のランダム上限
            float jitterDeg = (toThrow - 1) * 1.5f;
            var rng = level.getRandom();
            for (int i = 0; i < toThrow; i++) {
                float fanYaw = toThrow == 1 ? 0f
                    : (-arcDeg / 2f) + arcDeg * i / (float)(toThrow - 1);
                float randYaw   = (rng.nextFloat() - 0.5f) * 2.0f * jitterDeg;
                float randPitch = (rng.nextFloat() - 0.5f) * 2.0f * jitterDeg;

                ThrowingKnifeEntity knife = new ThrowingKnifeEntity(level, player);
                knife.setItem(new ItemStack(ammoItem));
                knife.setKnifeType(mode);
                knife.shootFromRotation(player,
                    player.getXRot() + randPitch,
                    player.getYRot() + fanYaw + randYaw,
                    0.0f, 1.6f, 1.0f);
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
        tooltip.add(Component.literal("§7▶ 一度に投げる数: §f" + getCount(stack)
            + " §8(最大 " + MAX_COUNT + ")"));
        tooltip.add(Component.literal("§8シフト+左クリック: 技選択画面を開く"));
        tooltip.add(Component.literal("§8右クリック: 設定された技で投擲"));
        tooltip.add(Component.literal("§8※本数が多いほどブレが大きくなる"));
        double mp = manaCostPer(mode);
        if (mp > 0) {
            tooltip.add(Component.literal("§b✦ MP消費: §f" + (int)mp + " §8/本"));
        }
    }
}
