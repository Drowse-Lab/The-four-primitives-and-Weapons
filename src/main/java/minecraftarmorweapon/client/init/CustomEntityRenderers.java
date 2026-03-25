package minecraftarmorweapon.client.init;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.client.renderer.CommonSoldierRenderer;
import minecraftarmorweapon.client.renderer.EliteSoldierRenderer;
import minecraftarmorweapon.client.renderer.SingularityRenderer;
import minecraftarmorweapon.client.renderer.HeroicTierRenderer;
import minecraftarmorweapon.client.renderer.DebugMobRenderer;
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
    }
}