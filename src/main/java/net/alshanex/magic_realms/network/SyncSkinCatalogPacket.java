package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.skins_management.SkinCatalog;
import net.alshanex.magic_realms.skins_management.SkinPart;
import net.alshanex.magic_realms.skins_management.SkinPreset;
import net.alshanex.magic_realms.util.MRUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class SyncSkinCatalogPacket implements CustomPacketPayload {
    public static final Type<SyncSkinCatalogPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "sync_skin_catalog"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSkinCatalogPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncSkinCatalogPacket::write, SyncSkinCatalogPacket::new);

    private final List<SkinPart> parts;
    private final List<SkinPreset> presets;

    public SyncSkinCatalogPacket(SkinCatalog catalog) {
        this.parts = catalog.allParts();
        this.presets = catalog.allPresets();
    }

    public SyncSkinCatalogPacket(List<SkinPart> parts, List<SkinPreset> presets) {
        this.parts = parts;
        this.presets = presets;
    }

    public SyncSkinCatalogPacket(FriendlyByteBuf buf) {
        int partCount = buf.readVarInt();
        this.parts = new java.util.ArrayList<>(partCount);
        for (int i = 0; i < partCount; i++) parts.add(SkinPart.readFromBuf(buf));

        int presetCount = buf.readVarInt();
        this.presets = new java.util.ArrayList<>(presetCount);
        for (int i = 0; i < presetCount; i++) presets.add(SkinPreset.readFromBuf(buf));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(parts.size());
        for (SkinPart p : parts) SkinPart.writeToBuf(buf, p);
        buf.writeVarInt(presets.size());
        for (SkinPreset p : presets) SkinPreset.writeToBuf(buf, p);
    }

    public static void handle(SyncSkinCatalogPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            MRUtils.handleSkinCatalogSync(packet.parts, packet.presets);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
