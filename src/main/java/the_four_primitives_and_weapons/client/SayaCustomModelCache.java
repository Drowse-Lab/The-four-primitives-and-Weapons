package the_four_primitives_and_weapons.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * カスタム鞘モデル ResourceLocation → BakedModel のクライアント側キャッシュ。
 * {@link SayaDynamicModelEvents} がベイク完了時に登録し、
 * {@link SayaModelWrapper} がレンダリング時に参照する。
 */
@OnlyIn(Dist.CLIENT)
public final class SayaCustomModelCache {

    private static final Map<ResourceLocation, BakedModel> CACHE = new HashMap<>();

    private SayaCustomModelCache() {}

    public static void clear() {
        CACHE.clear();
    }

    public static void put(ResourceLocation loc, BakedModel model) {
        if (loc == null || model == null) return;
        CACHE.put(loc, model);
    }

    @Nullable
    public static BakedModel get(ResourceLocation loc) {
        return loc == null ? null : CACHE.get(loc);
    }
}
