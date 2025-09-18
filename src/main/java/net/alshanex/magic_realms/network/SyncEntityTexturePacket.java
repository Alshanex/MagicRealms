package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.MRUtils;
import net.alshanex.magic_realms.util.humans.CombinedTextureManager;
import net.alshanex.magic_realms.util.humans.DynamicTextureManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

public class SyncEntityTexturePacket implements CustomPacketPayload {
    public static final Type<SyncEntityTexturePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "sync_entity_texture"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncEntityTexturePacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncEntityTexturePacket::write, SyncEntityTexturePacket::new);

    private final UUID entityUUID;
    private final int entityId; // Add this
    private final byte[] textureData;
    private final String textureName;
    private final boolean isPresetTexture;

    public SyncEntityTexturePacket(UUID entityUUID, int entityId, byte[] textureData, String textureName, boolean isPresetTexture) {
        this.entityUUID = entityUUID;
        this.entityId = entityId; // Include entity ID
        this.textureData = textureData;
        this.textureName = textureName != null ? textureName : "";
        this.isPresetTexture = isPresetTexture;
    }

    public SyncEntityTexturePacket(FriendlyByteBuf buf) {
        this.entityUUID = buf.readUUID();
        this.entityId = buf.readVarInt(); // Read entity ID
        int dataLength = buf.readVarInt();
        this.textureData = new byte[dataLength];
        buf.readBytes(this.textureData);
        this.textureName = buf.readUtf();
        this.isPresetTexture = buf.readBoolean();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(entityUUID);
        buf.writeVarInt(entityId); // Write entity ID
        buf.writeVarInt(textureData.length);
        buf.writeBytes(textureData);
        buf.writeUtf(textureName);
        buf.writeBoolean(isPresetTexture);
    }

    public static void handle(SyncEntityTexturePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                MRUtils.handleClientSideTextureSync(packet.entityUUID, packet.entityId, packet.textureData, packet.textureName, packet.isPresetTexture);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
