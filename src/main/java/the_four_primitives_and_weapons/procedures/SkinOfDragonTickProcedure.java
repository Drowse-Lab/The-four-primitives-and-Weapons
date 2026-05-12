package the_four_primitives_and_weapons.procedures;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandFunction;

import the_four_primitives_and_weapons.network.TheFourPrimitivesAndWeaponsModVariables;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModEnchantments;

import javax.annotation.Nullable;

import java.util.Optional;

@Mod.EventBusSubscriber
public class SkinOfDragonTickProcedure {
	/** 重い /execute 全体スキャンの実行間隔 (tick). 元実装は毎 tick × プレイヤー数 で激重だった. */
	private static final int GLOBAL_INTERVAL = 10;

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		// プレイヤー固有処理は 5 tick おき (元実装は毎 tick で実害的なラグの一因)
		if (event.player.tickCount % 5 != 0) return;
		execute(event, VersionHelper.getLevel(event.player), event.player.getX(), event.player.getY(), event.player.getZ(), event.player);
	}

	/**
	 * グローバル @e クエリ (アーマースタンドクリーンアップ + レシピ監視) は
	 * ServerTickEvent で 1 回だけ実行 → プレイヤー数倍の重複呼出を排除.
	 * 本体の execute メソッドは「per-player 部分のみ」になっているのでここでは呼び出さない.
	 */
	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		if (event.getServer().getTickCount() % GLOBAL_INTERVAL != 0) return;
		for (ServerLevel level : event.getServer().getAllLevels()) {
			runGlobalCommands(level);
		}
	}

	/** プレイヤー位置に依存しない @e ベースのバックグラウンド処理を 1 回実行. */
	private static void runGlobalCommands(ServerLevel level) {
		if (level.getServer() == null) return;
		Vec3 origin = Vec3.ZERO;
		CommandSourceStack src = new CommandSourceStack(CommandSource.NULL,
				origin, Vec2.ZERO, level, 4, "", Component.literal(""), level.getServer(), null).withSuppressedOutput();

		// アーマースタンドクリーンアップ (luna 持ってないやつ kill)
		level.getServer().getCommands().performPrefixedCommand(src,
				"execute as @e[type=minecraft:armor_stand, tag=the_four_primitives_and_weapons_item_stand_amor_stand] if entity @s[nbt=!{HandItems:[{id:\"the_four_primitives_and_weapons:luna\"}]}] run kill @s");
		// nether_star + bone + iron_block 検出レシピ
		level.getServer().getCommands().performPrefixedCommand(src,
				"execute at @e[type=item,nbt={Item:{id:\"minecraft:nether_star\"}}] if entity @e[type=item,nbt={Item:{id:\"minecraft:bone\",Count:16b}},distance=..1.5] if entity @e[type=item,nbt={Item:{id:\"minecraft:iron_block\",Count:64b}},distance=..1.5] run function the_four_primitives_and_weapons:hardentity_function");
		level.getServer().getCommands().performPrefixedCommand(src,
				"execute at @e[type=item,nbt={Item:{id:\"minecraft:nether_star\"}}] if entity @e[type=item,nbt={Item:{id:\"the_four_primitives_and_weapons:wither_bone\",Count:16b}},distance=..1.5] if entity @e[type=item,nbt={Item:{id:\"minecraft:iron_block\",Count:64b}},distance=..1.5] run function the_four_primitives_and_weapons:hardentity_function_wither_skeleton");
		level.getServer().getCommands().performPrefixedCommand(src,
				"execute at @e[type=item,nbt={Item:{id:\"minecraft:nether_star\"}}] if entity @e[type=item,nbt={Item:{id:\"the_four_primitives_and_weapons:stray_bone\",Count:16b}},distance=..1.5] if entity @e[type=item,nbt={Item:{id:\"minecraft:iron_block\",Count:64b}},distance=..1.5] run function the_four_primitives_and_weapons:hardentity_function_stray");
		// アーマースタンド (luna 用 / old_katana 用)
		level.getServer().getCommands().performPrefixedCommand(src,
				"execute as @e[type=minecraft:armor_stand, tag=the_four_primitives_and_weapons_item_stand_amor_stand_luna] if entity @s[nbt=!{HandItems:[{id:\"the_four_primitives_and_weapons:luna\"}]}] run kill @s");
		level.getServer().getCommands().performPrefixedCommand(src,
				"execute as @e[type=minecraft:armor_stand, tag=the_four_primitives_and_weapons_item_stand_amor_stand_old_katana] if entity @s[nbt=!{HandItems:[{id:\"the_four_primitives_and_weapons:old_katana\"}]}] run kill @s");
		// alchemy_craft_block 魔法陣エフェクト
		level.getServer().getCommands().performPrefixedCommand(src,
				"execute as @e[type=minecraft:armor_stand, tag=the_four_primitives_and_weapons_alchemy_craft_block_mahouzinn] run effect give @s the_four_primitives_and_weapons:alchemy_craft_block_effect 1 1 true");
		// iron_katana + beacon + iron_block on alchemy_craft_block レシピ
		level.getServer().getCommands().performPrefixedCommand(src,
				"execute at @e[type=item,nbt={Item:{id:\"the_four_primitives_and_weapons:iron_katana\"}}] if entity @e[type=item,nbt={Item:{id:\"minecraft:beacon\",Count:16b}},distance=..0.5] if entity @e[type=item,nbt={Item:{id:\"minecraft:iron_block\",Count:64b}},distance=..0.5] if block ~ ~-1 ~ the_four_primitives_and_weapons:alchemy_craft_block run function the_four_primitives_and_weapons:sword_stand_luna");
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof Player) {
			{
				Entity _ent = entity;
				if (!_ent.level().isClientSide() && _ent.getServer() != null) {
					_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
							_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent),
							"team join the_four_primitives_and_weapons_dark_purple @e[type=item,limit=1,sort=nearest,nbt={Item:{id:\"the_four_primitives_and_weapons:skin_of_dragon\"}}]");
				}
			}
			if ((entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).aaa == 5
					&& (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.LOKI_THE_TRICKSTER.get()) {
				if (EnchantmentHelper.getItemEnchantmentLevel(TheFourPrimitivesAndWeaponsModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
					if (world instanceof ServerLevel _level && _level.getServer() != null) {
						Optional<CommandFunction> _fopt = _level.getServer().getFunctions().get(new ResourceLocation("the_four_primitives_and_weapons:armor_stand_tobasu_tick_kill"));
						if (_fopt.isPresent())
							_level.getServer().getFunctions().execute(_fopt.get(), new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null));
					}
				} else {
					if (world instanceof ServerLevel _level && _level.getServer() != null) {
						Optional<CommandFunction> _fopt = _level.getServer().getFunctions().get(new ResourceLocation("the_four_primitives_and_weapons:armor_stand_tobasu_tick"));
						if (_fopt.isPresent())
							_level.getServer().getFunctions().execute(_fopt.get(), new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null));
					}
				}
			} else {
				if (world instanceof ServerLevel _level && _level.getServer() != null) {
					Optional<CommandFunction> _fopt = _level.getServer().getFunctions().get(new ResourceLocation("the_four_primitives_and_weapons:armor_stand_tobasu_enma_tick_kill"));
					if (_fopt.isPresent())
						_level.getServer().getFunctions().execute(_fopt.get(), new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null));
				}
			}
			// Kurikaraken の CustomModelData 自動リセット (player 固有なのでここに残す)
			if (!(entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.TUNDERBOLTEFFRCT.get()) : false)
					&& (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().getDouble("CustomModelData") == 1
					&& ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.KURIKARAKEN.get()
							|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.KURIKARAKENSWORD.get()
							|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.KURIKARAKENUTIGATANA.get())) {
				(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getOrCreateTag().putDouble("CustomModelData", 0);
			}
		}
		// グローバル @e コマンドは onServerTick → runGlobalCommands に移動済み
	}
}
