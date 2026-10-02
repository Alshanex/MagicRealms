package net.alshanex.magic_realms.network;

import io.netty.buffer.ByteBuf;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.events.MercenaryTeleportHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** Sent when the player presses "Teleport" in the contracted mercenaries screen. */
public record TeleportMercenaryPacket(UUID mercenaryId) implements CustomPacketPayload {

    public static final Type<TeleportMercenaryPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "teleport_mercenary"));

    public static final StreamCodec<ByteBuf, TeleportMercenaryPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, TeleportMercenaryPacket::mercenaryId,
            TeleportMercenaryPacket::new
    );

    public static void handle(TeleportMercenaryPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                MercenaryTeleportHandler.requestTeleport(serverPlayer, packet.mercenaryId());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}