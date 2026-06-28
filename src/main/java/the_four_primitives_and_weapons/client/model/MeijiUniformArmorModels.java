package the_four_primitives_and_weapons.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.Map;

/**
 * 明治制服のカスタム3D防具モデルを スロットごとに HumanoidModel へ組み立てるヘルパー。
 * ( クライアントのみ。 ArmorItem#initializeClient のラムダ内からのみ呼ばれる )
 *
 *   兜      : head に帽子ジオメトリ
 *   上着    : body + 両袖
 *   ズボン  : 両脚（ズボン）
 *   ブーツ  : 両脚スロットに boot ジオメトリ（脛から下のみ）
 */
public final class MeijiUniformArmorModels {

	private MeijiUniformArmorModels() {}

	private static ModelMeijiUniform<?> baked() {
		return new ModelMeijiUniform<>(
				Minecraft.getInstance().getEntityModels().bakeLayer(ModelMeijiUniform.LAYER_LOCATION));
	}

	/** 着用者が slim ( Alex ) スキンなら slim レイヤー、 そうでなければ wide レイヤーをベイク。 */
	private static ModelMeijiUniform<?> bakedFor(LivingEntity living) {
		boolean slim = living instanceof net.minecraft.client.player.AbstractClientPlayer acp
				&& "slim".equals(acp.getModelName());
		return new ModelMeijiUniform<>(Minecraft.getInstance().getEntityModels().bakeLayer(
				slim ? ModelMeijiUniform.LAYER_LOCATION_SLIM : ModelMeijiUniform.LAYER_LOCATION));
	}

	private static ModelPart empty() {
		return new ModelPart(Collections.emptyList(), Collections.emptyMap());
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static HumanoidModel finish(HumanoidModel model, LivingEntity living, HumanoidModel def) {
		model.crouching = living.isShiftKeyDown();
		model.riding = def.riding;
		model.young = living.isBaby();
		return model;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static HumanoidModel helmet(LivingEntity living, HumanoidModel def) {
		ModelMeijiUniform<?> m = baked();
		HumanoidModel model = new HumanoidModel(new ModelPart(Collections.emptyList(), Map.of(
				"head", m.head, "hat", empty(), "body", empty(),
				"right_arm", empty(), "left_arm", empty(),
				"right_leg", empty(), "left_leg", empty())));
		return finish(model, living, def);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static HumanoidModel chest(LivingEntity living, HumanoidModel def) {
		ModelMeijiUniform<?> m = bakedFor(living); // 上着は袖を含むので slim/wide を切替
		HumanoidModel model = new HumanoidModel(new ModelPart(Collections.emptyList(), Map.of(
				"head", empty(), "hat", empty(), "body", m.body,
				"right_arm", m.right_arm, "left_arm", m.left_arm,
				"right_leg", empty(), "left_leg", empty())));
		return finish(model, living, def);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static HumanoidModel leggings(LivingEntity living, HumanoidModel def) {
		ModelMeijiUniform<?> m = baked();
		HumanoidModel model = new HumanoidModel(new ModelPart(Collections.emptyList(), Map.of(
				"head", empty(), "hat", empty(), "body", empty(),
				"right_arm", empty(), "left_arm", empty(),
				"right_leg", m.right_leg, "left_leg", m.left_leg)));
		return finish(model, living, def);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static HumanoidModel boots(LivingEntity living, HumanoidModel def) {
		ModelMeijiUniform<?> m = baked();
		HumanoidModel model = new HumanoidModel(new ModelPart(Collections.emptyList(), Map.of(
				"head", empty(), "hat", empty(), "body", empty(),
				"right_arm", empty(), "left_arm", empty(),
				"right_leg", m.right_boot, "left_leg", m.left_boot)));
		return finish(model, living, def);
	}
}
