package minecraftarmorweapon.ai;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import java.util.Iterator;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import minecraftarmorweapon.command.CustomDifficultyCommand;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 安全なTrue Crafter Mode実装
 * AIゴールの変更を安全に行い、ConcurrentModificationExceptionを防ぐ
 */
@Mod.EventBusSubscriber
public class SafeTrueCrafterAI {
    
    // エンティティごとの強化状態を管理
    private static final Set<UUID> enhancedEntities = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, MobEnhancementData> enhancementData = new ConcurrentHashMap<>();
    
    // 一時ブロックの管理
    private static final Map<BlockPos, Long> temporaryBlocks = new ConcurrentHashMap<>();
    private static final long BLOCK_DECAY_TIME = 15000; // 15秒
    
    private static class MobEnhancementData {
        boolean isEnhanced = false;
        int weaponSwitchCooldown = 0;
        int dodgeCooldown = 0;
        int blockPlaceCooldown = 0;
        ItemStack meleeWeapon = ItemStack.EMPTY;
        ItemStack rangedWeapon = ItemStack.EMPTY;
        long lastUpdateTick = 0;
    }
    
    // 革防具に色を付けるヘルパーメソッド
    private static ItemStack createColoredLeatherArmor(ItemStack armorItem, int color) {
        if (armorItem.getItem() instanceof DyeableLeatherItem) {
            ((DyeableLeatherItem) armorItem.getItem()).setColor(armorItem, color);
        }
        return armorItem;
    }
    
