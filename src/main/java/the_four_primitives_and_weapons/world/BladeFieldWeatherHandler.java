package the_four_primitives_and_weapons.world;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;
import the_four_primitives_and_weapons.entity.StabbedWeaponEntity;

/** 剣界の局地天候と、自然現象に長く晒された刺突武器のLv.3化。 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public final class BladeFieldWeatherHandler {
    private static final String FIRE_EXPOSURE = "BladeFieldFireExposure";
    private static final String FIRE_VENT_UNTIL = "BladeFieldFireVentUntil";

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || !level.dimension().equals(BladeDimensionTravelHandler.BLADE_FIELD)) return;
        long time = level.getGameTime();
        // 武器数が多い次元なので5秒ごとにまとめて処理する。
        if (time % 100 != 0) return;

        // 降水はディメンション共通だが、雨/雪の描画は各バイオームの設定に従う。
        if (time % 200 == 0) {
            boolean wet = level.players().stream().anyMatch(p -> {
                String biome = biome(level, p.blockPosition());
                return biome.equals("blade_field_water") || biome.equals("blade_field_ice")
                        || biome.equals("blade_field_thunder");
            });
            boolean thunder = level.players().stream()
                    .anyMatch(p -> biome(level, p.blockPosition()).equals("blade_field_thunder"));
            if (wet) level.setWeatherParameters(0, 1400, true, thunder);
        }

        Set<UUID> handled = new HashSet<>();
        for (var player : level.players()) {
            AABB area = player.getBoundingBox().inflate(48.0D, 24.0D, 48.0D);
            for (StabbedWeaponEntity stabbed : level.getEntitiesOfClass(StabbedWeaponEntity.class, area)) {
                if (!handled.add(stabbed.getUUID())) continue;
                processWeapon(level, stabbed, time);
            }
        }
    }

    private static void processWeapon(ServerLevel level, StabbedWeaponEntity stabbed, long time) {
        BlockPos pos = stabbed.blockPosition();
        String biome = biome(level, pos);
        ItemStack weapon = stabbed.getItem();
        if (weapon.isEmpty()) return;

        if (biome.equals("blade_field_fire")) {
            long until = stabbed.getPersistentData().getLong(FIRE_VENT_UNTIL);
            if (until > 0 && time >= until) {
                if (level.getBlockState(pos).is(Blocks.LAVA)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                stabbed.getPersistentData().remove(FIRE_VENT_UNTIL);
                stabbed.getPersistentData().remove(FIRE_EXPOSURE);
            } else if (level.getFluidState(pos).is(net.minecraft.tags.FluidTags.LAVA)) {
                int exposure = stabbed.getPersistentData().getInt(FIRE_EXPOSURE) + 1;
                stabbed.getPersistentData().putInt(FIRE_EXPOSURE, exposure);
                level.sendParticles(ParticleTypes.FLAME, stabbed.getX(), stabbed.getY() + 0.4, stabbed.getZ(), 4, .25, .3, .25, .01);
                if (exposure >= 2) empower(stabbed, weapon, ElementType.FIRE);
            } else if (level.random.nextInt(180) == 0 && level.getBlockState(pos).canBeReplaced()) {
                level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
                stabbed.getPersistentData().putLong(FIRE_VENT_UNTIL, time + 300);
                stabbed.getPersistentData().putInt(FIRE_EXPOSURE, 0);
            }
            return;
        }

        if (level.random.nextInt(240) != 0) return;
        switch (biome) {
            case "blade_field_thunder" -> {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    // ダメージ処理とは分離した視覚用落雷。座標を明示し、剣の真上へ表示する。
                    bolt.setVisualOnly(true);
                    bolt.moveTo(stabbed.getX(), stabbed.getY(), stabbed.getZ());
                    level.addFreshEntity(bolt);
                }
                // 描画距離やシェーダーに左右されても分かるよう、空から剣まで発光粒子を通す。
                for (int py = 0; py <= 24; py += 2)
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            stabbed.getX(), stabbed.getY() + py, stabbed.getZ(),
                            7, 0.16D, 1.0D, 0.16D, 0.08D);
                level.sendParticles(ParticleTypes.FLASH,
                        stabbed.getX(), stabbed.getY() + 0.8D, stabbed.getZ(),
                        3, 0.25D, 0.4D, 0.25D, 0.0D);
                level.playSound(null, stabbed.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.WEATHER, 4.0F, 0.9F + level.random.nextFloat() * 0.2F);
                level.playSound(null, stabbed.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                        SoundSource.WEATHER, 2.0F, 1.0F);
                empower(stabbed, weapon, ElementType.THUNDER);
            }
            case "blade_field_ice" -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, stabbed.getX(), stabbed.getY() + 1, stabbed.getZ(), 45, 1.2, 1.5, 1.2, .02);
                empower(stabbed, weapon, ElementType.ICE);
            }
            case "blade_field_water" -> {
                level.sendParticles(ParticleTypes.SPLASH, stabbed.getX(), stabbed.getY() + 1, stabbed.getZ(), 55, 1.4, 1.8, 1.4, .08);
                empower(stabbed, weapon, ElementType.WATER);
            }
            case "blade_field_wind" -> {
                level.sendParticles(ParticleTypes.CLOUD, stabbed.getX(), stabbed.getY() + .8, stabbed.getZ(), 35, 2.0, .5, 2.0, .16);
                empower(stabbed, weapon, ElementType.WIND);
            }
            case "blade_field_blood" -> {
                level.sendParticles(ParticleTypes.DRIPPING_DRIPSTONE_LAVA, stabbed.getX(), stabbed.getY() + 2, stabbed.getZ(), 25, 1.0, 1.5, 1.0, .01);
                empower(stabbed, weapon, ElementType.BLOOD);
            }
            case "blade_field_corrosion" -> {
                level.sendParticles(ParticleTypes.WITCH, stabbed.getX(), stabbed.getY() + 1, stabbed.getZ(), 40, 1.1, 1.4, 1.1, .03);
                empower(stabbed, weapon, ElementType.CORROSION);
            }
        }
    }

    private static void empower(StabbedWeaponEntity stabbed, ItemStack weapon, ElementType element) {
        ElementalDamageUtils.setElement(weapon, element, 3);
        stabbed.setItem(weapon);
    }

    private static String biome(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("blade_field");
    }

    private BladeFieldWeatherHandler() {}
}
