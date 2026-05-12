package the_four_primitives_and_weapons.procedures;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.BlockPos;

import org.joml.Vector3f;

import java.util.List;

/**
 * arrow_1 (arrowkill) MobEffect の毎 tick 処理。
 *
 * 元の意図: effect 装着者の周囲に来た "他人の" 飛翔体
 * (Arrow / SpectralArrow / Trident / Fireball / Snowball / Egg) を撃ち落とす結界効果。
 *
 * MCreator 生成の旧版は「自分の矢」判定で {@code Math.pow(diff, 0.4)} (≠二乗) という壊れた
 * 距離計算をしており、装着者自身が放った矢/トライデントまで誤爆で kill されていた
 * (= 「クロスボウやトライデントの entity が出てこない」症状の原因)。
 *
 * 修正版: 距離判定を捨て、{@link Projectile#getOwner()} で
 * owner == 装着者なら撃ち落とさない、で本来の意図を保ったまま誤爆を消す。
 */
public class Arrow1ehuekutogaYouXiaoShinoteitukuProcedure {

	/** 撃ち落とす半径 (block). 旧版の最大スキャン範囲 inflate(5/2)=2.5 を踏襲。 */
	private static final double KILL_RADIUS = 2.5;

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null) return;

		Vec3 center = new Vec3(x, y, z);
		AABB box = new AABB(center, center).inflate(KILL_RADIUS);
		List<Entity> found = world.getEntitiesOfClass(Entity.class, box, e -> true);

		for (Entity it : found) {
			if (!isHostileProjectile(it)) continue;
			// 既に撃ち落とし処理済み → 再処理しない (旧版の Check2 と同義の重複防止)
			if (it.getPersistentData().getBoolean("Check2")) continue;
			// 自分が放った飛翔体は除外 — クロスボウの矢/トライデントが自爆消滅しないように
			if (it instanceof Projectile p && p.getOwner() == entity) continue;

			it.getPersistentData().putBoolean("Check2", true);

			// SE 2連: 着弾風 (anvil.land) + 撃ち落とし時の "shoot" 音
			if (world instanceof Level level && !level.isClientSide()) {
				level.playSound(null,
					BlockPos.containing(it.getX(), it.getY(), it.getZ()),
					ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.anvil.land")),
					SoundSource.VOICE, 1, 1);
				level.playSound(null,
					BlockPos.containing(x, y, z),
					ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.drowned.shoot")),
					SoundSource.VOICE, 1, 1);
			}

			// パーティクル: sweep_attack + 暗赤 dust (旧版の見た目を維持)
			world.addParticle(ParticleTypes.SWEEP_ATTACK,
				it.getX(),
				it.getY() + Mth.nextDouble(RandomSource.create(), -0.1, 0.5),
				it.getZ(), 0, 1, 0);
			if (world instanceof ServerLevel sl) {
				DustParticleOptions dust = new DustParticleOptions(
					new Vector3f(0.639f, 0.169f, 0.169f), 1.0f);
				sl.sendParticles(dust,
					it.getX(), it.getY() + 0.5, it.getZ(),
					10, 0.3, 0.1, 0.3, 0.1);
			}

			it.discard();
		}
	}

	private static boolean isHostileProjectile(Entity e) {
		return e instanceof Arrow
			|| e instanceof SpectralArrow
			|| e instanceof ThrownTrident
			|| e instanceof LargeFireball
			|| e instanceof DragonFireball
			|| e instanceof Snowball
			|| e instanceof ThrownEgg
			|| e instanceof SmallFireball;
	}
}
