package the_four_primitives_and_weapons.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.util.SayaRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 鞘(saya) アイテムの動的モデル選択を支える ModelEvent ハンドラ。
 *
 * <p><b>動作の流れ</b>:</p>
 * <ol>
 *   <li>{@link ModelEvent.RegisterAdditional}: resource pack 内の
 *       {@code assets/&lt;modid&gt;/models/custom/saya/{katana,sword,tyokuto}/*.json}
 *       を全部スキャンし、ベイク対象として登録する (規約: 先頭 _ は無視)。
 *       これによりアドオン側は規約フォルダにモデルを置くだけで自動でベイクされる。</li>
 *   <li>{@link ModelEvent.BakingCompleted}: ベイク済みモデルをキャッシュへ。
 *       saya / sword_saya / tyokuto_saya の本体モデルを
 *       {@link SayaModelWrapper} で包んで差し替え、NBT 動的選択を有効化する。</li>
 * </ol>
 *
 * <p>アドオンの saya.jsonc で値に <b>ResourceLocation 文字列</b> を書いた場合
 * (例: {@code "your_mod:item": "your_mod:custom/saya/katana/saya_foo"}) には、
 * その ResourceLocation がここでベイクされ、SayaModelWrapper が NBT から
 * 引いて表示する。本体MOD の saya.json overrides を一切いじらずに済む。</p>
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SayaDynamicModelEvents {

    private SayaDynamicModelEvents() {}

    private static final String[] SAYA_SUBDIRS = { "katana", "sword", "tyokuto" };
    private static final String MODELS_PREFIX = "models/custom/saya/";

    /** スキャンで見つかった全カスタム鞘モデルの ResourceLocation 集合。 */
    private static final Set<ResourceLocation> DISCOVERED_MODELS = new HashSet<>();

    /** saya 系アイテムの inventory モデルキー (ベイク結果差し替え用)。 */
    private static final ModelResourceLocation MRL_SAYA =
            new ModelResourceLocation(new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "saya"), "inventory");
    private static final ModelResourceLocation MRL_SWORD_SAYA =
            new ModelResourceLocation(new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "sword_saya"), "inventory");
    private static final ModelResourceLocation MRL_TYOKUTO_SAYA =
            new ModelResourceLocation(new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "tyokuto_saya"), "inventory");

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        DISCOVERED_MODELS.clear();
        ResourceManager rm = Minecraft.getInstance().getResourceManager();

        // 規約フォルダ配下の全 .json をスキャン (_ で始まるテンプレは除外)
        for (String sub : SAYA_SUBDIRS) {
            String prefix = MODELS_PREFIX + sub;
            rm.listResources(prefix, loc -> {
                String path = loc.getPath();
                int slash = path.lastIndexOf('/');
                String fileName = slash >= 0 ? path.substring(slash + 1) : path;
                return path.endsWith(".json") && !fileName.startsWith("_");
            }).keySet().forEach(loc -> {
                // "models/custom/saya/sword/foo.json" → "custom/saya/sword/foo"
                String p = loc.getPath();
                if (!p.startsWith("models/") || !p.endsWith(".json")) return;
                String modelPath = p.substring("models/".length(), p.length() - ".json".length());
                ResourceLocation modelLoc = new ResourceLocation(loc.getNamespace(), modelPath);
                DISCOVERED_MODELS.add(modelLoc);
                event.register(modelLoc);
            });
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        // 注: このイベントは BakingResult を直接操作できる唯一のフック。
        //     event.getModels() は Map<ResourceLocation, BakedModel> (キーは
        //     アイテム/ブロック用は ModelResourceLocation、追加登録モデルは
        //     生の ResourceLocation)。

        // ----- (1) 発見した全カスタム鞘モデルをキャッシュ -----
        SayaCustomModelCache.clear();
        Set<ResourceLocation> toCache = new HashSet<>(DISCOVERED_MODELS);
        // SayaRegistry に文字列で登録されている他環境のモデルも合わせて登録対象に
        toCache.addAll(SayaRegistry.getAllCustomModelLocations());
        for (ResourceLocation loc : toCache) {
            BakedModel baked = event.getModels().get(loc);
            if (baked != null) {
                SayaCustomModelCache.put(loc, baked);
            }
        }

        // ----- (2) 本体3種の saya アイテムモデルを SayaModelWrapper で包む -----
        wrapSaya(event, MRL_SAYA, SayaRegistry.SayaType.KATANA);
        wrapSaya(event, MRL_SWORD_SAYA, SayaRegistry.SayaType.SWORD);
        wrapSaya(event, MRL_TYOKUTO_SAYA, SayaRegistry.SayaType.TYOKUTO);
    }

    private static void wrapSaya(ModelEvent.ModifyBakingResult event,
                                 ModelResourceLocation mrl, SayaRegistry.SayaType type) {
        BakedModel current = event.getModels().get(mrl);
        if (current == null) return;
        // 既に Wrapper で包まれてたら二重ラップを避ける
        if (current instanceof SayaModelWrapper) return;
        event.getModels().put(mrl, new SayaModelWrapper(current, type));
    }
}
