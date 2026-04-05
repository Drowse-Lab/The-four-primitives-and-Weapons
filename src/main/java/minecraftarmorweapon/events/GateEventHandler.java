package minecraftarmorweapon.events;

import minecraftarmorweapon.item.GateItem;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gate剣のメインロジック
 *
 * poof.mcfunction の完全移植:
 *
 * 【儀式（召喚）】
 *   golden_sword + nether_star x64 + gold_block x64 をドロップ状態で
 *   半径1.5ブロック以内に集めると Gate剣に変換される。
 *
 * 【右クリック発動】
 *   Gate剣を持って右クリック → fr1（追尾プロジェクタイル）を発射
 *   Resistance付与・サウンド・パーティクル演出
 *
 * 【fr1 プロジェクタイル挙動】
 *   - 毎tick fr（ターゲットマーカー）の方向に向かって移動
 *   - 射程50ブロック前進で消滅
 *   - 敵（4ブロック以内）に当たるとTNT召喚→自爆
 *   - 50tick経過で自然消滅
 *   - 地面ブロックに触れると爆発
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class GateEventHandler {

    // ── 定数 ──────────────────────────────────────────────────────
    private static final String TAG_FR   = "gate_fr";   // ターゲットマーカー
    private static final String TAG_FR1  = "gate_fr1";  // 追尾プロジェクタイル
    private static final String TAG_FL   = "gate_fl";   // プレイヤー足元マーカー
    private static final String TAG_FL1  = "gate_fl1";  // 発射フラグ付きマーカー

    private static final int PROJECTILE_MAX_TICK = 50;
    private static final double PROJECTILE_SPEED = 4.0;
    private static final double EXPLODE_RANGE    = 4.0;
    private static final double RITUAL_RANGE     = 1.5;

    // fr1のtickカウンタ { entityUUID -> tick }
    private static final Map<UUID, Integer> fr1TickMap = new ConcurrentHashMap<>();

    // 右クリック検知フラグ { playerUUID -> tick }
    private static final Map<UUID, Integer> useMap = new ConcurrentHashMap<>();

    // ──────────────────────────────────────────────────────────────
    // 右クリック検知
    // ──────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        if (!GateItem.isGateSword(held)) return;
        if (player.level().isClientSide()) return;

        useMap.put(player.getUUID(), 1);
    }

    // ──────────────────────────────────────────────────────────────
    // ServerTick — poof.mcfunction の毎tick処理
    // ──────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            tickRitual(level);
            tickPlayers(level);
            tickProjectiles(level);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 儀式検知
    // golden_sword + nether_star x64 + gold_block x64 が半径1.5以内
    // ──────────────────────────────────────────────────────────────

    private static void tickRitual(ServerLevel level) {
        // ドロップしている golden_sword を検索
        List<ItemEntity> swords = level.getEntitiesOfClass(ItemEntity.class,
                new AABB(-30000, -64, -30000, 30000, 320, 30000),
                e -> e.getItem().getItem() == Items.GOLDEN_SWORD);

        for (ItemEntity swordEntity : swords) {
            Vec3 pos = swordEntity.position();
            AABB range = new AABB(pos, pos).inflate(RITUAL_RANGE);

            // nether_star x64 があるか
            boolean hasStar = level.getEntitiesOfClass(ItemEntity.class, range,
                    e -> e.getItem().getItem() == Items.NETHER_STAR
                            && e.getItem().getCount() >= 64).size() > 0;
            // gold_block x64 があるか
            boolean hasGold = level.getEntitiesOfClass(ItemEntity.class, range,
                    e -> e.getItem().getItem() == Items.GOLD_BLOCK
                            && e.getItem().getCount() >= 64).size() > 0;

            if (!hasStar || !hasGold) continue;

            // 儀式成功 → Gate剣をドロップアイテムから回収して付近プレイヤーに渡す
            List<Player> nearPlayers = level.getEntitiesOfClass(Player.class, range.inflate(10));
            if (nearPlayers.isEmpty()) continue;

            Player nearest = nearPlayers.get(0);
            ItemStack gateSword = new ItemStack(
                    net.minecraftforge.registries.ForgeRegistries.ITEMS
                            .getValue(new net.minecraft.resources.ResourceLocation(
                                    "minecraft_armor_weapon", "gate_sword")));
            nearest.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, gateSword);

            // 周囲アイテムを全消去（元: kill @e[type=item,distance=..10]）
            level.getEntitiesOfClass(ItemEntity.class,
                    new AABB(pos, pos).inflate(10)).forEach(Entity::kill);

            // ターゲットマーカー（fr）と待機マーカー（k32）を召喚
            spawnArmorStand(level, pos.add(0, -0.5, 0), TAG_FR, false);

            // サウンド＆パーティクル
            level.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.VOICE, 10f, 2f);
            level.sendParticles(
                    new DustParticleOptions(new Vector3f(1f, 1f, 0f), 1f),
                    pos.x - 0.4, pos.y + 0.8, pos.z + 0.5,
                    1000, 0.2, 0.2, 0.2, 2.0);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // プレイヤーtick処理
    // ──────────────────────────────────────────────────────────────

    private static void tickPlayers(ServerLevel level) {
        for (Player player : level.players()) {
            ItemStack held = player.getMainHandItem();
            if (!GateItem.isGateSword(held)) continue;

            Vec3 pos = player.position();

            // 持っている間はパーティクル（元: aq2カウント＆パーティクル）
            level.sendParticles(
                    new DustParticleOptions(new Vector3f(1f, 0.816f, 0f), 1f),
                    pos.x - 0.4, pos.y + 1.0, pos.z + 0.5,
                    1, 0.2, 0.2, 0.2, 0.1);

            // frマーカーをプレイヤーの前方50ブロックに移動
            level.getEntitiesOfClass(ArmorStand.class,
                    new AABB(pos, pos).inflate(200),
                    e -> hasTag(e, TAG_FR)
            ).forEach(fr -> {
                Vec3 look = player.getLookAngle();
                fr.setPos(pos.add(look.scale(50)).add(0, 1.5, 0));
            });

            // 右クリック発動
            if (useMap.containsKey(player.getUUID())) {
                useMap.remove(player.getUUID());
                fireProjectile(level, player);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // プロジェクタイル発射（元: use=1..のブロック）
    // ──────────────────────────────────────────────────────────────

    private static void fireProjectile(ServerLevel level, Player player) {
        Vec3 pos = player.position();
        Vec3 look = player.getLookAngle();

        // Resistance付与
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 100, true, false));

        // サウンド
        for (int i = 0; i < 4; i++) {
            level.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.WITHER_SHOOT, SoundSource.VOICE, 2f, 1f);
        }

        // fr1を3方向に召喚（元: 3体のarmor_stand fr1）
        double[] offsets = {3.5, 0, -3.5};
        for (double xOff : offsets) {
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize().scale(xOff);
            Vec3 spawnPos = pos.add(right).add(look.scale(-2)).add(0, 0.6, 0);
            ArmorStand fr1 = spawnArmorStand(level, spawnPos, TAG_FR1, true);
            if (fr1 != null) {
                fr1TickMap.put(fr1.getUUID(), 0);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // プロジェクタイルtick（元: fr1のtick処理ブロック）
    // ──────────────────────────────────────────────────────────────

    private static void tickProjectiles(ServerLevel level) {
        List<ArmorStand> fr1List = level.getEntitiesOfClass(ArmorStand.class,
                new AABB(-30000, -64, -30000, 30000, 320, 30000),
                e -> hasTag(e, TAG_FR1));

        // frターゲット（一番近いもの）
        List<ArmorStand> frList = level.getEntitiesOfClass(ArmorStand.class,
                new AABB(-30000, -64, -30000, 30000, 320, 30000),
                e -> hasTag(e, TAG_FR));

        for (ArmorStand fr1 : fr1List) {
            UUID id  = fr1.getUUID();
            int tick = fr1TickMap.getOrDefault(id, 0) + 1;
            fr1TickMap.put(id, tick);

            Vec3 pos = fr1.position();

            // パーティクル
            level.sendParticles(
                    new DustParticleOptions(new Vector3f(1f, 0.816f, 0f), 1f),
                    pos.x - 0.45, pos.y + 1.0, pos.z,
                    3, 0.2, 0.2, 0.2, 0.5);

            // frに向かって移動（tick 1〜15: 旋回、15〜: 直進）
            if (!frList.isEmpty()) {
                ArmorStand fr = frList.get(0);
                Vec3 dir = fr.position().subtract(pos).normalize();

                if (tick <= 15) {
                    // 徐々にfrの方向へ旋回
                    Vec3 current = fr1.getDeltaMovement().add(dir.scale(0.3)).normalize()
                            .scale(PROJECTILE_SPEED);
                    fr1.setDeltaMovement(current);
                } else {
                    // 直進（元: ^^ ^4 facing fr）
                    fr1.setDeltaMovement(dir.scale(PROJECTILE_SPEED));
                }
                fr1.setPos(pos.add(fr1.getDeltaMovement()));
            }

            // 50tick で消滅（元: aq2=50..）
            if (tick >= PROJECTILE_MAX_TICK) {
                fr1.kill();
                fr1TickMap.remove(id);
                continue;
            }

            // 敵に当たったら TNT + 自爆（元: aq2=21..）
            if (tick >= 21) {
                List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(pos, pos).inflate(EXPLODE_RANGE),
                        e -> !(e instanceof ArmorStand) && !(e instanceof Player));

                if (!nearby.isEmpty()) {
                    // TNT召喚
                    net.minecraft.world.entity.item.PrimedTnt tnt =
                            new net.minecraft.world.entity.item.PrimedTnt(level,
                                    pos.x, pos.y, pos.z, null);
                    tnt.setFuse(0);
                    level.addFreshEntity(tnt);
                    fr1.kill();
                    fr1TickMap.remove(id);
                    continue;
                }
            }

            // 地面ブロックに触れたら爆発（元: if block ~ ~-0.5 ~ dirt/stone... run summon tnt）
            net.minecraft.core.BlockPos below = fr1.blockPosition().below();
            net.minecraft.world.level.block.state.BlockState bs = level.getBlockState(below);
            if (!bs.isAir() && !(bs.getBlock() instanceof net.minecraft.world.level.block.AirBlock)) {
                net.minecraft.world.entity.item.PrimedTnt tnt =
                        new net.minecraft.world.entity.item.PrimedTnt(level,
                                pos.x, pos.y, pos.z, null);
                tnt.setFuse(0);
                level.addFreshEntity(tnt);
                fr1.kill();
                fr1TickMap.remove(id);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // ユーティリティ
    // ──────────────────────────────────────────────────────────────

    private static ArmorStand spawnArmorStand(ServerLevel level, Vec3 pos,
                                               String tag, boolean noGravity) {
        ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, level);
        stand.setPos(pos.x, pos.y, pos.z);
        stand.setNoGravity(noGravity);
        stand.setInvisible(true);
        stand.setShowArms(true);
        stand.addTag(tag);
        level.addFreshEntity(stand);
        return stand;
    }

    private static boolean hasTag(Entity entity, String tag) {
        return entity.getTags().contains(tag);
    }
}
