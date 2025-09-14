package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.util.MRUtils;
import net.alshanex.magic_realms.util.humans.AdvancedNameManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class SyncEntityLevelPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncEntityLevelPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "sync_entity_level"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncEntityLevelPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncEntityLevelPacket::write, SyncEntityLevelPacket::new);

    private final int entityId;
    private final UUID entityUUID;
    private final int level;

    public SyncEntityLevelPacket(int entityId, UUID entityUUID, int level) {
        this.entityId = entityId;
        this.entityUUID = entityUUID;
        this.level = level;
    }

    public SyncEntityLevelPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.entityUUID = buf.readUUID();
        this.level = buf.readVarInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUUID(entityUUID);
        buf.writeVarInt(level);
    }

    public static void handle(SyncEntityLevelPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            MRUtils.handleEntityLevelSync(packet.entityId, packet.level, packet.entityUUID);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
