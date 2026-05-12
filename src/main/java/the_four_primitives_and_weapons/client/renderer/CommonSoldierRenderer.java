package the_four_primitives_and_weapons.client.renderer;

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
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.client.Minecraft;

import the_four_primitives_and_weapons.entity.CommonSoldierEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 一般兵エンティティのレンダラー
 *
 * プレイヤーモデル（Steve/Slim）を使用し、武器と防具を表示します
 * slim/とwide/フォルダから動的にスキンを読み込み
 */
public class CommonSoldierRenderer extends HumanoidMobRenderer<CommonSoldierEntity, PlayerModel<CommonSoldierEntity>> {

    private final PlayerModel<CommonSoldierEntity> normalModel;  // Wide（太い腕）
    private final PlayerModel<CommonSoldierEntity> slimModel;    // Slim（細い腕）

    // スキンバリエーション（動的に読み込み）
    private static List<ResourceLocation> wideSkins = new ArrayList<>();
    private static List<ResourceLocation> slimSkins = new ArrayList<>();
    private static boolean skinsInitialized = false;

    public CommonSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);

        // 通常モデル（Wide - 太い腕）
        this.normalModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);

        // スリムモデル（Slim - 細い腕）
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);

        // 防具レイヤーを追加（鉄の防具を表示）
        this.addLayer(new HumanoidArmorLayer<>(
            this,
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
            context.getModelManager()
        ));

        // 武器レイヤーを追加（手に持っている刀を表示）
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));

        // AIレベル表示レイヤーは SpecialLabelLayerRegister が全 LivingEntity に一括登録

        // スキンを初期化
        if (!skinsInitialized) {
            initializeSkins();
        }
    }

    /**
     * slimとwideフォルダからスキンファイルを動的に読み込む
     */
    private static void initializeSkins() {
        try {
            // デフォルトスキンを追加
            wideSkins.add(new ResourceLocation("textures/entity/player/wide/steve.png"));
            slimSkins.add(new ResourceLocation("textures/entity/player/slim/alex.png"));

            // slim フォルダから既知のスキンを追加
            // 実際に存在するファイル名を使用
            slimSkins.add(new ResourceLocation("the_four_primitives_and_weapons", "textures/entity/slim/37da0e0fbea5d2f3.png"));
            slimSkins.add(new ResourceLocation("the_four_primitives_and_weapons", "textures/entity/slim/4cafd512b6f9b293.png"));
            slimSkins.add(new ResourceLocation("the_four_primitives_and_weapons", "textures/entity/slim/c87054d6c91e47c2.png"));
            slimSkins.add(new ResourceLocation("the_four_primitives_and_weapons", "textures/entity/slim/cb8323d05e90afdf.png"));

            // wideフォルダは現在空なのでデフォルトのみ
            // 今後追加する場合はここに追加

            skinsInitialized = true;
            System.out.println("[CommonSoldier] Initialized skins: " + wideSkins.size() + " wide, " + slimSkins.size() + " slim");
        } catch (Exception e) {
            System.err.println("[CommonSoldier] Error initializing skins: " + e.getMessage());
            e.printStackTrace();
            // デフォルトスキンは最低限追加されている
        }
    }

    @Override
    public void render(CommonSoldierEntity entity, float entityYaw, float partialTicks,
                      PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // UUID-based model selection - 各エンティティで一貫して同じモデルを使用
        boolean isSlim = shouldUseSlimModel(entity);
        if (isSlim) {
            this.model = this.slimModel;
        } else {
            this.model = this.normalModel;
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected void scale(CommonSoldierEntity entity, PoseStack poseStack, float partialTicks) {
        // スリムモデルの場合、全体を少し細くする（よりスリムな体型に）
        if (shouldUseSlimModel(entity)) {
            // X軸（幅）を95%に縮小、Y軸（高さ）は維持、Z軸（奥行き）を95%に縮小
            poseStack.scale(0.95f, 1.0f, 0.95f);
        }

        super.scale(entity, poseStack, partialTicks);
    }

    /**
     * スリムモデル（Alex）を使用するかどうか
     * NBTタグで指定されている場合はそれを使用、されていない場合はUUIDベースでランダム
     */
    private boolean shouldUseSlimModel(CommonSoldierEntity entity) {
        int isSlimNBT = entity.getIsSlim();

        // NBTタグで指定されている場合
        if (isSlimNBT >= 0) {
            return isSlimNBT == 1;
        }

        // NBTタグで指定されていない場合はUUIDベースでランダム（約30%の確率でSlimモデル）
        long uuidBits = entity.getUUID().getMostSignificantBits();
        int skinVariant = Math.abs((int)(uuidBits % 10));

        // 0-2 = Slim (30%), 3-9 = Normal (70%)
        return skinVariant < 3;
    }

    /**
     * スキンのインデックスを取得
     * NBTタグで指定されている場合はそれを使用、されていない場合はUUIDベースでランダム
     */
    private int getSkinIndex(CommonSoldierEntity entity, boolean isSlim) {
        int skinIndexNBT = entity.getSkinIndex();

        List<ResourceLocation> skins = isSlim ? slimSkins : wideSkins;
        if (skins.isEmpty()) {
            return 0;
        }

        // NBTタグで指定されている場合
        if (skinIndexNBT >= 0) {
            // 範囲外の場合は0に設定
            return Math.min(skinIndexNBT, skins.size() - 1);
        }

        // NBTタグで指定されていない場合はUUIDベースでランダム
        long uuidBits = entity.getUUID().getLeastSignificantBits();
        return Math.abs((int)(uuidBits % skins.size()));
    }

    @Override
    public ResourceLocation getTextureLocation(CommonSoldierEntity entity) {
        boolean isSlim = shouldUseSlimModel(entity);
        int skinIndex = getSkinIndex(entity, isSlim);

        List<ResourceLocation> skins = isSlim ? slimSkins : wideSkins;

        // スキンリストが空の場合はデフォルトを返す
        if (skins.isEmpty()) {
            return isSlim ?
                new ResourceLocation("textures/entity/player/slim/alex.png") :
                new ResourceLocation("textures/entity/player/wide/steve.png");
        }

        return skins.get(skinIndex);
    }
}
