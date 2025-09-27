package minecraftarmorweapon.client.init;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.client.renderer.DarkProjectileRenderer;
import minecraftarmorweapon.init.CustomEntityInit;

/**
 * カスタムエンティティのレンダラー登録用クラス
 * MCreatorによって上書きされないように別クラスとして作成
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CustomEntityRenderers {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // エンティティが登録されていることを確認してから登録
        if (CustomEntityInit.DARK_PROJECTILE != null && CustomEntityInit.DARK_PROJECTILE.isPresent()) {
            event.registerEntityRenderer(CustomEntityInit.DARK_PROJECTILE.get(), DarkProjectileRenderer::new);
        }
    }
}