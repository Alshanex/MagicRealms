package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.util.MRUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class SyncPresetNamePacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPresetNamePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "sync_preset_name"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPresetNamePacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncPresetNamePacket::write, SyncPresetNamePacket::new);

    private final UUID entityUUID;
    private final String presetName;

    public SyncPresetNamePacket(UUID entityUUID, String presetName) {
        this.entityUUID = entityUUID;
        this.presetName = presetName;
    }

    public SyncPresetNamePacket(FriendlyByteBuf buf) {
        this.entityUUID = buf.readUUID();
        this.presetName = buf.readUtf();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(entityUUID);
        buf.writeUtf(presetName);
    }

    public static void handle(SyncPresetNamePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            MRUtils.handlePresetNameSync(context.player(), packet.entityUUID, packet.presetName);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}