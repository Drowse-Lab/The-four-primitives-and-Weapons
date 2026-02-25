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

import minecraftarmorweapon.entity.EliteSoldierEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * エリート兵エンティティのレンダラー（ティア2）
 *
 * プレイヤーモデル（Steve/Slim）を使用し、武器と防具を表示します
 */
public class EliteSoldierRenderer extends HumanoidMobRenderer<EliteSoldierEntity, PlayerModel<EliteSoldierEntity>> {

    private final PlayerModel<EliteSoldierEntity> normalModel;  // Wide（太い腕）
    private final PlayerModel<EliteSoldierEntity> slimModel;    // Slim（細い腕）

    // スキンバリエーション
    private static List<ResourceLocation> wideSkins = new ArrayList<>();
    private static List<ResourceLocation> slimSkins = new ArrayList<>();
    private static boolean skinsInitialized = false;

    public EliteSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);

        // 通常モデル（Wide - 太い腕）
        this.normalModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);

        // スリムモデル（Slim - 細い腕）
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);

        // 防具レイヤーを追加（ダイヤモンドの防具を表示）
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
            System.out.println("[EliteSoldier] Initialized skins: " + wideSkins.size() + " wide, " + slimSkins.size() + " slim");
        } catch (Exception e) {
            System.err.println("[EliteSoldier] Error initializing skins: " + e.getMessage());
        }
    }

    @Override
    public void render(EliteSoldierEntity entity, float entityYaw, float partialTicks,
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
    protected void scale(EliteSoldierEntity entity, PoseStack poseStack, float partialTicks) {
        if (shouldUseSlimModel(entity)) {
            poseStack.scale(0.95f, 1.0f, 0.95f);
        }

        super.scale(entity, poseStack, partialTicks);
    }

    private boolean shouldUseSlimModel(EliteSoldierEntity entity) {
        int isSlimNBT = entity.getIsSlim();

        if (isSlimNBT >= 0) {
            return isSlimNBT == 1;
        }

        long uuidBits = entity.getUUID().getMostSignificantBits();
        int skinVariant = Math.abs((int)(uuidBits % 10));

        return skinVariant < 3;
    }

    private int getSkinIndex(EliteSoldierEntity entity, boolean isSlim) {
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
    public ResourceLocation getTextureLocation(EliteSoldierEntity entity) {
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
