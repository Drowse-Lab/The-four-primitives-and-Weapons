// package minecraftarmorweapon.events;

// import net.minecraftforge.fml.common.Mod;
// import net.minecraftforge.eventbus.api.SubscribeEvent;
// import net.minecraftforge.eventbus.api.EventPriority;
// import net.minecraftforge.event.entity.player.AttackEntityEvent;
// import net.minecraftforge.event.entity.player.PlayerInteractEvent;
// import net.minecraftforge.event.entity.player.PlayerEvent;
// import net.minecraftforge.common.MinecraftForge;

// import net.minecraft.world.entity.player.Player;
// import net.minecraft.world.entity.LivingEntity;
// import net.minecraft.world.item.ItemStack;
// import net.minecraft.world.InteractionHand;
// import net.minecraft.server.level().ServerLevel;
// import net.minecraft.world.level().Level;
// import net.minecraft.core.particles.ParticleTypes;
// import net.minecraft.sounds.SoundEvents;
// import net.minecraft.sounds.SoundSource;

// import minecraftarmorweapon.procedures.MagicKatanaComboSystemProcedure;
// import minecraftarmorweapon.item.MagischesFeenKatanaItem;
// import minecraftarmorweapon.item.MagicalKatanaItem;

// import java.util.HashMap;
// import java.util.Map;
// import java.util.UUID;

// @Mod.EventBusSubscriber
// public class MagicKatanaComboHandler {

//     private static final Map<UUID, Long> LAST_ATTACK_TIME = new HashMap<>();
//     private static final long ATTACK_COOLDOWN = 200; // 200ms between attacks

//     /**
//      * 左クリック攻撃時のイベント
//      */
//     @SubscribeEvent(priority = EventPriority.HIGH)
//     public static void onAttackEntity(AttackEntityEvent event) {
//         Player player = event.getEntity();
//         ItemStack heldItem = player.getMainHandItem();

//         // 魔法刀を持っているかチェック
//         if (!isMagicKatana(heldItem)) return;

//         // クールダウンチェック
//         UUID playerId = player.getUUID();
//         long currentTime = System.currentTimeMillis();
//         Long lastAttack = LAST_ATTACK_TIME.get(playerId);

//         if (lastAttack != null && currentTime - lastAttack < ATTACK_COOLDOWN) {
//             event.setCanceled(true);
//             return;
//         }

//         LAST_ATTACK_TIME.put(playerId, currentTime);

//         // コンボ攻撃を実行
//         Level world = player.level();
//         MagicKatanaComboSystemProcedure.executeComboAttack(
//             world,
//             player.getX(),
//             player.getY(),
//             player.getZ(),
//             player,
//             heldItem
//         );

//         // 通常の攻撃をキャンセルして独自処理を行う
//         event.setCanceled(true);

//         // 攻撃対象にダメージインジケーター
//         if (event.getTarget() instanceof LivingEntity target && world instanceof ServerLevel serverLevel) {
//             serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
//                 target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
//                 5, 0.2, 0.2, 0.2, 0.1);
//         }
//     }

//     /**
//      * 右クリック時のイベント（コンボリセット、特殊アクション）
//      */
//     @SubscribeEvent
//     public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
//         Player player = event.getEntity();
//         ItemStack heldItem = event.getItemStack();

//         if (!isMagicKatana(heldItem)) return;

//         // スニーク中は特殊アクション
//         if (player.isShiftKeyDown()) {
//             executeSpecialAction(player, heldItem);
//         } else {
//             // 通常右クリックはコンボリセット
//             MagicKatanaComboSystemProcedure.resetCombo(player);
//         }

//         event.setCanceled(true);
//     }

//     /**
//      * 特殊アクション（スニーク+右クリック）
//      */
//     private static void executeSpecialAction(Player player, ItemStack itemstack) {
//         Level world = player.level();

//         // 瞬間移動斬り
//         if (world instanceof ServerLevel serverLevel) {
//             // ターゲットを探す
//             LivingEntity target = null;
//             double minDistance = Double.MAX_VALUE;

//             for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class,
//                     player.getBoundingBox().inflate(10))) {
//                 if (entity != player && player.hasLineOfSight(entity)) {
//                     double distance = entity.distanceTo(player);
//                     if (distance < minDistance) {
//                         minDistance = distance;
//                         target = entity;
//                     }
//                 }
//             }

//             if (target != null) {
//                 // ターゲットの背後にテレポート
//                 double angle = Math.atan2(player.getZ() - target.getZ(), player.getX() - target.getX());
//                 double teleportX = target.getX() - Math.cos(angle) * 2;
//                 double teleportZ = target.getZ() - Math.sin(angle) * 2;

//                 // テレポートエフェクト
//                 serverLevel.sendParticles(ParticleTypes.PORTAL,
//                     player.getX(), player.getY() + 1, player.getZ(),
//                     30, 0.5, 1, 0.5, 0.1);

//                 player.teleportTo(teleportX, target.getY(), teleportZ);

//                 serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
//                     player.getX(), player.getY() + 1, player.getZ(),
//                     30, 0.5, 1, 0.5, 0.1);

//                 // 背後からの攻撃
//                 minecraftarmorweapon.util.DamageCalculator.dealDamage(
//                     player, target, 25.0f, itemstack
//                 );

//                 world.playSound(null, player.getX(), player.getY(), player.getZ(),
//                     SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.2f);
//                 world.playSound(null, target.getX(), target.getY(), target.getZ(),
//                     SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.5f, 1.0f);
//             } else {
//                 // ターゲットがいない場合は前方ダッシュ
//                 double dashDistance = 8;
//                 double yaw = Math.toRadians(player.getYRot());
//                 double newX = player.getX() - Math.sin(yaw) * dashDistance;
//                 double newZ = player.getZ() + Math.cos(yaw) * dashDistance;

//                 // ダッシュエフェクト
//                 for (int i = 0; i <= 10; i++) {
//                     double ratio = i / 10.0;
//                     double particleX = player.getX() + (newX - player.getX()) * ratio;
//                     double particleZ = player.getZ() + (newZ - player.getZ()) * ratio;

//                     serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH,
//                         particleX, player.getY() + 1, particleZ,
//                         1, 0, 0, 0, 0);
//                 }

//                 player.teleportTo(newX, player.getY(), newZ);

//                 world.playSound(null, player.getX(), player.getY(), player.getZ(),
//                     SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 2.0f);
//             }
//         }
//     }

//     /**
//      * プレイヤーログアウト時のクリーンアップ
//      */
//     @SubscribeEvent
//     public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
//         Player player = event.getEntity();
//         MagicKatanaComboSystemProcedure.clearPlayerCombo(player);
//         LAST_ATTACK_TIME.remove(player.getUUID());
//     }

//     /**
//      * アイテムが魔法刀かチェック
//      */
//     private static boolean isMagicKatana(ItemStack stack) {
//         return stack.getItem() instanceof MagischesFeenKatanaItem ||
//                stack.getItem() instanceof MagicalKatanaItem;
//     }

//     /**
//      * ハンドラーを登録
//      */
//     public static void register() {
//         MinecraftForge.EVENT_BUS.register(MagicKatanaComboHandler.class);
//     }
// }