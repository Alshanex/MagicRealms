package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.item.FloatingArrowItem;
import net.alshanex.magic_realms.util.MRUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: the player's current floating-arrow mode.
 * <p>
 * Sent only when the mode changes (held/released a mouse button), to keep traffic minimal.
 * The server validates the player is actually holding a {@link FloatingArrowItem} before applying.
 */
public record FloatingArrowModePacket(byte mode) implements CustomPacketPayload {

    public static final Type<FloatingArrowModePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "floating_arrow_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FloatingArrowModePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BYTE, FloatingArrowModePacket::mode,
                    FloatingArrowModePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FloatingArrowModePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            MRUtils.handleFlyingArrowPacket(ctx.player(), pkt.mode());
        });
    }
}
