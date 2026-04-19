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

public class SaveSkinPartsPacket implements CustomPacketPayload {
    public static final Type<SaveSkinPartsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "save_skin_parts"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveSkinPartsPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SaveSkinPartsPacket::write, SaveSkinPartsPacket::new);

    private final UUID entityUUID;
    private final String skin, clothes, eyes, hair;

    public SaveSkinPartsPacket(UUID entityUUID, String skin, String clothes, String eyes, String hair) {
        this.entityUUID = entityUUID;
        this.skin = skin;
        this.clothes = clothes;
        this.eyes = eyes;
        this.hair = hair;
    }

    public SaveSkinPartsPacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(entityUUID);
        buf.writeUtf(skin);
        buf.writeUtf(clothes);
        buf.writeUtf(eyes);
        buf.writeUtf(hair);
    }

    public static void handle(SaveSkinPartsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            MRUtils.handleTextureCustomizationSave(context.player(), packet.entityUUID, packet.skin, packet.clothes, packet.eyes, packet.hair);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
