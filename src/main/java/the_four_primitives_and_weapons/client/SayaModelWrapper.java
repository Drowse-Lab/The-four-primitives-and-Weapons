package the_four_primitives_and_weapons.client;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import the_four_primitives_and_weapons.util.SayaRegistry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * 鞘 (saya/tyokuto_saya/sword_saya) 用の動的モデル選択 BakedModel。
 *
 * <p>納刀中の刀の NBT (StoredSword) を読み、SayaRegistry に登録されたエントリの
 * うち <b>カスタムモデルパス</b> が指定されているならそのモデルを差し替え描画する。
 * 整数スロット (custom_model_data) で登録されている、あるいは未登録の場合は
 * 元の BakedModel (本体MOD 内蔵のスロット用 overrides) を使う。</p>
 *
 * <p>これによりアドオン側は本体MOD の saya.json overrides を一切いじらずに
 * 自分の独自鞘モデルを追加できる。</p>
 */
@OnlyIn(Dist.CLIENT)
public class SayaModelWrapper implements BakedModel {

    private final BakedModel wrapped;
    private final SayaRegistry.SayaType sayaType;
    private final DynamicOverrides overrides;

    public SayaModelWrapper(BakedModel wrapped, SayaRegistry.SayaType sayaType) {
        this.wrapped = wrapped;
        this.sayaType = sayaType;
        this.overrides = new DynamicOverrides(this);
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    /** カスタムモデル解決の本体。 */
    private final class DynamicOverrides extends ItemOverrides {
        private final SayaModelWrapper outer;

        DynamicOverrides(SayaModelWrapper outer) {
            this.outer = outer;
        }

        @Nullable
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                  @Nullable LivingEntity entity, int seed) {
            // ベース → 仕立て ( wrap テクスチャ差し替え ) → 納刀中の刀の拵え色を反映
            // ( 持っている刀と同じく gray/black 変種テクスチャに差し替え、 見た目を一致させる )。
            BakedModel base = withStyle(resolveBase(stack, level, entity, seed), stack);
            return withFittingColor(base, stack);
        }

        /** 納刀中の刀の柄/鍔/頭の色に応じて、 鞘の該当面を gray/black 変種へ差し替える ( 持っている刀と同方式 )。 */
        private BakedModel withFittingColor(BakedModel base, ItemStack sayaStack) {
            ItemStack weapon = getStoredWeapon(sayaStack, outer.sayaType);
            if (weapon.isEmpty()) return base;
            int[] mode = new int[5]; // 1=柄 2=鍔 3=頭
            mode[1] = variantMode(the_four_primitives_and_weapons.util.KatanaFittings.tsukaRgb(weapon));
            mode[2] = variantMode(the_four_primitives_and_weapons.util.KatanaFittings.tsubaRgb(weapon));
            mode[3] = variantMode(the_four_primitives_and_weapons.util.KatanaFittings.kashiraRgb(weapon));
            return KatanaTsukaModel.grayForTint(base, mode);
        }

        private int variantMode(int rgb) {
            if (rgb < 0) return 0;
            return the_four_primitives_and_weapons.util.KatanaFittings.isNearBlack(rgb) ? 2 : 1;
        }

        /** 納刀中の武器 / custom_model_data に応じた「ベース」モデルを解決する。 */
        private BakedModel resolveBase(ItemStack stack, @Nullable ClientLevel level,
                                       @Nullable LivingEntity entity, int seed) {
            // 納刀中の武器を取り出して SayaRegistry から Entry を引く
            ItemStack stored = getStoredWeapon(stack, outer.sayaType);
            if (!stored.isEmpty()) {
                SayaRegistry.Entry e = SayaRegistry.getEntry(outer.sayaType, stored);
                if (e != null && e.hasCustomModel()) {
                    BakedModel custom = resolveCachedModel(e.modelLocation(), stack, level, entity, seed);
                    if (custom != null) return custom;
                }
            }
            // カスタムモデル未指定 or 解決失敗 → 元の overrides (custom_model_data 用) に委譲
            ItemOverrides orig = outer.wrapped.getOverrides();
            if (orig != null) {
                BakedModel resolved = orig.resolve(outer.wrapped, stack, level, entity, seed);
                if (resolved != null) return resolved;
            }
            return outer.wrapped;
        }

        @Nullable
        private BakedModel resolveCachedModel(@Nullable ResourceLocation loc, ItemStack stack,
                                              @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            if (loc == null) return null;
            BakedModel custom = SayaCustomModelCache.get(loc);
            if (custom == null) return null;
            // 解決されたモデルもさらに overrides を持つ可能性があるので再帰
            ItemOverrides sub = custom.getOverrides();
            if (sub != null && sub != this) {
                BakedModel resolved = sub.resolve(custom, stack, level, entity, seed);
                if (resolved != null) return resolved;
            }
            return custom;
        }

        /** ベースモデルの鞘本体 wrap を 仕立て ( 木目/着せ/刻/石目/鮫/漆系 ) のテクスチャへ差し替える。 */
        private BakedModel withStyle(BakedModel base, ItemStack stack) {
            return SayaStyleModel.maybe(base, the_four_primitives_and_weapons.util.SayaDesign.getStyle(stack));
        }
    }

