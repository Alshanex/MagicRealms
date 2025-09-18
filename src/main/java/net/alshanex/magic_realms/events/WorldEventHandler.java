package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.humans.CombinedTextureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.nio.file.Files;
import java.nio.file.Path;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class WorldEventHandler {

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onPlayerJoinWorld(ClientPlayerNetworkEvent.LoggingIn event) {
        try {
            Minecraft mc = Minecraft.getInstance();

            // Check if we're in a singleplayer world
            IntegratedServer server = mc.getSingleplayerServer();
            if (server != null) {
                // Singleplayer world - the ServerWorldEventHandler should have already created the directory
                Path worldSaveDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
                String worldName = server.getWorldData().getLevelName();

                // Verify the directory exists (it should have been created by the server-side handler)
                Path expectedTextureDir = worldSaveDir.resolve("magic_realms_textures").resolve("entity").resolve("human");
                if (!Files.exists(expectedTextureDir)) {
                    MagicRealms.LOGGER.warn("Expected texture directory not found for world '{}', creating it now: {}",
                            worldName, expectedTextureDir);
                    try {
                        Files.createDirectories(expectedTextureDir);
                    } catch (Exception e) {
                        MagicRealms.LOGGER.error("Failed to create missing texture directory", e);
                    }
                }

                MagicRealms.LOGGER.info("Player joined singleplayer world: '{}' - using texture directory: {}",
                        worldName, expectedTextureDir);
                CombinedTextureManager.setWorldDirectory(worldSaveDir, worldName);
            } else if (mc.getCurrentServer() != null) {
                // Multiplayer world - DON'T create local directories
                String serverName = mc.getCurrentServer().name;

                MagicRealms.LOGGER.info("Player joined multiplayer server: '{}' - textures will be received from server", serverName);

                // Set a null/dummy directory to indicate we're in multiplayer mode
                CombinedTextureManager.setMultiplayerMode(serverName);
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to determine world directory on player join", e);
            // For multiplayer, don't create fallback directories
            Minecraft mc = Minecraft.getInstance();
            if (mc.getSingleplayerServer() != null) {
                // Only fallback for singleplayer
                CombinedTextureManager.initializeDirectories();
            }
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onPlayerLeaveWorld(ClientPlayerNetworkEvent.LoggingOut event) {
        MagicRealms.LOGGER.debug("Player left world, clearing texture cache");
        CombinedTextureManager.onWorldUnload();
    }
}
