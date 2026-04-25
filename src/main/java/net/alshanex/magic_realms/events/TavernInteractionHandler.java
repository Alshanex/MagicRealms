package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.tavernkeep.TavernKeeperEntity;
import net.alshanex.magic_realms.registry.MRBlocks;
import net.alshanex.magic_realms.registry.MRItems;
import net.alshanex.magic_realms.util.humans.mercenaries.chat.MercenaryMessageFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;

@EventBusSubscriber(modid = MagicRealms.MODID)
public class TavernInteractionHandler {

    private static final ResourceLocation TAVERN_ID = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "tavern_plains");

    // Tracks which specific tavern each player is officially inside (and has been seen by the keeper)
    private static final Map<UUID, Long> PLAYERS_IN_TAVERNS = new HashMap<>();

    private static StructureStart getTavernAt(ServerLevel level, BlockPos pos) {
        Structure tavernStructure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TAVERN_ID);
        if (tavernStructure != null) {
            StructureStart start = level.structureManager().getStructureWithPieceAt(pos, tavernStructure);
            if (start.isValid()) {
                return start;
            }
        }
        return null;
    }

    /**
     * Scans for a valid tavern setup.
     * If valid, returns the TavernKeeperEntity. If invalid, returns null.
     */
    private static TavernKeeperEntity getValidKeeper(ServerLevel level, BoundingBox box) {
        AABB aabb = AABB.of(box);
        List<TavernKeeperEntity> keepers = level.getEntitiesOfClass(TavernKeeperEntity.class, aabb);

        // If the keeper is dead or missing, the tavern is invalid
        if (keepers.isEmpty()) return null;

        boolean foundBlock = false;

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    BlockState state = level.getBlockState(new BlockPos(x, y, z));

                    if (state.is(MRBlocks.WOODEN_CHAIR.get())) {
                        foundBlock = true;
                        break;
                    }
                }
                if (foundBlock) break;
            }
            if (foundBlock) break;
        }

        // If both the chair and keeper are present, return the keeper
        if (foundBlock) {
            return keepers.getFirst();
        }

        return null;
    }

    private static TavernKeeperEntity getValidKeeperAt(ServerLevel level, BlockPos pos) {
        StructureStart start = getTavernAt(level, pos);
        if (start == null) return null;
        return getValidKeeper(level, start.getBoundingBox());
    }

    // Events

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // Check once per second to save performance
        if (!player.level().isClientSide() && player.tickCount % 10 == 0) {
            ServerLevel serverLevel = (ServerLevel) player.level();
            UUID playerId = player.getUUID();

            StructureStart start = getTavernAt(serverLevel, player.blockPosition());

            if (start != null) {
                long currentTavernId = start.getChunkPos().toLong();
                Long previousTavernId = PLAYERS_IN_TAVERNS.get(playerId);

                // If they are physically inside the building, but NOT officially logged in our tracker yet
                if (previousTavernId == null || previousTavernId != currentTavernId) {

                    TavernKeeperEntity keeper = getValidKeeper(serverLevel, start.getBoundingBox());

                    // THE CHECK: Does the keeper exist, AND can they see the player?
                    if (keeper != null && keeper.hasLineOfSight(player)) {

                        int randomMessageIndex = player.getRandom().nextInt(3) + 1;
                        player.sendSystemMessage(MercenaryMessageFormatter.buildFor(
                                keeper,
                                "message.magic_realms.tavernkeep_welcome" + randomMessageIndex));

                        // ONLY log them into the tracker AFTER they have been seen
                        PLAYERS_IN_TAVERNS.put(playerId, currentTavernId);
                    }
                }
            } else {
                // The player is NOT in a tavern structure. If they were previously tracked, it means they just walked out the door.
                if (PLAYERS_IN_TAVERNS.containsKey(playerId)) {

                    // Remove them from the tracker first
                    PLAYERS_IN_TAVERNS.remove(playerId);

                    // Because they just stepped out of the bounding box, we don't have the 'start' variable anymore.
                    // Instead, we scan a 20-block radius around the player to find the keeper they just left.
                    AABB searchBox = player.getBoundingBox().inflate(20.0);
                    List<TavernKeeperEntity> nearbyKeepers = serverLevel.getEntitiesOfClass(TavernKeeperEntity.class, searchBox);

                    for (TavernKeeperEntity keeper : nearbyKeepers) {
                        if (keeper.distanceToSqr(player) <= 400.0D) {
                            int randomMessageIndex = player.getRandom().nextInt(3) + 1;
                            player.sendSystemMessage(MercenaryMessageFormatter.buildFor(
                                    keeper,
                                    "message.magic_realms.tavernkeep_farewell" + randomMessageIndex));
                            break;
                        }
                    }
                }
            }
        }
    }

    public static boolean isPositionInTavern(ServerLevel level, BlockPos pos) {
        StructureStart start = getTavernAt(level, pos);
        // We ensure the structure exists AND the keeper/chairs are intact
        return start != null && getValidKeeper(level, start.getBoundingBox()) != null;
    }

    @SubscribeEvent
    public static void onPlayerSleep(CanPlayerSleepEvent event) {
        Player player = event.getEntity();
        if (player.level() instanceof ServerLevel serverLevel) {

            // Resolve the tavern's keeper (if any).
            TavernKeeperEntity keeper = getValidKeeperAt(serverLevel, event.getPos());
            if (keeper != null) {

                // Check if they have the sleeping pass anywhere in their inventory
                boolean hasPass = false;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    if (player.getInventory().getItem(i).is(MRItems.SLEEPING_PASS.get())) {
                        hasPass = true;
                        break;
                    }
                }

                // If they don't have a pass, stop them from sleeping
                if (!hasPass) {
                    event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
                    player.sendSystemMessage(MercenaryMessageFormatter.buildForWithBodyColor(
                            keeper,
                            "message.magic_realms.tavern.unable_to_sleep",
                            ChatFormatting.RED));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {

            // The night was successfully skipped! Loop through all players in this dimension
            for (Player player : serverLevel.players()) {

                // Only target players who are actively sleeping right now
                if (player.isSleeping()) {
                    Optional<BlockPos> sleepPos = player.getSleepingPos();

                    // Check if they are sleeping inside a tavern bed
                    if (sleepPos.isPresent() && isPositionInTavern(serverLevel, sleepPos.get())) {

                        // Consume 1 pass from their inventory
                        boolean consumed = false;
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            ItemStack stack = player.getInventory().getItem(i);
                            if (stack.is(MRItems.SLEEPING_PASS.get())) {
                                stack.shrink(1);
                                consumed = true;
                                break;
                            }
                        }

                        // If successfully consumed, grant the buffs
                        if (consumed) {
                            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 6000, 0));
                            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 5));
                        }
                    }
                }
            }
        }
    }

    /**
     * Memory cleanup
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PLAYERS_IN_TAVERNS.remove(event.getEntity().getUUID());
    }
}
