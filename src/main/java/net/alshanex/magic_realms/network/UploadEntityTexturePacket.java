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

public class UploadEntityTexturePacket implements CustomPacketPayload {
    public static final Type<UploadEntityTexturePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "upload_entity_texture"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UploadEntityTexturePacket> STREAM_CODEC =
            CustomPacketPayload.codec(UploadEntityTexturePacket::write, UploadEntityTexturePacket::new);

    private final UUID entityUUID;
    private final byte[] textureData;
    private final String textureName;
    private final boolean isPresetTexture;

    public UploadEntityTexturePacket(UUID entityUUID, byte[] textureData, String textureName, boolean isPresetTexture) {
        this.entityUUID = entityUUID;
        this.textureData = textureData;
        this.textureName = textureName != null ? textureName : "";
        this.isPresetTexture = isPresetTexture;
    }

    public UploadEntityTexturePacket(FriendlyByteBuf buf) {
        this.entityUUID = buf.readUUID();
        int dataLength = buf.readVarInt();
        this.textureData = new byte[dataLength];
        buf.readBytes(this.textureData);
        this.textureName = buf.readUtf();
        this.isPresetTexture = buf.readBoolean();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(entityUUID);
        buf.writeVarInt(textureData.length);
        buf.writeBytes(textureData);
        buf.writeUtf(textureName);
        buf.writeBoolean(isPresetTexture);
    }

    public static void handle(UploadEntityTexturePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isServerbound()) {
                MRUtils.handleServerSideTextureUpload(context.player(), packet.entityUUID, packet.textureData, packet.textureName, packet.isPresetTexture);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
