package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.screens.ContractHumanInfoMenu;
import net.alshanex.magic_realms.screens.ContractInventoryMenu;
import net.alshanex.magic_realms.util.MRUtils;
import net.alshanex.magic_realms.util.contracts.ContractUtils;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SwitchAttributesMenuPacket implements CustomPacketPayload {
    public static final Type<SwitchAttributesMenuPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "switch_attributes_menu"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchAttributesMenuPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SwitchAttributesMenuPacket::write, SwitchAttributesMenuPacket::new);

    private final ContractHumanInfoMenu.Tab tab;

    public SwitchAttributesMenuPacket(ContractHumanInfoMenu.Tab tab) {
        this.tab = tab;
    }

    public SwitchAttributesMenuPacket(FriendlyByteBuf buf) {
        this.tab = buf.readEnum(ContractHumanInfoMenu.Tab.class);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(tab);
    }

    public static void handle(SwitchAttributesMenuPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            MRUtils.switchToAttributesScreen(context.player(), packet.tab );
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