    // ランダムな防具を取得（MOD装備含む）
    private static ItemStack getRandomArmor(RandomSource random, EquipmentSlot slot, int tier) {
        List<Item> availableArmors = new ArrayList<>();
        
        // バニラ防具を追加
        if (slot == EquipmentSlot.HEAD) {
            availableArmors.addAll(Arrays.asList(
                Items.LEATHER_HELMET, Items.GOLDEN_HELMET, 
                Items.CHAINMAIL_HELMET, Items.IRON_HELMET,
                Items.DIAMOND_HELMET, Items.NETHERITE_HELMET,
                Items.TURTLE_HELMET
            ));
        } else if (slot == EquipmentSlot.CHEST) {
            availableArmors.addAll(Arrays.asList(
                Items.LEATHER_CHESTPLATE, Items.GOLDEN_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE,
                Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE
            ));
        } else if (slot == EquipmentSlot.LEGS) {
            availableArmors.addAll(Arrays.asList(
                Items.LEATHER_LEGGINGS, Items.GOLDEN_LEGGINGS,
                Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS,
                Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS
            ));
        } else if (slot == EquipmentSlot.FEET) {
            availableArmors.addAll(Arrays.asList(
                Items.LEATHER_BOOTS, Items.GOLDEN_BOOTS,
                Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS,
                Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS
            ));
        }
        
        // MOD防具を検索して追加
        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof ArmorItem) {
                ArmorItem armor = (ArmorItem) item;
                if (armor.getSlot() == slot) {
                    // minecraft_armor_weapon MODの装備を優先的に追加
                    var registryName = ForgeRegistries.ITEMS.getKey(item);
                    if (registryName != null && 
                        registryName.getNamespace().equals("minecraft_armor_weapon")) {
                        availableArmors.add(item);
                    }
                    // 他のMODの装備も低確率で追加
                    else if (registryName != null && 
                             !registryName.getNamespace().equals("minecraft") && 
                             random.nextFloat() < 0.3f) {
                        availableArmors.add(item);
                    }
                }
            }
        }
        
        // ティアに応じてフィルタリング
        List<Item> filteredArmors = new ArrayList<>();
        for (Item item : availableArmors) {
            if (item instanceof ArmorItem) {
                ArmorItem armor = (ArmorItem) item;
                // ティア0: 革、金、チェインメイル
                if (tier == 0) {
                    if (armor.getMaterial() == ArmorMaterials.LEATHER ||
                        armor.getMaterial() == ArmorMaterials.GOLD ||
                        armor.getMaterial() == ArmorMaterials.CHAIN ||
                        (random.nextFloat() < 0.1f)) { // 10%で他の素材も
                        filteredArmors.add(item);
                    }
                }
                // ティア1: チェインメイル、鉄、金
                else if (tier == 1) {
                    if (armor.getMaterial() == ArmorMaterials.CHAIN ||
                        armor.getMaterial() == ArmorMaterials.IRON ||
                        armor.getMaterial() == ArmorMaterials.GOLD ||
                        (random.nextFloat() < 0.2f)) { // 20%で他の素材も
                        filteredArmors.add(item);
                    }
                }
                // ティア2: 鉄、ダイヤ、ネザライト、MOD装備
                else {
                    if (armor.getMaterial() == ArmorMaterials.IRON ||
                        armor.getMaterial() == ArmorMaterials.DIAMOND ||
                        armor.getMaterial() == ArmorMaterials.NETHERITE) {
                        filteredArmors.add(item);
                    } else {
                        // MOD装備もティア2に追加
                        var registryName = ForgeRegistries.ITEMS.getKey(item);
                        if (registryName != null && !registryName.getNamespace().equals("minecraft")) {
                            filteredArmors.add(item);
                        }
                    }
                }
            }
        }
        
        // ランダムに選択
        if (!filteredArmors.isEmpty()) {
            Item selectedArmor = filteredArmors.get(random.nextInt(filteredArmors.size()));
            return new ItemStack(selectedArmor);
        }
        
        // デフォルト
        return ItemStack.EMPTY;
    }
    
    // ランダムな武器を取得（MOD武器含む）
    private static ItemStack getRandomWeapon(RandomSource random, int tier, boolean preferAxe) {
        List<Item> availableWeapons = new ArrayList<>();
        
        // バニラ武器を追加
        availableWeapons.addAll(Arrays.asList(
            Items.WOODEN_SWORD, Items.STONE_SWORD, Items.GOLDEN_SWORD,
            Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD,
            Items.WOODEN_AXE, Items.STONE_AXE, Items.GOLDEN_AXE,
            Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE,
            Items.TRIDENT
        ));
        
        // MOD武器を検索して追加
        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof SwordItem || item instanceof AxeItem) {
                // minecraft_armor_weapon MODの武器を優先的に追加
                var registryName = ForgeRegistries.ITEMS.getKey(item);
                if (registryName != null && 
                    registryName.getNamespace().equals("minecraft_armor_weapon")) {
                    availableWeapons.add(item);
                }
                // 他のMODの武器も低確率で追加
                else if (registryName != null && 
                         !registryName.getNamespace().equals("minecraft") && 
                         random.nextFloat() < 0.2f) {
                    availableWeapons.add(item);
                }
            }
        }
        
        // ティアに応じてフィルタリング
        List<Item> filteredWeapons = new ArrayList<>();
        for (Item item : availableWeapons) {
            boolean shouldAdd = false;
            
            // ティア0: 木、石、金
            if (tier == 0) {
                if (item == Items.WOODEN_SWORD || item == Items.WOODEN_AXE ||
                    item == Items.STONE_SWORD || item == Items.STONE_AXE ||
                    item == Items.GOLDEN_SWORD || item == Items.GOLDEN_AXE) {
                    shouldAdd = true;
                }
            }
            // ティア1: 石、鉄、金
            else if (tier == 1) {
                if (item == Items.STONE_SWORD || item == Items.STONE_AXE ||
                    item == Items.IRON_SWORD || item == Items.IRON_AXE ||
                    item == Items.GOLDEN_SWORD || item == Items.GOLDEN_AXE) {
                    shouldAdd = true;
                }
            }
            // ティア2: 鉄、ダイヤ、ネザライト、MOD武器
            else {
                if (item == Items.IRON_SWORD || item == Items.IRON_AXE ||
                    item == Items.DIAMOND_SWORD || item == Items.DIAMOND_AXE ||
                    item == Items.NETHERITE_SWORD || item == Items.NETHERITE_AXE ||
                    item == Items.TRIDENT) {
                    shouldAdd = true;
                } else {
                    // MOD武器もティア2に追加
                    var registryName = ForgeRegistries.ITEMS.getKey(item);
                    if (registryName != null && !registryName.getNamespace().equals("minecraft")) {
                        shouldAdd = true;
                    }
                }
            }
            
            // 斧優先の場合
            if (shouldAdd) {
                if (preferAxe && item instanceof AxeItem) {
                    filteredWeapons.add(item);
                } else if (!preferAxe && item instanceof SwordItem) {
                    filteredWeapons.add(item);
                } else if (random.nextFloat() < 0.3f) {
                    filteredWeapons.add(item);
                }
            }
        }
        
        // ランダムに選択
        if (!filteredWeapons.isEmpty()) {
            Item selectedWeapon = filteredWeapons.get(random.nextInt(filteredWeapons.size()));
            return new ItemStack(selectedWeapon);
        }
        
        // デフォルト
        return new ItemStack(tier == 0 ? Items.WOODEN_SWORD : (tier == 1 ? Items.IRON_SWORD : Items.DIAMOND_SWORD));
    }
    
    // ランダムな色を生成（ティアに応じた色の傾向）
    private static int getRandomArmorColor(RandomSource random, int tier) {
        if (tier == 0) {
            // 通常: 茶色・グレー系
            int[] colors = {
                0x8B4513, // サドルブラウン
                0x696969, // ディムグレー
                0x654321, // ダークブラウン
                0x808080, // グレー
                0x5C4033, // ダークチョコレート
                0x704214  // セピア
            };
            return colors[random.nextInt(colors.length)];
        } else if (tier == 1) {
            // 精鋭: 赤・青系
            int[] colors = {
                0x8B0000, // ダークレッド
                0x4B0082, // インディゴ
                0x191970, // ミッドナイトブルー
                0x800020, // バーガンディ
                0x483D8B, // ダークスレートブルー
                0x2F4F4F  // ダークスレートグレー
            };
            return colors[random.nextInt(colors.length)];
        } else {
            // チャンピオン: 黒・紫・金系
            int[] colors = {
                0x1C1C1C, // 濃い黒
                0x4B0082, // インディゴ
                0x6A0DAD, // パープル
                0x301934, // ダークパープル
                0xB8860B, // ダークゴールデンロッド
                0x8B008B  // ダークマゼンタ
            };
            return colors[random.nextInt(colors.length)];
        }
    }
    
    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!CustomDifficultyCommand.isTrueCrafterEnabled()) {
            return;
        }
        
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }
        
        if (VersionHelper.getLevel(monster) == null || monster.level.isClientSide) {
            return;
        }
        
        UUID entityId = monster.getUUID();
        
        // すでに強化済みならスキップ
        if (enhancedEntities.contains(entityId)) {
            return;
        }
        
        // サーバーの次のティックで処理（エンティティが完全に初期化された後）
        if (VersionHelper.getLevel(monster) instanceof ServerLevel serverLevel) {
            serverLevel.getServer().execute(() -> {
                try {
                    enhanceMonster(monster);
                    enhancedEntities.add(entityId);
                } catch (Exception e) {
                    // エラーをログに記録するが、クラッシュは防ぐ
                    System.err.println("Failed to enhance monster: " + e.getMessage());
                }
            });
        }
    }
    
    private static void enhanceMonster(Monster monster) {
        if (monster instanceof Skeleton skeleton) {
            enhanceSkeleton(skeleton);
        } else if (monster instanceof Zombie zombie) {
            enhanceZombie(zombie);
        } else if (monster instanceof Spider spider) {
            enhanceSpider(spider);
        } else if (monster instanceof Creeper creeper) {
            enhanceCreeper(creeper);
        } else if (monster instanceof Witch witch) {
            enhanceWitch(witch);
        }
    }
    
    private static void enhanceSkeleton(Skeleton skeleton) {
        RandomSource random = skeleton.getRandom();
        
        // ティアシステム（0: 通常, 1: 精鋭, 2: チャンピオン）
        int tier = random.nextFloat() < 0.7f ? 0 : (random.nextFloat() < 0.8f ? 1 : 2);
        
        // 弓の設定（ティアに応じて強化）
        ItemStack bow = new ItemStack(Items.BOW);
        if (tier == 0) {
            // 通常スケルトン
            bow.enchant(Enchantments.POWER_ARROWS, 1);
            if (random.nextBoolean()) {
                bow.enchant(Enchantments.PUNCH_ARROWS, 1);
            }
        } else if (tier == 1) {
            // 精鋭スケルトン
            bow.enchant(Enchantments.POWER_ARROWS, 2 + random.nextInt(2));
            bow.enchant(Enchantments.PUNCH_ARROWS, 1);
            if (random.nextFloat() < 0.3f) {
                bow.enchant(Enchantments.FLAMING_ARROWS, 1);
            }
        } else {
            // チャンピオンスケルトン
            bow.enchant(Enchantments.POWER_ARROWS, 3 + random.nextInt(2));
            bow.enchant(Enchantments.PUNCH_ARROWS, 2);
            bow.enchant(Enchantments.FLAMING_ARROWS, 1);
            if (random.nextFloat() < 0.5f) {
                bow.enchant(Enchantments.INFINITY_ARROWS, 1);
            }
        }
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, bow);
        
        // 防具の設定（ランダム）
        int armorColor = getRandomArmorColor(random, tier);
        if (tier == 0) {
            // 通常: 軽装
            if (random.nextFloat() < 0.6f) {
                if (random.nextBoolean()) {
                    ItemStack helmet = createColoredLeatherArmor(new ItemStack(Items.LEATHER_HELMET), armorColor);
                    skeleton.setItemSlot(EquipmentSlot.HEAD, helmet);
                } else {
                    skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
                }
            }
            if (random.nextFloat() < 0.3f) {
                ItemStack chestplate = createColoredLeatherArmor(new ItemStack(Items.LEATHER_CHESTPLATE), armorColor);
                skeleton.setItemSlot(EquipmentSlot.CHEST, chestplate);
            }
        } else if (tier == 1) {
            // 精鋭: 中装
            skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
            if (random.nextFloat() < 0.7f) {
                skeleton.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            }
            if (random.nextFloat() < 0.5f) {
                skeleton.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
            }
        } else {
            // チャンピオン: 重装
            ItemStack helmet = new ItemStack(Items.IRON_HELMET);
            helmet.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 1 + random.nextInt(3));
            skeleton.setItemSlot(EquipmentSlot.HEAD, helmet);
            
            ItemStack chestplate = new ItemStack(Items.IRON_CHESTPLATE);
            if (random.nextFloat() < 0.5f) {
                chestplate.enchant(Enchantments.PROJECTILE_PROTECTION, 2);
            }
            skeleton.setItemSlot(EquipmentSlot.CHEST, chestplate);
            
            if (random.nextFloat() < 0.7f) {
                skeleton.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            }
            if (random.nextFloat() < 0.5f) {
                skeleton.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            }
        }
        
        // 盾の設定（ティアが高いほど確率が上がる）
        float shieldChance = tier == 0 ? 0.2f : (tier == 1 ? 0.5f : 0.8f);
        if (random.nextFloat() < shieldChance) {
            skeleton.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        }
        
        // 近接武器を準備（ティアに応じて）
        ItemStack sword;
        if (tier == 0) {
            sword = new ItemStack(random.nextBoolean() ? Items.STONE_SWORD : Items.IRON_SWORD);
        } else if (tier == 1) {
            sword = new ItemStack(Items.IRON_SWORD);
            sword.enchant(Enchantments.SHARPNESS, 1 + random.nextInt(2));
        } else {
            sword = new ItemStack(random.nextFloat() < 0.3f ? Items.DIAMOND_SWORD : Items.IRON_SWORD);
            sword.enchant(Enchantments.SHARPNESS, 2 + random.nextInt(2));
            if (random.nextFloat() < 0.3f) {
                sword.enchant(Enchantments.FIRE_ASPECT, 1);
            }
        }
        
        MobEnhancementData data = enhancementData.computeIfAbsent(skeleton.getUUID(), k -> new MobEnhancementData());
        data.meleeWeapon = sword;
        data.rangedWeapon = bow;
        data.isEnhanced = true;
        
        // ドロップ率を0に
        skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        skeleton.setDropChance(EquipmentSlot.HEAD, 0.0f);
        skeleton.setDropChance(EquipmentSlot.CHEST, 0.0f);
        skeleton.setDropChance(EquipmentSlot.LEGS, 0.0f);
        skeleton.setDropChance(EquipmentSlot.FEET, 0.0f);
        skeleton.setDropChance(EquipmentSlot.OFFHAND, 0.0f);
        
        // ステータス強化（ティアに応じて）
        if (skeleton.getAttribute(Attributes.MAX_HEALTH) != null) {
            double health = tier == 0 ? 20.0 + random.nextInt(10) : (tier == 1 ? 30.0 + random.nextInt(10) : 40.0 + random.nextInt(20));
            skeleton.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
            skeleton.setHealth((float)health);
        }
        if (skeleton.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            double speed = 0.25 + (tier * 0.03) + (random.nextFloat() * 0.05);
            skeleton.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        }
        if (skeleton.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            skeleton.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16.0 + tier * 4.0);
        }
        
        // AIゴールの追加は避ける（ConcurrentModificationExceptionを防ぐため）
        // 代わりにデータを保存して、LivingTickEventで処理する
    }
    
    private static void enhanceZombie(Zombie zombie) {
        RandomSource random = zombie.getRandom();
        
        // ティアシステム（0: 通常, 1: 戦士, 2: バーサーカー）
        int tier = random.nextFloat() < 0.6f ? 0 : (random.nextFloat() < 0.75f ? 1 : 2);
        
        // 防具の設定（ティアとランダム性）
        int armorColor = getRandomArmorColor(random, tier);
        if (tier == 0) {
            // 通常ゾンビ: 部分的な装備
            if (random.nextFloat() < 0.7f) {
                if (random.nextBoolean()) {
                    ItemStack helmet = createColoredLeatherArmor(new ItemStack(Items.LEATHER_HELMET), armorColor);
                    zombie.setItemSlot(EquipmentSlot.HEAD, helmet);
                } else {
                    zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
                }
            }
            if (random.nextFloat() < 0.5f) {
                ItemStack chestplate = createColoredLeatherArmor(new ItemStack(Items.LEATHER_CHESTPLATE), armorColor);
                zombie.setItemSlot(EquipmentSlot.CHEST, chestplate);
            }
            if (random.nextFloat() < 0.3f) {
                ItemStack leggings = createColoredLeatherArmor(new ItemStack(Items.LEATHER_LEGGINGS), armorColor);
                zombie.setItemSlot(EquipmentSlot.LEGS, leggings);
            }
            if (random.nextFloat() < 0.4f) {
                ItemStack boots = createColoredLeatherArmor(new ItemStack(Items.LEATHER_BOOTS), armorColor);
                zombie.setItemSlot(EquipmentSlot.FEET, boots);
            }
        } else if (tier == 1) {
            // 戦士ゾンビ: チェインメイル主体（一部染色革）
            zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(
                random.nextFloat() < 0.3f ? Items.IRON_HELMET : Items.CHAINMAIL_HELMET
            ));
            // 胸部は革とチェインメイルの混合
            if (random.nextFloat() < 0.8f) {
                if (random.nextFloat() < 0.2f) {
                    // 20%の確率で染色革
                    ItemStack chestplate = createColoredLeatherArmor(new ItemStack(Items.LEATHER_CHESTPLATE), armorColor);
                    chestplate.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 1);
                    zombie.setItemSlot(EquipmentSlot.CHEST, chestplate);
                } else {
                    zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
                }
            }
            if (random.nextFloat() < 0.7f) {
                zombie.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
            }
            if (random.nextFloat() < 0.6f) {
                if (random.nextFloat() < 0.15f) {
                    // 15%の確率で染色革ブーツ
                    ItemStack boots = createColoredLeatherArmor(new ItemStack(Items.LEATHER_BOOTS), armorColor);
                    boots.enchant(Enchantments.FALL_PROTECTION, 2);
                    zombie.setItemSlot(EquipmentSlot.FEET, boots);
                } else {
                    zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(
                        random.nextBoolean() ? Items.CHAINMAIL_BOOTS : Items.IRON_BOOTS
                    ));
                }
            }
        } else {
            // バーサーカーゾンビ: フル鉄装備
            ItemStack helmet = new ItemStack(random.nextFloat() < 0.2f ? Items.DIAMOND_HELMET : Items.IRON_HELMET);
            if (random.nextFloat() < 0.5f) {
                helmet.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 1 + random.nextInt(2));
            }
            zombie.setItemSlot(EquipmentSlot.HEAD, helmet);
            
            ItemStack chestplate = new ItemStack(Items.IRON_CHESTPLATE);
            if (random.nextFloat() < 0.4f) {
                chestplate.enchant(Enchantments.THORNS, 1 + random.nextInt(2));
            }
            zombie.setItemSlot(EquipmentSlot.CHEST, chestplate);
            
            zombie.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        }
        
        // 武器の設定
        ItemStack weapon;
        if (tier == 0) {
            // 通常: 木〜鉄の武器
            if (random.nextFloat() < 0.3f) {
                weapon = ItemStack.EMPTY; // 素手
            } else if (random.nextFloat() < 0.5f) {
                weapon = new ItemStack(Items.WOODEN_SWORD);
            } else {
                weapon = new ItemStack(random.nextBoolean() ? Items.STONE_SWORD : Items.IRON_SWORD);
            }
        } else if (tier == 1) {
            // 戦士: 鉄武器中心
            if (random.nextFloat() < 0.3f) {
                // 斧使い
                weapon = new ItemStack(Items.IRON_AXE);
                weapon.enchant(Enchantments.SHARPNESS, 1);
            } else {
                weapon = new ItemStack(Items.IRON_SWORD);
                weapon.enchant(Enchantments.SHARPNESS, 1 + random.nextInt(2));
            }
        } else {
            // バーサーカー: 強力な武器
            if (random.nextFloat() < 0.2f) {
                // ダイヤ武器
                weapon = new ItemStack(random.nextBoolean() ? Items.DIAMOND_SWORD : Items.DIAMOND_AXE);
                weapon.enchant(Enchantments.SHARPNESS, 2 + random.nextInt(2));
            } else {
                // エンチャント付き鉄武器
                weapon = new ItemStack(random.nextFloat() < 0.4f ? Items.IRON_AXE : Items.IRON_SWORD);
                weapon.enchant(Enchantments.SHARPNESS, 2 + random.nextInt(2));
                if (random.nextFloat() < 0.3f) {
                    weapon.enchant(Enchantments.KNOCKBACK, 1);
                }
            }
        }
        
        if (!weapon.isEmpty()) {
            zombie.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        }
        
        // 盾の設定（ティアが高いほど確率上昇）
        float shieldChance = tier == 0 ? 0.1f : (tier == 1 ? 0.4f : 0.7f);
        if (random.nextFloat() < shieldChance && !weapon.isEmpty()) {
            zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        } else if (tier == 2 && random.nextFloat() < 0.2f) {
            // バーサーカーの一部は両手武器
            zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_SWORD));
        }
        
        // ドロップ率を0に
        zombie.setDropChance(EquipmentSlot.HEAD, 0.0f);
        zombie.setDropChance(EquipmentSlot.CHEST, 0.0f);
        zombie.setDropChance(EquipmentSlot.LEGS, 0.0f);
        zombie.setDropChance(EquipmentSlot.FEET, 0.0f);
        zombie.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        zombie.setDropChance(EquipmentSlot.OFFHAND, 0.0f);
        
        // ステータス強化（ティアに応じて）
        if (zombie.getAttribute(Attributes.MAX_HEALTH) != null) {
            double health = tier == 0 ? 25.0 + random.nextInt(10) : (tier == 1 ? 35.0 + random.nextInt(15) : 50.0 + random.nextInt(20));
            zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
            zombie.setHealth((float)health);
        }
        if (zombie.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            double speed = tier == 0 ? 0.23 : (tier == 1 ? 0.25 : 0.28);
            speed += random.nextFloat() * 0.03;
            zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        }
        if (zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
            zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.2 + tier * 0.2);
        }
        if (zombie.getAttribute(Attributes.ARMOR) != null) {
            zombie.getAttribute(Attributes.ARMOR).setBaseValue(tier * 2.0);
        }
        if (zombie.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            zombie.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3.0 + tier * 2.0);
        }
        
        // ドア破壊能力（ティアが高いほど確率上昇）
        zombie.setCanBreakDoors(tier > 0 || random.nextFloat() < 0.3f);
        
        // AIゴールの追加は避ける（ConcurrentModificationExceptionを防ぐため）
    }
    
    private static void enhanceSpider(Spider spider) {
        // ステータス強化
        if (spider.getAttribute(Attributes.MAX_HEALTH) != null) {
            spider.getAttribute(Attributes.MAX_HEALTH).setBaseValue(24.0);
            spider.setHealth(24.0f);
        }
        if (spider.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            spider.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35);
        }
        if (spider.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            spider.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5.0);
        }
        
        // AIゴールの追加は避ける（ConcurrentModificationExceptionを防ぐため）
    }
    
    private static void enhanceCreeper(Creeper creeper) {
        // ステータス強化
        if (creeper.getAttribute(Attributes.MAX_HEALTH) != null) {
            creeper.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30.0);
            creeper.setHealth(30.0f);
        }
        if (creeper.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            creeper.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
        }
        if (creeper.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            creeper.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(20.0);
        }
    }
    
    private static void enhanceWitch(Witch witch) {
        // ステータス強化
        if (witch.getAttribute(Attributes.MAX_HEALTH) != null) {
            witch.getAttribute(Attributes.MAX_HEALTH).setBaseValue(35.0);
            witch.setHealth(35.0f);
        }
        if (witch.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            witch.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
        }
    }
    
    // 毎ティック更新処理
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (!CustomDifficultyCommand.isTrueCrafterEnabled()) {
            return;
        }
        
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }
        
        if (VersionHelper.getLevel(monster) == null || monster.level.isClientSide) {
            return;
        }
        
        UUID entityId = monster.getUUID();
        if (!enhancedEntities.contains(entityId)) {
            return;
        }
        
        MobEnhancementData data = enhancementData.get(entityId);
        if (data == null) {
            return;
        }
        
        // クールダウンを減らす
        if (data.weaponSwitchCooldown > 0) {
            data.weaponSwitchCooldown--;
        }
        if (data.dodgeCooldown > 0) {
            data.dodgeCooldown--;
        }
        if (data.blockPlaceCooldown > 0) {
            data.blockPlaceCooldown--;
        }
        
        // スケルトンの武器切り替え処理（AIゴールを追加せずに直接処理）
        if (monster instanceof Skeleton skeleton) {
            // 武器データが初期化されていない場合は初期化
            if (data.meleeWeapon.isEmpty() || data.rangedWeapon.isEmpty()) {
                // デフォルト武器を設定
                if (data.rangedWeapon.isEmpty()) {
                    data.rangedWeapon = skeleton.getMainHandItem().copy();
                    if (data.rangedWeapon.isEmpty()) {
                        data.rangedWeapon = new ItemStack(Items.BOW);
                    }
                }
                if (data.meleeWeapon.isEmpty()) {
                    data.meleeWeapon = new ItemStack(Items.IRON_SWORD);
                }
            }
            
            // 武器切り替え処理
            if (data.weaponSwitchCooldown == 0) {
                LivingEntity target = skeleton.getTarget();
                if (target != null) {
                    double distance = skeleton.distanceToSqr(target);
                    ItemStack currentWeapon = skeleton.getMainHandItem();
                    
                    // 近距離（4ブロック以内）なら剣に切り替え
                    if (distance < 16.0) {
                        if (!currentWeapon.is(data.meleeWeapon.getItem())) {
                            skeleton.setItemSlot(EquipmentSlot.MAINHAND, data.meleeWeapon.copy());
                            data.weaponSwitchCooldown = 40; // 2秒のクールダウン
                            skeleton.setAggressive(true);
                        }
                    }
                    // 遠距離（4ブロック以上）なら弓に切り替え
                    else if (distance >= 25.0) { // 5ブロック以上
                        if (!currentWeapon.is(data.rangedWeapon.getItem())) {
                            skeleton.setItemSlot(EquipmentSlot.MAINHAND, data.rangedWeapon.copy());
                            data.weaponSwitchCooldown = 40;
                            skeleton.setAggressive(false);
                        }
                    }
                }
            }
            
            // 盾防御処理（AIゴールを使わずに直接処理）
            if (!skeleton.getOffhandItem().isEmpty() && skeleton.getOffhandItem().is(Items.SHIELD)) {
                LivingEntity target = skeleton.getTarget();
                if (target != null && skeleton.distanceToSqr(target) < 16.0) {
                    // 近距離で20%の確率で盾を構える
                    if (skeleton.getRandom().nextFloat() < 0.2f && !skeleton.isUsingItem()) {
                        skeleton.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
                    }
                }
            }
        }
        
        // ゾンビのブロック設置と飛びかかり処理
        if (monster instanceof Zombie zombie) {
            LivingEntity target = zombie.getTarget();
            if (target != null) {
                double distance = zombie.distanceToSqr(target);
                
                // ブロック設置処理（lunatic以上の難易度で有効）
                if (data.blockPlaceCooldown == 0 && CustomDifficultyCommand.isTrueCrafterEnabled()) {
                    // ターゲットが高い位置にいる場合
                    if (target.getY() > zombie.getY() + 1.5) {
                        BlockPos zombiePos = zombie.blockPosition();
                        BlockPos frontPos = zombiePos.relative(zombie.getDirection());
                        
                        // 前方にブロックを設置して登る
                        if (zombie.level.getBlockState(frontPos).isAir() && 
                            !zombie.level.getBlockState(frontPos.below()).isAir()) {
                            zombie.level.setBlock(frontPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                            data.blockPlaceCooldown = 40; // 2秒のクールダウン
                            
                            // 一時ブロックとして記録（15秒後に削除）
                            temporaryBlocks.put(frontPos, System.currentTimeMillis());
                        }
                        // 足元にブロックを設置して階段を作る
                        else if (zombie.level.getBlockState(zombiePos.below()).isAir()) {
                            zombie.level.setBlock(zombiePos.below(), Blocks.COBBLESTONE.defaultBlockState(), 3);
                            data.blockPlaceCooldown = 30;
                            temporaryBlocks.put(zombiePos.below(), System.currentTimeMillis());
                        }
                    }
                    
                    // 谷や水を渡るための橋を作る
                    BlockPos frontPos = zombie.blockPosition().relative(zombie.getDirection());
                    BlockPos belowFront = frontPos.below();
                    BlockState belowState = zombie.level.getBlockState(belowFront);
                    
                    if (belowState.isAir() || belowState.getMaterial().isLiquid()) {
                        zombie.level.setBlock(belowFront, Blocks.COBBLESTONE.defaultBlockState(), 3);
                        data.blockPlaceCooldown = 20;
                        temporaryBlocks.put(belowFront, System.currentTimeMillis());
                    }
                }
                
                // 飛びかかり攻撃
                if (data.dodgeCooldown == 0 && zombie.isOnGround()) {
                    if (distance > 9.0 && distance < 36.0) { // 3-6ブロック
                        Vec3 direction = target.position().subtract(zombie.position()).normalize();
                        zombie.setDeltaMovement(direction.x * 0.8, 0.4, direction.z * 0.8);
                        zombie.hasImpulse = true;
                        data.dodgeCooldown = 60; // 3秒のクールダウン
                    }
                }
                
                // 盾防御処理
                if (!zombie.getOffhandItem().isEmpty() && zombie.getOffhandItem().is(Items.SHIELD)) {
                    if (distance < 16.0) {
                        // 近距離で30%の確率で盾を構える
                        if (zombie.getRandom().nextFloat() < 0.3f && !zombie.isUsingItem()) {
                            zombie.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
                        }
                    }
                }
            }
        }
        
        // クモのクモの巣設置処理
        if (monster instanceof Spider spider) {
            LivingEntity target = spider.getTarget();
            if (target != null && data.blockPlaceCooldown == 0) {
                double distance = spider.distanceToSqr(target);
                if (distance < 16.0 && spider.getRandom().nextFloat() < 0.1f) {
                    BlockPos targetPos = target.blockPosition();
                    if (spider.level.getBlockState(targetPos).isAir()) {
                        spider.level.setBlock(targetPos, Blocks.COBWEB.defaultBlockState(), 3);
                        data.blockPlaceCooldown = 100; // 5秒のクールダウン
                        temporaryBlocks.put(targetPos, System.currentTimeMillis());
                    }
                }
            }
        }
        
        // 回避行動（全モンスター共通）
        if (data.dodgeCooldown == 0 && monster.getTarget() != null) {
            // 矢を検知して回避
            List<AbstractArrow> arrows = monster.level.getEntitiesOfClass(
                AbstractArrow.class, 
                monster.getBoundingBox().inflate(3.0),
                arrow -> arrow.getOwner() != monster && !arrow.isNoGravity()
            );
            
            if (!arrows.isEmpty()) {
                // 横に回避
                Vec3 dodgeVec = new Vec3(
                    monster.getRandom().nextGaussian() * 0.5,
                    0.2,
                    monster.getRandom().nextGaussian() * 0.5
                );
                monster.setDeltaMovement(monster.getDeltaMovement().add(dodgeVec));
                data.dodgeCooldown = 20; // 1秒のクールダウン
            }
        }
    }
    
    // カスタムAIゴール - スケルトンの盾防御（攻撃を妨げないように改良）
    private static class SkeletonShieldGoal extends Goal {
        private final Skeleton skeleton;
        private int blockingTime = 0;
        private int blockCooldown = 0;
        private boolean wasHurt = false;
        
        public SkeletonShieldGoal(Skeleton skeleton) {
            this.skeleton = skeleton;
            // 攻撃AIと競合しないようにフラグを設定しない
            this.setFlags(EnumSet.noneOf(Goal.Flag.class));
        }
        
        @Override
        public boolean canUse() {
            // 盾を持っていて、ターゲットがいて、弓を使用中でない場合のみ
            return !this.skeleton.getOffhandItem().isEmpty() 
                && this.skeleton.getOffhandItem().getItem() == Items.SHIELD 
                && this.skeleton.getTarget() != null
                && !this.skeleton.isUsingItem(); // 弓を使用中は盾を使わない
        }
        
        @Override
        public void start() {
            blockingTime = 0;
            wasHurt = false;
        }
        
        @Override
        public void tick() {
            // 弓を使用中なら盾は使わない
            if (this.skeleton.getMainHandItem().getItem() == Items.BOW && this.skeleton.isUsingItem()) {
                return;
            }
            
            if (blockCooldown > 0) {
                blockCooldown--;
                return;
            }
            
            LivingEntity target = this.skeleton.getTarget();
            if (target == null) {
                return;
            }
            
            // ダメージを受けたかチェック
            if (this.skeleton.getLastHurtByMob() != null && this.skeleton.getLastHurtByMobTimestamp() + 20 > this.skeleton.level.getGameTime()) {
                wasHurt = true;
            }
            
            double distance = this.skeleton.distanceToSqr(target);
            
            // 非常に近距離（3ブロック以内）またはダメージを受けた後のみ防御
            if ((distance < 9.0 || wasHurt) && distance < 25.0) {
                // ターゲットがこちらを向いている場合
                Vec3 targetLook = target.getLookAngle();
                Vec3 toSkeleton = this.skeleton.position().subtract(target.position()).normalize();
                double dot = targetLook.dot(toSkeleton);
                
                // 非常に近いか、ダメージを受けた後で相手がこちらを向いている
                if (distance < 4.0 || (wasHurt && dot > 0.3)) {
                    // 盾を構える（オフハンドのみ）
                    this.skeleton.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
                    blockingTime++;
                    
                    // 0.5秒（10tick）防御したら一旦解除
                    if (blockingTime > 10) {
                        this.skeleton.stopUsingItem();
                        blockCooldown = 60; // 3秒のクールダウン
                        blockingTime = 0;
                        wasHurt = false;
                    }
                } else {
                    // 条件を満たさない場合は盾を下げる
                    this.skeleton.stopUsingItem();
                    blockingTime = 0;
                }
            } else {
                // 遠距離では盾を使わない
                blockingTime = 0;
            }
        }
        
        @Override
        public void stop() {
            this.skeleton.stopUsingItem();
            blockingTime = 0;
            wasHurt = false;
        }
        
        @Override
        public boolean canContinueToUse() {
            // 弓を使用中なら継続しない
            if (this.skeleton.getMainHandItem().getItem() == Items.BOW && this.skeleton.isUsingItem()) {
                return false;
            }
            return this.skeleton.getTarget() != null;
        }
    }
    
    // カスタムAIゴール - ゾンビ用
    private static class EnhancedZombieGoal extends Goal {
        private final Zombie zombie;
        private int leapCooldown = 0;
        
        public EnhancedZombieGoal(Zombie zombie) {
            this.zombie = zombie;
            this.setFlags(EnumSet.of(Goal.Flag.JUMP));
        }
        
        @Override
        public boolean canUse() {
            return this.zombie.getTarget() != null;
        }
        
        @Override
        public void tick() {
            if (leapCooldown > 0) {
                leapCooldown--;
                return;
            }
            
            LivingEntity target = this.zombie.getTarget();
            if (target == null) {
                return;
            }
            
            double distance = this.zombie.distanceToSqr(target);
            
            // リープアタック（距離が離れている時）
            if (distance > 9.0 && distance < 36.0 && this.zombie.isOnGround()) {
                Vec3 leapVec = target.position().subtract(this.zombie.position()).normalize();
                this.zombie.setDeltaMovement(
                    leapVec.x * 0.8, 
                    0.4, 
                    leapVec.z * 0.8
                );
                leapCooldown = 60; // 3秒のクールダウン
            }
        }
    }
    
    // カスタムAIゴール - ゾンビの盾防御
    private static class ZombieShieldGoal extends Goal {
        private final Zombie zombie;
        private int blockingTime = 0;
        private int blockCooldown = 0;
        
        public ZombieShieldGoal(Zombie zombie) {
            this.zombie = zombie;
            this.setFlags(EnumSet.noneOf(Goal.Flag.class));
        }
        
        @Override
        public boolean canUse() {
            return !this.zombie.getOffhandItem().isEmpty() 
                && this.zombie.getOffhandItem().getItem() == Items.SHIELD 
                && this.zombie.getTarget() != null;
        }
        
        @Override
        public void tick() {
            if (blockCooldown > 0) {
                blockCooldown--;
                this.zombie.stopUsingItem();
                return;
            }
            
            LivingEntity target = this.zombie.getTarget();
            if (target == null) {
                this.zombie.stopUsingItem();
                return;
            }
            
            double distance = this.zombie.distanceToSqr(target);
            
            // 近距離で防御
            if (distance < 16.0) {
                Vec3 targetLook = target.getLookAngle();
                Vec3 toZombie = this.zombie.position().subtract(target.position()).normalize();
                double dot = targetLook.dot(toZombie);
                
                if (dot > 0.3 || distance < 4.0) {
                    this.zombie.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
                    blockingTime++;
                    
                    if (blockingTime > 30) {
                        this.zombie.stopUsingItem();
                        blockCooldown = 30;
                        blockingTime = 0;
                    }
                } else {
                    this.zombie.stopUsingItem();
                    blockingTime = 0;
                }
            } else {
                this.zombie.stopUsingItem();
                blockingTime = 0;
            }
        }
        
        @Override
        public void stop() {
            this.zombie.stopUsingItem();
            blockingTime = 0;
        }
    }
    
    // カスタムAIゴール - クモ用
    private static class EnhancedSpiderGoal extends Goal {
        private final Spider spider;
        private int webCooldown = 0;
        
        public EnhancedSpiderGoal(Spider spider) {
            this.spider = spider;
        }
        
        @Override
        public boolean canUse() {
            return this.spider.getTarget() != null;
        }
        
        @Override
        public void tick() {
            if (webCooldown > 0) {
                webCooldown--;
                return;
            }
            
            LivingEntity target = this.spider.getTarget();
            if (target == null) {
                return;
            }
            
            double distance = this.spider.distanceToSqr(target);
            
            // ウェブ設置（プレイヤーの足元）
            if (distance < 64.0 && this.spider.getRandom().nextInt(100) == 0) {
                BlockPos targetPos = target.blockPosition();
                if (this.spider.level.getBlockState(targetPos).isAir()) {
                    this.spider.level.setBlock(targetPos, Blocks.COBWEB.defaultBlockState(), 3);
                    webCooldown = 100; // 5秒のクールダウン
                }
            }
        }
    }
    
    // エンティティが死亡した時のクリーンアップ
    @SubscribeEvent
    public static void onEntityDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof Monster) {
            UUID entityId = event.getEntity().getUUID();
            enhancedEntities.remove(entityId);
            enhancementData.remove(entityId);
        }
    }
    
    // 一時ブロックの削除処理
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // 20ティックごとに処理（1秒ごと）
        if (event.getServer().getTickCount() % 20 == 0) {
            long currentTime = System.currentTimeMillis();
            Iterator<Map.Entry<BlockPos, Long>> iterator = temporaryBlocks.entrySet().iterator();
            
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, Long> entry = iterator.next();
                BlockPos pos = entry.getKey();
                Long placeTime = entry.getValue();
                
                // 時間経過でブロックを削除
                if (currentTime - placeTime > BLOCK_DECAY_TIME) {
                    // すべてのワールドで削除を試みる
                    event.getServer().getAllLevels().forEach(level -> {
                        BlockState state = level.getBlockState(pos);
                        if (state.is(Blocks.COBBLESTONE) || state.is(Blocks.COBWEB)) {
                            // パーティクルを出しながら削除
                            level.destroyBlock(pos, false); // falseでアイテムドロップなし
                        }
                    });
                    iterator.remove();
                }
            }
        }
        
        // メモリクリーンアップ（5分ごと）
        if (event.getServer().getTickCount() % 6000 == 0) {
            // 古いエンティティデータを削除
            if (enhancedEntities.size() > 100) {
                enhancedEntities.clear();
            }
            if (enhancementData.size() > 100) {
                enhancementData.clear();
            }
        }
    }
}