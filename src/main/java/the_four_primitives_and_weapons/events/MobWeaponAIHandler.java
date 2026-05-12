package the_four_primitives_and_weapons.events;

import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.InteractionHand;

import the_four_primitives_and_weapons.ai.PlayerLikeAIGoal;
import the_four_primitives_and_weapons.entity.CommonSoldierEntity;
import the_four_primitives_and_weapons.entity.EliteSoldierEntity;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 武器を持つMobに自動的にプレイヤーのようなAIを追加するハンドラー
 *
 * このハンドラーは、ワールドにMobがスポーンした際に：
 * 1. Mobが武器を持っているかチェック
 * 2. 武器の種類に応じてティアを判定
 * 3. PlayerLikeAIGoalを自動的に追加
 *
 * これにより、すべてのMobが武器を持つとプレイヤーのような動作をするようになります
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class MobWeaponAIHandler {

    // 既にAIを追加したMobを記録（重複防止）
    private static final Set<UUID> processedMobs = new HashSet<>();

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        // サーバー側でのみ処理
        if (event.getLevel().isClientSide()) {
            return;
        }

        // エンティティがMobでない場合はスキップ
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        // 既に処理済みのMobはスキップ
        UUID mobId = mob.getUUID();
        if (processedMobs.contains(mobId)) {
            return;
        }

        // このmodの専用エンティティ（一般兵・精鋭兵）以外はスキルを使わせない
        if (!(mob instanceof CommonSoldierEntity) && !(mob instanceof EliteSoldierEntity)) {
            return;
        }

        // 武器を持っているかチェック
        ItemStack mainHandItem = mob.getItemInHand(InteractionHand.MAIN_HAND);
        if (!isWeapon(mainHandItem)) {
            return;
        }

        // 武器の種類に応じてティアを判定
        int tier = getWeaponTier(mainHandItem);

        // PlayerLikeAIGoalを追加
        PlayerLikeAIGoal playerLikeAI = new PlayerLikeAIGoal(mob, tier);
        mob.goalSelector.addGoal(1, playerLikeAI);

        // 処理済みとしてマーク
        processedMobs.add(mobId);

        // デバッグログ
        System.out.println("[MobWeaponAI] " + mob.getName().getString() +
                          " にティア" + tier + "のAIを追加しました（武器: " +
                          mainHandItem.getDisplayName().getString() + "）");
    }

    /**
     * アイテムが武器かどうかを判定
     */
    private static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // SwordItemまたはカスタム武器かチェック
        if (stack.getItem() instanceof SwordItem) {
            return true;
        }

        String itemName = stack.getItem().getClass().getSimpleName();
        return itemName.contains("Katana") ||
               itemName.contains("Sword") ||
               itemName.contains("Blade") ||
               itemName.contains("katana") ||
               itemName.equals("RiversOfBloodItem") ||
               itemName.equals("KatanaNiguHumerusItem") ||
               itemName.equals("LunaItem") ||
               itemName.equals("ReplicaSwordOfLightItem") ||
               itemName.equals("SwordOfNightItem");
    }

    /**
     * 武器の種類に応じてティアを判定
     *
     * ティアシステム:
     * - ティア1 (一般兵): 石、鉄、金の武器
     * - ティア2 (エリート兵): ネザライト、ウィザー系
     * - ティア3 (特異点): 闇、試作品
     * - ティア4 (英雄級): 血の川、光のレプリカ
     * - ティア5 (神話級): 魔法の刀
     * - ティア6 (天使級): 妖精の刀
     * - ティア7 (神聖級): 夜の剣、ルナ
     */
    private static int getWeaponTier(ItemStack weapon) {
        if (weapon.isEmpty()) {
            return 1;
        }

        // アイテムのレジストリ名またはクラス名で判定
        String itemName = weapon.getItem().getClass().getSimpleName();

        // ティア7: 神聖級（最強）
        if (weapon.getItem() == TheFourPrimitivesAndWeaponsModItems.SWORD_OF_NIGHT.get() ||
            weapon.getItem() == TheFourPrimitivesAndWeaponsModItems.LUNA.get() ||
            weapon.getItem() == TheFourPrimitivesAndWeaponsModItems.LOKI_THE_TRICKSTER.get()) {
            return 7;
        }

        // ティア6: 天使級
        if (weapon.getItem() == TheFourPrimitivesAndWeaponsModItems.MAGISCHES_FEEN_KATANA.get() ||
            itemName.contains("Feen")) {
            return 6;
        }

        // ティア5: 神話級
        if (weapon.getItem() == TheFourPrimitivesAndWeaponsModItems.MAGICAL_KATANA.get() ||
            itemName.contains("Magical") ||
            itemName.contains("Magic")) {
            return 5;
        }

        // ティア4: 英雄級
        if (weapon.getItem() == TheFourPrimitivesAndWeaponsModItems.RIVERS_OF_BLOOD.get() ||
            weapon.getItem() == TheFourPrimitivesAndWeaponsModItems.REPLICA_SWORD_OF_LIGHT.get() ||
            weapon.getItem() == TheFourPrimitivesAndWeaponsModItems.KURIKARAKEN.get() ||
            itemName.contains("Kurikara")) {
            return 4;
        }

        // ティア3: 特異点
        if (weapon.getItem() == TheFourPrimitivesAndWeaponsModItems.DARKNESS_KATANA.get() ||
            weapon.getItem() == TheFourPrimitivesAndWeaponsModItems.PROTOTYPE_KATANA.get() ||
            weapon.getItem() == TheFourPrimitivesAndWeaponsModItems.OLD_KATANA.get() ||
            itemName.contains("Darkness") ||
            itemName.contains("Prototype")) {
            return 3;
        }

        // ティア2: エリート兵
        if (weapon.getItem() == TheFourPrimitivesAndWeaponsModItems.NETHERITE_KATANA.get() ||
            weapon.getItem() == TheFourPrimitivesAndWeaponsModItems.WITHER_KATANA.get() ||
            itemName.contains("Netherite") ||
            itemName.contains("Wither")) {
            return 2;
        }

        // ティア1: 一般兵（デフォルト）
        // 石、鉄、金、その他の武器
        return 1;
    }

    /**
     * 定期的に処理済みMobのリストをクリーンアップ
     * （メモリリーク防止）
     */
    public static void cleanupProcessedMobs() {
        if (processedMobs.size() > 1000) {
            processedMobs.clear();
            System.out.println("[MobWeaponAI] 処理済みMobリストをクリアしました");
        }
    }
}
