package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.FixedPersonalityCatalog;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.FixedPersonalityCatalogHolder;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.FixedPersonalityDef;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Ships the fixed personality catalog from server to client on datapack sync.
 */
public class SyncFixedPersonalityCatalogPacket implements CustomPacketPayload {

    public static final Type<SyncFixedPersonalityCatalogPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "sync_fixed_personality_catalog"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncFixedPersonalityCatalogPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncFixedPersonalityCatalogPacket::write, SyncFixedPersonalityCatalogPacket::new);

    private final List<FixedPersonalityDef> defs;

    public SyncFixedPersonalityCatalogPacket(FixedPersonalityCatalog catalog) {
        this.defs = catalog.all();
    }

    public SyncFixedPersonalityCatalogPacket(List<FixedPersonalityDef> defs) {
        this.defs = defs;
    }

    public SyncFixedPersonalityCatalogPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<FixedPersonalityDef> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(FixedPersonalityDef.readFromBuf(buf));
        this.defs = list;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(defs.size());
        for (FixedPersonalityDef d : defs) FixedPersonalityDef.writeToBuf(buf, d);
    }

    public static void handle(SyncFixedPersonalityCatalogPacket packet, IPayloadContext context) {
        context.enqueueWork(() ->
                FixedPersonalityCatalogHolder.setClient(new FixedPersonalityCatalog(packet.defs)));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
