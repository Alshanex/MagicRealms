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
 * Network payload to sync chimera assembly data from server to client.
 */
public record SyncChimeraAssemblyPayload(int entityId, CompoundTag assemblyData) implements CustomPacketPayload {

    public static final Type<SyncChimeraAssemblyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "sync_assembly"));

    public static final StreamCodec<ByteBuf, SyncChimeraAssemblyPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, SyncChimeraAssemblyPayload::entityId,
                    ByteBufCodecs.COMPOUND_TAG, SyncChimeraAssemblyPayload::assemblyData,
                    SyncChimeraAssemblyPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handle this payload on the client side.
     */
    public static void handle(SyncChimeraAssemblyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            MRClientUtils.handleSyncPacket(payload.entityId(), payload.assemblyData());
        });
    }
}
