package com.tacz_caliber_ammo.platform;

import com.tacz_caliber_ammo.TaczCaliberAmmo;
import com.tacz_caliber_ammo.caliber.CaliberManager;
import com.tacz_caliber_ammo.network.CMsgPouchDeposit;
import com.tacz_caliber_ammo.network.CMsgPouchPattern;
import com.tacz_caliber_ammo.network.CMsgPouchWithdraw;
import com.tacz_caliber_ammo.network.CMsgUnloadAmmo;
import com.tacz_caliber_ammo.network.SMsgCaliberData;

import net.minecraft.server.level.ServerPlayer;
//? if forge {
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
//?} else {
/*import java.util.function.Consumer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
*///?}

/** Loader-specific C2S transport and server-authoritative caliber data synchronization. */
public final class PlatformNetwork {

        private static final String PROTOCOL_VERSION = "2";

    //? if forge {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            TaczCaliberAmmo.prefix("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, CMsgUnloadAmmo.class,
                CMsgUnloadAmmo::encode, CMsgUnloadAmmo::decode,
                (message, context) -> handleForge(message::handle, context));
        CHANNEL.registerMessage(id++, CMsgPouchWithdraw.class,
                CMsgPouchWithdraw::encode, CMsgPouchWithdraw::decode,
                (message, context) -> handleForge(message::handle, context));
        CHANNEL.registerMessage(id++, CMsgPouchPattern.class,
                CMsgPouchPattern::encode, CMsgPouchPattern::decode,
                (message, context) -> handleForge(message::handle, context));
        CHANNEL.registerMessage(id++, CMsgPouchDeposit.class,
                CMsgPouchDeposit::encode, CMsgPouchDeposit::decode,
                (message, context) -> handleForge(message::handle, context));
        CHANNEL.registerMessage(id, SMsgCaliberData.class,
                SMsgCaliberData::encode, SMsgCaliberData::decode,
                (message, context) -> handleForgeClient(message::handle, context),
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    private static void handleForge(Consumer<ServerPlayer> handler, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                handler.accept(player);
            }
        });
        context.setPacketHandled(true);
    }

        private static void handleForgeClient(Runnable handler, Supplier<NetworkEvent.Context> contextSupplier) {
                NetworkEvent.Context context = contextSupplier.get();
                context.enqueueWork(handler);
                context.setPacketHandled(true);
        }

    public static void send(CMsgUnloadAmmo message) { CHANNEL.sendToServer(message); }
    public static void send(CMsgPouchWithdraw message) { CHANNEL.sendToServer(message); }
    public static void send(CMsgPouchPattern message) { CHANNEL.sendToServer(message); }
    public static void send(CMsgPouchDeposit message) { CHANNEL.sendToServer(message); }
        public static void syncCaliberData(ServerPlayer player) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                                new SMsgCaliberData(CaliberManager.createSyncSnapshot()));
        }
    //?} else {
    /*public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(UnloadPayload.TYPE, UnloadPayload.CODEC,
                (payload, context) -> handleNeoForge(payload.message()::handle, context));
        registrar.playToServer(WithdrawPayload.TYPE, WithdrawPayload.CODEC,
                (payload, context) -> handleNeoForge(payload.message()::handle, context));
        registrar.playToServer(PatternPayload.TYPE, PatternPayload.CODEC,
                (payload, context) -> handleNeoForge(payload.message()::handle, context));
        registrar.playToServer(DepositPayload.TYPE, DepositPayload.CODEC,
                (payload, context) -> handleNeoForge(payload.message()::handle, context));
        registrar.playToClient(CaliberDataPayload.TYPE, CaliberDataPayload.CODEC,
                (payload, context) -> context.enqueueWork(payload.message()::handle));
    }

    private static void handleNeoForge(Consumer<ServerPlayer> handler, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> handler.accept(player));
        }
    }

    public static void send(CMsgUnloadAmmo message) { PacketDistributor.sendToServer(new UnloadPayload(message)); }
    public static void send(CMsgPouchWithdraw message) { PacketDistributor.sendToServer(new WithdrawPayload(message)); }
    public static void send(CMsgPouchPattern message) { PacketDistributor.sendToServer(new PatternPayload(message)); }
    public static void send(CMsgPouchDeposit message) { PacketDistributor.sendToServer(new DepositPayload(message)); }
        public static void syncCaliberData(ServerPlayer player) {
                PacketDistributor.sendToPlayer(player,
                                new CaliberDataPayload(new SMsgCaliberData(CaliberManager.createSyncSnapshot())));
        }

    private record UnloadPayload(CMsgUnloadAmmo message) implements CustomPacketPayload {
        private static final Type<UnloadPayload> TYPE = new Type<>(TaczCaliberAmmo.prefix("unload_ammo"));
        private static final StreamCodec<RegistryFriendlyByteBuf, UnloadPayload> CODEC = StreamCodec.of(
                (buffer, payload) -> payload.message().encode(buffer),
                buffer -> new UnloadPayload(CMsgUnloadAmmo.decode(buffer)));
        @Override public Type<UnloadPayload> type() { return TYPE; }
    }

    private record WithdrawPayload(CMsgPouchWithdraw message) implements CustomPacketPayload {
        private static final Type<WithdrawPayload> TYPE = new Type<>(TaczCaliberAmmo.prefix("pouch_withdraw"));
        private static final StreamCodec<RegistryFriendlyByteBuf, WithdrawPayload> CODEC = StreamCodec.of(
                (buffer, payload) -> payload.message().encode(buffer),
                buffer -> new WithdrawPayload(CMsgPouchWithdraw.decode(buffer)));
        @Override public Type<WithdrawPayload> type() { return TYPE; }
    }

    private record PatternPayload(CMsgPouchPattern message) implements CustomPacketPayload {
        private static final Type<PatternPayload> TYPE = new Type<>(TaczCaliberAmmo.prefix("pouch_pattern"));
        private static final StreamCodec<RegistryFriendlyByteBuf, PatternPayload> CODEC = StreamCodec.of(
                (buffer, payload) -> payload.message().encode(buffer),
                buffer -> new PatternPayload(CMsgPouchPattern.decode(buffer)));
        @Override public Type<PatternPayload> type() { return TYPE; }
    }

    private record DepositPayload(CMsgPouchDeposit message) implements CustomPacketPayload {
        private static final Type<DepositPayload> TYPE = new Type<>(TaczCaliberAmmo.prefix("pouch_deposit"));
        private static final StreamCodec<RegistryFriendlyByteBuf, DepositPayload> CODEC = StreamCodec.of(
                (buffer, payload) -> payload.message().encode(buffer),
                buffer -> new DepositPayload(CMsgPouchDeposit.decode(buffer)));
        @Override public Type<DepositPayload> type() { return TYPE; }
    }

        private record CaliberDataPayload(SMsgCaliberData message) implements CustomPacketPayload {
                private static final Type<CaliberDataPayload> TYPE = new Type<>(TaczCaliberAmmo.prefix("caliber_data"));
                private static final StreamCodec<RegistryFriendlyByteBuf, CaliberDataPayload> CODEC = StreamCodec.of(
                                (buffer, payload) -> payload.message().encode(buffer),
                                buffer -> new CaliberDataPayload(SMsgCaliberData.decode(buffer)));
                @Override public Type<CaliberDataPayload> type() { return TYPE; }
        }
    *///?}

    private PlatformNetwork() {
    }
}