package minecraftarmorweapon.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import minecraftarmorweapon.command.CustomDifficultyCommand;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class TrueCrafterAI {
    
    // 一時ブロックの管理
    private static final Map<BlockPos, Long> temporaryBlocks = new ConcurrentHashMap<>();
    private static final long BLOCK_DECAY_TIME = 15000; // 15秒後に消える
    
    // モンスターのスポーン時にAIを強化
    @SubscribeEvent
    public static void onEntitySpawn(LivingSpawnEvent.SpecialSpawn event) {
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }
        
        CustomDifficultyCommand.CustomDifficulty difficulty = CustomDifficultyCommand.getCurrentDifficulty();
        
        // nightmare以上の難易度で強制的にTrue Crafter AIを有効化
        if (!isTrueCrafterEnabled(difficulty)) {
            return;
        }
        
        // 装備を追加
        equipMonster(monster, difficulty);
        
        // カスタムAIゴールを追加
        addCustomAIGoals(monster, difficulty);
    }
    
    // モンスターに装備を追加
    private static void equipMonster(Monster monster, CustomDifficultyCommand.CustomDifficulty difficulty) {
        net.minecraft.util.RandomSource random = monster.getRandom();
        int difficultyLevel = getDifficultyLevel(difficulty);
        
        // 全モンスターにヘルメット（日光耐性）
        if (!monster.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            // 既にヘルメットがある場合はスキップ
        } else if (difficultyLevel >= 3) {
            // lunatic以上は鉄ヘルメット
            monster.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        } else if (difficultyLevel >= 1) {
            // nightmare以上は革または鉄ヘルメット
            monster.setItemSlot(EquipmentSlot.HEAD, 
                new ItemStack(random.nextFloat() < 0.5f ? Items.LEATHER_HELMET : Items.IRON_HELMET));
        }
        
        if (monster instanceof Zombie zombie) {
            // ゾンビに剣と弓を持たせる
            zombie.setItemSlot(EquipmentSlot.MAINHAND, 
                new ItemStack(random.nextFloat() < 0.5f ? Items.IRON_SWORD : Items.BOW));
            
            // 盾を追加
            zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            
            // 重装備（難易度が高いほど重い）
            if (difficultyLevel >= 2) {
                zombie.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                
                // ノックバック耐性を追加
                zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(
                    0.3 + difficultyLevel * 0.15  // 30%から最大105%
                );
            }
            
            // エリトラ（低確率）
            if (difficultyLevel >= 4 && random.nextFloat() < 0.15f) { // 15%の確率
                zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
            } else if (difficultyLevel >= 2) {
                // エリトラじゃない場合は胸当て
                zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            }
            
            // 弓の場合は矢も持たせる
            if (zombie.getMainHandItem().getItem() instanceof BowItem) {
                zombie.setCanPickUpLoot(true);
            }
            
            // アイテムを拾えるようにする
            zombie.setCanPickUpLoot(true);
            
        } else if (monster instanceof Skeleton skeleton) {
            // スケルトンに剣も持たせる（初期は弓を持っているので剣を準備）
            skeleton.setCanPickUpLoot(true);
            
            // 盾を追加（難易度が高い場合）
            if (difficultyLevel >= 3) {
                skeleton.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            }
            
            // エリトラ（低確率）
            if (difficultyLevel >= 4 && random.nextFloat() < 0.1f) { // 10%の確率
                skeleton.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
            }
        } else if (monster instanceof Spider spider) {
            // クモも強化
            if (difficulty.getName().contains("lunatic")) {
                spider.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
                    spider.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * 1.3
                );
            }
        } else if (monster instanceof Creeper creeper) {
            // クリーパーも速度強化
            if (difficultyLevel >= 3) {
                creeper.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
                    creeper.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * 1.2
                );
            }
        }
    }
    
    // True Crafterモードが有効かチェック（nightmare以上で強制有効）
    private static boolean isTrueCrafterEnabled(CustomDifficultyCommand.CustomDifficulty difficulty) {
        int level = getDifficultyLevel(difficulty);
        // nightmare（レベル1）以上で強制的に有効
        return level >= 1;
    }
    
    // 難易度レベルを取得（0-5）
    private static int getDifficultyLevel(CustomDifficultyCommand.CustomDifficulty difficulty) {
        switch (difficulty.getName()) {
            case "nightmare": return 1;  // True Crafter開始レベル
            case "realistic": return 2;
            case "lunatic": return 3;
            case "lunatic+": return 4;
            case "lunatic_extreme": return 5;
            default: return 0;  // peaceful, easy, normal, hard, creative+は無効
        }
    }
    
    // カスタムAIゴールを追加
    private static void addCustomAIGoals(Monster monster, CustomDifficultyCommand.CustomDifficulty difficulty) {
        GoalSelector goalSelector = monster.goalSelector;
        int level = getDifficultyLevel(difficulty);
        
        // 基本戦術（nightmare以上）
        goalSelector.addGoal(1, new WeaponSwitchingGoal(monster, level));
        goalSelector.addGoal(2, new AcrobaticDodgeGoal(monster, level));
        goalSelector.addGoal(3, new ShieldBlockingGoal(monster, level));
        
        // ゾンビ特有の飛びつき攻撃
        if (monster instanceof Zombie && level >= 2) {
            goalSelector.addGoal(2, new LeapAttackGoal(monster, level));
        }
        
        // 水中でガーディアンに乗る
        if (level >= 2) {
            goalSelector.addGoal(7, new GuardianRidingGoal(monster, level));
        }
        
        // 中級戦術（realistic以上）
        if (level >= 2) {
            goalSelector.addGoal(4, new BlockPlacingGoal(monster, level));
            goalSelector.addGoal(5, new BridgeBuildingGoal(monster, level));
        }
        
        // 上級戦術（lunatic_extreme）
        if (level >= 5) {
            goalSelector.addGoal(6, new ElytraGlidingGoal(monster, level));
        }
    }
    
    // 武器切り替えAI
    public static class WeaponSwitchingGoal extends Goal {
        private final Monster monster;
        private final int difficultyLevel;
        private int switchCooldown = 0;
        private ItemStack savedBow = ItemStack.EMPTY;
        private ItemStack savedSword = ItemStack.EMPTY;
        
        public WeaponSwitchingGoal(Monster monster, int difficultyLevel) {
            this.monster = monster;
            this.difficultyLevel = difficultyLevel;
            
            // モンスタータイプに応じて初期装備を設定
            if (monster instanceof Skeleton) {
                // スケルトンは弓を持っているので剣を準備
                savedBow = new ItemStack(Items.BOW);
                savedSword = new ItemStack(Items.IRON_SWORD);
                // 初期装備として弓を持たせる（スケルトンはデフォルトで弓を持つ）
                if (monster.getMainHandItem().isEmpty()) {
                    monster.setItemSlot(EquipmentSlot.MAINHAND, savedBow.copy());
                }
            } else if (monster instanceof Zombie) {
                // ゾンビは初期装備を保存
                if (monster.getMainHandItem().getItem() instanceof BowItem) {
                    savedBow = monster.getMainHandItem().copy();
                    savedSword = new ItemStack(Items.IRON_SWORD);
                } else if (monster.getMainHandItem().getItem() instanceof SwordItem) {
                    savedSword = monster.getMainHandItem().copy();
                    savedBow = new ItemStack(Items.BOW);
                } else {
                    // 何も持っていない場合はランダムに設定
                    savedSword = new ItemStack(Items.IRON_SWORD);
                    savedBow = new ItemStack(Items.BOW);
                    monster.setItemSlot(EquipmentSlot.MAINHAND, 
                        monster.getRandom().nextFloat() < 0.5f ? savedSword.copy() : savedBow.copy());
                }
            }
        }
        
        @Override
        public boolean canUse() {
            return (monster instanceof Zombie || monster instanceof Skeleton) && 
                   monster.getTarget() != null && 
                   switchCooldown <= 0;
        }
        
        @Override
        public void tick() {
            LivingEntity target = monster.getTarget();
            if (target == null) return;
            
            double distance = monster.distanceTo(target);
            ItemStack currentWeapon = monster.getMainHandItem();
            
            // 距離に応じて武器を切り替え（スケルトンとゾンビ両方）
            if (distance > 8 && !(currentWeapon.getItem() instanceof BowItem)) {
                // 遠距離なら弓に切り替え
                if (!savedBow.isEmpty()) {
                    monster.setItemSlot(EquipmentSlot.MAINHAND, savedBow.copy());
                    // 難易度が高いほどクールダウンが短い
                    switchCooldown = Math.max(20, 60 - difficultyLevel * 8);
                }
            } else if (distance < 4 && !(currentWeapon.getItem() instanceof SwordItem)) {
                // 近距離なら剣に切り替え
                if (!savedSword.isEmpty()) {
                    monster.setItemSlot(EquipmentSlot.MAINHAND, savedSword.copy());
                    switchCooldown = Math.max(20, 60 - difficultyLevel * 8);
                }
            }
            
            if (switchCooldown > 0) {
                switchCooldown--;
            }
        }
    }
    
    // アクロバティック回避AI
    public static class AcrobaticDodgeGoal extends Goal {
        private final Monster monster;
        private final int difficultyLevel;
        private int dodgeCooldown = 0;
        
        public AcrobaticDodgeGoal(Monster monster, int difficultyLevel) {
            this.monster = monster;
            this.difficultyLevel = difficultyLevel;
        }
        
        @Override
        public boolean canUse() {
            return monster.getTarget() != null && monster.hurtTime > 0 && dodgeCooldown <= 0;
        }
        
        @Override
        public void tick() {
            if (dodgeCooldown <= 0) {
                // ダメージを受けたら回避行動
                net.minecraft.util.RandomSource random = monster.getRandom();
                
                // 難易度が高いほど回避距離が長い
                double dodgeMultiplier = 2.0 + difficultyLevel * 0.5;
                double dodgeX = (random.nextDouble() - 0.5) * dodgeMultiplier;
                double dodgeZ = (random.nextDouble() - 0.5) * dodgeMultiplier;
                
                // ジャンプしながら横移動
                Vec3 motion = new Vec3(dodgeX, 0.4 + difficultyLevel * 0.1, dodgeZ);
                monster.setDeltaMovement(motion);
                monster.hasImpulse = true;
                
                // 難易度が高いほどクールダウンが短い
                dodgeCooldown = Math.max(10, 40 - difficultyLevel * 5);
            }
            
            if (dodgeCooldown > 0) {
                dodgeCooldown--;
            }
        }
    }
    
    // 盾防御AI
    public static class ShieldBlockingGoal extends Goal {
        private final Monster monster;
        private final int difficultyLevel;
        private boolean isBlocking = false;
        
        public ShieldBlockingGoal(Monster monster, int difficultyLevel) {
            this.monster = monster;
            this.difficultyLevel = difficultyLevel;
        }
        
        @Override
        public boolean canUse() {
            return monster.getTarget() != null && 
                   monster.getOffhandItem().getItem() instanceof ShieldItem;
        }
        
        @Override
        public void tick() {
            LivingEntity target = monster.getTarget();
            if (target == null) return;
            
            double distance = monster.distanceTo(target);
            
            // 難易度が高いほど防御確率が高い
            double blockChance = 0.3 + difficultyLevel * 0.15; // 30%から最大105%
            double blockRange = 4 + difficultyLevel * 0.5; // 4から最大6.5ブロック
            
            // 近距離で攻撃を受けそうなら盾を構える
            if (distance < blockRange && target instanceof Player player) {
                // プレイヤーが攻撃態勢なら防御（難易度が高いほど反応が良い）
                float attackThreshold = Math.max(0.1f, 0.7f - difficultyLevel * 0.1f);
                if ((player.swingTime > 0 || player.getAttackStrengthScale(0) > attackThreshold) 
                    && monster.getRandom().nextDouble() < blockChance) {
                    monster.startUsingItem(InteractionHand.OFF_HAND);
                    isBlocking = true;
                    
                    // 防御中は移動速度低下（属性の存在を確認）
                    if (monster.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                        try {
                            monster.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
                                monster.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * 0.5
                            );
                        } catch (Exception e) {
                            // 属性が設定できない場合は無視
                        }
                    }
                }
            } else if (isBlocking) {
                monster.stopUsingItem();
                isBlocking = false;
                
                // 移動速度を戻す（属性の存在を確認）
                if (monster.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                    try {
                        monster.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
                            monster.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * 2
                        );
                    } catch (Exception e) {
                        // 属性が設定できない場合は無視
                    }
                }
            }
        }
    }
    
    // ブロック設置AI（登る用）
    public static class BlockPlacingGoal extends Goal {
        private final Monster monster;
        private final int difficultyLevel;
        private int placeCooldown = 0;
        
        public BlockPlacingGoal(Monster monster, int difficultyLevel) {
            this.monster = monster;
            this.difficultyLevel = difficultyLevel;
        }
        
        @Override
        public boolean canUse() {
            return monster.getTarget() != null && placeCooldown <= 0;
        }
        
        @Override
        public void tick() {
            LivingEntity target = monster.getTarget();
            if (target == null) return;
            
            // 高さの差がある場合、ブロックを積んで登る
            if (target.getY() > monster.getY() + 2) {
                BlockPos placePos = monster.blockPosition();
                
                // 足元にブロックを設置
                if (monster.level.getBlockState(placePos.below()).isSolidRender(monster.level, placePos.below())) {
                    BlockPos buildPos = placePos.relative(monster.getDirection());
                    
                    if (monster.level.getBlockState(buildPos).isAir()) {
                        // 土ブロックを設置
                        monster.level.setBlock(buildPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                        
                        // 一時ブロックとして記録
                        temporaryBlocks.put(buildPos, System.currentTimeMillis());
                        
                        // その上に登る
                        monster.setPos(buildPos.getX() + 0.5, buildPos.getY() + 1, buildPos.getZ() + 0.5);
                        monster.setDeltaMovement(0, 0.2, 0);
                        
                        // 難易度が高いほど設置速度が速い
                        placeCooldown = Math.max(5, 20 - difficultyLevel * 3);
                    }
                }
            }
            
            if (placeCooldown > 0) {
                placeCooldown--;
            }
        }
    }
    
    // エリトラ滑空AI
    public static class ElytraGlidingGoal extends Goal {
        private final Monster monster;
        private final int difficultyLevel;
        private int glideCooldown = 0;
        
        public ElytraGlidingGoal(Monster monster, int difficultyLevel) {
            this.monster = monster;
            this.difficultyLevel = difficultyLevel;
        }
        
        @Override
        public boolean canUse() {
            return monster.getTarget() != null && 
                   monster.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA) &&
                   !monster.isOnGround() && glideCooldown <= 0;
        }
        
        @Override
        public void tick() {
            LivingEntity target = monster.getTarget();
            if (target == null) return;
            
            // エリトラで滑空
            if (monster.fallDistance > 2) {
                // エリトラ滑空効果を模擬（モンスターはstartFallFlyingメソッドを持たないため）
                // 落下速度を減少させ、水平方向に滑空
                Vec3 currentMotion = monster.getDeltaMovement();
                
                // ターゲットに向かって滑空（難易度が高いほど速い）
                Vec3 direction = target.position().subtract(monster.position()).normalize();
                double speed = 0.6 + difficultyLevel * 0.1;
                
                // Y軸の速度を緩やかに（滑空効果）
                double glideY = Math.max(-0.08, currentMotion.y * 0.6);
                Vec3 glideMotion = new Vec3(
                    direction.x * speed,
                    glideY,
                    direction.z * speed
                );
                
                monster.setDeltaMovement(glideMotion);
                monster.fallDistance = 0; // 落下ダメージをリセット
                
                // 難易度が高いほどクールダウンが短い
                glideCooldown = Math.max(40, 120 - difficultyLevel * 16);
            }
            
            if (glideCooldown > 0) {
                glideCooldown--;
            }
        }
    }
    
    // 橋建設AI
    public static class BridgeBuildingGoal extends Goal {
        private final Monster monster;
        private final int difficultyLevel;
        private int buildCooldown = 0;
        
        public BridgeBuildingGoal(Monster monster, int difficultyLevel) {
            this.monster = monster;
            this.difficultyLevel = difficultyLevel;
        }
        
        @Override
        public boolean canUse() {
            return monster.getTarget() != null && buildCooldown <= 0;
        }
        
        @Override
        public void tick() {
            LivingEntity target = monster.getTarget();
            if (target == null) return;
            
            // 前方の足元を確認
            BlockPos frontPos = monster.blockPosition().relative(monster.getDirection());
            BlockPos belowFront = frontPos.below();
            
            // 谷や水を渡る必要がある場合、橋を作る
            BlockState belowState = monster.level.getBlockState(belowFront);
            if (belowState.isAir() || belowState.is(Blocks.WATER) || belowState.is(Blocks.LAVA)) {
                
                // 橋用のブロックを設置
                monster.level.setBlock(belowFront, Blocks.COBBLESTONE.defaultBlockState(), 3);
                
                // 一時ブロックとして記録
                temporaryBlocks.put(belowFront, System.currentTimeMillis());
                
                // 難易度が高いほど建設速度が速い
                buildCooldown = Math.max(2, 8 - difficultyLevel);
            }
            
            if (buildCooldown > 0) {
                buildCooldown--;
            }
        }
    }
    
    // ゾンビの飛びつき攻撃AI
    public static class LeapAttackGoal extends Goal {
        private final Monster monster;
        private final int difficultyLevel;
        private int leapCooldown = 0;
        
        public LeapAttackGoal(Monster monster, int difficultyLevel) {
            this.monster = monster;
            this.difficultyLevel = difficultyLevel;
        }
        
        @Override
        public boolean canUse() {
            return monster.getTarget() != null && 
                   monster.isOnGround() && 
                   leapCooldown <= 0;
        }
        
        @Override
        public void tick() {
            LivingEntity target = monster.getTarget();
            if (target == null) return;
            
            double distance = monster.distanceTo(target);
            
            // 距離が適切なら飛びつく（2-8ブロック）
            if (distance >= 2 && distance <= 8) {
                // ターゲットに向かって飛びつく
                Vec3 direction = target.position().subtract(monster.position()).normalize();
                
                // 難易度が高いほど飛距離が長い
                double horizontalPower = 0.8 + difficultyLevel * 0.2;
                double verticalPower = 0.4 + difficultyLevel * 0.1;
                
                Vec3 leapMotion = new Vec3(
                    direction.x * horizontalPower,
                    verticalPower,
                    direction.z * horizontalPower
                );
                
                monster.setDeltaMovement(leapMotion);
                monster.hasImpulse = true;
                
                // 難易度が高いほどクールダウンが短い
                leapCooldown = Math.max(20, 80 - difficultyLevel * 12);
            }
            
            if (leapCooldown > 0) {
                leapCooldown--;
            }
        }
    }
    
    // ガーディアンに乗るAI
    public static class GuardianRidingGoal extends Goal {
        private final Monster monster;
        private final int difficultyLevel;
        private int searchCooldown = 0;
        
        public GuardianRidingGoal(Monster monster, int difficultyLevel) {
            this.monster = monster;
            this.difficultyLevel = difficultyLevel;
        }
        
        @Override
        public boolean canUse() {
            // 水から出たら降りる
            if (monster.isPassenger() && !monster.isInWater()) {
                monster.stopRiding();
                return false;
            }
            
            return monster.isInWater() && 
                   !monster.isPassenger() && 
                   searchCooldown <= 0;
        }
        
        @Override
        public void tick() {
            // 水から出たら降りる
            if (monster.isPassenger() && !monster.isInWater()) {
                monster.stopRiding();
                return;
            }
            
            // 水中にいる場合、近くのガーディアンまたはイカを探す
            if (monster.isInWater() && !monster.isPassenger()) {
                List<LivingEntity> nearbyWaterMobs = monster.level.getEntitiesOfClass(
                    LivingEntity.class,
                    monster.getBoundingBox().inflate(10),
                    entity -> (entity instanceof Guardian || entity instanceof Squid) && 
                             !entity.isVehicle() &&
                             entity != monster
                );
                
                if (!nearbyWaterMobs.isEmpty()) {
                    // 最も近い水中生物を選択
                    LivingEntity closestMob = nearbyWaterMobs.stream()
                        .min((a, b) -> Double.compare(
                            monster.distanceToSqr(a),
                            monster.distanceToSqr(b)
                        ))
                        .orElse(null);
                    
                    if (closestMob != null && monster.distanceToSqr(closestMob) < 25) {
                        // 乗る
                        monster.startRiding(closestMob);
                        
                        // 難易度が高いほど再検索までの時間が短い
                        searchCooldown = Math.max(40, 120 - difficultyLevel * 16);
                    }
                } else {
                    // ガーディアンがいない場合、召喚を試みる（高難易度のみ）
                    if (difficultyLevel >= 4 && monster.getRandom().nextFloat() < 0.1f) {
                        try {
                            Guardian guardian = new Guardian(net.minecraft.world.entity.EntityType.GUARDIAN, monster.level);
                            guardian.setPos(monster.getX(), monster.getY() - 1, monster.getZ());
                            monster.level.addFreshEntity(guardian);
                            monster.startRiding(guardian);
                        } catch (Exception e) {
                            // ガーディアン召喚に失敗した場合は無視
                        }
                    }
                }
            }
            
            if (searchCooldown > 0) {
                searchCooldown--;
            }
        }
    }
    
    // 一時ブロックの削除処理
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // 20ティックごとに処理（1秒に1回）
        if (event.getServer().getTickCount() % 20 == 0) {
            long currentTime = System.currentTimeMillis();
            Iterator<Map.Entry<BlockPos, Long>> iterator = temporaryBlocks.entrySet().iterator();
            
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, Long> entry = iterator.next();
                
                // 時間経過でブロックを削除
                if (currentTime - entry.getValue() > BLOCK_DECAY_TIME) {
                    // すべてのワールドで削除
                    event.getServer().getAllLevels().forEach(level -> {
                        if (level.getBlockState(entry.getKey()).is(Blocks.COBBLESTONE)) {
                            level.setBlock(entry.getKey(), Blocks.AIR.defaultBlockState(), 3);
                        }
                    });
                    iterator.remove();
                }
            }
        }
    }
}