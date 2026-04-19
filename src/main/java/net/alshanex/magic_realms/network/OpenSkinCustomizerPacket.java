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

public class OpenSkinCustomizerPacket implements CustomPacketPayload {
    public static final Type<OpenSkinCustomizerPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "open_skin_customizer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSkinCustomizerPacket> STREAM_CODEC =
            CustomPacketPayload.codec(OpenSkinCustomizerPacket::write, OpenSkinCustomizerPacket::new);

    public final UUID entityUUID;
    public final String gender;
    public final String entityClass;
    public final String currentSkin, currentClothes, currentEyes, currentHair;
    public final String currentName;

    public OpenSkinCustomizerPacket(UUID entityUUID, String gender, String entityClass, String currentName,
                                    String currentSkin, String currentClothes, String currentEyes, String currentHair) {
        this.entityUUID = entityUUID;
        this.gender = gender;
        this.entityClass = entityClass;
        this.currentName = currentName;
        this.currentSkin = currentSkin;
        this.currentClothes = currentClothes;
        this.currentEyes = currentEyes;
        this.currentHair = currentHair;
    }

    public OpenSkinCustomizerPacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readUtf(),
                buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(entityUUID);
        buf.writeUtf(gender);
        buf.writeUtf(entityClass);
        buf.writeUtf(currentName);
        buf.writeUtf(currentSkin);
        buf.writeUtf(currentClothes);
        buf.writeUtf(currentEyes);
        buf.writeUtf(currentHair);
    }

    public static void handle(OpenSkinCustomizerPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> MRUtils.openTextureCustomizationScreen(packet));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
