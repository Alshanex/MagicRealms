package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.humans.titles.Title;
import net.alshanex.magic_realms.util.humans.titles.TitleCatalog;
import net.alshanex.magic_realms.util.humans.titles.TitleCatalogHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Ships the title catalog (datapack-loaded definitions) from server to client on datapack sync, so the contract
 * screen can render title names, colours and descriptions without a per-entity round trip.
 */
public class SyncTitleCatalogPacket implements CustomPacketPayload {

    public static final Type<SyncTitleCatalogPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "sync_title_catalog"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTitleCatalogPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncTitleCatalogPacket::write, SyncTitleCatalogPacket::new);

    private final List<Title> titles;

    public SyncTitleCatalogPacket(TitleCatalog catalog) {
        this.titles = catalog.all();
    }

    public SyncTitleCatalogPacket(List<Title> titles) {
        this.titles = titles;
    }

    public SyncTitleCatalogPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Title> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(Title.readFromBuf(buf));
        this.titles = list;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(titles.size());
        for (Title t : titles) Title.writeToBuf(buf, t);
    }

    public static void handle(SyncTitleCatalogPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> TitleCatalogHolder.setClient(new TitleCatalog(packet.titles)));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