    /**
     * 鞘 NBT から納刀中の武器 ItemStack を再構築する。
     * 鞘の種類によって参照する NBT キーが異なる:
     *   KATANA  → "StoredKatana"
     *   TYOKUTO → "StoredSword"
     *   SWORD   → "StoredSword"
     *   RAPIER  → "StoredRapier"
     * いずれが入っていても拾えるようフォールバックで全キーを試す。
     */
    private static ItemStack getStoredWeapon(ItemStack sayaStack, SayaRegistry.SayaType type) {
        if (sayaStack.isEmpty()) return ItemStack.EMPTY;
        CompoundTag tag = sayaStack.getTag();
        if (tag == null) return ItemStack.EMPTY;
        String primary = primaryKeyFor(type);
        if (primary != null && tag.contains(primary)) {
            return ItemStack.of(tag.getCompound(primary));
        }
        // フォールバック (旧データや手動編集タグ救済)
        for (String key : new String[] { "StoredKatana", "StoredSword", "StoredRapier", "StoredDagger" }) {
            if (tag.contains(key)) return ItemStack.of(tag.getCompound(key));
        }
        return ItemStack.EMPTY;
    }

    private static String primaryKeyFor(SayaRegistry.SayaType type) {
        switch (type) {
            case KATANA:  return "StoredKatana";
            case TYOKUTO: return "StoredSword";
            case SWORD:   return "StoredSword";
            case RAPIER:  return "StoredRapier";
            case DAGGER:  return "StoredDagger";
            default:      return null;
        }
    }

    // ====== BakedModel 委譲 ======

    @Override
    public List<BakedQuad> getQuads(@Nullable net.minecraft.world.level.block.state.BlockState state,
                                    @Nullable Direction direction, net.minecraft.util.RandomSource rand) {
        return wrapped.getQuads(state, direction, rand);
    }

    @Override
    public boolean useAmbientOcclusion() { return wrapped.useAmbientOcclusion(); }

    @Override
    public boolean isGui3d() { return wrapped.isGui3d(); }

    @Override
    public boolean usesBlockLight() { return wrapped.usesBlockLight(); }

    @Override
    public boolean isCustomRenderer() { return wrapped.isCustomRenderer(); }

    @SuppressWarnings("deprecation")
    @Override
    public TextureAtlasSprite getParticleIcon() { return wrapped.getParticleIcon(); }

    @SuppressWarnings("deprecation")
    @Override
    public ItemTransforms getTransforms() { return wrapped.getTransforms(); }

    // ===== Forge 拡張: モデル変換などはラップ対象のものを使う =====

    @Override
    public BakedModel applyTransform(net.minecraft.world.item.ItemDisplayContext context,
                                     com.mojang.blaze3d.vertex.PoseStack poseStack, boolean leftFlip) {
        wrapped.applyTransform(context, poseStack, leftFlip);
        return this;
    }
}
