package the_four_primitives_and_weapons.damage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import the_four_primitives_and_weapons.init.MawExtraAttributes;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModDamageTypes;
import the_four_primitives_and_weapons.util.CuriosScabbardHelper;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 属性/呪を持つ武器の持ち歩きデバフ。
 *
 * 対象:
 * - 直接持っている属性武器
 * - Feyn:"sigiled" ではない鞘に納刀された属性武器
 *
 * 封付き鞘は中の武器の属性/呪を遮断する。
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class ElementalCarryDebuffHandler {

    private static final UUID CURSED_HEALTH_MODIFIER_UUID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CURSED_ATTACK_MODIFIER_UUID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CORROSION_ARMOR_MODIFIER_UUID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ICE_MOVEMENT_MODIFIER_UUID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ELECTRIC_ATTACK_SPEED_MODIFIER_UUID =
            UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID THUNDER_TOUGHNESS_MODIFIER_UUID =
            UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID DARK_ATTACK_DAMAGE_MODIFIER_UUID =
            UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID SOUL_HEALTH_MODIFIER_UUID =
            UUID.fromString("88888888-8888-8888-8888-888888888888");

    private static final int FIRE_BACKLASH_COOLDOWN_TICKS = 60;
    private static final int ELECTRIC_CONDUCTION_INTERVAL_TICKS = 60;
    private static final int WATER_AIR_DRAIN_INTERVAL = 10;
    private static final int BLOOD_DRAIN_INTERVAL_TICKS = 100;
    private static final int VISUAL_REFRESH_INTERVAL = 20;
    private static final int ERASURE_INSTABILITY_INTERVAL_TICKS = 80;
    private static final int HOLY_UNDEAD_LURE_INTERVAL_TICKS = 20;

    private static final Map<UUID, Long> LAST_FIRE_BACKLASH_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_ELECTRIC_CONDUCTION_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_BLOOD_DRAIN_TICK = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player == null || player.level().isClientSide) return;

        CarryState state = collectState(player);

        int electricLevel = effectiveDebuffLevel(player, state, ElementType.ELECTRIC);
        int darkLevel = effectiveDebuffLevel(player, state, ElementType.DARK);
        int holyLevel = effectiveDebuffLevel(player, state, ElementType.HOLY);
        int soulLevel = effectiveDebuffLevel(player, state, ElementType.SOUL);
        int soulFireLevel = effectiveDebuffLevel(player, state, ElementType.SOUL_FIRE);
        int soulLikeLevel = Math.max(soulLevel, soulFireLevel);
        boolean cursed = state.cursed && getCurseAptitude(player) < 1.0D;
        boolean changed = syncCursedModifiers(player, cursed);
        changed |= syncIceModifier(player, effectiveDebuffLevel(player, state, ElementType.ICE));
        changed |= syncElectricModifier(player, electricLevel);
        changed |= syncCorrosionModifier(player, effectiveDebuffLevel(player, state, ElementType.CORROSION));
        changed |= syncThunderModifier(player, effectiveDebuffLevel(player, state, ElementType.THUNDER));
        changed |= syncDarkModifier(player, darkLevel);
        changed |= syncSoulModifier(player, soulLikeLevel);

        applyWindExhaustion(player, effectiveDebuffLevel(player, state, ElementType.WIND));
        applyElectricConductionDamage(player, electricLevel);
        applyWaterAirDrain(player, effectiveDebuffLevel(player, state, ElementType.WATER));
        applyHolyExhaustion(player, holyLevel);
        lureUndeadToHolyBearer(player, holyLevel);
        applyDarkShroud(player, darkLevel);
        applyBloodDrain(player, effectiveDebuffLevel(player, state, ElementType.BLOOD));
        applyErasureInstability(player, effectiveDebuffLevel(player, state, ElementType.ERASURE));
        applySoulEcho(player, soulLikeLevel);

        if (changed && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundUpdateAttributesPacket(
                    player.getId(), player.getAttributes().getSyncableAttributes()
            ));
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide || event.getAmount() <= 0.0F) return;

        CarryState state = collectState(player);
        int fireLevel = effectiveDebuffLevel(player, state, ElementType.FIRE);
        int soulFireLevel = effectiveDebuffLevel(player, state, ElementType.SOUL_FIRE);
        if (fireLevel <= 0 && soulFireLevel <= 0) return;

        long gameTime = player.level().getGameTime();
        Long lastTick = LAST_FIRE_BACKLASH_TICK.get(player.getUUID());
        if (lastTick != null && gameTime - lastTick < FIRE_BACKLASH_COOLDOWN_TICKS) return;

        LAST_FIRE_BACKLASH_TICK.put(player.getUUID(), gameTime);
        int level = Math.max(fireLevel, soulFireLevel);
        int duration = Math.min(70, 20 + level * 2);
        float damagePerTick = Math.min(0.045F, 0.006F * Math.max(1, level));
        ElementType backlashType = soulFireLevel > 0 ? ElementType.SOUL_FIRE : ElementType.FIRE;
        ElementalDoTHandler.apply(player, duration, damagePerTick, backlashType);
        if (backlashType == ElementType.SOUL_FIRE) {
            SoulFireHandler.setSoulFire(player, duration);
        }
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide || event.getAmount() <= 0.0F) return;

        CarryState state = collectState(player);
        int miasmaLevel = effectiveDebuffLevel(player, state, ElementType.MIASMA);
        if (miasmaLevel <= 0) return;

        float reduction = Math.min(0.8F, 0.15F + 0.03F * miasmaLevel);
        event.setAmount(event.getAmount() * (1.0F - reduction));
    }

    private static boolean syncCursedModifiers(Player player, boolean cursed) {
        boolean changed = false;
        if (cursed) {
            double maxHealth = player.getAttributeBaseValue(Attributes.MAX_HEALTH);
            changed |= syncModifier(player, Attributes.MAX_HEALTH, CURSED_HEALTH_MODIFIER_UUID,
                    "Cursed Health Down", -maxHealth * 0.2, AttributeModifier.Operation.ADDITION);

            double attackDamage = player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
            changed |= syncModifier(player, Attributes.ATTACK_DAMAGE, CURSED_ATTACK_MODIFIER_UUID,
                    "Cursed Attack Up", attackDamage * 0.14, AttributeModifier.Operation.ADDITION);
        } else {
            changed |= removeModifier(player, Attributes.MAX_HEALTH, CURSED_HEALTH_MODIFIER_UUID);
            changed |= removeModifier(player, Attributes.ATTACK_DAMAGE, CURSED_ATTACK_MODIFIER_UUID);
        }
        return changed;
    }

    private static boolean syncIceModifier(Player player, int level) {
        if (level <= 0) {
            return removeModifier(player, Attributes.MOVEMENT_SPEED, ICE_MOVEMENT_MODIFIER_UUID);
        }

        double speedDown = -Math.min(0.35, 0.04 + level * 0.01);
        return syncModifier(player, Attributes.MOVEMENT_SPEED, ICE_MOVEMENT_MODIFIER_UUID,
                "Ice Movement Down", speedDown, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static boolean syncElectricModifier(Player player, int level) {
        if (level <= 0) {
            return removeModifier(player, Attributes.ATTACK_SPEED, ELECTRIC_ATTACK_SPEED_MODIFIER_UUID);
        }

        double attackSpeedDown = -Math.min(0.35, 0.04 + level * 0.0125);
        return syncModifier(player, Attributes.ATTACK_SPEED, ELECTRIC_ATTACK_SPEED_MODIFIER_UUID,
                "Electric Attack Speed Down", attackSpeedDown, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static boolean syncCorrosionModifier(Player player, int level) {
        if (level <= 0) {
            return removeModifier(player, Attributes.ARMOR, CORROSION_ARMOR_MODIFIER_UUID);
        }

        double armorDown = -Math.min(12.0, 1.0 + level * 0.35);
        return syncModifier(player, Attributes.ARMOR, CORROSION_ARMOR_MODIFIER_UUID,
                "Corrosion Defense Down", armorDown, AttributeModifier.Operation.ADDITION);
    }

    private static boolean syncThunderModifier(Player player, int level) {
        if (level <= 0) {
            return removeModifier(player, Attributes.ARMOR_TOUGHNESS, THUNDER_TOUGHNESS_MODIFIER_UUID);
        }

        double toughnessDown = -Math.min(8.0, 0.5 + level * 0.3);
        return syncModifier(player, Attributes.ARMOR_TOUGHNESS, THUNDER_TOUGHNESS_MODIFIER_UUID,
                "Thunder Toughness Down", toughnessDown, AttributeModifier.Operation.ADDITION);
    }

    private static boolean syncDarkModifier(Player player, int level) {
        if (level <= 0) {
            return removeModifier(player, Attributes.ATTACK_DAMAGE, DARK_ATTACK_DAMAGE_MODIFIER_UUID);
        }

        double attackDown = -Math.min(0.3, 0.03 + level * 0.01);
        return syncModifier(player, Attributes.ATTACK_DAMAGE, DARK_ATTACK_DAMAGE_MODIFIER_UUID,
                "Dark Attack Focus Down", attackDown, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static boolean syncSoulModifier(Player player, int level) {
        if (level <= 0) {
            return removeModifier(player, Attributes.MAX_HEALTH, SOUL_HEALTH_MODIFIER_UUID);
        }

        double healthDown = -Math.min(6.0, 1.0 + level * 0.25);
        return syncModifier(player, Attributes.MAX_HEALTH, SOUL_HEALTH_MODIFIER_UUID,
                "Soul Max Health Down", healthDown, AttributeModifier.Operation.ADDITION);
    }

    private static void applyWindExhaustion(Player player, int level) {
        if (level <= 0 || player.isCreative() || player.isSpectator()) return;

        float exhaustion = Math.min(0.08F, 0.003F * Math.max(1, level));
        player.causeFoodExhaustion(exhaustion);
    }

    private static void applyWaterAirDrain(Player player, int level) {
        if (level <= 0 || player.isCreative() || player.isSpectator()) return;
        if (player.tickCount % WATER_AIR_DRAIN_INTERVAL != 0) return;
        if (!player.isEyeInFluid(FluidTags.WATER)) return;

        int extraDrain = Math.min(8, 1 + Math.max(1, level) / 2);
        player.setAirSupply(Math.max(-20, player.getAirSupply() - extraDrain));
    }

    private static void applyElectricConductionDamage(Player player, int level) {
        if (level <= 0 || player.isCreative() || player.isSpectator()) return;

        int conductiveArmorPieces = ElectricElementDamageHandler.countConductiveArmorPieces(player);
        if (conductiveArmorPieces <= 0) return;

        long gameTime = player.level().getGameTime();
        Long lastTick = LAST_ELECTRIC_CONDUCTION_TICK.get(player.getUUID());
        if (lastTick != null && gameTime - lastTick < ELECTRIC_CONDUCTION_INTERVAL_TICKS) return;

        LAST_ELECTRIC_CONDUCTION_TICK.put(player.getUUID(), gameTime);
        float damage = Math.min(6.0F, 0.25F + 0.08F * Math.max(1, level) * conductiveArmorPieces);
        DamageSource source = ModDamageSources.of(player.level(), TheFourPrimitivesAndWeaponsModDamageTypes.ELECTRIC);

        if (player.hurt(source, damage) && player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                    6 + conductiveArmorPieces * 3, 0.35, 0.45, 0.35, 0.08);
        }
    }

    private static void applyHolyExhaustion(Player player, int level) {
        if (level <= 0 || player.isCreative() || player.isSpectator()) return;

        float exhaustion = Math.min(0.10F, 0.004F * Math.max(1, level));
        player.causeFoodExhaustion(exhaustion);
    }

    private static void lureUndeadToHolyBearer(Player player, int level) {
        if (level <= 0 || player.isCreative() || player.isSpectator()) return;
        if (player.tickCount % HOLY_UNDEAD_LURE_INTERVAL_TICKS != 0) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        double radius = Math.min(48.0D, 18.0D + Math.max(1, level) * 3.0D);
        double radiusSqr = radius * radius;
        for (Mob mob : serverLevel.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius),
                mob -> isUndeadMob(mob) && mob.isAlive() && mob.canAttack(player))) {
            double distanceSqr = mob.distanceToSqr(player);
            if (distanceSqr > radiusSqr) continue;
            if (!shouldLureUndead(mob, player, distanceSqr)) continue;

            mob.setTarget(player);
            mob.getNavigation().moveTo(player, 1.0D);
        }
    }

    private static boolean isUndeadMob(Mob mob) {
        return mob.getMobType() == MobType.UNDEAD;
    }

    private static boolean shouldLureUndead(Mob mob, Player player, double distanceSqr) {
        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget == null || !currentTarget.isAlive()) return true;
        if (currentTarget == player) return true;
        if (currentTarget instanceof Player) return false;
        return distanceSqr + 16.0D < mob.distanceToSqr(currentTarget);
    }

    private static void applyDarkShroud(Player player, int level) {
        if (level <= 0) return;
        if (player.tickCount % VISUAL_REFRESH_INTERVAL != 0) return;

        if (player.level() instanceof ServerLevel serverLevel) {
            double mid = player.getY() + player.getBbHeight() * 0.6;
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    player.getX(), mid, player.getZ(),
                    Math.min(12, 3 + level), 0.28, 0.35, 0.28, 0.02);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    player.getX(), mid, player.getZ(),
                    Math.min(10, 2 + level), 0.35, 0.35, 0.35, 0.03);
        }
    }

    private static void applyBloodDrain(Player player, int level) {
        if (level <= 0 || player.isCreative() || player.isSpectator()) return;

        long gameTime = player.level().getGameTime();
        Long lastTick = LAST_BLOOD_DRAIN_TICK.get(player.getUUID());
        if (lastTick != null && gameTime - lastTick < BLOOD_DRAIN_INTERVAL_TICKS) return;

        LAST_BLOOD_DRAIN_TICK.put(player.getUUID(), gameTime);
        int duration = Math.min(50, 20 + level);
        float damagePerTick = Math.min(0.025F, 0.003F * Math.max(1, level));
        ElementalDoTHandler.apply(player, duration, damagePerTick, ElementType.BLOOD);

        // 出血の原因が分かるように血属性の粒子を出す (闇/消滅/魂と同じ視覚フィードバック)。
        if (player.level() instanceof ServerLevel serverLevel) {
            ElementalParticles.spawn(serverLevel, ElementType.BLOOD,
                    player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                    Math.min(10, 3 + level / 2));
        }
    }

    private static void applyErasureInstability(Player player, int level) {
        if (level <= 0) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (player.tickCount % ERASURE_INSTABILITY_INTERVAL_TICKS != 0) return;

        long seed = player.level().getGameTime() ^ player.getUUID().getLeastSignificantBits();
        double angle = (seed & 1023L) * (Math.PI * 2.0D / 1024.0D);
        double strength = Math.min(0.25D, 0.035D + level * 0.005D);
        player.setDeltaMovement(player.getDeltaMovement().add(
                Math.cos(angle) * strength,
                0.0D,
                Math.sin(angle) * strength));
        player.hurtMarked = true;

        if (player.level() instanceof ServerLevel serverLevel) {
            double mid = player.getY() + player.getBbHeight() * 0.5;
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    player.getX(), mid, player.getZ(),
                    Math.min(18, 5 + level), 0.35, 0.45, 0.35, 0.04);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    player.getX(), mid, player.getZ(),
                    Math.min(8, 2 + level / 2), 0.25, 0.3, 0.25, 0.08);
        }
    }

    private static void applySoulEcho(Player player, int level) {
        if (level <= 0) return;
        if (player.tickCount % VISUAL_REFRESH_INTERVAL != 0) return;

        if (player.level() instanceof ServerLevel serverLevel) {
            double mid = player.getY() + player.getBbHeight() * 0.55;
            serverLevel.sendParticles(ParticleTypes.SOUL,
                    player.getX(), mid, player.getZ(),
                    Math.min(12, 3 + level), 0.25, 0.4, 0.25, 0.02);
        }
    }

    private static CarryState collectState(Player player) {
        CarryState state = new CarryState();

        collectFromDirectWeapon(state, player.getMainHandItem());
        collectFromDirectWeapon(state, player.getOffhandItem());

        for (CuriosScabbardHelper.DrawableWeaponInfo info : CuriosScabbardHelper.findAllLoadedScabbards(player)) {
            ItemStack scabbard = info.scabbardStack;
            if (scabbard.isEmpty() || isSealedScabbard(scabbard)) continue;
            collectFromDirectWeapon(state, info.weaponStack);
        }
        collectFromAllCurioScabbards(state, player);

        return state;
    }

    private static void collectFromAllCurioScabbards(CarryState state, Player player) {
        try {
            CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler ->
                    handler.getCurios().values().forEach(stacksHandler -> {
                        collectFromCurioStacks(state, stacksHandler.getStacks());
                        if (stacksHandler.hasCosmetic()) {
                            collectFromCurioStacks(state, stacksHandler.getCosmeticStacks());
                        }
                    }));
        } catch (Exception ignored) {
            // Curios未ロード/slot未生成の環境では、通常インベントリ側の判定だけ続ける。
        }
    }

    private static void collectFromCurioStacks(CarryState state, IDynamicStackHandler stacks) {
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);
            if (!CuriosScabbardHelper.isScabbard(stack)
                    || !CuriosScabbardHelper.hasStoredWeapon(stack)
                    || isSealedScabbard(stack)) {
                continue;
            }

            collectFromDirectWeapon(state, CuriosScabbardHelper.extractWeaponFromScabbard(stack));
        }
    }

    private static void collectFromDirectWeapon(CarryState state, ItemStack stack) {
        if (stack.isEmpty()) return;

        if (CuriosScabbardHelper.isScabbard(stack)) {
            if (!isSealedScabbard(stack) && CuriosScabbardHelper.hasStoredWeapon(stack)) {
                collectFromDirectWeapon(state, CuriosScabbardHelper.extractWeaponFromScabbard(stack));
            }
            return;
        }

        if (hasCursedFeyn(stack)) {
            state.cursed = true;
        }

        if (ElementalDamageUtils.isOneToOneFireSoul(stack)) {
            state.addElement(ElementType.SOUL_FIRE, Math.max(1, ElementalDamageUtils.getEffectiveElementLevel(stack)));
            return;
        }

        ElementType type = ElementalDamageUtils.getElementType(stack);
        if (type == ElementType.NONE) return;

        int level = Math.max(1, ElementalDamageUtils.getElementLevel(stack));
        state.addElement(type, level);

        ElementType secondaryType = ElementalDamageUtils.getSecondaryElementType(stack);
        if (secondaryType != ElementType.NONE) {
            int secondaryLevel = Math.max(1, ElementalDamageUtils.getSecondaryElementLevel(stack));
            state.addElement(secondaryType, secondaryLevel);
        }
    }

    private static boolean hasCursedFeyn(ItemStack stack) {
        return stack.hasTag() && "cursed".equals(stack.getTag().getString("Feyn"));
    }

    private static boolean isSealedScabbard(ItemStack stack) {
        if (!stack.hasTag()) return false;
        CompoundTag tag = stack.getTag();
        return tag != null && "sigiled".equals(tag.getString("Feyn"));
    }

    private static int effectiveDebuffLevel(Player player, CarryState state, ElementType type) {
        int level = state.level(type);
        if (level <= 0) return 0;

        double aptitude = getAptitude(player, type);
        double effective = level - aptitude;
        if (effective <= 0.0D) return 0;
        return Math.max(1, (int) Math.ceil(effective));
    }

    private static double getAptitude(Player player, ElementType type) {
        Attribute attribute = MawExtraAttributes.getAptitudeAttribute(type);
        return getAttributeValue(player, attribute);
    }

    private static double getCurseAptitude(Player player) {
        return getAttributeValue(player, MawExtraAttributes.CURSE_APTITUDE.get());
    }

    private static double getAttributeValue(Player player, Attribute attribute) {
        if (attribute == null) return 0.0D;
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return 0.0D;
        return Math.max(0.0D, instance.getValue());
    }

    private static boolean syncModifier(Player player, Attribute attribute, UUID uuid, String name,
                                        double value, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return false;

        AttributeModifier existing = instance.getModifier(uuid);
        if (existing != null
                && Math.abs(existing.getAmount() - value) < 0.0001D
                && existing.getOperation() == operation) {
            return false;
        }

        if (existing != null) {
            instance.removeModifier(existing);
        }

        AttributeModifier modifier = new AttributeModifier(uuid, name, value, operation);
        instance.addPermanentModifier(modifier);

        if (attribute == Attributes.MAX_HEALTH) {
            float health = player.getHealth();
            float maxHealth = (float) instance.getValue();
            if (health > maxHealth) {
                player.setHealth(maxHealth);
            }
        }

        return true;
    }

    private static boolean removeModifier(Player player, Attribute attribute, UUID uuid) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return false;

        AttributeModifier modifier = instance.getModifier(uuid);
        if (modifier != null) {
            instance.removeModifier(modifier);
            return true;
        }
        return false;
    }

    private static class CarryState {
        private boolean cursed;
        private final EnumMap<ElementType, Integer> elementLevels = new EnumMap<>(ElementType.class);

        private void addElement(ElementType type, int level) {
            elementLevels.merge(type, level, Math::max);
        }

        private int level(ElementType type) {
            return elementLevels.getOrDefault(type, 0);
        }
    }
}
