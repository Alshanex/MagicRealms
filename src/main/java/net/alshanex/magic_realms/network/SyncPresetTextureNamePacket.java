package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.MRUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class SyncPresetTextureNamePacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPresetTextureNamePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "sync_preset_texture_name"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPresetTextureNamePacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncPresetTextureNamePacket::write, SyncPresetTextureNamePacket::new);

    private final UUID entityUUID;
    private final String presetTextureName;
    private final boolean hasPresetTexture;

    public SyncPresetTextureNamePacket(UUID entityUUID, String presetTextureName, boolean hasPresetTexture) {
        this.entityUUID = entityUUID;
        this.presetTextureName = presetTextureName != null ? presetTextureName : "";
        this.hasPresetTexture = hasPresetTexture;
    }

    public SyncPresetTextureNamePacket(FriendlyByteBuf buf) {
        this.entityUUID = buf.readUUID();
        this.presetTextureName = buf.readUtf();
        this.hasPresetTexture = buf.readBoolean();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(entityUUID);
        buf.writeUtf(presetTextureName);
        buf.writeBoolean(hasPresetTexture);
    }

    public static void handle(SyncPresetTextureNamePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            MRUtils.handlePresetTextureNameSync(context.player(), packet.entityUUID, packet.presetTextureName, packet.hasPresetTexture);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
