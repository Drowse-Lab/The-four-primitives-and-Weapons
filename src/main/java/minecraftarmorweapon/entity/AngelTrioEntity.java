package minecraftarmorweapon.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 天使の三人組エンティティ（共通基底クラス）。
 *
 * 3つのEntityTypeで登録される:
 *   angel_serious  — 真面目（女）
 *   angel_mocker1  — 馬鹿にする（女）
 *   angel_mocker2  — 馬鹿にする（男Slim）
 *
 * チャットは右クリック時にのみ発生。
 * 自動で話しかけてこない。
 */
public class AngelTrioEntity extends PathfinderMob {

    public static final int PERSONALITY_SERIOUS = 0;
    public static final int PERSONALITY_MOCKER_1 = 1;
    public static final int PERSONALITY_MOCKER_2 = 2;

    private static final String NBT_PERSONALITY = "AngelPersonality";
    private static final String NBT_ANGRY = "AngelAngry";
    private static final String NBT_ASK_COUNT = "AngelAskCount";

    private int personality = 0;
    private boolean angry = false;
    private int askCount = 0;

    // 右クリッククールダウン（連打防止）
    private int interactCooldown = 0;

    // 抜刀状態: false=納刀 (メイン=鞘), true=抜刀 (メイン=刀)
    private boolean drawn = false;
    // 戦闘状態のヒステリシスtick: ターゲット消失後もこの間だけ抜刀維持
    private int sheathCooldown = 0;

    // チャットメッセージ
    private static final String[][] MOCKER_MESSAGES = {
        // MOCKER_1（ノリのいい女）
        {
            "§7[§f???§7] §fえ？おもろｗなにそれｗ",
            "§7[§f???§7] §fねーねー、さっき死んでたでしょ？ウケるんだけどｗｗ",
            "§7[§f???§7] §fまじ？そのレベルでうろうろしてんの？やばｗ",
            "§7[§f???§7] §fいやそれはないってｗ",
            "§7[§f???§7] §fあはは、弱すぎておもろいんだけどｗ",
            "§7[§f???§7] §fね、その武器ウケるんだけどｗｗ",
            "§7[§f???§7] §f私たちに勝てると思ってんの？ｗｗまじ？ｗ",
        },
        // MOCKER_2（チャラい男）
        {
            "§7[§f???§7] §fうぇーいｗ元気？",
            "§7[§f???§7] §fてかさー、その装備やばくね？ｗ",
            "§7[§f???§7] §fあー、まぁまぁじゃん？いいじゃんいいじゃん",
            "§7[§f???§7] §fえ、まじ？ウケるんだけどｗ",
            "§7[§f???§7] §fてかこの辺かわいい子いねーかなー",
            "§7[§f???§7] §fいいじゃんいいじゃん、がんばれってｗ",
            "§7[§f???§7] §fやっばｗおもしろいやつじゃんｗ",
        }
    };

    private static final String[] SERIOUS_IDLE_MESSAGES = {
        "§7[§a???§7] §f…こんにちは",
        "§7[§a???§7] §f…気をつけてね",
        "§7[§a???§7] §fこの辺りは危険だよ",
        "§7[§a???§7] §f…無理しないでね",
        "§7[§a???§7] §f何か用かな？",
    };

    private static final String[] SERIOUS_ASK_MESSAGES = {
        "§7[§a???§7] §fその本…渡してくれない？",
        "§7[§a???§7] §fお願い、その本は危険なの。渡して",
        "§7[§a???§7] §c最後に言うよ。その本を渡して",
    };

