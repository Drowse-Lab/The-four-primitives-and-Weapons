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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Vector3f;

import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

import java.util.Map;
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
 *      - 侵食属性 Lv 12 ( = XII 表記、 ERROR 級ダメージ )
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
    private static final String UNLOCKED_KEY      = "MagicalKatanaUnlocked";
    /** player persistent data: 結晶化前の magical katana NBT を保存して、 具現化版に転送する */
    private static final String PLAYER_SAVED_MK_NBT_KEY = "SavedMagicalKatanaNBT";

    private static final int    CRYSTAL_LIFE        = 600;  // 30 sec auto-expire
    private static final float  CRYSTAL_MAX_HEALTH  = 0.5f; // 素手 1 撃 (1 ダメージ) で破壊できる
    private static final int    MATERIALIZED_LEVEL  = 12;   // Lv 12 = XII 表記

    /** owner UUID → 既存結晶があるなら entity UUID ( 同時に複数生やさない ) */
    private static final Map<UUID, UUID> existingCrystal = new ConcurrentHashMap<>();

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
     */
    public static int destroyAllOwnedMaterialized(net.minecraft.server.MinecraftServer server, UUID ownerId) {
        if (server == null) return 0;
        int destroyed = 0;
        for (ServerLevel sl : server.getAllLevels()) {
            for (Player p : sl.players()) {
                var inv = p.getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack s = inv.getItem(i);
                    if (!isMaterialized(s)) continue;
                    CompoundTag tg = s.getTag();
                    if (tg == null || !tg.hasUUID(MAT_OWNER_KEY)) continue;
                    if (!tg.getUUID(MAT_OWNER_KEY).equals(ownerId)) continue;
                    // 破壊 = 完全消滅 ( 残骸の Magical Katana は残さない )
                    inv.setItem(i, ItemStack.EMPTY);
                    destroyed++;
                    // 演出 — 侵食属性のイメージカラー ( 赤紫 + ピンク寄り赤紫 )
                    if (sl != null) {
                        spawnShatterParticles(sl, p.getX(), p.getY() + 1.0, p.getZ());
                        sl.playSound(null, p.getX(), p.getY(), p.getZ(),
                                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
                    }
                }
            }
            // 落下しているアイテムにも対応
            for (Entity e : sl.getAllEntities()) {
                if (!(e instanceof net.minecraft.world.entity.item.ItemEntity ie)) continue;
                ItemStack s = ie.getItem();
                if (!isMaterialized(s)) continue;
                CompoundTag tg = s.getTag();
                if (tg == null || !tg.hasUUID(MAT_OWNER_KEY)) continue;
                if (!tg.getUUID(MAT_OWNER_KEY).equals(ownerId)) continue;
                ie.discard();
                destroyed++;
            }
        }
        return destroyed;
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
        return weapon;
    }

    /**
     * Magical Katana の侵食特殊技で結晶を生成。 視線方向に 2.5 ブロック先に置く。
     * 既に owner の結晶があれば破壊してから新しく作る。
     */
    public static void spawnCrystal(Player player) {
        spawnCrystal(player, ItemStack.EMPTY);
    }

    /**
     * 結晶生成 + 結晶化前の Magical Katana NBT を player に保存。
     * sourceStack の NBT ( = エンチャント / レアリティ等 ) を player.persistentData に保存しておき、
     * 具現化版が作られるときにそこから取り出して引き継ぐ。
     */
    public static void spawnCrystal(Player player, ItemStack sourceStack) {
        if (player == null || player.level().isClientSide()) return;
        ServerLevel sl = (ServerLevel) player.level();

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

        ArmorStand stand = new ArmorStand(sl, spawn.x, spawn.y - 0.5, spawn.z);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        // setShowArms / setSmall は protected アクセスなので NBT 経由で設定
        CompoundTag prelim = new CompoundTag();
        stand.addAdditionalSaveData(prelim);
        prelim.putBoolean("Small", true);
        prelim.putBoolean("ShowArms", false);
        prelim.putBoolean("Marker", false); // false = ヒット判定残す ( 殴れる )
        stand.readAdditionalSaveData(prelim);
        // ArmorStand は default invulnerable がきつい設定なので明示的に hurtable に
        stand.setInvulnerable(false);
        stand.setCustomName(Component.literal("§5魔の結晶"));
        stand.setCustomNameVisible(true);
        stand.getPersistentData().putUUID(CRYSTAL_OWNER_KEY, player.getUUID());
        stand.getPersistentData().putInt(CRYSTAL_LIFE_KEY, CRYSTAL_LIFE);
        // ArmorStand 自身の HP は固定 (= killer は player 想定、 1 撃で死ぬのを避けたい場合は調整)
        try {
            stand.setHealth(CRYSTAL_MAX_HEALTH);
        } catch (Throwable ignored) {}

        sl.addFreshEntity(stand);
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
        // 破壊 = 完全消滅 ( 残骸の Magical Katana は残さない )
        player.setItemInHand(hand, ItemStack.EMPTY);
        // 破壊演出 — 侵食属性のイメージカラー ( 赤紫 + ピンク寄り赤紫 )
        if (player.level() instanceof ServerLevel sl) {
            spawnShatterParticles(sl, player.getX(), player.getY() + 1.0, player.getZ());
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.2f, 0.8f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    // ─────────────────────────────────────────────────────────────
    // 内部
    // ─────────────────────────────────────────────────────────────

    /** 具現化武器を作って指定 player の手に入れる ( 入らなければ落下 ItemEntity ) */
    private static void materializeFor(ServerLevel sl, Vec3 pos, UUID ownerId, ItemStack templateForEnchants) {
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

        // owner に渡す ( 居なければ落下 )
        Player owner = sl.getServer().getPlayerList().getPlayer(ownerId);
        if (owner != null && owner.isAlive()) {
            // 手に持たせたい — main hand が空ならそこへ、 そうでなければ inventory.add
            if (owner.getMainHandItem().isEmpty()) {
                owner.setItemInHand(InteractionHand.MAIN_HAND, weapon);
            } else if (!owner.getInventory().add(weapon)) {
                owner.drop(weapon, false);
            }
        } else {
            net.minecraft.world.entity.item.ItemEntity ie =
                    new net.minecraft.world.entity.item.ItemEntity(sl, pos.x, pos.y, pos.z, weapon);
            sl.addFreshEntity(ie);
        }

        // 演出
        DustParticleOptions burst = new DustParticleOptions(
                new Vector3f(1.0f, 0.35f, 0.7f), 1.8f);
        sl.sendParticles(burst, pos.x, pos.y, pos.z, 60, 0.5, 0.5, 0.5, 0.15);
        sl.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        sl.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5f, 1.2f);
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
     * ArmorStand を Player が殴ると ArmorStand.hurt() は LivingEntity.die() を経由せず
     * 直接 kill() → remove() するため LivingDeathEvent が発火しない。
     * そのため AttackEntityEvent で先回りして検知し、 結晶ならその場で武器を具現化する。
     */
    @SubscribeEvent
    public static void onAttackEntity(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        Entity target = event.getTarget();
        if (!(target instanceof ArmorStand stand)) return;
        CompoundTag pd = stand.getPersistentData();
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

    /**
     * 結晶がオーナーと攻撃者の間にあれば、 オーナーへのダメージを結晶が肩代わり。
     * これで「抜刀時の結晶で防御できる」を実現。
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

        // 結晶が攻撃者とオーナーの間にあるかを軽量チェック
        Entity attacker = event.getSource().getEntity();
        Vec3 ownerPos    = owner.position();
        Vec3 crystalPos  = stand.position();
        Vec3 attackerPos = (attacker != null) ? attacker.position() : ownerPos.add(0, 0, 1);

        Vec3 oa = attackerPos.subtract(ownerPos);
        Vec3 oc = crystalPos.subtract(ownerPos);
        // 結晶がオーナーから攻撃者方向に存在し、 直線距離が近ければ防御成立
        boolean inFront = oa.lengthSqr() > 1.0E-4 && oc.dot(oa.normalize()) > 0.0;
        boolean closeEnough = crystalPos.distanceToSqr(ownerPos) <= 16.0; // 4m 以内
        if (!inFront || !closeEnough) return;

        // 結晶が代わりにダメージを受ける ( キャンセル )
        event.setCanceled(true);
        try {
            stand.hurt(event.getSource(), Math.max(1.0f, event.getAmount() * 0.5f));
        } catch (Throwable ignored) {}
        // 結晶のヒット演出
        DustParticleOptions hit = new DustParticleOptions(
                new Vector3f(1.0f, 0.35f, 0.7f), 1.8f);
        sl.sendParticles(hit, crystalPos.x, crystalPos.y + 0.6, crystalPos.z,
                20, 0.3, 0.4, 0.3, 0.1);
        sl.playSound(null, crystalPos.x, crystalPos.y, crystalPos.z,
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    /** 結晶の自動消滅タイマー + パーティクル + プレイヤーが Sneak で武器発行 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null) return;
        if ((event.getServer().getTickCount() % 5) != 0) return; // 5tick おきの軽量化

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

        for (ServerLevel sl : event.getServer().getAllLevels()) {
            for (Entity e : sl.getAllEntities()) {
                if (!(e instanceof ArmorStand stand)) continue;
                CompoundTag pd = stand.getPersistentData();
                if (!pd.contains(CRYSTAL_OWNER_KEY)) continue;

                int life = pd.getInt(CRYSTAL_LIFE_KEY);
                life -= 5;
                if (life <= 0) {
                    UUID ownerId = pd.getUUID(CRYSTAL_OWNER_KEY);
                    existingCrystal.remove(ownerId);
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
        }
    }
}
