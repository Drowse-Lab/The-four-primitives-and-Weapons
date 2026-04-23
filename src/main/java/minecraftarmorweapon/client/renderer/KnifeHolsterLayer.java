package minecraftarmorweapon.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import minecraftarmorweapon.init.CustomEntityInit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.WeakHashMap;

/**
 * プレイヤーのインベントリに KnifeLauncher がある時、腰にナイフケースを描画する。
 *
 * 検出対象はメインインベントリ + Curios + バンドル系ストレージ (再帰 1 段)。
 * 描画結果は 1 tick キャッシュして毎フレームのスキャンを回避する。
 */
public class KnifeHolsterLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static ItemStack sharedStack;

    /** プレイヤーごとの検出結果を 1 tick だけキャッシュ (描画時の毎フレームスキャン抑制)。 */
    private static final WeakHashMap<UUID, CacheEntry> CACHE = new WeakHashMap<>();

    private record CacheEntry(int tick, boolean hasHolder) {}

    public KnifeHolsterLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible() || player.isSpectator()) return;
        if (!hasHolderCached(player)) return;

        ItemStack stack = sharedStack;
        if (stack == null) {
            stack = new ItemStack(CustomEntityInit.KNIFE_LAUNCHER.get());
            sharedStack = stack;
        }

        pose.pushPose();
        // 体 (body) の変換を適用してから腰のオフセットで配置
        this.getParentModel().body.translateAndRotate(pose);
        // 腰右側: 右 0.22, 下 0.3 (モデル座標), 奥に少し
        pose.translate(0.22, 0.30, 0.10);
        // 斜めに傾けて"ケースを差している"ように
        pose.mulPose(Axis.ZP.rotationDegrees(45f));
        pose.mulPose(Axis.YP.rotationDegrees(90f));
        pose.scale(0.6f, 0.6f, 0.6f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
            stack,
            ItemDisplayContext.FIXED,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            pose,
            buffer,
            player.level(),
            player.getId()
        );
        pose.popPose();
    }

    private static boolean hasHolderCached(Player player) {
        int tick = player.tickCount;
        CacheEntry ce = CACHE.get(player.getUUID());
        if (ce != null && ce.tick == tick) return ce.hasHolder;
        boolean found = hasHolderDeep(player);
        CACHE.put(player.getUUID(), new CacheEntry(tick, found));
        return found;
    }

    private static boolean hasHolderDeep(Player player) {
        Item target = CustomEntityInit.KNIFE_LAUNCHER.get();

        // 1. メインインベントリ (hotbar + main + armor + offhand, size=41)
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (containsLauncher(s, target)) return true;
        }

        // 2. Curios スロット (存在すればすべて)
        try {
            boolean[] found = { false };
            top.theillusivec4.curios.api.CuriosApi.getCuriosHelper()
                .getEquippedCurios(player).ifPresent(h -> {
                    for (int i = 0; i < h.getSlots(); i++) {
                        ItemStack s = h.getStackInSlot(i);
                        if (containsLauncher(s, target)) { found[0] = true; return; }
                    }
                });
            if (found[0]) return true;
        } catch (Throwable ignored) {}

        return false;
    }

    /**
     * スタック自身が KnifeLauncher、または KnifeLauncher のバンドル (StoredKnives)
     * 内部にさらに格納されている場合に true。ネストは 1 段まで (無限再帰防止)。
     */
    private static boolean containsLauncher(ItemStack stack, Item target) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() == target) return true;
        // StoredKnives ListTag を持つ別スタック (例: KnifeLauncher 内部) も走査
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("StoredKnives", Tag.TAG_LIST)) {
                ListTag list = tag.getList("StoredKnives", Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    ItemStack inner = ItemStack.of(list.getCompound(i));
                    if (!inner.isEmpty() && inner.getItem() == target) return true;
                }
            }
        }
        return false;
    }
}
