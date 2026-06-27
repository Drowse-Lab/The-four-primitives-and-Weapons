package the_four_primitives_and_weapons.client.init;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.client.renderer.CommonSoldierRenderer;
import the_four_primitives_and_weapons.client.renderer.EliteSoldierRenderer;
import the_four_primitives_and_weapons.client.renderer.SingularityRenderer;
import the_four_primitives_and_weapons.client.renderer.HeroicTierRenderer;
import the_four_primitives_and_weapons.client.renderer.DebugMobRenderer;
import the_four_primitives_and_weapons.client.renderer.AngelTrioRenderer;
import the_four_primitives_and_weapons.client.renderer.DarkProjectileRenderer;
import the_four_primitives_and_weapons.client.renderer.TornadoRenderer;
import the_four_primitives_and_weapons.init.CustomEntityInit;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModCustomEntities;

/**
 * カスタムエンティティのレンダラー登録用クラス
 * MCreatorによって上書きされないように別クラスとして作成
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CustomEntityRenderers {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register entity renderers
        event.registerEntityRenderer(TheFourPrimitivesAndWeaponsModCustomEntities.TORNADO.get(), TornadoRenderer::new);
        event.registerEntityRenderer(TheFourPrimitivesAndWeaponsModCustomEntities.DARK_PROJECTILE.get(), DarkProjectileRenderer::new);

        // A-Life AI エンティティ
        event.registerEntityRenderer(CustomEntityInit.COMMON_SOLDIER.get(), CommonSoldierRenderer::new);
        event.registerEntityRenderer(CustomEntityInit.ELITE_SOLDIER.get(), EliteSoldierRenderer::new);
        event.registerEntityRenderer(CustomEntityInit.SINGULARITY.get(), SingularityRenderer::new);
        event.registerEntityRenderer(CustomEntityInit.HEROIC_TIER.get(), HeroicTierRenderer::new);
        event.registerEntityRenderer(CustomEntityInit.DEBUG_MOB.get(), DebugMobRenderer::new);
        event.registerEntityRenderer(CustomEntityInit.ANGEL_SERIOUS.get(), AngelTrioRenderer::new);
        event.registerEntityRenderer(CustomEntityInit.ANGEL_MOCKER1.get(), AngelTrioRenderer::new);
        event.registerEntityRenderer(CustomEntityInit.ANGEL_MOCKER2.get(), AngelTrioRenderer::new);

        // Gate飛び道具（切先が進行方向を向く金の直刀）
        event.registerEntityRenderer(the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModEntities.GATE_PROJECTILE.get(),
                the_four_primitives_and_weapons.client.renderer.GateProjectileRenderer::new);

        // 投げナイフ飛翔体 (FlyingAttackerEntity が投げナイフを持った時と同じ見た目)
        event.registerEntityRenderer(CustomEntityInit.THROWING_KNIFE_ENTITY.get(),
                the_four_primitives_and_weapons.client.renderer.ThrowingKnifeRenderer::new);

        // Arsenal式 武器ラック
        event.registerEntityRenderer(CustomEntityInit.WEAPON_RACK.get(),
                the_four_primitives_and_weapons.client.renderer.WeaponRackRenderer::new);

        // Re:Cross Hookshot
        event.registerEntityRenderer(CustomEntityInit.RECROSS_HOOK_ENTITY.get(),
                the_four_primitives_and_weapons.client.renderer.RecrossHookRenderer::new);

        // 上腕骨刀 特殊技「巨骨の腕」
        event.registerEntityRenderer(TheFourPrimitivesAndWeaponsModCustomEntities.GIANT_BONE_ARM.get(),
                the_four_primitives_and_weapons.client.renderer.GiantBoneArmRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // 「巨骨の腕」モデルのレイヤー定義
        event.registerLayerDefinition(
                the_four_primitives_and_weapons.entity.model.GiantBoneArmModel.LAYER_LOCATION,
                the_four_primitives_and_weapons.entity.model.GiantBoneArmModel::createBodyLayer);
        // ガード専用「肋骨の籠」モデルのレイヤー定義 (籠の bakeLayer に必須)
        event.registerLayerDefinition(
                the_four_primitives_and_weapons.entity.model.BoneCageModel.LAYER_LOCATION,
                the_four_primitives_and_weapons.entity.model.BoneCageModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerExtraModels(ModelEvent.RegisterAdditional event) {
        // weapon_rack のブロックモデル(Block未登録のため明示ロード)。
        // 天井=チェーン版 / 床=A-frameスタンド / 壁=フック単体 を direction で切替。
        event.register(the_four_primitives_and_weapons.client.renderer.WeaponRackRenderer.MODEL_CHAIN);
        event.register(the_four_primitives_and_weapons.client.renderer.WeaponRackRenderer.MODEL_HOOK);
        event.register(the_four_primitives_and_weapons.client.renderer.WeaponRackRenderer.MODEL_STAND);
        // pk_racks ベースの head 形状ラック バリアント (CC BY-NC-SA 4.0, KawaMood)
        for (net.minecraft.resources.ResourceLocation rl : the_four_primitives_and_weapons.client.renderer.WeaponRackRenderer.MODEL_PK_VARIANTS) {
            event.register(rl);
        }
        // 木の種類別 chain/stand/hook バリアント (oak 以外)。
        String[] woods = the_four_primitives_and_weapons.entity.WeaponRackEntity.WOOD_TYPES;
        for (int i = 1; i < woods.length; i++) {
            String w = woods[i];
            event.register(new net.minecraft.resources.ResourceLocation("the_four_primitives_and_weapons", "block/weapon_rack_chain_" + w));
            event.register(new net.minecraft.resources.ResourceLocation("the_four_primitives_and_weapons", "block/weapon_rack_stand_" + w));
            event.register(new net.minecraft.resources.ResourceLocation("the_four_primitives_and_weapons", "block/weapon_rack_hook_" + w));
        }
    }
}