package minecraftarmorweapon.client;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.item.RecrossHookshotItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Re:Cross Hookshot 装備中のレティクル下に届く/届かないインジケーターを描画.
 *
 * 持っているアイテムから {@link RecrossHookshotItem#maxRange} を取って ray-cast 距離を決める →
 * Long (80) と Short (60) で表示距離が変わる.
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID, value = Dist.CLIENT)
public class RecrossReticleOverlay {

    public static final int BAR_WIDTH = 16;
    public static final int BAR_HEIGHT = 1;
    public static final int BAR_OFFSET_Y = 8;
    public static final int COLOR_REACHABLE = 0xFF44FF44;
    public static final int COLOR_OUT_OF_RANGE = 0xFFFF4444;

    private static long lastRaycastNanos = 0;
    private static boolean cachedCanHook = false;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui) return;
        Player p = mc.player;

        RecrossHookshotItem held = getHookshotItem(p);
        if (held == null) return;
        double maxReach = held.maxRange;

        long now = System.nanoTime();
        boolean canHook;
        if (now - lastRaycastNanos > 50_000_000L) {
            Vec3 eye = p.getEyePosition(event.getPartialTick());
            Vec3 dir = p.getViewVector(event.getPartialTick());
            Vec3 end = eye.add(dir.scale(maxReach));
            BlockHitResult bhr = mc.level.clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            canHook = bhr.getType() != HitResult.Type.MISS;

            if (!canHook) {
                AABB box = new AABB(eye, end).inflate(0.5);
                for (Entity e : mc.level.getEntities(p, box, en -> en != p)) {
                    Vec3 toEnt = e.position().subtract(eye);
                    double along = toEnt.dot(dir);
                    if (along <= 0 || along > maxReach) continue;
                    Vec3 closest = eye.add(dir.scale(along));
                    if (closest.distanceToSqr(e.position()) < 1.5) {
                        canHook = true;
                        break;
                    }
                }
            }
            cachedCanHook = canHook;
            lastRaycastNanos = now;
        } else {
            canHook = cachedCanHook;
        }

        GuiGraphics g = event.getGuiGraphics();
        int screenW = g.guiWidth();
        int screenH = g.guiHeight();
        int x1 = screenW / 2 - BAR_WIDTH / 2;
        int y1 = screenH / 2 + BAR_OFFSET_Y;
        int x2 = x1 + BAR_WIDTH;
        int y2 = y1 + BAR_HEIGHT;
        int color = canHook ? COLOR_REACHABLE : COLOR_OUT_OF_RANGE;
        g.fill(x1, y1, x2, y2, color);
    }

    private static RecrossHookshotItem getHookshotItem(Player p) {
        if (p.getMainHandItem().getItem() instanceof RecrossHookshotItem h) return h;
        if (p.getOffhandItem().getItem() instanceof RecrossHookshotItem h) return h;
        return null;
    }
}
