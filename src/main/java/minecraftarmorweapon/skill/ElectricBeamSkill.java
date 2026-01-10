package minecraftarmorweapon.skill;

import minecraftarmorweapon.block.ElectricConductBlock;
import minecraftarmorweapon.client.ElectricBeamRenderer;
import minecraftarmorweapon.damage.ElementType;
import minecraftarmorweapon.damage.ElectricElementDamageHandler;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * ELECTRIC ビームスキル（瞬間到達）
 */
public final class ElectricBeamSkill {

    private ElectricBeamSkill() {}

    public static void fire(Player player) {
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();

        double maxDistance = 50.0;
        double damage = 8.0;

        RayTraceResult result = world.rayTrace(
                start,
                direction,
                maxDistance,
                FluidCollisionMode.NEVER,
                true,
                0.3,
                entity -> entity != player
        );

        Location end = (result != null)
                ? result.getHitPosition().toLocation(world)
                : start.clone().add(direction.multiply(maxDistance));

        // 描画（瞬間）
        ElectricBeamRenderer.render(world, start, end);

        if (result == null) return;

        // エンティティ命中
        if (result.getHitEntity() instanceof LivingEntity target) {
            ElectricElementDamageHandler.apply(
                    target,
                    damage,
                    ElementType.ELECTRIC
            );
        }

        // ブロック命中
        Block hitBlock = result.getHitBlock();
        if (hitBlock == null) return;

        if (ElectricConductBlock.isConductive(hitBlock)) {
            ElectricConductBlock.conduct(hitBlock, damage);
        }
    }
}
