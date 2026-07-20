package the_four_primitives_and_weapons.events;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Vector3f;

import the_four_primitives_and_weapons.damage.CorrosionElementDamageHandler;
import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;
import the_four_primitives_and_weapons.damage.ModDamageSources;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Magical Katana の侵食属性特殊技 ( 結晶生成 → 殴ると武器具現化 ).
 *
 * 仕様:
 *   1. Magical Katana を持って侵食属性の特殊技をチャージ発動すると、 視線方向に
 *      "魔の結晶" ( = invisible ArmorStand に NBT タグ + 赤紫 particles ) を生成
 *   2. プレイヤーが結晶を殴る → 結晶 HP が減って 0 で破壊
 *   3. 破壊された瞬間に、 オーナーの手に「具現化 Magical Katana」が入る
 *      - NBT: MaterializedFor = owner UUID
 *      - NBT: Materialized = true
     *      - 侵食属性 Lv 12 ( = XII 表記、 ERASURE 級ダメージ )
 *   4. 具現化武器を納刀すると、 武器が砕けて消滅 ( ダメージ無し、 演出のみ )
 *
 * 未対応 ( phase 2 ):
 *   - エンチャント転送 ( 元 Magical Katana の enchant を引き継ぐ )
 *   - ガード解除で結晶を破壊できる
 *   - 他人インベントリの自分 UUID 武器を破壊 UI
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class MagicalKatanaCrystalHandler {

    private static final String CRYSTAL_OWNER_KEY = "MagicalKatanaCrystalOwner";
    private static final String CRYSTAL_LIFE_KEY  = "MagicalKatanaCrystalLifeTicks";
    private static final String MAT_TAG_KEY       = "Materialized";
    private static final String MAT_OWNER_KEY     = "MaterializedFor";
    /** 具現化武器に埋め込む「結晶化前の元装備」の完全 NBT ( 破壊で元に戻すために使う ) */
    private static final String MAT_ORIGINAL_KEY  = "OriginalEquipment";
    private static final String UNLOCKED_KEY      = "MagicalKatanaUnlocked";
    /** player persistent data: 結晶化前の magical katana NBT を保存して、 具現化版に転送する */
    private static final String PLAYER_SAVED_MK_NBT_KEY = "SavedMagicalKatanaNBT";

    private static final int    CRYSTAL_LIFE        = 600;  // 30 sec auto-expire
    private static final float  CRYSTAL_MAX_HEALTH  = 0.5f; // 素手 1 撃 (1 ダメージ) で破壊できる
    private static final int    MATERIALIZED_LEVEL  = 12;   // Lv 12 = XII 表記
    /** 他人インベントリで具現化武器を破壊された時、 そのホルダーに入るダメージ ( 1 本ごと ) */
    private static final float  REMOTE_DESTROY_DAMAGE = 6.0f;
    /** 結晶を「ブロック相当」 として扱う際の AABB 膨張量 ( 結晶自体は ArmorStand-small なので大きめに ) */
    private static final double CRYSTAL_BLOCK_AABB_INFLATE = 0.4;
    /** ドロップ中の具現化武器が破壊されたとき、 周辺エンティティに与える AoE 侵食ダメージ */
    private static final float  DROPPED_DESTROY_AOE_DAMAGE = 12.0f;
    /** ドロップ破壊時 AoE の半径 ( ブロック単位 ) */
    private static final double DROPPED_DESTROY_AOE_RADIUS = 4.0;

    /** owner UUID → 既存結晶があるなら entity UUID ( 同時に複数生やさない ) */
    private static final Map<UUID, UUID> existingCrystal = new ConcurrentHashMap<>();

    // --- ポーチ破壊→回収結晶 ---
    /** 回収結晶 ArmorStand に埋め込むポーチ ItemStack の NBT。 */
    private static final String POUCH_RECOVERY_NBT  = "PouchRecoveryNBT";
    private static final String POUCH_RECOVERY_LIFE = "PouchRecoveryLife";
    /** 回収結晶の寿命 = 10 分 ( 12000 tick )。 */
    private static final int    POUCH_RECOVERY_TICKS = 12000;
    /** 生成済みの回収結晶 ( standUUID → stand )。 寿命/演出管理用。 */
    private static final Map<UUID, ArmorStand> recoveryCrystals = new ConcurrentHashMap<>();
    /** 監視中のポーチ ItemEntity ( マグマ/炎/サボテン等での破壊を検知して回収結晶へ変換 )。 */
    private static final java.util.Set<net.minecraft.world.entity.item.ItemEntity> trackedPouches =
            java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

    // ─────────────────────────────────────────────────────────────
    // 公開 API
    // ─────────────────────────────────────────────────────────────

    /**
     * Viewer の UUID が刻まれた具現化武器がサーバ上にあるか走査して、 chat に一覧を出す。
     * R キー押下時に呼ばれる ( = 納刀 UI の代替 )。 自分のインベントリ外にあるものも含める。
     * 末尾に「全部破壊」のクリック可能テキストを付ける。
     */
    public static void listOwnedToChat(net.minecraft.server.level.ServerPlayer viewer) {
        if (viewer == null) return;
        net.minecraft.server.MinecraftServer server = viewer.getServer();
        if (server == null) return;
        UUID viewerId = viewer.getUUID();

        java.util.Map<String, Integer> playerCounts = new java.util.LinkedHashMap<>();
        int dropped = 0;
        int self = 0;
        for (ServerLevel sl : server.getAllLevels()) {
            for (Player holder : sl.players()) {
                var inv = holder.getInventory();
                int count = 0;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack s = inv.getItem(i);
                    if (!isMaterialized(s)) continue;
                    CompoundTag tg = s.getTag();
                    if (tg == null || !tg.hasUUID(MAT_OWNER_KEY)) continue;
                    if (!tg.getUUID(MAT_OWNER_KEY).equals(viewerId)) continue;
                    count++;
                }
                if (count > 0) {
                    if (holder.getUUID().equals(viewerId)) self += count;
                    else playerCounts.merge(holder.getName().getString(), count, Integer::sum);
                }
            }
            for (Entity e : sl.getAllEntities()) {
                if (!(e instanceof net.minecraft.world.entity.item.ItemEntity ie)) continue;
                ItemStack s = ie.getItem();
                if (!isMaterialized(s)) continue;
                CompoundTag tg = s.getTag();
                if (tg == null || !tg.hasUUID(MAT_OWNER_KEY)) continue;
                if (!tg.getUUID(MAT_OWNER_KEY).equals(viewerId)) continue;
                dropped++;
            }
        }

        int total = self + dropped;
        for (int c : playerCounts.values()) total += c;
        if (total <= 0) return; // 何もなければ何も出さない

        viewer.sendSystemMessage(Component.literal("§6=== 自分の具現化武器 ( 全 " + total + " 本 ) ==="));
        if (self > 0) {
            viewer.sendSystemMessage(Component.literal("§7• 自分のインベントリ: §a" + self + " 本"));
        }
        for (var entry : playerCounts.entrySet()) {
            viewer.sendSystemMessage(Component.literal(
                    "§7• " + entry.getKey() + " のスロット: §c" + entry.getValue() + " 本"));
        }
        if (dropped > 0) {
            viewer.sendSystemMessage(Component.literal("§7• 落下中: §e" + dropped + " 本"));
        }
        // 全部破壊ボタン ( クリックで /crystal destroy_mine を実行 )
        net.minecraft.network.chat.MutableComponent btn =
                Component.literal("§c§l[全部破壊]§r")
                        .withStyle(style -> style
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                        net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                                        "/crystal destroy_mine"))
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                                        net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("§7クリックで /crystal destroy_mine を実行"))));
        viewer.sendSystemMessage(Component.literal("§7→ ").append(btn));
    }

    /**
     * 全プレイヤーのインベントリを走査し、 owner UUID が一致する具現化武器を全部破壊する。
     * 戻り値は破壊した本数。 ( /crystal destroy_mine コマンドから呼ばれる )
     *
     * 仕様:
     *   - 自分自身のインベントリにあるものはダメージ無しで破壊
     *   - 他人のインベントリにあるものは破壊と同時にホルダーへ侵食ダメージ
     *   - ただし owner と同じチームのプレイヤーには ダメージを与えない ( 味方判定 )
     */
    public static int destroyAllOwnedMaterialized(net.minecraft.server.MinecraftServer server, Player owner) {
        if (server == null || owner == null) return 0;
        // 増殖対策: 結晶ポーチが無ければ破壊できない
        if (the_four_primitives_and_weapons.item.MaterializedPouchItem.findFirst(owner).isEmpty()) {
            if (owner instanceof net.minecraft.server.level.ServerPlayer sp)
                sp.displayClientMessage(Component.literal("§c結晶ポーチが無いため破壊できません"), true);
            return 0;
        }
        UUID ownerId = owner.getUUID();
        int destroyed = 0;
        // 自分の具現化「防具」と「武器」は returnToPouch で確実に解除する
        //   ( 防具 → 元装備に復元してポーチへ / 武器 → ポーチへ )。 これで R の破壊で防具も壊せる。
        if (owner instanceof net.minecraft.server.level.ServerPlayer ownerSp) {
            destroyed += the_four_primitives_and_weapons.util.LoadoutPouchHelper.returnToPouch(ownerSp);
        }
        for (ServerLevel sl : server.getAllLevels()) {
            for (Player p : sl.players()) {
                var inv = p.getInventory();
                int destroyedInThisInv = 0;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack s = inv.getItem(i);
                    if (!isOwnedMaterialized(s, ownerId)) continue; // UUID基準: 具現化武器・防具を全種類対象に
                    // 破壊 = 完全消滅 ( 残骸の Magical Katana は残さない )
                    inv.setItem(i, ItemStack.EMPTY);
                    destroyed++;
                    destroyedInThisInv++;
                    // 演出 — 侵食属性のイメージカラー ( 赤紫 + ピンク寄り赤紫 )
                    spawnShatterParticles(sl, p.getX(), p.getY() + 1.0, p.getZ());
                    sl.playSound(null, p.getX(), p.getY(), p.getZ(),
                            SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
                }
                // 鞘 ( saya ) に納刀された具現化武器も破壊対象にする ( 「破壊で出てこない」 対策 )
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack sc = inv.getItem(i);
                    if (!the_four_primitives_and_weapons.util.CuriosScabbardHelper.isScabbard(sc)) continue;
                    if (!the_four_primitives_and_weapons.util.CuriosScabbardHelper.hasStoredWeapon(sc)) continue;
                    ItemStack stored = the_four_primitives_and_weapons.util.CuriosScabbardHelper.extractWeaponFromScabbard(sc);
                    if (!isOwnedMaterialized(stored, ownerId)) continue;
                    the_four_primitives_and_weapons.util.CuriosScabbardHelper.clearWeaponFromScabbard(sc);
                    inv.setItem(i, sc);
                    destroyed++;
                    destroyedInThisInv++;
                    spawnShatterParticles(sl, p.getX(), p.getY() + 1.0, p.getZ());
                    sl.playSound(null, p.getX(), p.getY(), p.getZ(),
                            SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
                }
                // 他人のインベで破壊された場合は ホルダーにダメージ ( 同チーム除外 )
                if (destroyedInThisInv > 0
                        && !p.getUUID().equals(ownerId)
                        && !areAllies(owner, p)) {
                    float dmg = REMOTE_DESTROY_DAMAGE * destroyedInThisInv;
                    p.hurt(ModDamageSources.ofElement(sl, ElementType.CORROSION, owner), dmg);
                }
            }
            // 落下しているアイテム — 破壊時に周辺 AoE 侵食ダメージ ( Lv12 )
            //   ダメージ発行元は UUID で紐付く owner エンティティ
            //   同チームのプレイヤーのみ除外 ( オーナー本人も巻き込まれる )
            for (Entity e : sl.getAllEntities()) {
                if (!(e instanceof net.minecraft.world.entity.item.ItemEntity ie)) continue;
                ItemStack s = ie.getItem();
                if (!isOwnedMaterialized(s, ownerId)) continue; // UUID基準: 落下中の具現化品を全種類対象に

                // ドロップ位置で AoE 侵食ダメージ
                //   ItemEntity の getBoundingBox() は ~0.25 cube なので inflate で 中心から
                //   DROPPED_DESTROY_AOE_RADIUS ブロック広げる。
                Vec3 ic = ie.position();
                AABB area = new AABB(
                        ic.x - DROPPED_DESTROY_AOE_RADIUS, ic.y - DROPPED_DESTROY_AOE_RADIUS, ic.z - DROPPED_DESTROY_AOE_RADIUS,
                        ic.x + DROPPED_DESTROY_AOE_RADIUS, ic.y + DROPPED_DESTROY_AOE_RADIUS, ic.z + DROPPED_DESTROY_AOE_RADIUS);
                int aoeHits = 0;
                int aoeScanned = 0;
                for (net.minecraft.world.entity.LivingEntity le
                        : sl.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area)) {
                    aoeScanned++;
                    // 距離での厳密チェック ( AABB の角は radius 超え部分があるので球で絞り込み )
                    if (le.distanceToSqr(ic) > DROPPED_DESTROY_AOE_RADIUS * DROPPED_DESTROY_AOE_RADIUS) continue;
                    // owner と同チームのプレイヤーは除外 ( = 味方判定 )
                    if (le instanceof Player p && !p.getUUID().equals(ownerId) && areAllies(owner, p)) continue;

                    // invulnerableTime を 0 にして 連続ダメージが落ちないように
                    le.invulnerableTime = 0;
                    boolean hit = false;
                    // 1) 侵食属性 DamageSource で hurt 試行 ( 主経路 )
                    try {
                        CorrosionElementDamageHandler.applyCorrosionDamage(
                                le, DROPPED_DESTROY_AOE_DAMAGE, owner, MATERIALIZED_LEVEL);
                        hit = true;
                    } catch (Throwable ignored) {}
                    // 2) 上で失敗 / 入らなかった場合は vanilla magic damage に fallback
                    if (!hit || !le.isDeadOrDying()) {
                        try {
                            le.invulnerableTime = 0;
                            le.hurt(owner.damageSources().magic(), DROPPED_DESTROY_AOE_DAMAGE);
                            hit = true;
                        } catch (Throwable ignored) {}
                    }
                    if (hit) aoeHits++;
                }
                if (owner instanceof net.minecraft.server.level.ServerPlayer sp) {
                    sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§7爆散 §c" + aoeHits + " §7体ヒット ( 半径 " + (int) DROPPED_DESTROY_AOE_RADIUS
                                    + " m に §e" + aoeScanned + " §7体 )"));
                }
                // 演出
                spawnShatterParticles(sl, ie.getX(), ie.getY() + 0.2, ie.getZ());
                sl.playSound(null, ie.getX(), ie.getY(), ie.getZ(),
                        SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);

                ie.discard();
                destroyed++;
            }
        }
        return destroyed;
    }

    /**
     * 結晶 ArmorStand に block 相当の 1×1×1 AABB を強制セットする。
     * ArmorStand 既定 hitbox ( 0.5w × 1.975h ) は block 相当の遮蔽として小さく、
     * 通り抜け / 投射物の擦り抜けが起こりやすいので明示的に拡張する。
     * tick で refreshDimensions が走るとリセットされるので、 onServerTick でも再設定する。
     */
    private static void applyCrystalBoundingBox(ArmorStand stand, Vec3 center) {
        AABB box = new AABB(
                center.x - 0.5, center.y - 0.5, center.z - 0.5,
                center.x + 0.5, center.y + 0.5, center.z + 0.5);
        try { stand.setBoundingBox(box); } catch (Throwable ignored) {}
    }

    /** 同チーム判定 ( チームが両方非 null かつ同じ team なら味方 )。 vanilla の isAlliedTo に委譲。 */
    private static boolean areAllies(Player a, Player b) {
        if (a == null || b == null) return false;
        if (a.getUUID().equals(b.getUUID())) return true;
        return a.getTeam() != null && a.getTeam() == b.getTeam();
    }

    /**
     * 破壊時の共通パーティクル — 侵食属性のイメージカラー
     *   - 濃い赤紫 ( マゼンタ寄り )
     *   - ピンク寄り赤紫
     * の 2 色を派手にスポーン + 桜の花びらでアクセント。
     */
    private static void spawnShatterParticles(ServerLevel sl, double x, double y, double z) {
        // 濃い赤紫 ( メイン、 多め )
        DustParticleOptions magenta = new DustParticleOptions(
                new Vector3f(0.75f, 0.1f, 0.55f), 1.6f);
        sl.sendParticles(magenta, x, y, z, 30, 0.4, 0.5, 0.4, 0.08);
        // ピンク寄り赤紫 ( highlight )
        DustParticleOptions pinkMagenta = new DustParticleOptions(
                new Vector3f(1.0f, 0.35f, 0.7f), 1.2f);
        sl.sendParticles(pinkMagenta, x, y, z, 20, 0.4, 0.5, 0.4, 0.06);
        // 桜の花びら ( アンビエント )
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, x, y, z, 8, 0.3, 0.4, 0.3, 0.0);
    }

    /** Magical Katana が手にあるかどうか ( 具現化版含む ) */
    public static boolean isMagicalKatana(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() == TheFourPrimitivesAndWeaponsModItems.MAGICAL_KATANA.get();
    }

    /** 具現化された Magical Katana か ( NBT で判定 ) */
    public static boolean isMaterialized(ItemStack stack) {
        if (!isMagicalKatana(stack)) return false;
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(MAT_TAG_KEY);
    }

    /**
     * 具現化マーカー ( {@link #MAT_TAG_KEY} ) が付いた「あらゆるアイテム」か。
     * ローダウトポーチで防具 / Curios 品 / 武器を具現化版として扱うために、
     * Magical Katana 限定の {@link #isMaterialized} とは別に汎用判定を提供する。
     */
    public static boolean isAnyMaterialized(ItemStack stack) {
        if (stack.isEmpty()) return false;
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(MAT_TAG_KEY);
    }

    /**
     * 任意の装備の「具現化版コピー」を作る ( 壊れ物 )。
     *   - Materialized = true / MaterializedFor = owner
     *   - 元装備 ( {@link #MAT_ORIGINAL_KEY} ) を埋め込み、 破壊で元に戻せるようにする
     *   - Magical Katana の場合は侵食属性 Lv12 も付与 ( 既存仕様に合わせる )
     */
    public static ItemStack materializeCopy(ItemStack original, UUID ownerId) {
        if (original == null || original.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = original.copy();
        copy.setCount(1);
        CompoundTag tag = copy.getOrCreateTag();
        tag.putBoolean(MAT_TAG_KEY, true);
        tag.putUUID(MAT_OWNER_KEY, ownerId);
        if (isMagicalKatana(copy)) {
            try { ElementalDamageUtils.setElement(copy, ElementType.CORROSION, MATERIALIZED_LEVEL); } catch (Throwable ignored) {}
        }
        embedOriginal(copy, original);
        return copy;
    }

    /**
     * 特殊技 ( 結晶生成 ) を解放済みか。
     *   解放条件:
     *     - 一度 Saya に納刀された ( performSheathing で setUnlocked )
     *     - 具現化版を shatter したベース ( shatterOnSheathe / destroyAll で setUnlocked )
     *     - /give 等で CORROSION 属性 Lv>=12 がセットされている ( ElementalDamageUtils で自動 )
     *   既に具現化済みのものは常に unlocked 扱い。
     */
    public static boolean isUnlocked(ItemStack stack) {
        if (!isMagicalKatana(stack)) return false;
        if (isMaterialized(stack)) return true;
        CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        if (tag.getBoolean(UNLOCKED_KEY)) return true;
        // /give 等で ElementalDamageUtils を経由せず直接 NBT セットされたケース:
        //   ElementType == "corrosion" && ElementLevel >= 12 でも unlocked 扱い
        if (tag.contains("ElementType") && tag.contains("ElementLevel")) {
            String type = tag.getString("ElementType");
            int level = tag.getInt("ElementLevel");
            if ("corrosion".equalsIgnoreCase(type) && level >= 12) return true;
        }
        return false;
    }

    /** Magical Katana を解放状態にする ( 通常版 stack に NBT flag をセット )。 */
    public static void setUnlocked(ItemStack stack) {
        if (!isMagicalKatana(stack)) return;
        stack.getOrCreateTag().putBoolean(UNLOCKED_KEY, true);
    }

    /**
     * 具現化 Magical Katana を生成 ( /crystal give_materialized で使用 )。
     *   - Materialized = true
     *   - MaterializedFor = ownerId ( 破壊判定用 )
     *   - 侵食属性 Lv 12 ( XII )
     */
    public static ItemStack createMaterialized(UUID ownerId) {
        ItemStack weapon = new ItemStack(TheFourPrimitivesAndWeaponsModItems.MAGICAL_KATANA.get());
        CompoundTag tag = weapon.getOrCreateTag();
        tag.putBoolean(MAT_TAG_KEY, true);
        tag.putUUID(MAT_OWNER_KEY, ownerId);
        ElementalDamageUtils.setElement(weapon, ElementType.CORROSION, MATERIALIZED_LEVEL);
        // 元装備が不明なので「素の Magical Katana」を復元対象として埋め込む
        embedOriginal(weapon, new ItemStack(TheFourPrimitivesAndWeaponsModItems.MAGICAL_KATANA.get()));
        return weapon;
    }

    /**
     * 具現化武器に「結晶化前の元装備」の完全 NBT を埋め込む。
     * R キーの破壊で {@link #revertToOriginal} がこれを読んで元装備に戻す。
     */
    public static void embedOriginal(ItemStack materialized, ItemStack original) {
        if (materialized.isEmpty() || original == null || original.isEmpty()) return;
        ItemStack clean = original.copy();
        // 元装備に具現化マーカーが残っていたら剥がす ( 入れ子防止 )
        CompoundTag ct = clean.getTag();
        if (ct != null) {
            ct.remove(MAT_TAG_KEY);
            ct.remove(MAT_OWNER_KEY);
            ct.remove(MAT_ORIGINAL_KEY);
        }
        materialized.getOrCreateTag().put(MAT_ORIGINAL_KEY, clean.save(new CompoundTag()));
    }

    /**
     * 具現化武器を「元装備していたもの」に戻す。
     *   - 埋め込まれた {@link #MAT_ORIGINAL_KEY} があればそれを復元
     *   - 無ければ具現化マーカー / 侵食属性を剥がした素の Magical Katana にフォールバック
     */
    public static ItemStack revertToOriginal(ItemStack materialized) {
        if (!isAnyMaterialized(materialized)) return ItemStack.EMPTY;
        CompoundTag tag = materialized.getTag();
        if (tag != null && tag.contains(MAT_ORIGINAL_KEY, 10)) { // 10 = COMPOUND
            ItemStack orig = ItemStack.of(tag.getCompound(MAT_ORIGINAL_KEY));
            if (!orig.isEmpty()) return orig;
        }
        // フォールバック: 具現化マーカーを剥がす
        ItemStack base = materialized.copy();
        CompoundTag bt = base.getTag();
        if (bt != null) {
            bt.remove(MAT_TAG_KEY);
            bt.remove(MAT_OWNER_KEY);
            bt.remove(MAT_ORIGINAL_KEY);
        }
        try {
            ElementalDamageUtils.setElement(base, ElementType.NONE, 0);
        } catch (Throwable ignored) {}
        return base;
    }

    /** player のインベントリ / 装備のどこかに、 自分の UUID 付き具現化版アイテムがあるか。 */
    public static boolean hasAnyOwnedMaterialized(Player player) {
        if (player == null) return false;
        UUID id = player.getUUID();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isOwnedMaterialized(inv.getItem(i), id)) return true;
        }
        return false;
    }

    /** owner の UUID が刻まれた具現化版アイテムか ( 防具 / Curios / 武器 すべて対象 ) */
    public static boolean isOwnedMaterialized(ItemStack s, UUID ownerId) {
        if (!isAnyMaterialized(s)) return false;
        CompoundTag tg = s.getTag();
        return tg != null && tg.hasUUID(MAT_OWNER_KEY) && tg.getUUID(MAT_OWNER_KEY).equals(ownerId);
    }


    /**
     * 具現化防具を「外した」 ( = 装備スロットから main インベントリ / オフハンドに移動した ) ら、
     * その防具だけ結晶化を解いて元装備に戻す。 装備中 ( armor slot ) の物はそのまま。
     * weapon ( Magical Katana 等 ) は対象外 ( 防具だけ )。
     */
    @SubscribeEvent
    public static void onPlayerArmorSweep(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide()) return;
        if ((player.tickCount % 5) != 0) return; // 軽量化 ( 5tick おき )
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return;

        // 具現化防具を装備スロットから外したら、 破壊してポーチの元装備をそのスロットへ戻す
        the_four_primitives_and_weapons.util.LoadoutPouchHelper.sweepLooseArmor(sp);
    }

    /** 破壊演出 ( パーティクル + 音 ) を public で公開 ( ローダウト復帰時にも使う ) */
    public static void playShatterAt(ServerLevel sl, double x, double y, double z) {
        spawnShatterParticles(sl, x, y, z);
        sl.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0f, 0.9f);
        sl.playSound(null, x, y, z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7f, 1.2f);
    }

    /**
     * Magical Katana の侵食特殊技で結晶を生成。 視線方向に 2.5 ブロック先に置く。
     * 既に owner の結晶があれば破壊してから新しく作る。
     */
    public static void spawnCrystal(Player player) {
        spawnCrystal(player, ItemStack.EMPTY);
    }

    /** 登録した武器ロードアウト ( PlayerSkillData ) の非 null 枠数 = 具現化できる上限本数。 */
    public static int countRegisteredWeapons(Player player) {
        try {
            the_four_primitives_and_weapons.skill.PlayerSkillData.SkillStorage data =
                    the_four_primitives_and_weapons.skill.PlayerSkillData.getSkillData(player);
            int n = 0;
            for (int i = 0; i < the_four_primitives_and_weapons.skill.PlayerSkillData.MAX_WEAPON_SLOTS; i++) {
                if (data.getLoadoutAt(i) != null) n++;
            }
            return n;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /** owner の UUID が刻まれた具現化 Magical Katana の総数 ( 全プレイヤーのインベ + 落下中 )。 */
    public static int countOwnedMaterialized(net.minecraft.server.MinecraftServer server, UUID ownerId) {
        if (server == null) return 0;
        int count = 0;
        for (ServerLevel sl : server.getAllLevels()) {
            for (Player p : sl.players()) {
                var inv = p.getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (isOwnedMaterialized(inv.getItem(i), ownerId)
                            && isMagicalKatana(inv.getItem(i))) count++;
                }
            }
            for (Entity e : sl.getAllEntities()) {
                if (!(e instanceof net.minecraft.world.entity.item.ItemEntity ie)) continue;
                if (isOwnedMaterialized(ie.getItem(), ownerId) && isMagicalKatana(ie.getItem())) count++;
            }
        }
        return count;
    }

    /**
     * 結晶生成 + 結晶化前の Magical Katana NBT を player に保存。
     * sourceStack の NBT ( = エンチャント / レアリティ等 ) を player.persistentData に保存しておき、
     * 具現化版が作られるときにそこから取り出して引き継ぐ。
     */
    public static void spawnCrystal(Player player, ItemStack sourceStack) {
        if (player == null || player.level().isClientSide()) return;
        ServerLevel sl = (ServerLevel) player.level();

        // 利き手に結晶ポーチを持っている間は結晶化不可 ( 具現化武器でポーチを押し出さないため )
        if (player.getMainHandItem().getItem() instanceof the_four_primitives_and_weapons.item.MaterializedPouchItem) {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp0) {
                sp0.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§c結晶ポーチを持っている間は結晶化できません"), true);
            }
            return;
        }

        // 結晶化には「Magical Katana を収納した結晶ポーチ」がインベントリに必要
        //   ( ポーチが無い / ポーチに Magical Katana が入っていない場合は結晶化不可 )
        if (the_four_primitives_and_weapons.item.MaterializedPouchItem.findPouchWithMagicalKatana(player).isEmpty()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer spPouch) {
                boolean hasPouch = !the_four_primitives_and_weapons.item.MaterializedPouchItem.findFirst(player).isEmpty();
                spPouch.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        hasPouch
                                ? "§c結晶ポーチに Magical Katana が入っていないため結晶化できません"
                                : "§c結晶ポーチが無いため結晶化できません"), true);
            }
            return;
        }

        // 具現化できる本数 = 登録武器ロードアウト数。 既に上限なら結晶を生成しない。
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            // 登録武器数に応じて増えるが、 未登録 (0本) でも最低 1 本は具現化できるようにする
            int max = Math.max(1, countRegisteredWeapons(player));
            int current = countOwnedMaterialized(sp.getServer(), player.getUUID());
            if (current >= max) {
                sp.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§c具現化の上限です §7( 登録武器 " + max + " 本 / 現在 " + current + " 本 )"), true);
                return;
            }
        }

        // 結晶化前の NBT を player に保存 ( 具現化版に転送するため )
        if (!sourceStack.isEmpty() && isMagicalKatana(sourceStack) && sourceStack.getTag() != null) {
            player.getPersistentData().put(PLAYER_SAVED_MK_NBT_KEY, sourceStack.getTag().copy());
        } else {
            // 持ってる元 stack が無ければ過去の保存を維持 ( 削除しない )
        }

        // 既存結晶の掃除
        UUID prevId = existingCrystal.remove(player.getUUID());
        if (prevId != null) {
            Entity prev = sl.getEntity(prevId);
            if (prev != null) prev.discard();
        }

        // 視線方向 2.5 ブロック先 ( 視線が下向きすぎる場合は player の正面足元へ補正 )
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawn;
        if (lookVec.y < -0.4) {
            // 視線が下向きすぎ → 足元前方に補正
            Vec3 horiz = new Vec3(lookVec.x, 0, lookVec.z);
            if (horiz.lengthSqr() < 1.0E-4) horiz = new Vec3(0, 0, 1);
            horiz = horiz.normalize();
            spawn = player.position().add(horiz.scale(1.5)).add(0, 0.5, 0);
        } else {
            spawn = player.getEyePosition().add(lookVec.normalize().scale(2.5));
        }

        // 当たり判定を block 相当 ( 1×1×1 ) にする。
        //   - Small=false : フルサイズ ( 殴り/矢 のヒットしやすさ )
        //   - Marker=false : 物理的当たり判定 ON ( 通り抜け不可 )
        // 足元 = spawn.y - 0.5 にすると AABB ( 後で 1×1×1 に再設定 ) の中心が spawn.y 付近
        ArmorStand stand = new ArmorStand(sl, spawn.x, spawn.y - 0.5, spawn.z);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        CompoundTag prelim = new CompoundTag();
        stand.addAdditionalSaveData(prelim);
        prelim.putBoolean("Small", false);
        prelim.putBoolean("ShowArms", false);
        prelim.putBoolean("Marker", false);
        prelim.putBoolean("NoBasePlate", true);
        stand.readAdditionalSaveData(prelim);
        stand.setInvulnerable(false);
        stand.setCustomName(Component.literal("§5魔の結晶"));
        stand.setCustomNameVisible(true);
        stand.getPersistentData().putUUID(CRYSTAL_OWNER_KEY, player.getUUID());
        stand.getPersistentData().putInt(CRYSTAL_LIFE_KEY, CRYSTAL_LIFE);
        try {
            stand.setHealth(CRYSTAL_MAX_HEALTH);
        } catch (Throwable ignored) {}

        sl.addFreshEntity(stand);
        // addFreshEntity の後で AABB を 1×1×1 に強制 ( ArmorStand 既定 0.5w が小さいので拡張 )
        applyCrystalBoundingBox(stand, spawn);
        existingCrystal.put(player.getUUID(), stand.getUUID());

        // 生成エフェクト
        DustParticleOptions magenta = new DustParticleOptions(
                new Vector3f(0.75f, 0.1f, 0.55f), 1.5f);
        sl.sendParticles(magenta, spawn.x, spawn.y, spawn.z, 50, 0.4, 0.5, 0.4, 0.05);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, spawn.x, spawn.y, spawn.z,
                10, 0.3, 0.4, 0.3, 0.0);
        sl.playSound(null, spawn.x, spawn.y, spawn.z,
                SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.PLAYERS, 1.2f, 0.5f);

        // フィードバック ( 結晶が見えない場所に出ても「動いた」 ことを伝える )
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.displayClientMessage(Component.literal(
                    "§5魔の結晶を生成しました §7( 殴って具現化 )"), true);
        }
    }

    /**
     * 具現化された武器を納刀しようとした時の処理。 演出付きで破壊 ( ダメージ無し )。
     * 破壊後は **ベースの Magical Katana を 1 本返す** ( = 再抜刀できる )。
     */
    public static void shatterOnSheathe(Player player, ItemStack stack, InteractionHand hand) {
        if (!isMaterialized(stack)) return;
        // 増殖対策: 結晶ポーチが無ければ破壊できない
        if (the_four_primitives_and_weapons.item.MaterializedPouchItem.findFirst(player).isEmpty()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp)
                sp.displayClientMessage(Component.literal("§c結晶ポーチが無いため破壊できません"), true);
            return;
        }
        // ポーチ由来 ( deploy コピー ) は ここで消さず returnToPouch に任せる ( 原本をスロットへ戻すため )。
        //   虚空由来 ( 結晶割りデフォルト等 ) のみ即完全消去 ( 原本は素体としてインベントリに残る )。
        //   ※ 虚空由来をポーチへ回収すると素体と二重になり増殖するため、 ここで消す。
        if (!the_four_primitives_and_weapons.item.MaterializedPouchItem.isFromPouch(stack, player.getUUID())) {
            player.setItemInHand(hand, ItemStack.EMPTY);
        }
        // 破壊演出 — 侵食属性のイメージカラー ( 赤紫 + ピンク寄り赤紫 )
        if (player.level() instanceof ServerLevel sl) {
            spawnShatterParticles(sl, player.getX(), player.getY() + 1.0, player.getZ());
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.2f, 0.8f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);

        // Magical Katana が砕けたのと同じタイミングで、 結晶ローダウトの具現化防具も破壊して
        // 元装備に戻す ( 結晶化=装着 / 破壊=解除 を刀と防具で同期させる )。
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            try {
                the_four_primitives_and_weapons.util.LoadoutPouchHelper.returnToPouch(sp);
            } catch (Throwable ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 内部
    // ─────────────────────────────────────────────────────────────

    /** 具現化武器を作って指定 player の手に入れる ( 入らなければ落下 ItemEntity ) */
    private static void materializeFor(ServerLevel sl, Vec3 pos, UUID ownerId, ItemStack templateForEnchants) {
        Player owner = sl.getServer().getPlayerList().getPlayer(ownerId);
        // 武器スロットに中身があれば、 デフォルトの具現化 Magical Katana 手渡しは置き換える ( 武器スロットを手に装着 )
        boolean replaceWithLoadout = (owner instanceof net.minecraft.server.level.ServerPlayer sp0)
                && the_four_primitives_and_weapons.util.LoadoutPouchHelper.hasWeaponLoadout(sp0);
        if (!replaceWithLoadout) {
        ItemStack weapon = new ItemStack(TheFourPrimitivesAndWeaponsModItems.MAGICAL_KATANA.get());
        CompoundTag tag = weapon.getOrCreateTag();
        tag.putBoolean(MAT_TAG_KEY, true);
        tag.putUUID(MAT_OWNER_KEY, ownerId);

        // 侵食属性 Lv 12 ( XII 表記 )
        ElementalDamageUtils.setElement(weapon, ElementType.CORROSION, MATERIALIZED_LEVEL);

        // エンチャント転送 — まず template ( 手に残ってる元 Magical Katana ) から、
        // 次に player.persistentData に保存しておいた 結晶化前 NBT からも復元する。
        if (templateForEnchants != null && !templateForEnchants.isEmpty()) {
            try {
                var enchants = EnchantmentHelper.getEnchantments(templateForEnchants);
                if (!enchants.isEmpty()) {
                    EnchantmentHelper.setEnchantments(enchants, weapon);
                }
            } catch (Throwable ignored) {}
        }
        // player.persistentData に保存されてる原本 NBT があれば、 そこから追加でエンチャント転送
        Player ownerForRestore = sl.getServer().getPlayerList().getPlayer(ownerId);
        if (ownerForRestore != null) {
            CompoundTag savedNbt = null;
            CompoundTag pd = ownerForRestore.getPersistentData();
            if (pd.contains(PLAYER_SAVED_MK_NBT_KEY, 10)) { // 10 = COMPOUND
                savedNbt = pd.getCompound(PLAYER_SAVED_MK_NBT_KEY);
            }
            if (savedNbt != null) {
                ItemStack restored = new ItemStack(TheFourPrimitivesAndWeaponsModItems.MAGICAL_KATANA.get());
                restored.setTag(savedNbt.copy());
                try {
                    var enchants = EnchantmentHelper.getEnchantments(restored);
                    if (!enchants.isEmpty()) {
                        EnchantmentHelper.setEnchantments(enchants, weapon);
                    }
                } catch (Throwable ignored) {}
            }
        }

        // 「結晶化前の元装備」を埋め込む ( R キーの破壊で元に戻すため )。
        //   優先順: 手に残ってる元 template → player.persistentData の保存 NBT → 素の Magical Katana
        ItemStack original = null;
        if (templateForEnchants != null && !templateForEnchants.isEmpty()) {
            original = templateForEnchants.copy();
        } else if (ownerForRestore != null) {
            CompoundTag pd = ownerForRestore.getPersistentData();
            if (pd.contains(PLAYER_SAVED_MK_NBT_KEY, 10)) {
                ItemStack restored = new ItemStack(TheFourPrimitivesAndWeaponsModItems.MAGICAL_KATANA.get());
                restored.setTag(pd.getCompound(PLAYER_SAVED_MK_NBT_KEY).copy());
                original = restored;
            }
        }
        if (original == null || original.isEmpty()) {
            original = new ItemStack(TheFourPrimitivesAndWeaponsModItems.MAGICAL_KATANA.get());
        }
        embedOriginal(weapon, original);

        // owner に渡す ( 居なければ落下 )
        if (owner != null && owner.isAlive()) {
            // 利き手に他のアイテムがあっても、 具現化武器を利き手に装着する。
            //   持っていたものはインベントリへ退避 ( 入らなければ落下 )。
            ItemStack prevHeld = owner.getMainHandItem().copy();
            owner.setItemInHand(InteractionHand.MAIN_HAND, weapon);
            if (!prevHeld.isEmpty()) {
                if (!owner.getInventory().add(prevHeld)) owner.drop(prevHeld, false);
            }
        } else {
            net.minecraft.world.entity.item.ItemEntity ie =
                    new net.minecraft.world.entity.item.ItemEntity(sl, pos.x, pos.y, pos.z, weapon);
            sl.addFreshEntity(ie);
        }
        } // end if(!replaceWithLoadout): デフォルトの具現化 Magical Katana 手渡し

        // 演出
        DustParticleOptions burst = new DustParticleOptions(
                new Vector3f(1.0f, 0.35f, 0.7f), 1.8f);
        sl.sendParticles(burst, pos.x, pos.y, pos.z, 60, 0.5, 0.5, 0.5, 0.15);
        sl.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        sl.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5f, 1.2f);

        // 結晶破壊と同時に、 owner が結晶ローダウトポーチを持っていれば中身を
        // 具現化版として実スロットへ一括装着する ( 呼び出しは Deployed フラグで 1 回限り )。
        if (owner instanceof net.minecraft.server.level.ServerPlayer sp) {
            try {
                the_four_primitives_and_weapons.util.LoadoutPouchHelper.deploy(sp);
            } catch (Throwable ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────
    // event handlers
    // ─────────────────────────────────────────────────────────────

    /**
     * 自分のインベから magical katana が **破壊以外で** 消えた場合 ( = drop / 死亡 等 ) は、
     * saved NBT を invalidate する。 これで「ワールドから無くなったのに結晶化で再生」 を防ぐ。
     */
    @SubscribeEvent
    public static void onItemToss(net.minecraftforge.event.entity.item.ItemTossEvent event) {
        ItemStack thrown = event.getEntity().getItem();
        if (!isMagicalKatana(thrown)) return;
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide()) return;
        // 「破壊」 ルートは shatterOnSheathe で setItemInHand(EMPTY) するので drop しない。
        // ここで drop されたものは「ユーザーが捨てた」 = 破壊以外の喪失 → saved NBT 無効化。
        player.getPersistentData().remove("SavedMagicalKatanaNBT");
    }

    /**
     * プレイヤー死亡時、 saved NBT を invalidate ( ワールドから magical katana が無くなる可能性 )。
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        player.getPersistentData().remove("SavedMagicalKatanaNBT");
    }

    /**
     * グラインダーで具現化武器を投入したら、 出力アイテムから具現化 NBT + 侵食属性を剥がす
     * ( 通常 Magical Katana に戻る ) 。 Forge 1.20.1 では PlaceItem で setOutput を上書きする方法を取る。
     */
    @SubscribeEvent
    public static void onGrindstonePlace(net.minecraftforge.event.GrindstoneEvent.OnPlaceItem event) {
        ItemStack output = event.getOutput();
        if (output.isEmpty() || !isMagicalKatana(output)) return;
        boolean wasMat = isMaterialized(event.getTopItem()) || isMaterialized(event.getBottomItem());
        if (!wasMat) return;
        // 具現化 NBT を剥がす
        CompoundTag tag = output.getTag();
        if (tag != null) {
            tag.remove(MAT_TAG_KEY);
            tag.remove(MAT_OWNER_KEY);
        }
        try {
            ElementalDamageUtils.setElement(output, ElementType.NONE, 0);
        } catch (Throwable ignored) {}
        event.setOutput(output);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) return;
        CompoundTag pd = stand.getPersistentData();
        if (handlePouchRecoveryHit(stand, null)) return; // ポーチ回収結晶
        if (!pd.contains(CRYSTAL_OWNER_KEY)) return;
        UUID ownerId = pd.getUUID(CRYSTAL_OWNER_KEY);
        existingCrystal.remove(ownerId);
        if (!(stand.level() instanceof ServerLevel sl)) return;

        // 元の Magical Katana ( 持ち主の手に残ってる ) を template として enchant 転送
        Player owner = sl.getServer().getPlayerList().getPlayer(ownerId);
        ItemStack template = ItemStack.EMPTY;
        if (owner != null) {
            ItemStack main = owner.getMainHandItem();
            if (isMagicalKatana(main) && !isMaterialized(main)) template = main;
        }
        materializeFor(sl, stand.position().add(0, 0.5, 0), ownerId, template);
    }

    /**
     * 投射物 ( 矢 / トライデント / 雪玉等 ) が結晶にヒットしたら、 攻撃と同等に扱う。
     * AttackEntityEvent は近接のみで投射物は来ないので別ハンドラ。
     */
    @SubscribeEvent
    public static void onProjectileImpact(net.minecraftforge.event.entity.ProjectileImpactEvent event) {
        net.minecraft.world.phys.HitResult hr = event.getRayTraceResult();
        if (!(hr instanceof net.minecraft.world.phys.EntityHitResult ehr)) return;
        Entity target = ehr.getEntity();
        if (!(target instanceof ArmorStand stand)) return;
        CompoundTag pd = stand.getPersistentData();
        if (pd.contains(POUCH_RECOVERY_NBT, 10)) { // ポーチ回収結晶
            Entity shooter = event.getProjectile().getOwner();
            handlePouchRecoveryHit(stand, shooter instanceof Player pp ? pp : null);
            event.setCanceled(true);
            try { event.getProjectile().discard(); } catch (Throwable ignored) {}
            return;
        }
        if (!pd.contains(CRYSTAL_OWNER_KEY)) return;
        if (!(stand.level() instanceof ServerLevel sl)) return;

        UUID ownerId = pd.getUUID(CRYSTAL_OWNER_KEY);
        existingCrystal.remove(ownerId);
        Vec3 spawnPos = stand.position().add(0, 0.5, 0);

        Player owner = sl.getServer().getPlayerList().getPlayer(ownerId);
        ItemStack template = ItemStack.EMPTY;
        if (owner != null) {
            ItemStack main = owner.getMainHandItem();
            if (isMagicalKatana(main) && !isMaterialized(main)) template = main;
        }

        stand.discard();
        // 投射物自体は消費 ( 突き抜けさせない )
        event.setCanceled(true);
        try { event.getProjectile().discard(); } catch (Throwable ignored) {}

        materializeFor(sl, spawnPos, ownerId, template);
    }

    /**
     * ArmorStand を Player が殴ると ArmorStand.hurt() は LivingEntity.die() を経由せず
     * 直接 kill() → remove() するため LivingDeathEvent が発火しない。
     * そのため AttackEntityEvent で先回りして検知し、 結晶ならその場で武器を具現化する。
     */
    @SubscribeEvent
    public static void onAttackEntity(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        Entity target = event.getTarget();
        if (!(target instanceof ArmorStand stand)) return;
        CompoundTag pd = stand.getPersistentData();
        if (handlePouchRecoveryHit(stand, event.getEntity())) { event.setCanceled(true); return; } // ポーチ回収結晶
        if (!pd.contains(CRYSTAL_OWNER_KEY)) return;
        Player attacker = event.getEntity();
        if (attacker.level().isClientSide()) return;
        if (!(attacker.level() instanceof ServerLevel sl)) return;

        UUID ownerId = pd.getUUID(CRYSTAL_OWNER_KEY);
        existingCrystal.remove(ownerId);
        Vec3 spawnPos = stand.position().add(0, 0.5, 0);

        // template 取得 ( 元 Magical Katana を持っていれば enchant 転送 )
        Player owner = sl.getServer().getPlayerList().getPlayer(ownerId);
        ItemStack template = ItemStack.EMPTY;
        if (owner != null) {
            ItemStack main = owner.getMainHandItem();
            if (isMagicalKatana(main) && !isMaterialized(main)) template = main;
        }

        // 結晶を破壊
        stand.discard();
        // 元の attack 処理をキャンセル ( ArmorStand に余計なダメージ処理が走らないように )
        event.setCanceled(true);

        materializeFor(sl, spawnPos, ownerId, template);
    }

    // ─────────────────────────────────────────────────────────────
    // ポーチ破壊 → 回収結晶 ( 10 分 )
    // ─────────────────────────────────────────────────────────────

    /** 結晶ポーチの ItemEntity を監視対象に登録 ( マグマ/炎/サボテン等での破壊検知用 )。 */
    @SubscribeEvent
    public static void onPouchItemJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity ie
                && ie.getItem().getItem() instanceof the_four_primitives_and_weapons.item.MaterializedPouchItem) {
            trackedPouches.add(ie);
        }
    }

    /** 監視中ポーチが危険 ( マグマ/炎/サボテン/奈落 ) なら、 破壊される前に回収結晶へ変換。 */
    private static void tickPouchDanger() {
        if (trackedPouches.isEmpty()) return;
        java.util.Iterator<net.minecraft.world.entity.item.ItemEntity> it = trackedPouches.iterator();
        while (it.hasNext()) {
            net.minecraft.world.entity.item.ItemEntity ie = it.next();
            if (ie == null || ie.isRemoved() || !(ie.level() instanceof ServerLevel sl)) { it.remove(); continue; }
            boolean cactus = sl.getBlockState(ie.blockPosition()).is(net.minecraft.world.level.block.Blocks.CACTUS);
            boolean danger = ie.isInLava() || ie.isOnFire() || cactus || ie.getY() < sl.getMinBuildHeight() - 8;
            if (!danger) continue;
            ItemStack pouch = ie.getItem().copy();
            // 安全な ( 空気の ) 位置を上方に探す
            net.minecraft.core.BlockPos bp = ie.blockPosition();
            Vec3 spawn = ie.position().add(0, 1.0, 0);
            for (int dy = 0; dy < 8; dy++) {
                net.minecraft.core.BlockPos up = bp.above(dy + 1);
                if (sl.getBlockState(up).isAir() && sl.getFluidState(up).isEmpty()) {
                    spawn = new Vec3(up.getX() + 0.5, up.getY(), up.getZ() + 0.5); break;
                }
            }
            ie.discard();
            it.remove();
            spawnPouchRecoveryCrystal(sl, spawn, pouch);
        }
    }

    /** 回収結晶の寿命/演出 ( 5tick 毎 )。 10 分で消滅。 */
    private static void tickRecoveryCrystals() {
        if (recoveryCrystals.isEmpty()) return;
        java.util.Iterator<Map.Entry<UUID, ArmorStand>> it = recoveryCrystals.entrySet().iterator();
        while (it.hasNext()) {
            ArmorStand stand = it.next().getValue();
            if (stand == null || stand.isRemoved() || !(stand.level() instanceof ServerLevel sl)) { it.remove(); continue; }
            CompoundTag pd = stand.getPersistentData();
            int life = pd.getInt(POUCH_RECOVERY_LIFE) - 5;
            if (life <= 0) {
                DustParticleOptions smoke = new DustParticleOptions(new Vector3f(0.4f, 0.4f, 0.4f), 1.2f);
                sl.sendParticles(smoke, stand.getX(), stand.getY() + 0.5, stand.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
                stand.discard(); it.remove(); continue;
            }
            pd.putInt(POUCH_RECOVERY_LIFE, life);
            applyCrystalBoundingBox(stand, stand.position().add(0, 0.5, 0));
            sl.sendParticles(new DustParticleOptions(new Vector3f(0.85f, 0.2f, 0.6f), 1.0f),
                    stand.getX(), stand.getY() + 0.6, stand.getZ(), 3, 0.2, 0.3, 0.2, 0.005);
            sl.sendParticles(ParticleTypes.ENCHANT, stand.getX(), stand.getY() + 1.2, stand.getZ(), 2, 0.2, 0.2, 0.2, 0.01);
        }
    }

    /** ポーチ回収結晶を生成 ( 無敵 ArmorStand。 マグマ等で壊れない )。 */
    private static void spawnPouchRecoveryCrystal(ServerLevel sl, Vec3 pos, ItemStack pouch) {
        ArmorStand stand = new ArmorStand(sl, pos.x, pos.y - 0.5, pos.z);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        stand.setInvulnerable(true);
        CompoundTag prelim = new CompoundTag();
        stand.addAdditionalSaveData(prelim);
        prelim.putBoolean("Small", false);
        prelim.putBoolean("ShowArms", false);
        prelim.putBoolean("Marker", false);
        prelim.putBoolean("NoBasePlate", true);
        stand.readAdditionalSaveData(prelim);
        stand.setCustomName(Component.literal("§d結晶ポーチの残響 §7( 叩いて回収 )"));
        stand.setCustomNameVisible(true);
        CompoundTag pd = stand.getPersistentData();
        pd.put(POUCH_RECOVERY_NBT, pouch.save(new CompoundTag()));
        pd.putInt(POUCH_RECOVERY_LIFE, POUCH_RECOVERY_TICKS);
        sl.addFreshEntity(stand);
        applyCrystalBoundingBox(stand, pos);
        recoveryCrystals.put(stand.getUUID(), stand);
        sl.sendParticles(new DustParticleOptions(new Vector3f(0.85f, 0.2f, 0.6f), 1.5f),
                pos.x, pos.y, pos.z, 40, 0.4, 0.5, 0.4, 0.05);
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    /** 回収結晶が叩かれたらポーチを返す。 回収結晶でなければ false。 */
    private static boolean handlePouchRecoveryHit(ArmorStand stand, Player recipient) {
        CompoundTag pd = stand.getPersistentData();
        if (!pd.contains(POUCH_RECOVERY_NBT, 10)) return false;
        ItemStack pouch = ItemStack.of(pd.getCompound(POUCH_RECOVERY_NBT));
        if (stand.level() instanceof ServerLevel sl) {
            spawnShatterParticles(sl, stand.getX(), stand.getY() + 0.5, stand.getZ());
            sl.playSound(null, stand.getX(), stand.getY(), stand.getZ(),
                    SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0f, 1.2f);
            if (!pouch.isEmpty()) {
                if (recipient != null) {
                    if (!recipient.getInventory().add(pouch)) recipient.drop(pouch, false);
                } else {
                    sl.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                            sl, stand.getX(), stand.getY(), stand.getZ(), pouch));
                }
            }
        }
        recoveryCrystals.remove(stand.getUUID());
        stand.discard();
        return true;
    }

    /**
     * 結晶を「ブロック相当の遮蔽物」 として扱い、 攻撃者 → オーナーの線分が結晶の
     * AABB を貫けば防御成立 ( = エンティティ貫通攻撃でもブロックを貫通しない攻撃は防げる )。
     *
     * 注意:
     *   - 真にブロックを貫通する攻撃 ( bypasses_invulnerability の DamageSource ) は防げない
     *   - 攻撃元位置が分からない damage ( 毒 / 飢餓 等 ) は防御対象外
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player owner)) return;
        if (owner.level().isClientSide()) return;
        UUID crystalId = existingCrystal.get(owner.getUUID());
        if (crystalId == null) return;
        if (!(owner.level() instanceof ServerLevel sl)) return;
        Entity crystal = sl.getEntity(crystalId);
        if (!(crystal instanceof ArmorStand stand) || !stand.isAlive()) {
            existingCrystal.remove(owner.getUUID());
            return;
        }

        // bypasses_invulnerability ( /kill, void 等 ) は防げない
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        // 攻撃元位置を解決 ( direct entity → causing entity → source position の順 )
        Entity direct = event.getSource().getDirectEntity();
        Entity attacker = event.getSource().getEntity();
        boolean isExplosion = event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
        Vec3 from;
        if (direct != null) {
            from = isExplosion ? direct.position() : direct.getEyePosition();
        } else if (attacker != null) {
            from = isExplosion ? attacker.position() : attacker.getEyePosition();
        } else {
            Vec3 srcPos = event.getSource().getSourcePosition();
            if (srcPos == null) {
                // 爆発で原因不明 ( /summon TNT 経由等 ) は防げない
                return;
            }
            from = srcPos;
        }
        Vec3 to = owner.getEyePosition();

        // 結晶を「ブロック相当」 の AABB に膨らませて、 from→to の線分と交差判定
        AABB crystalAabb = stand.getBoundingBox().inflate(CRYSTAL_BLOCK_AABB_INFLATE);
        Optional<Vec3> intersect = crystalAabb.clip(from, to);
        if (intersect.isEmpty()) {
            // 爆発の追加判定:
            //   震源位置が ずれていたり ( =caused entity の眼位置 ≠ 実際の爆心 ) 、
            //   爆風が広範囲のため strict な line-of-sight 判定では漏れることがある。
            //   結晶が owner と震源の「間」 にあれば防御成立 ( 方向 cone + 距離 )。
            if (!isExplosion) return;
            Vec3 ownerPos   = owner.position().add(0, owner.getBbHeight() / 2.0, 0);
            Vec3 crystalCtr = stand.getBoundingBox().getCenter();
            Vec3 toCrystal  = crystalCtr.subtract(from);
            Vec3 toOwner    = ownerPos.subtract(from);
            double crystalDist = toCrystal.length();
            double ownerDist   = toOwner.length();
            if (crystalDist < 1.0E-3 || ownerDist < 1.0E-3) return;
            // 結晶が owner より震源に近い ( 間にある ) 必要
            if (crystalDist > ownerDist) return;
            // 結晶 と owner が震源から見て だいたい同方向 ( cos > 0.75 = 約 41° 以内 )
            double cosAngle = toCrystal.normalize().dot(toOwner.normalize());
            if (cosAngle < 0.75) return;
            // 通過 OK ( 防御成立 ) — 通常の AABB 経路に合流
        }

        // 結晶が代わりにダメージを受ける ( キャンセル )
        event.setCanceled(true);
        try {
            stand.hurt(event.getSource(), Math.max(1.0f, event.getAmount() * 0.5f));
        } catch (Throwable ignored) {}
        // 結晶のヒット演出
        Vec3 crystalPos = stand.position();
        DustParticleOptions hit = new DustParticleOptions(
                new Vector3f(1.0f, 0.35f, 0.7f), 1.8f);
        sl.sendParticles(hit, crystalPos.x, crystalPos.y + 0.6, crystalPos.z,
                20, 0.3, 0.4, 0.3, 0.1);
        sl.playSound(null, crystalPos.x, crystalPos.y, crystalPos.z,
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    /** 結晶の自動消滅タイマー + パーティクル + プレイヤーが Sneak で武器発行 + 当たり判定維持 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null) return;

        // ポーチ ItemEntity の危険検知 ( マグマ/炎/サボテン等 → 回収結晶へ変換 )
        tickPouchDanger();

        // === 毎 tick: 当たり判定 ( AABB ) を block 相当に再設定 + 周辺エンティティを押し出す ===
        //   ArmorStand 既定 hitbox は refreshDimensions でリセットされうるので毎 tick 再適用。
        //   かつ AABB 内に居る非味方を外側へ押し出して通り抜けを物理的に防ぐ。
        for (Map.Entry<UUID, UUID> entry : existingCrystal.entrySet()) {
            UUID ownerId = entry.getKey();
            Player owner = event.getServer().getPlayerList().getPlayer(ownerId);
            if (owner == null) continue;
            if (!(owner.level() instanceof ServerLevel sl0)) continue;
            Entity crystal0 = sl0.getEntity(entry.getValue());
            if (!(crystal0 instanceof ArmorStand stand0) || !stand0.isAlive()) continue;

            // 1) 1×1×1 AABB を強制
            Vec3 cp = stand0.position().add(0, 0.5, 0); // 中心は足元 + 0.5
            applyCrystalBoundingBox(stand0, cp);

            // 2) AABB に侵入したエンティティを 直接 外へ「テレポート」( push() の velocity 加算は
            //    走り込む player に追いつかず貫通する。 座標固定の方が確実 )。
            //    結晶の AABB を 少し縮めた "コア" に侵入を判定 → コアの外へ最短距離で押し戻す。
            AABB box = stand0.getBoundingBox();
            AABB core = box.deflate(0.05); // 端ぎりぎりは許容 ( 隙間 5cm )
            for (net.minecraft.world.entity.LivingEntity le
                    : sl0.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box)) {
                if (le == stand0) continue;
                // owner / 同チームは通り抜け OK ( 敵だけ通せない )
                if (le instanceof Player p
                        && (p.getUUID().equals(ownerId) || areAllies(owner, p))) continue;

                AABB eb = le.getBoundingBox();
                // 各軸での「コアからの食い込み量」 を計算 ( 軸ごとに 必要な押し戻し距離 )
                double overlapMinX = core.maxX - eb.minX;
                double overlapMaxX = eb.maxX - core.minX;
                double overlapMinZ = core.maxZ - eb.minZ;
                double overlapMaxZ = eb.maxZ - core.minZ;
                // 食い込んでない軸はスキップ
                if (overlapMinX <= 0 && overlapMaxX <= 0
                        && overlapMinZ <= 0 && overlapMaxZ <= 0) continue;

                // 最小の食い込み軸を選んで そっち向けにテレポート
                double dx = 0, dz = 0;
                double minPush = Double.MAX_VALUE;
                if (overlapMinX > 0 && overlapMinX < minPush) { minPush = overlapMinX; dx = -overlapMinX - 0.01; dz = 0; }
                if (overlapMaxX > 0 && overlapMaxX < minPush) { minPush = overlapMaxX; dx =  overlapMaxX + 0.01; dz = 0; }
                if (overlapMinZ > 0 && overlapMinZ < minPush) { minPush = overlapMinZ; dz = -overlapMinZ - 0.01; dx = 0; }
                if (overlapMaxZ > 0 && overlapMaxZ < minPush) { minPush = overlapMaxZ; dz =  overlapMaxZ + 0.01; dx = 0; }

                try {
                    le.teleportTo(le.getX() + dx, le.getY(), le.getZ() + dz);
                    le.hurtMarked = true;
                } catch (Throwable ignored) {}
            }
        }

        if ((event.getServer().getTickCount() % 5) != 0) return; // 以下は 5tick おきの軽量化

        // ポーチ回収結晶の寿命/演出
        tickRecoveryCrystals();

        // オーナーが Sneak している間に結晶を「ガード解除」 → 武器発行
        for (Map.Entry<UUID, UUID> entry : existingCrystal.entrySet()) {
            UUID ownerId = entry.getKey();
            Player owner = event.getServer().getPlayerList().getPlayer(ownerId);
            if (owner == null || !owner.isAlive()) continue;
            if (!owner.isShiftKeyDown()) continue;
            // Sneak が押されている → 結晶を破壊し、 武器発行
            ServerLevel sl = (ServerLevel) owner.level();
            Entity crystal = sl.getEntity(entry.getValue());
            if (!(crystal instanceof ArmorStand stand)) continue;
            stand.discard();
            existingCrystal.remove(ownerId);
            ItemStack template = ItemStack.EMPTY;
            ItemStack main = owner.getMainHandItem();
            if (isMagicalKatana(main) && !isMaterialized(main)) template = main;
            materializeFor(sl, stand.position().add(0, 0.5, 0), ownerId, template);
            break; // 一度に複数処理せず次 tick で
        }

        // 寿命減算 + hover パーティクル。
        // existingCrystal マップが全結晶を追跡しているので、 全ワールドの全エンティティ走査
        // ( 旧 getAllEntities ) はせず、 マップから直接 結晶スタンドを引く ( 毎 5tick の重い全走査を排除 )。
        java.util.Iterator<Map.Entry<UUID, UUID>> it = existingCrystal.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> entry = it.next();
            Player owner = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (owner == null || !(owner.level() instanceof ServerLevel sl)) continue; // オーナー不在時は据え置き
            Entity e = sl.getEntity(entry.getValue());
            if (!(e instanceof ArmorStand stand) || !stand.isAlive()) continue;
            CompoundTag pd = stand.getPersistentData();
            if (!pd.contains(CRYSTAL_OWNER_KEY)) continue;

            int life = pd.getInt(CRYSTAL_LIFE_KEY) - 5;
            if (life <= 0) {
                it.remove();
                stand.discard();
                // 期限切れ — 物理的に壊れただけなので武器は出ない (= 殴って壊さないと武器が出ない)
                DustParticleOptions smoke = new DustParticleOptions(
                        new Vector3f(0.4f, 0.4f, 0.4f), 1.2f);
                sl.sendParticles(smoke,
                        stand.getX(), stand.getY() + 0.5, stand.getZ(),
                        20, 0.3, 0.3, 0.3, 0.05);
                continue;
            }
            pd.putInt(CRYSTAL_LIFE_KEY, life);

            // 周囲に常時 hover パーティクル
            DustParticleOptions magenta = new DustParticleOptions(
                    new Vector3f(0.75f, 0.1f, 0.55f), 1.0f);
            sl.sendParticles(magenta,
                    stand.getX(), stand.getY() + 0.6, stand.getZ(),
                    3, 0.2, 0.3, 0.2, 0.005);
            sl.sendParticles(ParticleTypes.ENCHANT,
                    stand.getX(), stand.getY() + 1.2, stand.getZ(),
                    2, 0.2, 0.2, 0.2, 0.01);
        }

        // 孤立した結晶スタンドの掃除はまれ ( 200tick = 10秒毎 ) でよい。
        // ( サーバー再起動で existingCrystal が空になった後に残るスタンド等を回収する )
        if ((event.getServer().getTickCount() % 200) == 0) {
            for (ServerLevel sl : event.getServer().getAllLevels()) {
                for (Entity e : sl.getAllEntities()) {
                    if (!(e instanceof ArmorStand stand)) continue;
                    CompoundTag pd = stand.getPersistentData();
                    if (!pd.contains(CRYSTAL_OWNER_KEY)) continue;
                    UUID ownerId = pd.getUUID(CRYSTAL_OWNER_KEY);
                    // existingCrystal が追跡している現役の結晶は上のループに任せ、 孤立分だけ破棄
                    if (stand.getUUID().equals(existingCrystal.get(ownerId))) continue;
                    stand.discard();
                }
            }
        }
    }
}
