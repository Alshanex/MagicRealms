package net.alshanex.magic_realms.network;

import io.netty.buffer.ByteBuf;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.MRClientUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from server to client when a player joins, carrying all loaded chimera part definitions so the client can display them in the GUI.
 * <p>
 * The definitions are serialized as a CompoundTag containing one entry per entity, keyed by entity ResourceLocation string.
 */
public record SyncPartRegistryPayload(CompoundTag registryData) implements CustomPacketPayload {

    public static final Type<SyncPartRegistryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "sync_part_registry"));

    public static final StreamCodec<ByteBuf, SyncPartRegistryPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG, SyncPartRegistryPayload::registryData,
                    SyncPartRegistryPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncPartRegistryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            MRClientUtils.handlePrtRegistrySyncPacket(payload.registryData());
        });
    }
}
