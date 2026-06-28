package the_four_primitives_and_weapons.skill;

import the_four_primitives_and_weapons.damage.ElectricElementDamageHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * 直刀の固有スキル: 貫通する直線の「雷ランス」。
 * プレイヤーの視線方向へ一直線に極太の雷を放ち、 線上の敵を全て貫いて感電させる。
 * 壁で止まり、 着弾点に落雷風の爆ぜを出す。 ( 旧: 地面を這う地味なビーム を刷新 )
 */
public final class ElectricBeamSkill {

	private ElectricBeamSkill() {}

	private static final double MAX_DISTANCE = 32.0; // 最大射程
	private static final double HIT_RADIUS = 1.6;    // 線の周りの当たり半径 ( 貫通 )
	private static final float DAMAGE = 12.0f;

	public static void fire(Player player) {
		if (player.level().isClientSide()) return;
		ServerLevel level = (ServerLevel) player.level();

		Vec3 origin = player.getEyePosition();
		Vec3 dir = player.getLookAngle().normalize();

		// 壁で止める ( ブロックに当たるまで )
		Vec3 maxEnd = origin.add(dir.scale(MAX_DISTANCE));
		BlockHitResult bhr = level.clip(new ClipContext(origin, maxEnd,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 end = bhr.getType() != HitResult.Type.MISS ? bhr.getLocation() : maxEnd;
		double reach = end.subtract(origin).length();

		// 発射音
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.4f, 1.1f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.3f);

		// 極太の雷を描画 ( 芯 = END_ROD、 周囲 = ELECTRIC_SPARK のジグザグ )
		drawLance(level, origin, dir, reach);

		// 貫通ダメージ: 線の周囲 HIT_RADIUS 内の生物を全てヒット
		Set<Integer> damaged = new HashSet<>();
		AABB region = new AABB(origin, end).inflate(HIT_RADIUS + 0.5);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, region,
				e -> e != player && e.isAlive())) {
			Vec3 center = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
			Vec3 rel = center.subtract(origin);
			double proj = rel.dot(dir);                 // 線方向の射影
			if (proj < -0.5 || proj > reach + 0.5) continue;
			double perp = rel.subtract(dir.scale(proj)).length(); // 線までの垂直距離
			if (perp > HIT_RADIUS) continue;
			if (!damaged.add(entity.getId())) continue;
			entity.invulnerableTime = 0; // 直前の被弾無敵で弾かれないように
			ElectricElementDamageHandler.applyElectricDamage(entity, DAMAGE, player, 2);
			// 軽いノックバック ( 進行方向へ )
			entity.setDeltaMovement(entity.getDeltaMovement().add(dir.x * 0.4, 0.25, dir.z * 0.4));
			entity.hurtMarked = true;
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
					center.x, center.y, center.z, 16, 0.3, 0.3, 0.3, 0.08);
		}

		// 着弾点に落雷風の爆ぜ
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 40, 0.4, 0.4, 0.4, 0.15);
		level.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 12, 0.2, 0.2, 0.2, 0.05);
	}

	/** 視線方向に極太の雷ランスを描く。 芯 + 周囲のジグザグスパーク。 */
	private static void drawLance(ServerLevel level, Vec3 origin, Vec3 dir, double reach) {
		var rng = level.random;
		// 直交基底 ( 線の周りに散らすため )
		Vec3 side = Math.abs(dir.y) > 0.99
				? new Vec3(1, 0, 0)
				: new Vec3(-dir.z, 0, dir.x).normalize();
		Vec3 up = dir.cross(side).normalize();

		double step = 0.5;
		for (double d = 0; d <= reach; d += step) {
			Vec3 p = origin.add(dir.scale(d));
			// 芯
			level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
			// 周囲のジグザグスパーク ( 太さを出す )
			double r = 0.25 + rng.nextDouble() * 0.35;
			double ang = rng.nextDouble() * Math.PI * 2;
			Vec3 off = side.scale(Math.cos(ang) * r).add(up.scale(Math.sin(ang) * r));
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
					p.x + off.x, p.y + off.y, p.z + off.z, 2, 0.05, 0.05, 0.05, 0.02);
		}
	}
}
