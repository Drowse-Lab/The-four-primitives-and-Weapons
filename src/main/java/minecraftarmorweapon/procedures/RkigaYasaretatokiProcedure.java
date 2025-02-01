package minecraftarmorweapon.procedures;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;

import minecraftarmorweapon.network.MinecraftArmorWeaponModVariables;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

public class RkigaYasaretatokiProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		{
			double _setval = (entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa + 1;
			entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.aaa = _setval;
				capability.syncPlayerVariables(entity);
			});
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.LUNA.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS_KATANA.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.RIVERS_OF_BLOOD.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.MY_TEST_IRON_KATANA.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.MAGISCHES_FEEN_KATANA.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKEN.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKENSWORD.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKENUTIGATANA.get()) {
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 2) {
				{
					String _setval = "\u56DE\u8EE2\u65AC\u308A";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 3) {
				{
					String _setval = "\u8599\u304E\u6255\u3044";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 4) {
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.RIVERS_OF_BLOOD.get()) {
					{
						String _setval = "\u00A74\u00A7kdevour blood";
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ddd = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				} else {
					{
						String _setval = "\u7A81\u304D";
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ddd = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 5) {
				{
					String _setval = "\u65AC\u6483";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 6) {
				{
					String _setval = "\u00A7e\u65AC\u6483";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 7) {
				{
					String _setval = "\u4F55\u3082\u8D77\u304D\u306A\u3044";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa >= 8) {
				{
					double _setval = 2;
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.aaa = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				{
					String _setval = "\u56DE\u8EE2\u65AC\u308A";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
		} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.OLD_KATANA.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KATANA_NIGU_HUMERUS.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.ENMA.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.SMALL_SWORD.get()) {
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 2) {
				{
					String _setval = "\u56DE\u8EE2\u65AC\u308A";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 3) {
				{
					String _setval = "\u8599\u304E\u6255\u3044";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 4) {
				{
					String _setval = "\u7A81\u304D";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 5) {
				{
					String _setval = "\u65AC\u6483";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 6) {
				{
					String _setval = "\u00A7e\u65AC\u6483";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 7) {
				{
					String _setval = "\u4F55\u3082\u8D77\u304D\u306A\u3044";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa >= 8) {
				{
					double _setval = 2;
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.aaa = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				{
					String _setval = "\u56DE\u8EE2\u65AC\u308A";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
		} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.TEST_BOW.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.TEST_BOW.get()) {
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 2) {
				{
					String _setval = "\u666E\u901A\u306E\u77E2";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 3) {
				{
					String _setval = "\u76F4\u7DDA\u306E\u77E2";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 4) {
				{
					String _setval = "\u540C\u3058\u9AD8\u3055";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 5) {
				{
					String _setval = "\u62E1\u6563";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 6) {
				{
					String _setval = "\u901F\u3081\u306E\u77E2";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 7) {
				{
					String _setval = "\u5F57\u661F";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa >= 8) {
				{
					double _setval = 2;
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.aaa = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				{
					String _setval = "\u666E\u901A\u306E\u77E2";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
		} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.NINJATOU.get()) {
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 2) {
				{
					String _setval = "\u56DE\u8EE2\u65AC\u308A";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 3) {
				{
					String _setval = "\u8599\u304E\u6255\u3044";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 4) {
				{
					String _setval = "\u7A81\u304D";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 5) {
				{
					String _setval = "\u65AC\u6483";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 6) {
				{
					String _setval = "\u65AC\u3063\u3066\u308B\u3093\u3068\u3061\u3083\u3046\u305E";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 7) {
				{
					String _setval = "\u4F55\u3082\u8D77\u304D\u306A\u3044";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa >= 8) {
				{
					double _setval = 2;
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.aaa = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				{
					String _setval = "\u56DE\u8EE2\u65AC\u308A";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
		} else if (!((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.A_2.get())) {
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 2) {
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.SWORD_OF_NIGHT.get()) {
					{
						String _setval = "\u00A7d\u307D\u3093\u30D1\u30C1\u30D1\u30C1";
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ddd = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KABUSERU.get()) {
					{
						String _setval = "\u304B\u3076\u308C\u3084";
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ddd = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					(new ItemStack(MinecraftArmorWeaponModItems.KABUSERU.get())).setHoverName(Component.literal("\u304B\u3076\u308C\u3084"));
				} else {
					{
						String _setval = "\u56DE\u8EE2\u65AC\u308A";
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ddd = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 3) {
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.SWORD_OF_NIGHT.get()) {
					{
						String _setval = "\u00A71\u3066\u3085\u30FC\u3093\u3068\u3093";
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ddd = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KABUSERU.get()) {
					{
						String _setval = "\u80F4\u306B\u3053\u308C\u7740\u308D";
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ddd = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					(new ItemStack(MinecraftArmorWeaponModItems.KABUSERU.get())).setHoverName(Component.literal("\u80F4\u306B\u3053\u308C\u7740\u308D"));
				} else {
					{
						String _setval = "\u8599\u304E\u6255\u3044";
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ddd = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 4) {
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.SWORD_OF_NIGHT.get()) {
					{
						String _setval = "\u8599\u304E\u6255\u3044";
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ddd = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KABUSERU.get()) {
					{
						String _setval = "\u811A\u306B\u3053\u308C\u7740\u308D";
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ddd = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					(new ItemStack(MinecraftArmorWeaponModItems.KABUSERU.get())).setHoverName(Component.literal("\u811A\u306B\u3053\u308C\u7740\u308D"));
				} else {
					{
						String _setval = "\u7A81\u304D";
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ddd = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 5) {
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KABUSERU.get()) {
					{
						String _setval = "\u8DB3\u306B\u3053\u308C\u7740\u308D";
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ddd = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					(new ItemStack(MinecraftArmorWeaponModItems.KABUSERU.get())).setHoverName(Component.literal("\u8DB3\u306B\u3053\u308C\u7740\u308D"));
				} else {
					{
						String _setval = "\u65AC\u6483";
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ddd = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 6) {
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KABUSERU.get()) {
					{
						String _setval = "\u3053\u308C\u6301\u3066\u5229\u304D\u624B\u306B";
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ddd = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					(new ItemStack(MinecraftArmorWeaponModItems.KABUSERU.get())).setHoverName(Component.literal("\u3053\u308C\u6301\u3066\u5229\u304D\u624B\u306B"));
				} else {
					{
						String _setval = "\u4F55\u3082\u8D77\u304D\u306A\u3044";
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ddd = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				}
			}
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KABUSERU.get()) {
				if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 7) {
					{
						String _setval = "\u3053\u308C\u6301\u3066\u5229\u304D\u624B\u3058\u3083\u306A\u3044\u65B9\u306B";
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ddd = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					(new ItemStack(MinecraftArmorWeaponModItems.KABUSERU.get())).setHoverName(Component.literal("\u3053\u308C\u6301\u3066\u5229\u304D\u624B\u3058\u3083\u306A\u3044\u65B9\u306B"));
				}
				if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa >= 8) {
					{
						double _setval = 2;
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.aaa = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					{
						String _setval = "\u304B\u3076\u308C\u3084";
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ddd = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					(new ItemStack(MinecraftArmorWeaponModItems.KABUSERU.get())).setHoverName(Component.literal("\u304B\u3076\u308C\u3084"));
				}
			} else {
				if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa >= 7) {
					{
						double _setval = 2;
						entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.aaa = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.SWORD_OF_NIGHT.get()) {
						{
							String _setval = "\u00A7d\u307D\u3093\u30D1\u30C1\u30D1\u30C1";
							entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.ddd = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
					} else {
						{
							String _setval = "\u56DE\u8EE2\u65AC\u308A";
							entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.ddd = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
					}
				}
			}
		} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.A_2.get()) {
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 2) {
				{
					String _setval = "\u571F";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 3) {
				{
					String _setval = "\u77F3";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 4) {
				{
					String _setval = "\u6C34";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 5) {
				{
					String _setval = "\u7A7A\u6C17";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 6) {
				{
					String _setval = "\u6DF1\u5C64\u77F3";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa >= 7) {
				{
					double _setval = 2;
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.aaa = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				{
					String _setval = "\u571F";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
		}
	}
}