    public AngelTrioEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
        this.setPersistenceRequired();
        this.xpReward = 100;
        // 初期状態は納刀: メイン=鞘, オフ=刀
        this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MinecraftArmorWeaponModItems.SAYA.get()));
        this.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(MinecraftArmorWeaponModItems.NETHERITE_KATANA.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 200.0)
            .add(Attributes.MOVEMENT_SPEED, 0.45)
            .add(Attributes.ATTACK_DAMAGE, 15.0)
            .add(Attributes.ARMOR, 20.0)
            .add(Attributes.ARMOR_TOUGHNESS, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, false));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12.0f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (interactCooldown > 0) interactCooldown--;
        updateDrawState();
        // 自動チャットは一切しない
    }

    /** ターゲットの有無に応じて抜刀/納刀を切り替える (プレイヤー同様) */
    private void updateDrawState() {
        LivingEntity target = this.getTarget();
        boolean wantDrawn = target != null && target.isAlive();
        if (wantDrawn) {
            sheathCooldown = 60; // ターゲット消失後3秒は抜刀維持
        } else if (sheathCooldown > 0) {
            sheathCooldown--;
            wantDrawn = true;
        }

        if (wantDrawn && !drawn) {
            // 抜刀: メイン=刀, オフ=鞘
            ItemStack main = this.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack off = this.getItemInHand(InteractionHand.OFF_HAND);
            this.setItemInHand(InteractionHand.MAIN_HAND, off);
            this.setItemInHand(InteractionHand.OFF_HAND, main);
            drawn = true;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ARMOR_EQUIP_IRON, SoundSource.HOSTILE, 1.0f, 1.2f);
        } else if (!wantDrawn && drawn) {
            // 納刀: メイン=鞘, オフ=刀
            ItemStack main = this.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack off = this.getItemInHand(InteractionHand.OFF_HAND);
            this.setItemInHand(InteractionHand.MAIN_HAND, off);
            this.setItemInHand(InteractionHand.OFF_HAND, main);
            drawn = false;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ARMOR_EQUIP_IRON, SoundSource.HOSTILE, 0.8f, 0.9f);
        }
    }

    // =============================================
    // 右クリック → 会話確認メッセージ
    // =============================================
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) return InteractionResult.SUCCESS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (interactCooldown > 0) return InteractionResult.PASS;
        interactCooldown = 20; // 1秒クールダウン

        // === 真面目 + error本を手に持っている → 渡す ===
        if (personality == PERSONALITY_SERIOUS && !angry) {
            ItemStack held = player.getItemInHand(hand);
            if (isErrorBook(held)) {
                broadcastChat(player, "§7[§a???§7] §fありがとう…これで安全だよ");
                held.shrink(1);
                askCount = 0;
                return InteractionResult.SUCCESS;
            }
        }

        // === 会話するか確認メッセージを表示 ===
        if (player instanceof ServerPlayer sp) {
            showTalkConfirmation(sp);
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 「会話しますか？ [Yes] [No]」をチャットに表示する
     */
    private void showTalkConfirmation(ServerPlayer player) {
        String entityName = getDisplayNameText();

        MutableComponent message = Component.literal("§e" + entityName + " と会話しますか？ ");

        // [Yes] ボタン — クリックでコマンド実行
        MutableComponent yesButton = Component.literal("§a§l[Yes]")
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/angel_talk yes " + this.getUUID().toString()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("会話する"))));

        MutableComponent noButton = Component.literal("§c§l[No]")
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/angel_talk no " + this.getUUID().toString()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("会話しない"))));

        message.append(yesButton).append(Component.literal(" ")).append(noButton);

        player.sendSystemMessage(message);
    }

    /**
     * GUI開いた時の最初の一言を返す
     */
    public String getFirstMessage(Player player) {
        if (personality == PERSONALITY_SERIOUS) {
            if (!angry && playerHasErrorBook(player)) {
                if (askCount < SERIOUS_ASK_MESSAGES.length) {
                    // §7[§a???§7] §f のプレフィックスを除去して本文だけ返す
                    String raw = SERIOUS_ASK_MESSAGES[askCount];
                    askCount++;
                    return stripPrefix(raw);
                } else {
                    becomeAngry(player);
                    return "…ごめんね。でも、渡してくれないなら力づくで取るしかないの";
                }
            }
            String raw = SERIOUS_IDLE_MESSAGES[this.random.nextInt(SERIOUS_IDLE_MESSAGES.length)];
            return stripPrefix(raw);
        } else {
            int mockerIndex = personality == PERSONALITY_MOCKER_1 ? 0 : 1;
            String raw = MOCKER_MESSAGES[mockerIndex][this.random.nextInt(MOCKER_MESSAGES[mockerIndex].length)];
            return stripPrefix(raw);
        }
    }

    /** チャットメッセージから§色コードとプレフィックス "[???] " を除去 */
    private static String stripPrefix(String msg) {
        // §7[§a???§7] §f のようなプレフィックスを除去
        String stripped = msg.replaceAll("§.", "");
        int bracket = stripped.indexOf("] ");
        if (bracket >= 0) return stripped.substring(bracket + 2);
        return stripped;
    }

    private String getDisplayNameText() {
        return switch (personality) {
            case PERSONALITY_SERIOUS -> "???";
            case PERSONALITY_MOCKER_1 -> "???";
            case PERSONALITY_MOCKER_2 -> "???";
            default -> "???";
        };
    }

    /**
     * コマンドから呼ばれる: /angel_talk yes|no <uuid>
     */
    public static void handleTalkCommand(ServerPlayer player, String response, UUID entityUUID) {
        if ("yes".equals(response)) {
            if (player.level() instanceof ServerLevel serverLevel) {
                Entity entity = serverLevel.getEntity(entityUUID);
                if (entity instanceof AngelTrioEntity angel && angel.distanceTo(player) < 16) {
                    // 最初のノードを取得
                    minecraftarmorweapon.ai.lisp.DialogueManager.Node startNode =
                        minecraftarmorweapon.ai.lisp.DialogueManager.getStartNode(angel.personality);
                    if (startNode == null) {
                        player.sendSystemMessage(Component.literal("§c会話データがロードされていない"));
                        return;
                    }

                    java.util.List<String> choiceTexts = new java.util.ArrayList<>();
                    java.util.List<String> choiceNexts = new java.util.ArrayList<>();
                    for (minecraftarmorweapon.ai.lisp.DialogueManager.Choice c : startNode.choices) {
                        choiceTexts.add(c.text);
                        choiceNexts.add(c.next != null ? c.next : "start");
                    }

                    // GUI を開くパケットをクライアントに送信
                    minecraftarmorweapon.MinecraftArmorWeaponMod.PACKET_HANDLER.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new minecraftarmorweapon.network.AngelChatS2CPacket(
                            angel.getDisplayNameText(), angel.personality,
                            entityUUID.toString(), startNode.text,
                            choiceTexts, choiceNexts, startNode.isEnd));
                } else {
                    player.sendSystemMessage(Component.literal("§7遠すぎて会話できない…"));
                }
            }
        } else {
            player.sendSystemMessage(Component.literal("§7会話をやめた。"));
        }
    }

    // === error本検出 ===

    private boolean playerHasErrorBook(Player player) {
        if (isErrorBook(player.getMainHandItem())) return true;
        if (isErrorBook(player.getOffhandItem())) return true;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (isErrorBook(player.getInventory().getItem(i))) return true;
        }
        return false;
    }

    private boolean isErrorBook(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return false;
        String id = itemId.toString();
        return id.contains("error") || id.contains("tukaena");
    }

    // === 怒り ===

    private void becomeAngry(Player player) {
        // クリエイティブ/スペクテイターには怒らない
        if (player.isCreative() || player.isSpectator()) return;
        this.angry = true;
        broadcastChat(player, "§7[§c???§7] §c…ごめんね。でも、渡してくれないなら");
        broadcastChat(player, "§7[§c???§7] §c力づくで取るしかないの");
        this.setTarget(player);
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));

        // 仲間も怒らせる
        List<AngelTrioEntity> allies = this.level().getEntitiesOfClass(
            AngelTrioEntity.class, this.getBoundingBox().inflate(32.0),
            e -> e != this && e.isAlive()
        );
        for (AngelTrioEntity ally : allies) {
            ally.angry = true;
            ally.setTarget(player);
            ally.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(ally, Player.class, true));
        }
    }

    private void broadcastChat(Player player, String message) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal(message), false);
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return angry && super.canAttack(target);
    }

    @Override
    public Component getName() {
        return switch (personality) {
            case PERSONALITY_SERIOUS  -> Component.literal("§a???");
            case PERSONALITY_MOCKER_1 -> Component.literal("§f???");
            case PERSONALITY_MOCKER_2 -> Component.literal("§f???");
            default -> Component.literal("§f???");
        };
    }

    // === 永続化 ===

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(NBT_PERSONALITY, personality);
        tag.putBoolean(NBT_ANGRY, angry);
        tag.putInt(NBT_ASK_COUNT, askCount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(NBT_PERSONALITY)) personality = tag.getInt(NBT_PERSONALITY);
        if (tag.contains(NBT_ANGRY)) angry = tag.getBoolean(NBT_ANGRY);
        if (tag.contains(NBT_ASK_COUNT)) askCount = tag.getInt(NBT_ASK_COUNT);
        if (angry) {
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
    }

    public int getPersonality() { return personality; }
    public void setPersonality(int p) { this.personality = p; }
    public boolean isAngry() { return angry; }
}
