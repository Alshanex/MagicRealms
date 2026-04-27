package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.network.*;
import net.alshanex.magic_realms.util.humans.bandits.BanditProfileCatalogHolder;
import net.alshanex.magic_realms.util.humans.bandits.BanditProfileCatalogReloadListener;
import net.alshanex.magic_realms.util.humans.mercenaries.skins_management.SkinCatalogHolder;
import net.alshanex.magic_realms.util.humans.mercenaries.skins_management.SkinCatalogReloadListener;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ServerReloadEvents {
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        // Order matters: parts first, presets last (presets listener commits the catalog).
        event.addListener(SkinCatalogReloadListener.parts());
        event.addListener(SkinCatalogReloadListener.presets());
        event.addListener(new HobbyCatalogReloadListener());
        event.addListener(new FixedPersonalityCatalogReloadListener());
        event.addListener(new ArchetypeCatalogReloadListener());
        event.addListener(new ArchetypeInteractionCatalogReloadListener());
        event.addListener(new BanditProfileCatalogReloadListener());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        SyncSkinCatalogPacket packet = new SyncSkinCatalogPacket(SkinCatalogHolder.server());
        ServerPlayer target = event.getPlayer();
        if (target != null) {
            PacketDistributor.sendToPlayer(target, packet);
        } else {
            for (ServerPlayer p : event.getPlayerList().getPlayers()) {
                PacketDistributor.sendToPlayer(p, packet);
            }
        }

        SyncHobbyCatalogPacket hobbyPacket = new SyncHobbyCatalogPacket(HobbyCatalogHolder.server());
        if (target != null) {
            PacketDistributor.sendToPlayer(target, hobbyPacket);
        } else {
            for (ServerPlayer p : event.getPlayerList().getPlayers()) {
                PacketDistributor.sendToPlayer(p, hobbyPacket);
            }
        }

        SyncFixedPersonalityCatalogPacket fpPacket = new SyncFixedPersonalityCatalogPacket(FixedPersonalityCatalogHolder.server());
        if (target != null) {
            PacketDistributor.sendToPlayer(target, fpPacket);
        } else {
            for (ServerPlayer p : event.getPlayerList().getPlayers()) {
                PacketDistributor.sendToPlayer(p, fpPacket);
            }
        }

        SyncArchetypeCatalogPacket archetypePacket = new SyncArchetypeCatalogPacket(ArchetypeCatalogHolder.server());
        if (target != null) {
            PacketDistributor.sendToPlayer(target, archetypePacket);
        } else {
            for (ServerPlayer p : event.getPlayerList().getPlayers()) {
                PacketDistributor.sendToPlayer(p, archetypePacket);
            }
        }

        SyncArchetypeInteractionCatalogPacket interactionPacket = new SyncArchetypeInteractionCatalogPacket(ArchetypeInteractionCatalogHolder.server());
        if (target != null) {
            PacketDistributor.sendToPlayer(target, interactionPacket);
        } else {
            for (ServerPlayer p : event.getPlayerList().getPlayers()) {
                PacketDistributor.sendToPlayer(p, interactionPacket);
            }
        }

        SyncBanditProfileCatalogPacket banditPacket = new SyncBanditProfileCatalogPacket(BanditProfileCatalogHolder.server());
        if (target != null) {
            PacketDistributor.sendToPlayer(target, banditPacket);
        } else {
            for (ServerPlayer p : event.getPlayerList().getPlayers()) {
                PacketDistributor.sendToPlayer(p, banditPacket);
            }
        }
    }
}
