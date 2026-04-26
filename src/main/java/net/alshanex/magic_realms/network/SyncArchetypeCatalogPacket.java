package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.Archetype;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.ArchetypeCatalog;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.ArchetypeCatalogHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Ships the archetype catalog (datapack-loaded definitions) from server to client on datapack sync.
 */
public class SyncArchetypeCatalogPacket implements CustomPacketPayload {

    public static final Type<SyncArchetypeCatalogPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "sync_archetype_catalog"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncArchetypeCatalogPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncArchetypeCatalogPacket::write, SyncArchetypeCatalogPacket::new);

    private final List<Archetype> archetypes;

    public SyncArchetypeCatalogPacket(ArchetypeCatalog catalog) {
        this.archetypes = catalog.all();
    }

    public SyncArchetypeCatalogPacket(List<Archetype> archetypes) {
        this.archetypes = archetypes;
    }

    public SyncArchetypeCatalogPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Archetype> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(Archetype.readFromBuf(buf));
        this.archetypes = list;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(archetypes.size());
        for (Archetype a : archetypes) Archetype.writeToBuf(buf, a);
    }

    public static void handle(SyncArchetypeCatalogPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ArchetypeCatalogHolder.setClient(new ArchetypeCatalog(packet.archetypes)));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
