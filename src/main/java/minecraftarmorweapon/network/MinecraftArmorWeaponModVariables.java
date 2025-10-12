package minecraftarmorweapon.network;

import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.Capability;

import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.client.Minecraft;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MinecraftArmorWeaponModVariables {
	public static boolean rpghazimenogui = false;

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		MinecraftArmorWeaponMod.addNetworkMessage(SavedDataSyncMessage.class, SavedDataSyncMessage::buffer, SavedDataSyncMessage::new, SavedDataSyncMessage::handler);
		MinecraftArmorWeaponMod.addNetworkMessage(PlayerVariablesSyncMessage.class, PlayerVariablesSyncMessage::buffer, PlayerVariablesSyncMessage::new, PlayerVariablesSyncMessage::handler);
	}

	@SubscribeEvent
	public static void init(RegisterCapabilitiesEvent event) {
		event.register(PlayerVariables.class);
	}

	@Mod.EventBusSubscriber
	public static class EventBusVariableHandlers {
		@SubscribeEvent
		public static void onPlayerLoggedInSyncPlayerVariables(PlayerEvent.PlayerLoggedInEvent event) {
			if (!event.getEntity().level.isClientSide())
				((PlayerVariables) event.getEntity().getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).syncPlayerVariables(event.getEntity());
		}

		@SubscribeEvent
		public static void onPlayerRespawnedSyncPlayerVariables(PlayerEvent.PlayerRespawnEvent event) {
			if (!event.getEntity().level.isClientSide())
				((PlayerVariables) event.getEntity().getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).syncPlayerVariables(event.getEntity());
		}

		@SubscribeEvent
		public static void onPlayerChangedDimensionSyncPlayerVariables(PlayerEvent.PlayerChangedDimensionEvent event) {
			if (!event.getEntity().level.isClientSide())
				((PlayerVariables) event.getEntity().getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).syncPlayerVariables(event.getEntity());
		}

		@SubscribeEvent
		public static void clonePlayer(PlayerEvent.Clone event) {
			event.getOriginal().revive();
			PlayerVariables original = ((PlayerVariables) event.getOriginal().getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables()));
			PlayerVariables clone = ((PlayerVariables) event.getEntity().getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables()));
			clone.ddd = original.ddd;
			clone.aaa = original.aaa;
			clone.tab = original.tab;
			clone.yaw = original.yaw;
			clone.rpgbookgive = original.rpgbookgive;
			clone.playerHasCompletedQuestTaskOne = original.playerHasCompletedQuestTaskOne;
			clone.playerHasCompletedQuestTaskTwo = original.playerHasCompletedQuestTaskTwo;
			clone.playerHasCompletedQuestTaskThree = original.playerHasCompletedQuestTaskThree;
			clone.playerHasCompletedQuestTaskFour = original.playerHasCompletedQuestTaskFour;
			clone.playerHasCompletedQuestTaskFive = original.playerHasCompletedQuestTaskFive;
			clone.playerHasCompletedQuestGoal = original.playerHasCompletedQuestGoal;
			clone.playerHasCompletedQuestChallenge = original.playerHasCompletedQuestChallenge;
			clone.playerQuestTaskOneNumber = original.playerQuestTaskOneNumber;
			clone.playerQuestTaskTwoNumber = original.playerQuestTaskTwoNumber;
			clone.playerQuestTaskThreeNumber = original.playerQuestTaskThreeNumber;
			clone.playerQuestTaskFourNumber = original.playerQuestTaskFourNumber;
			clone.playerQuestTaskFiveNumber = original.playerQuestTaskFiveNumber;
			clone.playerQuestGoalNumber = original.playerQuestGoalNumber;
			clone.playerQuestChallengeNumber = original.playerQuestChallengeNumber;
			clone.questScreenPage = original.questScreenPage;
			clone.playerLockChallenge = original.playerLockChallenge;
			if (!event.isWasDeath()) {
				clone.noa = original.noa;
				clone.SpeedStaff = original.SpeedStaff;
			}
		}

		@SubscribeEvent
		public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
			if (!event.getEntity().level.isClientSide()) {
				SavedData mapdata = MapVariables.get(event.getEntity().level);
				SavedData worlddata = WorldVariables.get(event.getEntity().level);
				if (mapdata != null)
					MinecraftArmorWeaponMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()), new SavedDataSyncMessage(0, mapdata));
				if (worlddata != null)
					MinecraftArmorWeaponMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()), new SavedDataSyncMessage(1, worlddata));
			}
		}

		@SubscribeEvent
		public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
			if (!event.getEntity().level.isClientSide()) {
				SavedData worlddata = WorldVariables.get(event.getEntity().level);
				if (worlddata != null)
					MinecraftArmorWeaponMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()), new SavedDataSyncMessage(1, worlddata));
			}
		}
	}

	public static class WorldVariables extends SavedData {
		public static final String DATA_NAME = "minecraft_armor_weapon_worldvars";

		public static WorldVariables load(CompoundTag tag) {
			WorldVariables data = new WorldVariables();
			data.read(tag);
			return data;
		}

		public void read(CompoundTag nbt) {
		}

		@Override
		public CompoundTag save(CompoundTag nbt) {
			return nbt;
		}

		public void syncData(LevelAccessor world) {
			this.setDirty();
			if (world instanceof Level level && !level.isClientSide())
				MinecraftArmorWeaponMod.PACKET_HANDLER.send(PacketDistributor.DIMENSION.with(level::dimension), new SavedDataSyncMessage(1, this));
		}

		static WorldVariables clientSide = new WorldVariables();

		public static WorldVariables get(LevelAccessor world) {
			if (world instanceof ServerLevel level) {
				return level.getDataStorage().computeIfAbsent(e -> WorldVariables.load(e), WorldVariables::new, DATA_NAME);
			} else {
				return clientSide;
			}
		}
	}

	public static class MapVariables extends SavedData {
		public static final String DATA_NAME = "minecraft_armor_weapon_mapvars";
		public double questTimeLeft = 0;
		public String questTaskOne = "";
		public String questTaskTwo = "";
		public String questTaskThree = "";
		public String questTaskFour = "";
		public String questTaskFive = "";
		public String questGoal = "";
		public String questChallenge = "";
		public String questReward = "";
		public String questTimeLeftDisplay = "\"\"";
		public String questChallengeReward = "\"\"";
		public boolean playerLockGoal = false;

		public static MapVariables load(CompoundTag tag) {
			MapVariables data = new MapVariables();
			data.read(tag);
			return data;
		}

		public void read(CompoundTag nbt) {
			questTimeLeft = nbt.getDouble("questTimeLeft");
			questTaskOne = nbt.getString("questTaskOne");
			questTaskTwo = nbt.getString("questTaskTwo");
			questTaskThree = nbt.getString("questTaskThree");
			questTaskFour = nbt.getString("questTaskFour");
			questTaskFive = nbt.getString("questTaskFive");
			questGoal = nbt.getString("questGoal");
			questChallenge = nbt.getString("questChallenge");
			questReward = nbt.getString("questReward");
			questTimeLeftDisplay = nbt.getString("questTimeLeftDisplay");
			questChallengeReward = nbt.getString("questChallengeReward");
			playerLockGoal = nbt.getBoolean("playerLockGoal");
		}

		@Override
		public CompoundTag save(CompoundTag nbt) {
			nbt.putDouble("questTimeLeft", questTimeLeft);
			nbt.putString("questTaskOne", questTaskOne);
			nbt.putString("questTaskTwo", questTaskTwo);
			nbt.putString("questTaskThree", questTaskThree);
			nbt.putString("questTaskFour", questTaskFour);
			nbt.putString("questTaskFive", questTaskFive);
			nbt.putString("questGoal", questGoal);
			nbt.putString("questChallenge", questChallenge);
			nbt.putString("questReward", questReward);
			nbt.putString("questTimeLeftDisplay", questTimeLeftDisplay);
			nbt.putString("questChallengeReward", questChallengeReward);
			nbt.putBoolean("playerLockGoal", playerLockGoal);
			return nbt;
		}

		public void syncData(LevelAccessor world) {
			this.setDirty();
			if (world instanceof Level && !world.isClientSide())
				MinecraftArmorWeaponMod.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new SavedDataSyncMessage(0, this));
		}

		static MapVariables clientSide = new MapVariables();

		public static MapVariables get(LevelAccessor world) {
			if (world instanceof ServerLevelAccessor serverLevelAcc) {
				return serverLevelAcc.getLevel().getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(e -> MapVariables.load(e), MapVariables::new, DATA_NAME);
			} else {
				return clientSide;
			}
		}
	}

	public static class SavedDataSyncMessage {
		public int type;
		public SavedData data;

		public SavedDataSyncMessage(FriendlyByteBuf buffer) {
			this.type = buffer.readInt();
			this.data = this.type == 0 ? new MapVariables() : new WorldVariables();
			if (this.data instanceof MapVariables _mapvars)
				_mapvars.read(buffer.readNbt());
			else if (this.data instanceof WorldVariables _worldvars)
				_worldvars.read(buffer.readNbt());
		}

		public SavedDataSyncMessage(int type, SavedData data) {
			this.type = type;
			this.data = data;
		}

		public static void buffer(SavedDataSyncMessage message, FriendlyByteBuf buffer) {
			buffer.writeInt(message.type);
			buffer.writeNbt(message.data.save(new CompoundTag()));
		}

		public static void handler(SavedDataSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> {
				if (!context.getDirection().getReceptionSide().isServer()) {
					if (message.type == 0)
						MapVariables.clientSide = (MapVariables) message.data;
					else
						WorldVariables.clientSide = (WorldVariables) message.data;
				}
			});
			context.setPacketHandled(true);
		}
	}

	public static final Capability<PlayerVariables> PLAYER_VARIABLES_CAPABILITY = CapabilityManager.get(new CapabilityToken<PlayerVariables>() {
	});

	@Mod.EventBusSubscriber
	private static class PlayerVariablesProvider implements ICapabilitySerializable<Tag> {
		@SubscribeEvent
		public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
			if (event.getObject() instanceof Player && !(event.getObject() instanceof FakePlayer))
				event.addCapability(new ResourceLocation("minecraft_armor_weapon", "player_variables"), new PlayerVariablesProvider());
		}

		private final PlayerVariables playerVariables = new PlayerVariables();
		private final LazyOptional<PlayerVariables> instance = LazyOptional.of(() -> playerVariables);

		@Override
		public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
			return cap == PLAYER_VARIABLES_CAPABILITY ? instance.cast() : LazyOptional.empty();
		}

		@Override
		public Tag serializeNBT() {
			return playerVariables.writeNBT();
		}

		@Override
		public void deserializeNBT(Tag nbt) {
			playerVariables.readNBT(nbt);
		}
	}

	public static class PlayerVariables {
		public String ddd = "\"\"";
		public double aaa = 2.0;
		public double tab = 2.0;
		public double noa = 0;
		public boolean SpeedStaff = false;
		public double yaw = 0;
		public boolean rpgbookgive = true;
		public boolean playerHasCompletedQuestTaskOne = false;
		public boolean playerHasCompletedQuestTaskTwo = false;
		public boolean playerHasCompletedQuestTaskThree = false;
		public boolean playerHasCompletedQuestTaskFour = false;
		public boolean playerHasCompletedQuestTaskFive = false;
		public boolean playerHasCompletedQuestGoal = false;
		public boolean playerHasCompletedQuestChallenge = false;
		public double playerQuestTaskOneNumber = 0;
		public double playerQuestTaskTwoNumber = 0;
		public double playerQuestTaskThreeNumber = 0;
		public double playerQuestTaskFourNumber = 0;
		public double playerQuestTaskFiveNumber = 0;
		public double playerQuestGoalNumber = 0;
		public double playerQuestChallengeNumber = 0.0;
		public double questScreenPage = 1.0;
		public boolean playerLockChallenge = false;

		public void syncPlayerVariables(Entity entity) {
			if (entity instanceof ServerPlayer serverPlayer)
				MinecraftArmorWeaponMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new PlayerVariablesSyncMessage(this));
		}

		public Tag writeNBT() {
			CompoundTag nbt = new CompoundTag();
			nbt.putString("ddd", ddd);
			nbt.putDouble("aaa", aaa);
			nbt.putDouble("tab", tab);
			nbt.putDouble("noa", noa);
			nbt.putBoolean("SpeedStaff", SpeedStaff);
			nbt.putDouble("yaw", yaw);
			nbt.putBoolean("rpgbookgive", rpgbookgive);
			nbt.putBoolean("playerHasCompletedQuestTaskOne", playerHasCompletedQuestTaskOne);
			nbt.putBoolean("playerHasCompletedQuestTaskTwo", playerHasCompletedQuestTaskTwo);
			nbt.putBoolean("playerHasCompletedQuestTaskThree", playerHasCompletedQuestTaskThree);
			nbt.putBoolean("playerHasCompletedQuestTaskFour", playerHasCompletedQuestTaskFour);
			nbt.putBoolean("playerHasCompletedQuestTaskFive", playerHasCompletedQuestTaskFive);
			nbt.putBoolean("playerHasCompletedQuestGoal", playerHasCompletedQuestGoal);
			nbt.putBoolean("playerHasCompletedQuestChallenge", playerHasCompletedQuestChallenge);
			nbt.putDouble("playerQuestTaskOneNumber", playerQuestTaskOneNumber);
			nbt.putDouble("playerQuestTaskTwoNumber", playerQuestTaskTwoNumber);
			nbt.putDouble("playerQuestTaskThreeNumber", playerQuestTaskThreeNumber);
			nbt.putDouble("playerQuestTaskFourNumber", playerQuestTaskFourNumber);
			nbt.putDouble("playerQuestTaskFiveNumber", playerQuestTaskFiveNumber);
			nbt.putDouble("playerQuestGoalNumber", playerQuestGoalNumber);
			nbt.putDouble("playerQuestChallengeNumber", playerQuestChallengeNumber);
			nbt.putDouble("questScreenPage", questScreenPage);
			nbt.putBoolean("playerLockChallenge", playerLockChallenge);
			return nbt;
		}

		public void readNBT(Tag Tag) {
			CompoundTag nbt = (CompoundTag) Tag;
			ddd = nbt.getString("ddd");
			aaa = nbt.getDouble("aaa");
			tab = nbt.getDouble("tab");
			noa = nbt.getDouble("noa");
			SpeedStaff = nbt.getBoolean("SpeedStaff");
			yaw = nbt.getDouble("yaw");
			rpgbookgive = nbt.getBoolean("rpgbookgive");
			playerHasCompletedQuestTaskOne = nbt.getBoolean("playerHasCompletedQuestTaskOne");
			playerHasCompletedQuestTaskTwo = nbt.getBoolean("playerHasCompletedQuestTaskTwo");
			playerHasCompletedQuestTaskThree = nbt.getBoolean("playerHasCompletedQuestTaskThree");
			playerHasCompletedQuestTaskFour = nbt.getBoolean("playerHasCompletedQuestTaskFour");
			playerHasCompletedQuestTaskFive = nbt.getBoolean("playerHasCompletedQuestTaskFive");
			playerHasCompletedQuestGoal = nbt.getBoolean("playerHasCompletedQuestGoal");
			playerHasCompletedQuestChallenge = nbt.getBoolean("playerHasCompletedQuestChallenge");
			playerQuestTaskOneNumber = nbt.getDouble("playerQuestTaskOneNumber");
			playerQuestTaskTwoNumber = nbt.getDouble("playerQuestTaskTwoNumber");
			playerQuestTaskThreeNumber = nbt.getDouble("playerQuestTaskThreeNumber");
			playerQuestTaskFourNumber = nbt.getDouble("playerQuestTaskFourNumber");
			playerQuestTaskFiveNumber = nbt.getDouble("playerQuestTaskFiveNumber");
			playerQuestGoalNumber = nbt.getDouble("playerQuestGoalNumber");
			playerQuestChallengeNumber = nbt.getDouble("playerQuestChallengeNumber");
			questScreenPage = nbt.getDouble("questScreenPage");
			playerLockChallenge = nbt.getBoolean("playerLockChallenge");
		}
	}

	public static class PlayerVariablesSyncMessage {
		public PlayerVariables data;

		public PlayerVariablesSyncMessage(FriendlyByteBuf buffer) {
			this.data = new PlayerVariables();
			this.data.readNBT(buffer.readNbt());
		}

		public PlayerVariablesSyncMessage(PlayerVariables data) {
			this.data = data;
		}

		public static void buffer(PlayerVariablesSyncMessage message, FriendlyByteBuf buffer) {
			buffer.writeNbt((CompoundTag) message.data.writeNBT());
		}

		public static void handler(PlayerVariablesSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> {
				if (!context.getDirection().getReceptionSide().isServer()) {
					PlayerVariables variables = ((PlayerVariables) Minecraft.getInstance().player.getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables()));
					variables.ddd = message.data.ddd;
					variables.aaa = message.data.aaa;
					variables.tab = message.data.tab;
					variables.noa = message.data.noa;
					variables.SpeedStaff = message.data.SpeedStaff;
					variables.yaw = message.data.yaw;
					variables.rpgbookgive = message.data.rpgbookgive;
					variables.playerHasCompletedQuestTaskOne = message.data.playerHasCompletedQuestTaskOne;
					variables.playerHasCompletedQuestTaskTwo = message.data.playerHasCompletedQuestTaskTwo;
					variables.playerHasCompletedQuestTaskThree = message.data.playerHasCompletedQuestTaskThree;
					variables.playerHasCompletedQuestTaskFour = message.data.playerHasCompletedQuestTaskFour;
					variables.playerHasCompletedQuestTaskFive = message.data.playerHasCompletedQuestTaskFive;
					variables.playerHasCompletedQuestGoal = message.data.playerHasCompletedQuestGoal;
					variables.playerHasCompletedQuestChallenge = message.data.playerHasCompletedQuestChallenge;
					variables.playerQuestTaskOneNumber = message.data.playerQuestTaskOneNumber;
					variables.playerQuestTaskTwoNumber = message.data.playerQuestTaskTwoNumber;
					variables.playerQuestTaskThreeNumber = message.data.playerQuestTaskThreeNumber;
					variables.playerQuestTaskFourNumber = message.data.playerQuestTaskFourNumber;
					variables.playerQuestTaskFiveNumber = message.data.playerQuestTaskFiveNumber;
					variables.playerQuestGoalNumber = message.data.playerQuestGoalNumber;
					variables.playerQuestChallengeNumber = message.data.playerQuestChallengeNumber;
					variables.questScreenPage = message.data.questScreenPage;
					variables.playerLockChallenge = message.data.playerLockChallenge;
				}
			});
			context.setPacketHandled(true);
		}
	}
}
