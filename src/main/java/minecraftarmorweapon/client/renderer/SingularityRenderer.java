package minecraftarmorweapon.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.HumanoidModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

import minecraftarmorweapon.entity.SingularityEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 特異点エンティティのレンダラー（ティア3）
 *
 * プレイヤーモデル（Steve/Slim）を使用し、武器と防具を表示します
 */
public class SingularityRenderer extends HumanoidMobRenderer<SingularityEntity, PlayerModel<SingularityEntity>> {

    private final PlayerModel<SingularityEntity> normalModel;  // Wide（太い腕）
    private final PlayerModel<SingularityEntity> slimModel;    // Slim（細い腕）

    // スキンバリエーション
    private static List<ResourceLocation> wideSkins = new ArrayList<>();
    private static List<ResourceLocation> slimSkins = new ArrayList<>();
    private static boolean skinsInitialized = false;

    public SingularityRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);

        // 通常モデル（Wide - 太い腕）
        this.normalModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);

        // スリムモデル（Slim - 細い腕）
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);

        // 防具レイヤーを追加（強化ダイヤモンドの防具を表示）
        this.addLayer(new HumanoidArmorLayer<>(
            this,
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
            context.getModelManager()
        ));

        // 武器レイヤーを追加
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));

        // スキンを初期化
        if (!skinsInitialized) {
            initializeSkins();
        }
    }

    private static void initializeSkins() {
        try {
            // デフォルトスキンを追加
            wideSkins.add(new ResourceLocation("textures/entity/steve.png"));
            slimSkins.add(new ResourceLocation("textures/entity/alex.png"));

            // slim フォルダからスキンを追加
            slimSkins.add(new ResourceLocation("minecraft_armor_weapon", "textures/entity/slim/37da0e0fbea5d2f3.png"));
            slimSkins.add(new ResourceLocation("minecraft_armor_weapon", "textures/entity/slim/4cafd512b6f9b293.png"));
            slimSkins.add(new ResourceLocation("minecraft_armor_weapon", "textures/entity/slim/c87054d6c91e47c2.png"));
            slimSkins.add(new ResourceLocation("minecraft_armor_weapon", "textures/entity/slim/cb8323d05e90afdf.png"));

            skinsInitialized = true;
            System.out.println("[Singularity] Initialized skins: " + wideSkins.size() + " wide, " + slimSkins.size() + " slim");
        } catch (Exception e) {
            System.err.println("[Singularity] Error initializing skins: " + e.getMessage());
        }
    }

    @Override
    public void render(SingularityEntity entity, float entityYaw, float partialTicks,
                      PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        boolean isSlim = shouldUseSlimModel(entity);
        if (isSlim) {
            this.model = this.slimModel;
        } else {
            this.model = this.normalModel;
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected void scale(SingularityEntity entity, PoseStack poseStack, float partialTicks) {
        if (shouldUseSlimModel(entity)) {
            poseStack.scale(0.95f, 1.0f, 0.95f);
        }

        super.scale(entity, poseStack, partialTicks);
    }

    private boolean shouldUseSlimModel(SingularityEntity entity) {
        int isSlimNBT = entity.getSkinType();

        if (isSlimNBT >= 0) {
            return isSlimNBT == 1;
        }

        long uuidBits = entity.getUUID().getMostSignificantBits();
        int skinVariant = Math.abs((int)(uuidBits % 10));

        return skinVariant < 3;
    }

    private int getSkinIndex(SingularityEntity entity, boolean isSlim) {
        int skinIndexNBT = entity.getSkinIndex();

        List<ResourceLocation> skins = isSlim ? slimSkins : wideSkins;
        if (skins.isEmpty()) {
            return 0;
        }

        if (skinIndexNBT >= 0) {
            return Math.min(skinIndexNBT, skins.size() - 1);
        }

        long uuidBits = entity.getUUID().getLeastSignificantBits();
        return Math.abs((int)(uuidBits % skins.size()));
    }

    @Override
    public ResourceLocation getTextureLocation(SingularityEntity entity) {
        boolean isSlim = shouldUseSlimModel(entity);
        int skinIndex = getSkinIndex(entity, isSlim);

        List<ResourceLocation> skins = isSlim ? slimSkins : wideSkins;

        if (skins.isEmpty()) {
            return isSlim ?
                new ResourceLocation("textures/entity/alex.png") :
                new ResourceLocation("textures/entity/steve.png");
        }

        return skins.get(skinIndex);
    }
}
