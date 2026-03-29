package minecraftarmorweapon.trait;

import minecraftarmorweapon.command.CustomDifficultyCommand;
import minecraftarmorweapon.command.CustomDifficultyCommand.CustomDifficulty;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mob特性ハンドラー
 *
 * 特性一覧:
 *   鉄壁     — 防御+20, ノックバック耐性+0.8, 最大HP×1.5
 *   狂戦士   — 攻撃力×2.5, HP30%以下で更に攻撃力+50%&移動速度UP
 *   迅速     — 移動速度×2.0, 追跡範囲+16
 *   再生者   — 永続リジェネII, 最大HP×1.3
 *   蜘蛛糸   — ターゲットの足元に蜘蛛の巣を設置（一定間隔）
 *   呪縛者   — 攻撃ヒッ���時にランダムなデバフ（衰弱・盲目・毒・鈍化・空腹・暗闇）
 *   爆裂     — 攻撃ヒット時に小爆発, 死亡時に大爆発
 *   不死     — 不死のトーテム所持。使わ��るたびに再補充
 */
@Mod.EventBusSubscriber
public class MobTraitHandler {

    private static final Map<UUID, MobTrait> traitMap = new ConcurrentHashMap<>();
    private static final Set<UUID> traitApplied = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> undyingCount = new ConcurrentHashMap<>();
    private static final Set<UUID> berserkerRageActive = ConcurrentHashMap.newKeySet();

    // 蜘蛛糸の巣設置クールダウン（UUID → 残りtick）
    private static final Map<UUID, Integer> webCooldown = new ConcurrentHashMap<>();
    // 蜘蛛糸で設置した一時ブロック（自動消滅用）
    private static final Map<BlockPos, Long> temporaryWebs = new ConcurrentHashMap<>();
    private static final long WEB_DECAY_TIME = 8000; // 8秒で消滅

