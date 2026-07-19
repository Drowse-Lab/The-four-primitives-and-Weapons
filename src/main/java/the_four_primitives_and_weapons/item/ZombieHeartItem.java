
package the_four_primitives_and_weapons.item;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.Minecraft;

import the_four_primitives_and_weapons.client.model.Modelzombie_heart_head;

import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap;

import java.util.function.Consumer;
import java.util.UUID;
import java.util.Map;
import java.util.Collections;

public abstract class ZombieHeartItem extends ArmorItem {
	// ゾンビの心臓(頭): 防御力0 / ノックバック耐性0 / 最大体力+4
	private static final UUID MAX_HEALTH_UUID = UUID.fromString("b7c2f1e4-0a3d-4c9e-9b6f-2d5a7e8c1f30");

	public ZombieHeartItem(ArmorItem.Type type, Item.Properties properties) {
		super(new ArmorMaterial() {
			@Override
			public int getDurabilityForType(ArmorItem.Type type) {
				return new int[]{13, 15, 16, 11}[type.getSlot().getIndex()] * 0;
			}

			@Override
			public int getDefenseForType(ArmorItem.Type type) {
				return new int[]{0, 0, 0, 0}[type.getSlot().getIndex()];
			}

			@Override
			public int getEnchantmentValue() {
				return 0;
			}

			@Override
			public SoundEvent getEquipSound() {
				return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(""));
			}

			@Override
			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}

			@Override
			public String getName() {
				return "zombie_heart";
			}

			@Override
			public float getToughness() {
				return 0f;
			}

			@Override
			public float getKnockbackResistance() {
				return 0f;
			}
		}, type, properties);
	}

	public static class Helmet extends ZombieHeartItem {
		public Helmet() {
			super(ArmorItem.Type.HELMET, new Item.Properties());
		}

		// 最大体力 +4 は ZombieHeartHandler ( 装備イベント方式 ) で付与する。
		// ここでの getDefaultAttributeModifiers 上書きは、アイテムに AttributeModifiers NBT が
		// 付くと丸ごと無視される等の不確実さがあるため使わない。
		@Override
		public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level,
				java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
			super.appendHoverText(stack, level, tooltip, flag);
			tooltip.add(net.minecraft.network.chat.Component.literal("装備中: 最大体力 +4")
					.withStyle(net.minecraft.ChatFormatting.AQUA));
		}

		@Override
		public void initializeClient(Consumer<IClientItemExtensions> consumer) {
			consumer.accept(new IClientItemExtensions() {
				@Override
				public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
					HumanoidModel armorModel = new HumanoidModel(new ModelPart(Collections.emptyList(),
							Map.of("head", new Modelzombie_heart_head(Minecraft.getInstance().getEntityModels().bakeLayer(Modelzombie_heart_head.LAYER_LOCATION)).head, "hat", new ModelPart(Collections.emptyList(), Collections.emptyMap()),
									"body", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "right_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "left_arm",
									new ModelPart(Collections.emptyList(), Collections.emptyMap()), "right_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "left_leg",
									new ModelPart(Collections.emptyList(), Collections.emptyMap()))));
					armorModel.crouching = living.isShiftKeyDown();
					armorModel.riding = defaultModel.riding;
					armorModel.young = living.isBaby();
					return armorModel;
				}
			});
		}

		@Override
		public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
			return "the_four_primitives_and_weapons:textures/entities/zombie_heart.png";
		}
	}
}
