package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.ArchetypeInteraction;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.ArchetypeInteractionCatalog;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.ArchetypeInteractionCatalogHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Ships the archetype interaction catalog from server to client on datapack sync.
 */
public class SyncArchetypeInteractionCatalogPacket implements CustomPacketPayload {

    public static final Type<SyncArchetypeInteractionCatalogPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "sync_archetype_interaction_catalog"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncArchetypeInteractionCatalogPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncArchetypeInteractionCatalogPacket::write, SyncArchetypeInteractionCatalogPacket::new);

    private final List<ArchetypeInteraction> interactions;

    public SyncArchetypeInteractionCatalogPacket(ArchetypeInteractionCatalog catalog) {
        this.interactions = catalog.all();
    }

    public SyncArchetypeInteractionCatalogPacket(List<ArchetypeInteraction> interactions) {
        this.interactions = interactions;
    }

    public SyncArchetypeInteractionCatalogPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<ArchetypeInteraction> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(ArchetypeInteraction.readFromBuf(buf));
        this.interactions = list;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(interactions.size());
        for (ArchetypeInteraction i : interactions) ArchetypeInteraction.writeToBuf(buf, i);
    }

    public static void handle(SyncArchetypeInteractionCatalogPacket packet, IPayloadContext context) {
        context.enqueueWork(() ->
                ArchetypeInteractionCatalogHolder.setClient(new ArchetypeInteractionCatalog(packet.interactions)));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
