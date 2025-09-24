package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.screens.ContractHumanInfoMenu;
import net.alshanex.magic_realms.util.MRUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class SwitchTabPacket implements CustomPacketPayload {
    public static final Type<SwitchTabPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "switch_tab"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchTabPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SwitchTabPacket::write, SwitchTabPacket::new);

    private final String tabName; // Change to String to handle "INVENTORY" special case

    public SwitchTabPacket(ContractHumanInfoMenu.Tab tab) {
        this.tabName = tab.name();
    }

    // New constructor for inventory case
    public SwitchTabPacket(String tabName) {
        this.tabName = tabName;
    }

    public SwitchTabPacket(FriendlyByteBuf buf) {
        this.tabName = buf.readUtf();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(tabName);
    }

    public static void handle(SwitchTabPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            MRUtils.handleTabSwitch(context.player(), packet.tabName);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
