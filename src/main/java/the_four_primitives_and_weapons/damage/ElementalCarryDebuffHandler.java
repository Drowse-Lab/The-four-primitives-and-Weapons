package the_four_primitives_and_weapons.damage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

    private static final int FIRE_BACKLASH_COOLDOWN_TICKS = 60;
    private static final int WATER_AIR_DRAIN_INTERVAL = 10;
    private static final int BLOOD_DRAIN_INTERVAL_TICKS = 100;
    private static final int EFFECT_REFRESH_INTERVAL = 20;

    private static final Map<UUID, Long> LAST_FIRE_BACKLASH_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_BLOOD_DRAIN_TICK = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player == null || player.level().isClientSide) return;

        CarryState state = collectState(player);

        boolean changed = syncCursedModifiers(player, state.cursed);
        changed |= syncIceModifier(player, state.level(ElementType.ICE));
        changed |= syncElectricModifier(player, state.level(ElementType.ELECTRIC));
        changed |= syncCorrosionModifier(player, state.level(ElementType.CORROSION));
        changed |= syncThunderModifier(player, state.level(ElementType.THUNDER));

        applyWindExhaustion(player, state.level(ElementType.WIND));
        applyWaterAirDrain(player, state.level(ElementType.WATER));
        applyHolyGlow(player, state.level(ElementType.HOLY));
        applyDarkness(player, state.level(ElementType.DARK));
        applyBloodDrain(player, state.level(ElementType.BLOOD));
        applyErrorInstability(player, state.level(ElementType.ERROR));

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

        int fireLevel = collectState(player).level(ElementType.FIRE);
        if (fireLevel <= 0) return;

        long gameTime = player.level().getGameTime();
        Long lastTick = LAST_FIRE_BACKLASH_TICK.get(player.getUUID());
        if (lastTick != null && gameTime - lastTick < FIRE_BACKLASH_COOLDOWN_TICKS) return;

        LAST_FIRE_BACKLASH_TICK.put(player.getUUID(), gameTime);
        int duration = Math.min(60, 20 + fireLevel * 2);
        float damagePerTick = Math.min(0.04F, 0.006F * Math.max(1, fireLevel));
        ElementalDoTHandler.apply(player, duration, damagePerTick, ElementType.FIRE);
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide || event.getAmount() <= 0.0F) return;

        int miasmaLevel = collectState(player).level(ElementType.MIASMA);
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

    private static void applyHolyGlow(Player player, int level) {
        if (level <= 0) return;
        if (player.tickCount % EFFECT_REFRESH_INTERVAL != 0) return;

        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false));
    }

    private static void applyDarkness(Player player, int level) {
        if (level <= 0) return;
        if (player.tickCount % EFFECT_REFRESH_INTERVAL != 0) return;

        int duration = Math.min(100, 40 + level * 2);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, duration, 0, true, false));
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
    }

    private static void applyErrorInstability(Player player, int level) {
        if (level <= 0) return;
        if (player.tickCount % 80 != 0) return;

        int duration = Math.min(160, 60 + level * 5);
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, 0, true, false));
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

        if (hasCursedFeyn(stack)) {
            state.cursed = true;
        }

        ElementType type = ElementalDamageUtils.getElementType(stack);
        if (type == ElementType.NONE) return;

        int level = Math.max(1, ElementalDamageUtils.getElementLevel(stack));
        state.addElement(type, level);
    }

    private static boolean hasCursedFeyn(ItemStack stack) {
        return stack.hasTag() && "cursed".equals(stack.getTag().getString("Feyn"));
    }

    private static boolean isSealedScabbard(ItemStack stack) {
        if (!stack.hasTag()) return false;
        CompoundTag tag = stack.getTag();
        return tag != null && "sigiled".equals(tag.getString("Feyn"));
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
