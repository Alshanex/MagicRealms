package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.humans.mercenaries.MercenaryDrinkClientCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class SyncMercenaryDrinkPacket implements CustomPacketPayload {

    public static final Type<SyncMercenaryDrinkPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "sync_mercenary_drink"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMercenaryDrinkPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncMercenaryDrinkPacket::write, SyncMercenaryDrinkPacket::new);

    private final UUID entityUUID;
    private final ItemStack stack;

    public SyncMercenaryDrinkPacket(UUID entityUUID, ItemStack stack) {
        this.entityUUID = entityUUID;
        this.stack = stack;
    }

    public SyncMercenaryDrinkPacket(FriendlyByteBuf buf) {
        this.entityUUID = buf.readUUID();
        this.stack = ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(entityUUID);
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, stack);
    }

    public static void handle(SyncMercenaryDrinkPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> MercenaryDrinkClientCache.set(packet.entityUUID, packet.stack));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