    // ============================
    // 1. スポーン時に特性を付与
    // ============================
    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getEntity() == null || !(event.getEntity() instanceof Monster monster)) {
            return;
        }
        if (monster.level().isClientSide) {
            return;
        }

        CustomDifficulty diff = CustomDifficultyCommand.getCurrentDifficulty();
        UUID id = monster.getUUID();

        if (traitApplied.contains(id)) {
            return;
        }

        float traitChance = diff.getTraitChance();
        if (traitChance <= 0 || monster.getRandom().nextFloat() >= traitChance) {
            return;
        }

        MobTrait trait = MobTrait.rollTrait(monster.getRandom().nextFloat(), diff.getAiLevel());
        traitMap.put(id, trait);
        traitApplied.add(id);

        applyTraitStats(monster, trait, diff);

        // NBTに特性を保存（ワールド再読み込み時の復元用）
        monster.getPersistentData().putString("TraitName", trait.name());

        String baseName = monster.getType().getDescription().getString();
        monster.setCustomName(Component.literal(
            trait.getFormattedName() + " §f" + baseName
        ));
        monster.setCustomNameVisible(true);
    }

    // ============================
    // 1.5. summonコマンドのNBT / ワールド再読み込み時に特性を復元
    // ============================
    // 使い方: /summon minecraft:zombie ~ ~ ~ {ForgeData:{TraitName:"explosive"}}
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Monster monster)) return;

        UUID id = monster.getUUID();
        // 既にメモリ上で特性が適用済みならスキップ
        if (traitApplied.contains(id)) return;

        CompoundTag data = monster.getPersistentData();
        if (!data.contains("TraitName")) return;

        String traitName = data.getString("TraitName");
        MobTrait foundTrait = null;
        for (MobTrait t : MobTrait.values()) {
            if (t.name().equalsIgnoreCase(traitName) || t.getDisplayName().equals(traitName)) {
                foundTrait = t;
                break;
            }
        }

        if (foundTrait == null) return;

        // 特性を適用
        CustomDifficulty diff = CustomDifficultyCommand.getCurrentDifficulty();
        traitMap.put(id, foundTrait);
        traitApplied.add(id);

        applyTraitStats(monster, foundTrait, diff);

        // 名前が未設定の場合のみ設定（既に名前付きなら上書きしない）
        if (monster.getCustomName() == null) {
            String baseName = monster.getType().getDescription().getString();
            monster.setCustomName(Component.literal(
                foundTrait.getFormattedName() + " §f" + baseName
            ));
            monster.setCustomNameVisible(true);
        }
    }

    // ============================
    // 特性ごとのステータス変更
    // ============================
    /**
     * 特性ごとのステータス変更を適用 — aiLevelで段階的に強化
     *
     * aiLevel 1: 基礎的な特性効果
     * aiLevel 2: やや強化
     * aiLevel 3: 中程度
     * aiLevel 4: 強力
     * aiLevel 5: 最大効果
     */
    private static void applyTraitStats(Monster monster, MobTrait trait, CustomDifficulty diff) {
        int lv = Math.max(1, diff.getAiLevel()); // 最低1

        switch (trait) {
            case IRON_WALL -> {
                // 鉄壁: 防御 10+lv×3, 防具強度 2+lv×2, KB耐性 0.4+lv×0.1, HP×(1.2+lv×0.1)
                double armor = 10.0 + lv * 3.0;     // 13→25
                double toughness = 2.0 + lv * 2.0;  // 4→12
                double kbRes = Math.min(1.0, 0.4 + lv * 0.1); // 0.5→0.9
                double hpMul = 1.2 + lv * 0.1;      // 1.3→1.7
                int resLv = Math.min(lv / 2, 2);     // 0→2

                if (monster.getAttribute(Attributes.ARMOR) != null)
                    monster.getAttribute(Attributes.ARMOR).setBaseValue(
                        monster.getAttribute(Attributes.ARMOR).getBaseValue() + armor);
                if (monster.getAttribute(Attributes.ARMOR_TOUGHNESS) != null)
                    monster.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(
                        monster.getAttribute(Attributes.ARMOR_TOUGHNESS).getBaseValue() + toughness);
                if (monster.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null)
                    monster.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(kbRes);
                if (monster.getAttribute(Attributes.MAX_HEALTH) != null) {
                    double hp = monster.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * hpMul;
                    monster.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                    monster.setHealth((float) hp);
                }
                monster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 999999, resLv, false, true));
            }

            case BERSERKER -> {
                // 狂戦士: 攻撃×(1.5+lv×0.3), HP×(0.9-lv×0.02)
                double atkMul = 1.5 + lv * 0.3;     // 1.8→3.0
                double hpMul = Math.max(0.6, 0.9 - lv * 0.02); // 0.88→0.80
                int boostLv = Math.min(lv / 2, 2);   // 0→2

                if (monster.getAttribute(Attributes.ATTACK_DAMAGE) != null)
                    monster.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(
                        monster.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() * atkMul);
                if (monster.getAttribute(Attributes.MAX_HEALTH) != null) {
                    double hp = monster.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * hpMul;
                    monster.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                    monster.setHealth((float) hp);
                }
                monster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 999999, boostLv, false, true));
            }

            case SWIFT -> {
                // 迅速: 速度×(1.3+lv×0.15), 追跡+8+lv×3
                double spdMul = 1.3 + lv * 0.15;    // 1.45→2.05
                double rangePlus = 8.0 + lv * 3.0;  // 11→23
                int speedLv = Math.min(lv / 2, 2);   // 0→2

                if (monster.getAttribute(Attributes.MOVEMENT_SPEED) != null)
                    monster.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
                        monster.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() * spdMul);
                if (monster.getAttribute(Attributes.FOLLOW_RANGE) != null)
                    monster.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(
                        monster.getAttribute(Attributes.FOLLOW_RANGE).getBaseValue() + rangePlus);
                monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 999999, speedLv, false, true));
            }

            case REGENERATOR -> {
                // 再生者: HP×(1.1+lv×0.1), リジェネLv = lv/2 (0→2)
                double hpMul = 1.1 + lv * 0.1;      // 1.2→1.6
                int regenLv = Math.min(lv / 2, 3);   // 0→2 (lv5で最大Regen III)

                if (monster.getAttribute(Attributes.MAX_HEALTH) != null) {
                    double hp = monster.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * hpMul;
                    monster.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                    monster.setHealth((float) hp);
                }
                monster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 999999, regenLv, false, true));
                // lv4以上で追加で吸収も付く
                if (lv >= 4) {
                    monster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 999999, lv - 3, false, true));
                }
            }

            case WEB_SPINNER -> {
                // 蜘蛛糸: 速度×(1.1+lv×0.05), HP×(1.0+lv×0.05)
                double spdMul = 1.1 + lv * 0.05;    // 1.15→1.35
                double hpMul = 1.0 + lv * 0.05;     // 1.05→1.25

                if (monster.getAttribute(Attributes.MOVEMENT_SPEED) != null)
                    monster.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
                        monster.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() * spdMul);
                if (monster.getAttribute(Attributes.MAX_HEALTH) != null) {
                    double hp = monster.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * hpMul;
                    monster.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                    monster.setHealth((float) hp);
                }
                webCooldown.put(monster.getUUID(), 0);
            }

            case REFLECTOR -> {
                // 反射: HP×(1.1+lv×0.08), 防御+5+lv×2, トゲのような見た目
                double hpMul = 1.1 + lv * 0.08;     // 1.18→1.5
                double armorPlus = 5.0 + lv * 2.0;  // 7→15

                if (monster.getAttribute(Attributes.MAX_HEALTH) != null) {
                    double hp = monster.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * hpMul;
                    monster.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                    monster.setHealth((float) hp);
                }
                if (monster.getAttribute(Attributes.ARMOR) != null)
                    monster.getAttribute(Attributes.ARMOR).setBaseValue(
                        monster.getAttribute(Attributes.ARMOR).getBaseValue() + armorPlus);
                // トゲエフェクト（見た目用）
                monster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 999999, 0, false, true));
            }

            case ARROW_GUARD -> {
                // 矢盾: HP×(1.3+lv×0.1), 速度やや低下
                double hpMul = 1.3 + lv * 0.1;      // 1.4→1.8
                double spdMul = Math.max(0.8, 0.95 - lv * 0.02); // 0.93→0.85

                if (monster.getAttribute(Attributes.MAX_HEALTH) != null) {
                    double hp = monster.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * hpMul;
                    monster.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                    monster.setHealth((float) hp);
                }
                if (monster.getAttribute(Attributes.MOVEMENT_SPEED) != null)
                    monster.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
                        monster.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() * spdMul);
            }

            case POISONER -> {
                // 猛毒: HP×(1.3+lv×0.1), 自身は毒耐性（見た目で緑パーティクル）
                double hpMul = 1.3 + lv * 0.1;      // 1.4→1.8
                if (monster.getAttribute(Attributes.MAX_HEALTH) != null) {
                    double hp = monster.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * hpMul;
                    monster.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                    monster.setHealth((float) hp);
                }
                // 自身に毒エフェクト（ダメージなし、見た目用）
                monster.addEffect(new MobEffectInstance(MobEffects.POISON, 999999, 0, false, true));
            }

            case BLINDER -> {
                // 盲目: HP×(1.1+lv×0.08), 速度×(1.2+lv×0.1) — 暗闘型
                double hpMul = 1.1 + lv * 0.08;     // 1.18→1.5
                double spdMul = 1.2 + lv * 0.1;     // 1.3→1.7
                if (monster.getAttribute(Attributes.MAX_HEALTH) != null) {
                    double hp = monster.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * hpMul;
                    monster.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                    monster.setHealth((float) hp);
                }
                if (monster.getAttribute(Attributes.MOVEMENT_SPEED) != null)
                    monster.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
                        monster.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() * spdMul);
                monster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 999999, 0, false, false));
            }

            case NAUSEATOR -> {
                // 吐き気: HP×(1.2+lv×0.08), 速度×(1.1+lv×0.05) — 嫌がらせ型
                double hpMul = 1.2 + lv * 0.08;     // 1.28→1.6
                double spdMul = 1.1 + lv * 0.05;    // 1.15→1.35
                if (monster.getAttribute(Attributes.MAX_HEALTH) != null) {
                    double hp = monster.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * hpMul;
                    monster.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                    monster.setHealth((float) hp);
                }
                if (monster.getAttribute(Attributes.MOVEMENT_SPEED) != null)
                    monster.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
                        monster.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() * spdMul);
                monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, 999999, 0, false, true));
            }

            case WEAKENER -> {
                // 弱体: HP×(1.4+lv×0.12), 防御+5+lv — 鈍重だが硬い弱体化型
                double hpMul = 1.4 + lv * 0.12;     // 1.52→2.0
                double armorPlus = 5.0 + lv;        // 6→10
                if (monster.getAttribute(Attributes.MAX_HEALTH) != null) {
                    double hp = monster.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * hpMul;
                    monster.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                    monster.setHealth((float) hp);
                }
                if (monster.getAttribute(Attributes.ARMOR) != null)
                    monster.getAttribute(Attributes.ARMOR).setBaseValue(
                        monster.getAttribute(Attributes.ARMOR).getBaseValue() + armorPlus);
                monster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 999999, 0, false, true));
            }

            case EXPLOSIVE -> {
                // 爆裂: 攻撃×(1.2+lv×0.1), lv4+で炎上付与
                double atkMul = 1.2 + lv * 0.1;     // 1.3→1.7

                if (monster.getAttribute(Attributes.ATTACK_DAMAGE) != null)
                    monster.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(
                        monster.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() * atkMul);
                monster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 999999, 0, false, true));
            }

            case UNDYING -> {
                // 不死: HP×(1.5+lv×0.2), 攻撃×(1.1+lv×0.05), 吸収Lv = lv/2
                double hpMul = 1.5 + lv * 0.2;      // 1.7→2.5
                double atkMul = 1.1 + lv * 0.05;    // 1.15→1.35
                int absorbLv = Math.min(lv / 2, 3);  // 0→2

                if (monster.getAttribute(Attributes.MAX_HEALTH) != null) {
                    double hp = monster.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * hpMul;
                    monster.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                    monster.setHealth((float) hp);
                }
                if (monster.getAttribute(Attributes.ATTACK_DAMAGE) != null)
                    monster.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(
                        monster.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() * atkMul);
                monster.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
                monster.setDropChance(EquipmentSlot.OFFHAND, 0.0f);
                undyingCount.put(monster.getUUID(), 0);
                monster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 999999, absorbLv, false, true));
                monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, 999999, 0, false, true));
            }
        }
    }

    // ============================
    // 2. 反射・矢盾の被ダメージ処理
    // ============================
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTraitDefense(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }
        if (monster.level().isClientSide) {
            return;
        }

        UUID id = monster.getUUID();
        MobTrait trait = traitMap.get(id);
        if (trait == null) {
            return;
        }

        int lv = Math.max(1, CustomDifficultyCommand.getCurrentDifficulty().getAiLevel());

        // --- 反射: 近接ダメージの一部を攻撃者に反射 ---
        if (trait == MobTrait.REFLECTOR && event.getSource().getEntity() instanceof LivingEntity attacker) {
            // 飛び道具でなく近接攻撃の場合のみ反射
            if (event.getSource().getDirectEntity() == event.getSource().getEntity()) {
                // 反射率: 20% + lv×8% (最大60%)
                float reflectRate = Math.min(0.6f, 0.20f + lv * 0.08f);
                float reflectDmg = event.getAmount() * reflectRate;

                // 反射ダメージを攻撃者に与える（再帰防止のためマジックダメージ）
                attacker.hurt(monster.damageSources().thorns(monster), reflectDmg);

                // 受けるダメージを軽減 (10% + lv×3%)
                float reduction = Math.min(0.3f, 0.10f + lv * 0.03f);
                event.setAmount(event.getAmount() * (1.0f - reduction));
            }
        }

        // --- 矢盾: 飛び道具ダメージを大幅カット〜完全無効 ---
        if (trait == MobTrait.ARROW_GUARD) {
            // 飛び道具判定: directEntityとentityが異なる = 飛び道具
            boolean isProjectile = event.getSource().getDirectEntity() != event.getSource().getEntity()
                || event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE);

            if (isProjectile) {
                // カット率: 50% + lv×10% (lv5で100%=完全無効)
                float cutRate = Math.min(1.0f, 0.50f + lv * 0.10f);
                event.setAmount(event.getAmount() * (1.0f - cutRate));

                // lv5で完全無効化
                if (cutRate >= 1.0f) {
                    event.setCanceled(true);
                }
            }
        }
    }

    // ============================
    // 3. 狂戦士のレイジモード（HP30%以下で強化）
    // ============================
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBerserkerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }
        if (monster.level().isClientSide) {
            return;
        }

        UUID id = monster.getUUID();
        MobTrait trait = traitMap.get(id);

        if (trait != MobTrait.BERSERKER) {
            return;
        }

        float hpPercent = monster.getHealth() / monster.getMaxHealth();
        if (hpPercent <= 0.3f && !berserkerRageActive.contains(id)) {
            berserkerRageActive.add(id);

            if (monster.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                double atk = monster.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() * 1.5;
                monster.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(atk);
            }
            monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 999999, 2, false, true));

            String baseName = monster.getType().getDescription().getString();
            monster.setCustomName(Component.literal(
                "§4§l[狂戦士・怒] §f" + baseName
            ));
        }
    }

    // ============================
    // 3. 攻撃ヒット時の処理（爆裂 + 呪縛者）
    // ============================
    @SubscribeEvent
    public static void onTraitAttack(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Monster attacker)) {
            return;
        }
        if (attacker.level().isClientSide) {
            return;
        }

        MobTrait trait = traitMap.get(attacker.getUUID());
        if (trait == null) {
            return;
        }

        LivingEntity target = event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null;

        // --- 爆裂: 攻撃ヒット時に小爆発 ---
        if (trait == MobTrait.EXPLOSIVE) {
            if (attacker.level() instanceof ServerLevel serverLevel) {
                serverLevel.explode(
                    attacker,
                    event.getEntity().getX(),
                    event.getEntity().getY(),
                    event.getEntity().getZ(),
                    2.0f,
                    Level.ExplosionInteraction.MOB
                );
            }
        }

        // --- デバフ特性: 各特性が専門デバフを付与 ---
        if (target != null) {
            int aiLevel = CustomDifficultyCommand.getCurrentDifficulty().getAiLevel();
            int baseDuration = 60 + aiLevel * 20; // 3秒 + aiLevel×1秒
            int amplifier = Math.max(0, aiLevel / 2 - 1);

            if (trait == MobTrait.POISONER) {
                // 猛毒: 毒 + 高レベルで衰弱も追加
                target.addEffect(new MobEffectInstance(MobEffects.POISON, baseDuration + 60, amplifier + 1));
                if (aiLevel >= 3) {
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, baseDuration, amplifier));
                }
                if (aiLevel >= 5) {
                    target.addEffect(new MobEffectInstance(MobEffects.HUNGER, baseDuration, amplifier));
                }
            }

            if (trait == MobTrait.BLINDER) {
                // 盲目: 盲目 + 暗闇のコンボ、高レベルで長時間化
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, baseDuration + aiLevel * 10, 0));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, baseDuration + aiLevel * 10, 0));
                if (aiLevel >= 4) {
                    // 高レベルで鈍化も追加（暗闘+移動困難）
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, baseDuration, amplifier));
                }
            }

            if (trait == MobTrait.NAUSEATOR) {
                // 吐き気: 混乱 + 空腹 + 高レベルで採掘疲労
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, baseDuration + 40, 0));
                target.addEffect(new MobEffectInstance(MobEffects.HUNGER, baseDuration * 2, amplifier + 1));
                if (aiLevel >= 3) {
                    target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, baseDuration, amplifier));
                }
                if (aiLevel >= 5) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, baseDuration / 2, 0));
                }
            }

            if (trait == MobTrait.WEAKENER) {
                // 弱体: 弱体化 + 鈍化 + 高レベルで採掘疲労
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, baseDuration + 30, amplifier + 1));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, baseDuration + 20, amplifier));
                if (aiLevel >= 3) {
                    target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, baseDuration, amplifier));
                }
                if (aiLevel >= 4) {
                    // ノックバック耐性低下の代わりに更に鈍化を強化
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, baseDuration, amplifier + 2));
                }
            }
        }
    }

    // ============================
    // 4. 蜘蛛糸: 毎tick処理 — ターゲットの足元に蜘蛛の巣を設置
    // ============================
    @SubscribeEvent
    public static void onWebSpinnerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }
        if (monster.level().isClientSide) {
            return;
        }

        UUID id = monster.getUUID();
        MobTrait trait = traitMap.get(id);
        if (trait != MobTrait.WEB_SPINNER) {
            return;
        }

        // クールダウン管理
        int cooldown = webCooldown.getOrDefault(id, 0);
        if (cooldown > 0) {
            webCooldown.put(id, cooldown - 1);
            return;
        }

        LivingEntity target = monster.getTarget();
        if (target == null) {
            return;
        }

        double distSq = monster.distanceToSqr(target);
        // 8ブロック以内のターゲットの足元に巣を設置
        if (distSq < 64.0) {
            int aiLevel = CustomDifficultyCommand.getCurrentDifficulty().getAiLevel();
            BlockPos targetFeet = target.blockPosition();

            // 足元に蜘蛛の巣を設置
            if (monster.level().getBlockState(targetFeet).isAir()) {
                monster.level().setBlock(targetFeet, Blocks.COBWEB.defaultBlockState(), 3);
                temporaryWebs.put(targetFeet, System.currentTimeMillis());

                // aiLevelが高いほど周囲にも設置（aiLevel 3以上で十字、5で3×3）
                if (aiLevel >= 3) {
                    for (BlockPos offset : new BlockPos[]{
                        targetFeet.north(), targetFeet.south(),
                        targetFeet.east(), targetFeet.west()
                    }) {
                        if (monster.getRandom().nextFloat() < 0.3f
                            && monster.level().getBlockState(offset).isAir()) {
                            monster.level().setBlock(offset, Blocks.COBWEB.defaultBlockState(), 3);
                            temporaryWebs.put(offset, System.currentTimeMillis());
                        }
                    }
                }
                if (aiLevel >= 5) {
                    for (BlockPos offset : new BlockPos[]{
                        targetFeet.north().east(), targetFeet.north().west(),
                        targetFeet.south().east(), targetFeet.south().west()
                    }) {
                        if (monster.getRandom().nextFloat() < 0.2f
                            && monster.level().getBlockState(offset).isAir()) {
                            monster.level().setBlock(offset, Blocks.COBWEB.defaultBlockState(), 3);
                            temporaryWebs.put(offset, System.currentTimeMillis());
                        }
                    }
                }
            }

            // クールダウン: aiLevelが高いほど短い（3秒〜1.5秒）
            int cd = Math.max(30, 60 - aiLevel * 6);
            webCooldown.put(id, cd);
        }
    }

    // ============================
    // 5. 蜘蛛の巣の自動消滅
    // ============================
    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }
        // 20tickごと（1秒）にチェック
        if (event.getServer().getTickCount() % 20 != 0) {
            return;
        }
        long now = System.currentTimeMillis();
        var iterator = temporaryWebs.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now - entry.getValue() > WEB_DECAY_TIME) {
                BlockPos pos = entry.getKey();
                event.getServer().getAllLevels().forEach(level -> {
                    if (level.getBlockState(pos).is(Blocks.COBWEB)) {
                        level.destroyBlock(pos, false);
                    }
                });
                iterator.remove();
            }
        }
    }

    // ============================
    // 6. 不死のトーテム再補充 & 爆裂の死亡時爆発
    // ============================
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMobDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) {
            return;
        }
        if (monster.level().isClientSide) {
            return;
        }

        UUID id = monster.getUUID();
        MobTrait trait = traitMap.get(id);
        if (trait == null) {
            return;
        }

        // --- 不死の特性: トーテム発動処理 ---
        if (trait == MobTrait.UNDYING) {
            int count = undyingCount.getOrDefault(id, 0);

            event.setCanceled(true);

            float healAmount = Math.max(4.0f, 20.0f - count * 3.0f);
            monster.setHealth(healAmount);

            monster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            monster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            monster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));

            count++;
            undyingCount.put(id, count);

            monster.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            monster.setDropChance(EquipmentSlot.OFFHAND, 0.0f);

            String baseName = monster.getType().getDescription().getString();
            monster.setCustomName(Component.literal(
                "§4§l[不死 ×" + count + "] §f" + baseName
            ));

            if (monster.level() instanceof ServerLevel serverLevel) {
                serverLevel.broadcastEntityEvent(monster, (byte) 35);
            }
        }

        // --- 爆裂の特性: 死亡時大爆発 ---
        if (trait == MobTrait.EXPLOSIVE) {
            if (monster.level() instanceof ServerLevel serverLevel) {
                serverLevel.explode(
                    null,
                    monster.getX(), monster.getY(), monster.getZ(),
                    4.0f,
                    Level.ExplosionInteraction.MOB
                );
            }
        }

        if (trait != MobTrait.UNDYING) {
            cleanup(id);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMobDeathCleanup(LivingDeathEvent event) {
        if (event.getEntity() instanceof Monster) {
            if (!event.isCanceled()) {
                cleanup(event.getEntity().getUUID());
            }
        }
    }

    private static void cleanup(UUID id) {
        traitMap.remove(id);
        traitApplied.remove(id);
        undyingCount.remove(id);
        berserkerRageActive.remove(id);
        webCooldown.remove(id);
    }

    // ============================
    // ユーティリティ
    // ============================
    public static MobTrait getTraitFor(UUID entityId) {
        return traitMap.get(entityId);
    }

    public static int getUndyingCount(UUID entityId) {
        return undyingCount.getOrDefault(entityId, 0);
    }

    /**
     * 外部から特性を強制適用（コマンド用）
     */
    public static void applyTraitToMob(Monster monster, MobTrait trait) {
        UUID id = monster.getUUID();
        // 既存の特性をクリーンアップ
        cleanup(id);

        CustomDifficulty diff = CustomDifficultyCommand.getCurrentDifficulty();
        traitMap.put(id, trait);
        traitApplied.add(id);

        applyTraitStats(monster, trait, diff);

        // NBTに特性を保存
        monster.getPersistentData().putString("TraitName", trait.name());

        String baseName = monster.getType().getDescription().getString();
        monster.setCustomName(Component.literal(
            trait.getFormattedName() + " §f" + baseName
        ));
        monster.setCustomNameVisible(true);
    }
}
