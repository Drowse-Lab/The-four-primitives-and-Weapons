package minecraftarmorweapon.client.init;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.client.renderer.CommonSoldierRenderer;
import minecraftarmorweapon.client.renderer.EliteSoldierRenderer;
import minecraftarmorweapon.client.renderer.SingularityRenderer;
import minecraftarmorweapon.client.renderer.HeroicTierRenderer;
import minecraftarmorweapon.client.renderer.DebugMobRenderer;
import minecraftarmorweapon.client.renderer.AngelTrioRenderer;
import minecraftarmorweapon.client.renderer.DarkProjectileRenderer;
import minecraftarmorweapon.client.renderer.TornadoRenderer;
import minecraftarmorweapon.init.CustomEntityInit;
import minecraftarmorweapon.init.MinecraftArmorWeaponModCustomEntities;

/**
 * カスタムエンティティのレンダラー登録用クラス
 * MCreatorによって上書きされないように別クラスとして作成
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CustomEntityRenderers {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register entity renderers
        event.registerEntityRenderer(MinecraftArmorWeaponModCustomEntities.TORNADO.get(), TornadoRenderer::new);
        event.registerEntityRenderer(MinecraftArmorWeaponModCustomEntities.DARK_PROJECTILE.get(), DarkProjectileRenderer::new);

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
        event.registerEntityRenderer(minecraftarmorweapon.init.MinecraftArmorWeaponModEntities.GATE_PROJECTILE.get(),
                minecraftarmorweapon.client.renderer.GateProjectileRenderer::new);

        // 投げナイフ飛翔体 (FlyingAttackerEntity が投げナイフを持った時と同じ見た目)
        event.registerEntityRenderer(CustomEntityInit.THROWING_KNIFE_ENTITY.get(),
                minecraftarmorweapon.client.renderer.ThrowingKnifeRenderer::new);

        // Arsenal式 武器ラック
        event.registerEntityRenderer(CustomEntityInit.WEAPON_RACK.get(),
                minecraftarmorweapon.client.renderer.WeaponRackRenderer::new);

        // Re:Cross Hookshot
        event.registerEntityRenderer(CustomEntityInit.RECROSS_HOOK_ENTITY.get(),
                minecraftarmorweapon.client.renderer.RecrossHookRenderer::new);
    }

    @SubscribeEvent
    public static void registerExtraModels(ModelEvent.RegisterAdditional event) {
        // weapon_rack のブロックモデル(Block未登録のため明示ロード)。
        // 天井=チェーン版 / 床=A-frameスタンド / 壁=フック単体 を direction で切替。
        event.register(minecraftarmorweapon.client.renderer.WeaponRackRenderer.MODEL_CHAIN);
        event.register(minecraftarmorweapon.client.renderer.WeaponRackRenderer.MODEL_HOOK);
        event.register(minecraftarmorweapon.client.renderer.WeaponRackRenderer.MODEL_STAND);
        // pk_racks ベースの head 形状ラック バリアント (CC BY-NC-SA 4.0, KawaMood)
        for (net.minecraft.resources.ResourceLocation rl : minecraftarmorweapon.client.renderer.WeaponRackRenderer.MODEL_PK_VARIANTS) {
            event.register(rl);
        }
        // 木の種類別 chain/stand/hook バリアント (oak 以外)。
        String[] woods = minecraftarmorweapon.entity.WeaponRackEntity.WOOD_TYPES;
        for (int i = 1; i < woods.length; i++) {
            String w = woods[i];
            event.register(new net.minecraft.resources.ResourceLocation("minecraft_armor_weapon", "block/weapon_rack_chain_" + w));
            event.register(new net.minecraft.resources.ResourceLocation("minecraft_armor_weapon", "block/weapon_rack_stand_" + w));
            event.register(new net.minecraft.resources.ResourceLocation("minecraft_armor_weapon", "block/weapon_rack_hook_" + w));
        }
    }
}