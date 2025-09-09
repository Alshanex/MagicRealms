package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ServerWorldEventHandler {

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        // Only handle server-side world loading
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            try {
                // Create the magic_realms_textures directory structure in the world save folder
                Path worldSaveDir = serverLevel.getServer().getWorldPath(LevelResource.ROOT);
                Path textureDir = worldSaveDir.resolve("magic_realms_textures").resolve("entity").resolve("human");

                if (!Files.exists(textureDir)) {
                    Files.createDirectories(textureDir);
                    MagicRealms.LOGGER.info("Created texture directory structure for world '{}' at: {}",
                            serverLevel.dimension().location(), textureDir);
                } else {
                    MagicRealms.LOGGER.debug("Texture directory already exists for world '{}' at: {}",
                            serverLevel.dimension().location(), textureDir);
                }

                // Create a marker file to indicate this world supports Magic Realms textures
                Path markerFile = textureDir.resolve(".magic_realms_marker");
                if (!Files.exists(markerFile)) {
                    Files.createFile(markerFile);
                    Files.write(markerFile, ("Magic Realms texture directory created on: " +
                            java.time.Instant.now().toString()).getBytes());
                }

            } catch (IOException e) {
                MagicRealms.LOGGER.error("Failed to create texture directory for world '{}': {}",
                        serverLevel.dimension().location(), e.getMessage());
            }
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MagicRealms.LOGGER.info("Magic Realms server starting - texture directories will be created for each world as they load");
    }
}
