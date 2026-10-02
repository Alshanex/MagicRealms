package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractedMercenaryEntry;
import net.alshanex.magic_realms.util.humans.mercenaries.ClientContractedMercenaries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Full list of the player's contracted mercenaries in their <b>current</b> dimension.
 */
public record SyncContractedMercenariesPacket(List<ContractedMercenaryEntry> entries) implements CustomPacketPayload {

    public static final Type<SyncContractedMercenariesPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "sync_contracted_mercenaries"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncContractedMercenariesPacket> STREAM_CODEC =
            ContractedMercenaryEntry.STREAM_CODEC.apply(ByteBufCodecs.list())
                    .map(SyncContractedMercenariesPacket::new, SyncContractedMercenariesPacket::entries);

    public static void handle(SyncContractedMercenariesPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientContractedMercenaries.set(packet.entries()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}