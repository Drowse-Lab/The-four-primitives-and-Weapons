package minecraftarmorweapon.item;

import minecraftarmorweapon.entity.ThrowingKnifeEntity;
import minecraftarmorweapon.entity.ThrowingKnifeEntity.KnifeType;
import minecraftarmorweapon.init.CustomEntityInit;
import minecraftarmorweapon.mana.ManaHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
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

    public static final String TAG_MODE   = "Mode";
    public static final String TAG_COUNT  = "Count";
    public static final String TAG_STORED = "StoredKnives";
    /** モード共通の絶対上限 (通常用)。モード別上限は maxCountFor() で取得。 */
    public static final int MAX_COUNT = 10;
    /** ホルダー内に格納できる投げナイフの上限 */
    public static final int MAX_STORED = 1024;

    /**
     * スポーン位置オフセット (プレイヤー視線に対する垂直方向の最大ずれ、ブロック単位)。
     * 値は Lisp (data/minecraft_armor_weapon/knife_launcher/formula.lisp) から読む。
     */
    public static float spawnOffsetH() { return KnifeLauncherFormula.spawnOffsetH(); }
    public static float spawnOffsetV() { return KnifeLauncherFormula.spawnOffsetV(); }

    public KnifeLauncherItem() {
        super(new Item.Properties().stacksTo(1));
    }

    // Infinity 付与を許可する。通常は弓用 (EnchantmentCategory.BOW) だが、
    // 本ランチャーは「投擲武器」として同カテゴリのエンチャントを受け入れる。
    @Override
    public boolean isEnchantable(ItemStack stack) { return true; }

    @Override
    public int getEnchantmentValue() { return 5; }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack,
            net.minecraft.world.item.enchantment.Enchantment enchantment) {
        if (enchantment == net.minecraft.world.item.enchantment.Enchantments.INFINITY_ARROWS) return true;
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }

    /** 現在装着されている Infinity のレベル。0 = なし。 */
    private static int infinityLevel(ItemStack stack) {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
            net.minecraft.world.item.enchantment.Enchantments.INFINITY_ARROWS, stack);
    }

    // ----- Bundle 風のインベントリ操作 ------------------------------

    /** 内蔵できるのは通常の投げナイフだけ */
    private static boolean isAmmo(ItemStack s) {
        return !s.isEmpty() && s.getItem() == CustomEntityInit.THROWING_KNIFE.get();
    }

    /**
     * インベントリで「ランチャーをカーソル、他スロットに向けて右クリック」した時。
     * - 投げナイフのスロットを右クリック → その分をランチャーに吸収
     * - 空スロットを右クリック → ランチャーから投げナイフを 1 本そのスロットに出す
     */
    @Override
    public boolean overrideStackedOnOther(ItemStack holder, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack target = slot.getItem();
        if (target.isEmpty()) {
            // 空スロットへ先頭スタックを取り出し (LIFO)
            ItemStack popped = popStack(holder);
            if (popped.isEmpty()) return false;
            ItemStack remainder = slot.safeInsert(popped);
            if (!remainder.isEmpty()) {
                // スロットに全部入らなかった分は戻す
                insertStack(holder, remainder);
            }
            playRemoveOneSound(player);
            return true;
        }
        if (isAmmo(target)) {
            // 吸収 (同種はマージ、別 NBT なら新スタックとして先頭に入る)
            int added = insertStack(holder, target);
            if (added <= 0) return false;
            target.shrink(added);
            playInsertSound(player);
            return true;
        }
        return false;
    }

    /**
     * インベントリで「ランチャーを他スロット, カーソルにアイテム」で右クリックされた時。
     * - カーソルに投げナイフ → ランチャーに吸収
     * - カーソル空 → ランチャーから 1 本カーソルへ
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack holder, ItemStack cursor, Slot slot,
                                            ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        if (cursor.isEmpty()) {
            // ランチャーから先頭スタックをカーソルへ (LIFO で最新スタックが出る)
            ItemStack popped = popStack(holder);
            if (popped.isEmpty()) return false;
            access.set(popped);
            playRemoveOneSound(player);
            return true;
        }
        if (isAmmo(cursor)) {
            int added = insertStack(holder, cursor);
            if (added <= 0) return false;
            cursor.shrink(added);
            playInsertSound(player);
            return true;
        }
        return false;
    }

    private static void playInsertSound(Player p) {
        p.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 0.8f + p.getRandom().nextFloat() * 0.4f);
    }

    private static void playRemoveOneSound(Player p) {
        p.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 0.8f + p.getRandom().nextFloat() * 0.4f);
    }

    // ----- 耐久バー表示 (Bundle 風) --------------------------------

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getStored(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        // 0 = 0 幅, 13 = 最大幅
        return Math.min(1 + 12 * getStored(stack) / MAX_STORED, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // Bundle と同じ黄色系
        return 0xFFEE66;
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

    // --- 内蔵ナイフ在庫 (ホルダー内格納: 複数スタックを LIFO 順で保持) ----

    /** 格納スタックのリスト (NBT の TAG_STORED を ListTag として扱う) */
    private static ListTag getList(ItemStack holder) {
        CompoundTag tag = holder.getOrCreateTag();
        if (!tag.contains(TAG_STORED, Tag.TAG_LIST)) {
            // 旧形式 (int カウント) からのマイグレーション: PER_ENTRY_MAX で分割
            if (tag.contains(TAG_STORED, Tag.TAG_INT)) {
                int legacy = tag.getInt(TAG_STORED);
                tag.remove(TAG_STORED);
                ListTag list = new ListTag();
                int remaining = Math.max(0, Math.min(MAX_STORED, legacy));
                while (remaining > 0) {
                    int put = Math.min(PER_ENTRY_MAX, remaining);
                    ItemStack migrated = new ItemStack(CustomEntityInit.THROWING_KNIFE.get(), put);
                    CompoundTag entry = new CompoundTag();
                    migrated.save(entry);
                    list.add(entry);
                    remaining -= put;
                }
                tag.put(TAG_STORED, list);
                return list;
            }
            ListTag empty = new ListTag();
            tag.put(TAG_STORED, empty);
            return empty;
        }
        return tag.getList(TAG_STORED, Tag.TAG_COMPOUND);
    }

    /** 内蔵されている全スタックを新しいリストで返す (先頭が最新) */
    public static java.util.List<ItemStack> getStoredStacks(ItemStack holder) {
        ListTag list = getList(holder);
        java.util.List<ItemStack> out = new java.util.ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            out.add(ItemStack.of(list.getCompound(i)));
        }
        return out;
    }

    /**
     * 1 エントリあたりの最大 count。NBT の ItemStack.Count は signed byte (最大 127) なので
     * これを超えるとオーバーフローして在庫が壊れる (ユーザー報告: 112本以上で壊れる)。
     * 安全マージンを取って 64 をエントリ上限とし、超える分は新規スタックに分割する。
     */
    public static final int PER_ENTRY_MAX = 64;

    /** 合計本数 (全スタックの count 合算) — すべて ItemStack.of 経由で読むので型安全 */
    public static int getStored(ItemStack holder) {
        ListTag list = getList(holder);
        int total = 0;
        for (int i = 0; i < list.size(); i++) {
            total += ItemStack.of(list.getCompound(i)).getCount();
        }
        return Math.min(total, MAX_STORED);
    }

    /**
     * カーソルのスタックを内蔵に追加。同種スタックがあれば PER_ENTRY_MAX まで詰めて先頭に移動、
     * それでも残ったら新規スタック (こちらも PER_ENTRY_MAX で分割)。
     * ByteTag オーバーフローを避けるため 1 エントリを PER_ENTRY_MAX までに抑える。
     */
    public static int insertStack(ItemStack holder, ItemStack incoming) {
        if (incoming.isEmpty() || incoming.getItem() != CustomEntityInit.THROWING_KNIFE.get()) return 0;
        int total = getStored(holder);
        int room = MAX_STORED - total;
        if (room <= 0) return 0;
        int take = Math.min(room, incoming.getCount());
        if (take <= 0) return 0;

        ListTag list = getList(holder);
        int added = 0;

        // 1) 同種エントリ (PER_ENTRY_MAX 未満) を探して詰められるだけ詰める
        //    複数の同種エントリが並ぶ場合は先頭から順に満タンにする
        for (int i = 0; i < list.size() && added < take; i++) {
            CompoundTag entry = list.getCompound(i);
            ItemStack existing = ItemStack.of(entry);
            if (!ItemStack.isSameItemSameTags(existing, incoming)) continue;
            int space = PER_ENTRY_MAX - existing.getCount();
            if (space <= 0) continue;
            int put = Math.min(space, take - added);
            existing.grow(put);
            CompoundTag merged = new CompoundTag();
            existing.save(merged);
            list.remove(i);
            list.add(0, merged); // 触れたエントリは先頭へ (LIFO)
            added += put;
        }

        // 2) 残りは新規エントリとして PER_ENTRY_MAX 単位で作る
        while (added < take) {
            int put = Math.min(PER_ENTRY_MAX, take - added);
            ItemStack copy = incoming.copy();
            copy.setCount(put);
            CompoundTag entry = new CompoundTag();
            copy.save(entry);
            list.add(0, entry);
            added += put;
        }

        return added;
    }

    /** 先頭 1 スタックを取り出し (空なら EMPTY) */
    public static ItemStack popStack(ItemStack holder) {
        ListTag list = getList(holder);
        if (list.isEmpty()) return ItemStack.EMPTY;
        CompoundTag first = list.getCompound(0);
        list.remove(0);
        return ItemStack.of(first);
    }

    /**
     * 指定本数を先頭のスタックから順に消費。戻り値は実際に消費できた本数。
     * 投擲時の Ammo 消費に使う。
     */
    public static int consumeStored(ItemStack holder, int amount) {
        if (amount <= 0) return 0;
        ListTag list = getList(holder);
        int consumed = 0;
        int remaining = amount;
        while (!list.isEmpty() && remaining > 0) {
            CompoundTag first = list.getCompound(0);
            ItemStack s = ItemStack.of(first);
            int take = Math.min(s.getCount(), remaining);
            s.shrink(take);
            consumed += take;
            remaining -= take;
            list.remove(0);
            if (!s.isEmpty()) {
                CompoundTag updated = new CompoundTag();
                s.save(updated);
                list.add(0, updated);
            }
        }
        return consumed;
    }

    /** 互換用: 単純な count セット。PER_ENTRY_MAX で分割して ByteTag オーバーフローを回避。 */
    public static void setStored(ItemStack stack, int count) {
        ListTag list = getList(stack);
        list.clear();
        int remaining = Math.max(0, Math.min(MAX_STORED, count));
        while (remaining > 0) {
            int put = Math.min(PER_ENTRY_MAX, remaining);
            ItemStack knife = new ItemStack(CustomEntityInit.THROWING_KNIFE.get(), put);
            CompoundTag entry = new CompoundTag();
            knife.save(entry);
            list.add(entry);
            remaining -= put;
        }
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

    /** 通常 → スタン → スクリュー → オート → 通常 ... 新モードの上限より大きな数は clamp する。 */
    public static void cycleMode(ItemStack stack) {
        KnifeType[] modes = { KnifeType.NORMAL, KnifeType.STUN, KnifeType.SCREW, KnifeType.HOMING };
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

    /**
     * 弾薬は常に通常の投げナイフ (THROWING_KNIFE) 1 種類のみ。
     * モード別の効果はエンティティに KnifeType を設定することで実現するので、
     * 消費するアイテムは統一してよい。スタン/スクリュー用アイテムは入手不可とする。
     */
    public static Item ammoFor(KnifeType mode) {
        return CustomEntityInit.THROWING_KNIFE.get();
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
            case STUN   -> "§eスタン";
            case SCREW  -> "§bスクリュー";
            case GRIP   -> "§7グリップ";
            case HOMING -> "§dオート";
            default     -> "§f通常";
        };
    }

    // --- Interaction ---------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // シフト + 右クリック: ホルダー内蔵インベントリ GUI を開く
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                final InteractionHand openedHand = hand;
                net.minecraftforge.network.NetworkHooks.openScreen(sp,
                    new net.minecraft.world.MenuProvider() {
                        @Override
                        public net.minecraft.network.chat.Component getDisplayName() {
                            return net.minecraft.network.chat.Component.translatable(
                                "item.minecraft_armor_weapon.knife_launcher");
                        }
                        @Override
                        public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                                int id, net.minecraft.world.entity.player.Inventory inv,
                                net.minecraft.world.entity.player.Player p) {
                            return new minecraftarmorweapon.world.inventory.KnifeHolderMenu(id, inv, openedHand);
                        }
                    },
                    buf -> buf.writeBoolean(openedHand == InteractionHand.OFF_HAND));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        KnifeType mode = getMode(stack);
        int requested = getCount(stack);
        boolean creative = player.getAbilities().instabuild;

        // 弾の在庫確認: ホルダー内蔵 + プレイヤーインベントリの合算、不足なら全部放つ
        Item ammoItem = ammoFor(mode);
        int stored = getStored(stack);
        int invCount = creative ? requested : countItem(player, ammoItem);
        // Infinity: ホルダーに 1 本以上入っていれば在庫無限扱い (弓の Infinity と同じ仕様)。
        // 最初に入っているナイフが "種" になり、以降在庫は減らない。
        boolean infinite = !creative && infinityLevel(stack) > 0 && stored >= 1;
        int available = creative ? requested
                      : (infinite ? requested : (stored + invCount));
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

            // 弾を消費 (クリエイティブ / Infinity は消費しない)。
            // まずホルダー先頭スタックから LIFO、足りない分だけインベントリから。
            if (!creative && !infinite) {
                int fromHolder = consumeStored(stack, toThrow);
                int remaining = toThrow - fromHolder;
                if (remaining > 0) consumeItems(player, ammoItem, remaining);
            }
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

    /**
     * Shift + 右クリック: ホルダーへの投げナイフ出し入れ。
     *   - ホルダーが空 → インベントリから最大 MAX_STORED まで吸収
     *   - ホルダーに在庫あり → 全て取り出してプレイヤーインベントリへ戻す (溢れは地面)
     */
    private InteractionResultHolder<ItemStack> handleStorageToggle(Level level, Player player, ItemStack holder) {
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(holder, true);
        }
        int stored = getStored(holder);
        Item ammo = CustomEntityInit.THROWING_KNIFE.get();

        if (stored > 0) {
            // 全スタックを先頭から順に取り出してインベントリへ
            int total = 0;
            while (true) {
                ItemStack pop = popStack(holder);
                if (pop.isEmpty()) break;
                total += pop.getCount();
                if (!player.getInventory().add(pop)) {
                    player.drop(pop, false);
                }
            }
            player.displayClientMessage(
                Component.literal("§6▶ ホルダーから " + total + " 本 取り出した"), true);
        } else {
            // インベントリから一気に吸収 (複数スタック → 1 スタックとしてマージされる)
            int have = countItem(player, ammo);
            int canStore = Math.min(have, MAX_STORED);
            if (canStore <= 0) {
                player.displayClientMessage(
                    Component.literal("§7ホルダーに収納できる投げナイフが無い"), true);
                return InteractionResultHolder.fail(holder);
            }
            // 1 つの仮想スタックとして挿入 → 内部で 1 エントリになる
            ItemStack virtual = new ItemStack(ammo, canStore);
            int added = insertStack(holder, virtual);
            consumeItems(player, ammo, added);
            player.displayClientMessage(
                Component.literal("§6▶ ホルダーに " + added + " 本 収納"), true);
        }
        return InteractionResultHolder.sidedSuccess(holder, false);
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
        tooltip.add(Component.literal("§7▶ 内蔵ナイフ: §f" + getStored(stack)
            + "§8 / " + MAX_STORED));
        // 内蔵スタックを先頭 (最新) から順に表示
        java.util.List<ItemStack> stacks = getStoredStacks(stack);
        if (!stacks.isEmpty()) {
            int show = Math.min(stacks.size(), 6); // 表示は最大 6 行
            for (int i = 0; i < show; i++) {
                ItemStack s = stacks.get(i);
                String prefix = (i == 0) ? "§e▸ " : "§7  "; // 先頭は黄色マーカー
                tooltip.add(Component.literal(prefix + s.getHoverName().getString()
                    + " §7×§f" + s.getCount()));
            }
            if (stacks.size() > show) {
                tooltip.add(Component.literal("§8  ... 他 " + (stacks.size() - show) + " スタック"));
            }
        }
        tooltip.add(Component.literal("§8シフト+左クリック: 技選択画面"));
        tooltip.add(Component.literal("§8シフト+右クリック: ナイフ出し入れ"));
        tooltip.add(Component.literal("§8右クリック: 投擲 (内蔵→インベントリ の順に消費)"));
        double mp = manaCostPer(mode);
        if (mp > 0) {
            tooltip.add(Component.literal("§b✦ MP消費: §f" + (int)mp + " §8/本"));
        }
    }
}
