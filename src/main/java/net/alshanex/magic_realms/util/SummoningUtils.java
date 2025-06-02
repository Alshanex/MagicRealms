package net.alshanex.magic_realms.util;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.item.HumanInfoItem;
import net.alshanex.magic_realms.item.HumanTeamItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SummoningUtils {
    // Mapa para trackear las invocaciones activas por jugador
    private static final Map<UUID, SummonSession> ACTIVE_SUMMONS = new ConcurrentHashMap<>();

    // Mapa para trackear cooldowns por jugador
    private static final Map<UUID, Long> PLAYER_COOLDOWNS = new ConcurrentHashMap<>();

    // Constantes de tiempo (en milisegundos)
    private static final long SUMMON_DURATION = 120000L; // 2 minutos
    private static final long COOLDOWN_DURATION = 30000L; // 30 segundos adicionales

    public static void handleAlliesSummoning(ServerPlayer player) {
        if (!CurioUtils.isWearingTablet(player)) {
            player.sendSystemMessage(Component.literal("You need to wear a Summoning Tablet to use this!")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // Verificar cooldown
        if (isOnCooldown(player)) {
            long remainingMs = getRemainingCooldown(player);
            int remainingSeconds = (int) (remainingMs / 1000);
            player.sendSystemMessage(Component.literal("Summoning is on cooldown for " + remainingSeconds + " seconds!")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }

        // Verificar si ya tiene entidades sumoneadas
        if (hasActiveSummons(player)) {
            player.sendSystemMessage(Component.literal("You already have summoned allies!")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }

        ItemStack tabletItem = CurioUtils.getTablet(player);
        ItemStack[] teamMembers = HumanTeamItem.loadTeamFromItem(tabletItem);

        // Contar miembros válidos
        List<ItemStack> validMembers = new ArrayList<>();
        for (ItemStack member : teamMembers) {
            if (!member.isEmpty() && member.getItem() instanceof HumanInfoItem) {
                EntitySnapshot snapshot = HumanInfoItem.getEntitySnapshot(member);
                if (snapshot != null) {
                    validMembers.add(member);
                }
            }
        }

        if (validMembers.isEmpty()) {
            player.sendSystemMessage(Component.literal("No valid team members found in the tablet!")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // Crear sesión de invocación
        SummonSession session = new SummonSession(player, tabletItem, validMembers);

        // Intentar spawnear las entidades
        if (spawnAllies(session, player.serverLevel())) {
            ACTIVE_SUMMONS.put(player.getUUID(), session);

            // Programar la limpieza automática
            scheduleCleanup(player, session);

            player.sendSystemMessage(Component.literal("Summoned " + session.getSummonedEntities().size() + " allies for 2 minutes!")
                    .withStyle(ChatFormatting.GREEN));

            MagicRealms.LOGGER.info("Player {} summoned {} allies", player.getName().getString(), session.getSummonedEntities().size());
        } else {
            player.sendSystemMessage(Component.literal("Failed to summon allies!")
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static boolean spawnAllies(SummonSession session, ServerLevel level) {
        ServerPlayer player = session.getPlayer();
        List<ItemStack> memberItems = session.getMemberItems();

        for (ItemStack memberItem : memberItems) {
            EntitySnapshot snapshot = HumanInfoItem.getEntitySnapshot(memberItem);
            if (snapshot == null) continue;

            try {
                // Crear la entidad
                RandomHumanEntity entity = createEntityFromSnapshot(snapshot, level, player);

                // Encontrar posición de spawn válida
                Vec3 spawnPos = findValidSpawnPosition(player, level);
                entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

                // Aplicar el equipamiento del snapshot
                applyEquipmentFromSnapshot(entity, snapshot, level);

                // Finalizar spawn
                entity.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.MOB_SUMMONED, null);

                //Añadir como summoner
                entity.setSummoner(player);

                // Añadir al mundo
                level.addFreshEntity(entity);
                if(!entity.level().isClientSide){
                    MagicManager.spawnParticles(entity.level(), new BlastwaveParticleOptions(SchoolRegistry.EVOCATION.get().getTargetingColor(), 3), entity.getX(), entity.getY() + .165f, entity.getZ(), 1, 0, 0, 0, 0, true);
                }

                // Registrar en la sesión
                session.addSummonedEntity(entity, memberItem);

                MagicRealms.LOGGER.debug("Successfully spawned ally: {} at {}", entity.getEntityName(), spawnPos);

            } catch (Exception e) {
                MagicRealms.LOGGER.error("Failed to spawn ally from snapshot: {}", e.getMessage(), e);
                return false;
            }
        }

        return !session.getSummonedEntities().isEmpty();
    }

    private static RandomHumanEntity createEntityFromSnapshot(EntitySnapshot snapshot, Level level, ServerPlayer summoner) {
        RandomHumanEntity entity = new RandomHumanEntity(level, summoner);

        // Aplicar datos básicos del snapshot
        entity.setEntityName(snapshot.entityName);
        entity.setGender(snapshot.gender);
        entity.setEntityClass(snapshot.entityClass);
        entity.setStarLevel(snapshot.starLevel);
        entity.setHasShield(snapshot.hasShield);
        entity.setIsArcher(snapshot.isArcher);

        // Aplicar escuelas de magia
        entity.setMagicSchools(snapshot.magicSchools.stream()
                .map(schoolId -> {
                    try {
                        return io.redspace.ironsspellbooks.api.registry.SchoolRegistry.getSchool(
                                net.minecraft.resources.ResourceLocation.parse(schoolId));
                    } catch (Exception e) {
                        MagicRealms.LOGGER.warn("Failed to parse school ID: {}", schoolId);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList());

        // Aplicar level del kill tracker
        net.alshanex.magic_realms.events.KillTrackingHandler.setEntityLevel(entity, snapshot.currentLevel);

        entity.setInitialized(true);

        return entity;
    }

    private static void applyEquipmentFromSnapshot(RandomHumanEntity entity, EntitySnapshot snapshot, Level level) {
        var registryAccess = level.registryAccess();

        // Aplicar equipamiento desde el snapshot
        if (snapshot.equipment.contains("main_hand")) {
            ItemStack mainHand = ItemStack.parseOptional(registryAccess, snapshot.equipment.getCompound("main_hand"));
            entity.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, mainHand);
        }

        if (snapshot.equipment.contains("off_hand")) {
            ItemStack offHand = ItemStack.parseOptional(registryAccess, snapshot.equipment.getCompound("off_hand"));
            entity.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, offHand);
        }

        if (snapshot.equipment.contains("head")) {
            ItemStack head = ItemStack.parseOptional(registryAccess, snapshot.equipment.getCompound("head"));
            entity.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, head);
        }

        if (snapshot.equipment.contains("chest")) {
            ItemStack chest = ItemStack.parseOptional(registryAccess, snapshot.equipment.getCompound("chest"));
            entity.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest);
        }

        if (snapshot.equipment.contains("legs")) {
            ItemStack legs = ItemStack.parseOptional(registryAccess, snapshot.equipment.getCompound("legs"));
            entity.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, legs);
        }

        if (snapshot.equipment.contains("boots")) {
            ItemStack boots = ItemStack.parseOptional(registryAccess, snapshot.equipment.getCompound("boots"));
            entity.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, boots);
        }
    }

    private static Vec3 findValidSpawnPosition(ServerPlayer player, ServerLevel level) {
        Vec3 playerPos = player.position();
        RandomSource random = level.getRandom();

        // Intentar encontrar una posición válida en un radio de 3-7 bloques
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 3 + random.nextDouble() * 4; // Entre 3 y 7 bloques

            double x = playerPos.x + Math.cos(angle) * distance;
            double z = playerPos.z + Math.sin(angle) * distance;
            double y = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos((int) x, 0, (int) z)).getY();

            BlockPos spawnPos = new BlockPos((int) x, (int) y, (int) z);

            // Verificar si la posición es válida
            if (isValidSpawnPosition(level, spawnPos)) {
                return new Vec3(x, y, z);
            }
        }

        // Si no se encuentra una posición válida, usar la del jugador
        return playerPos;
    }

    private static boolean isValidSpawnPosition(ServerLevel level, BlockPos pos) {
        // Verificar que haya espacio suficiente (2 bloques de altura)
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        // Verificar que el bloque de abajo sea sólido
        if (!level.getBlockState(pos.below()).isSolid()) {
            return false;
        }

        return true;
    }

    private static void scheduleCleanup(ServerPlayer player, SummonSession session) {
        // Usar Timer para programar la limpieza después de 2 minutos
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // Ejecutar en el hilo principal del servidor
                player.server.execute(() -> {
                    if (ACTIVE_SUMMONS.get(player.getUUID()) == session) {
                        cleanupSummons(player, session, false);
                    }
                });
            }
        }, SUMMON_DURATION);
    }

    public static void cleanupSummons(ServerPlayer player, SummonSession session, boolean immediate) {
        try {
            MagicRealms.LOGGER.debug("Starting cleanup for player: {} (immediate: {})",
                    player.getName().getString(), immediate);

            // Actualizar los items con el estado actual de las entidades ANTES de despawnearlas
            updateItemsFromEntities(session);

            // Despawnear las entidades
            int despawnedCount = 0;
            for (RandomHumanEntity entity : session.getSummonedEntities()) {
                if (entity.isAlive()) {
                    entity.discard();
                    despawnedCount++;
                    MagicRealms.LOGGER.debug("Despawned entity: {}", entity.getEntityName());
                }
            }

            // Remover la sesión activa
            ACTIVE_SUMMONS.remove(player.getUUID());

            // Establecer cooldown si no es inmediato
            if (!immediate) {
                PLAYER_COOLDOWNS.put(player.getUUID(), System.currentTimeMillis() + COOLDOWN_DURATION);

                player.sendSystemMessage(Component.literal("Allies have been recalled (" + despawnedCount + " entities). Cooldown: 30 seconds.")
                        .withStyle(ChatFormatting.YELLOW));
            } else {
                player.sendSystemMessage(Component.literal("All summoned allies have been dismissed (" + despawnedCount + " entities).")
                        .withStyle(ChatFormatting.GRAY));
            }

            MagicRealms.LOGGER.info("Successfully cleaned up {} summons for player: {}",
                    despawnedCount, player.getName().getString());

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error during summon cleanup for player {}: {}",
                    player.getName().getString(), e.getMessage(), e);
        }
    }

    private static void updateItemsFromEntities(SummonSession session) {
        try {
            Map<RandomHumanEntity, ItemStack> entityItemMap = session.getEntityItemMap();
            ItemStack tabletItem = session.getTabletItem();
            ItemStack[] teamMembers = HumanTeamItem.loadTeamFromItem(tabletItem);

            MagicRealms.LOGGER.debug("Updating {} items from entities", entityItemMap.size());

            for (Map.Entry<RandomHumanEntity, ItemStack> entry : entityItemMap.entrySet()) {
                RandomHumanEntity entity = entry.getKey();
                ItemStack originalItem = entry.getValue();

                // Encontrar el índice del item en el array
                int itemIndex = -1;
                for (int i = 0; i < teamMembers.length; i++) {
                    if (teamMembers[i] == originalItem) {
                        itemIndex = i;
                        break;
                    }
                }

                if (itemIndex == -1) {
                    MagicRealms.LOGGER.warn("Could not find original item index for entity: {}", entity.getEntityName());
                    continue;
                }

                if (entity.isAlive()) {
                    // Actualizar el snapshot con el estado actual de la entidad
                    EntitySnapshot updatedSnapshot = EntitySnapshot.fromEntity(entity);
                    ItemStack updatedItem = HumanInfoItem.createVirtualItem(updatedSnapshot);

                    teamMembers[itemIndex] = updatedItem;

                    MagicRealms.LOGGER.debug("Updated item for living entity: {} at index: {}",
                            entity.getEntityName(), itemIndex);
                } else {
                    // Si la entidad murió, el slot ya debería estar vacío por onSummonedEntityDeath
                    // Pero por seguridad, asegurémonos
                    teamMembers[itemIndex] = ItemStack.EMPTY;

                    MagicRealms.LOGGER.debug("Ensured empty slot for dead entity: {} at index: {}",
                            entity.getEntityName(), itemIndex);
                }
            }

            updateEquippedTablet(session.getPlayer(), teamMembers);

            MagicRealms.LOGGER.debug("Successfully updated all team member items");

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error updating items from entities: {}", e.getMessage(), e);
        }
    }

    public static void onSummonedEntityDeath(RandomHumanEntity entity) {
        // Buscar la sesión que contiene esta entidad
        for (Map.Entry<UUID, SummonSession> entry : ACTIVE_SUMMONS.entrySet()) {
            SummonSession session = entry.getValue();
            if (session.getSummonedEntities().contains(entity)) {
                ServerPlayer player = session.getPlayer();

                // Actualizar el item con el estado final de la entidad antes de eliminarlo
                updateEntityItemBeforeDeath(session, entity);

                // Eliminar la entidad de la sesión
                session.getSummonedEntities().remove(entity);
                session.getEntityItemMap().remove(entity);

                player.sendSystemMessage(Component.literal("One of your allies has fallen!")
                        .withStyle(ChatFormatting.RED));

                MagicRealms.LOGGER.info("Summoned entity {} died for player {}, item removed from team",
                        entity.getEntityName(), player.getName().getString());
                break;
            }
        }
    }

    private static void updateEntityItemBeforeDeath(SummonSession session, RandomHumanEntity dyingEntity) {
        try {
            ItemStack correspondingItem = session.getEntityItemMap().get(dyingEntity);
            if (correspondingItem == null) {
                MagicRealms.LOGGER.warn("No corresponding item found for dying entity: {}", dyingEntity.getEntityName());
                return;
            }

            ItemStack tabletItem = session.getTabletItem();
            ItemStack[] teamMembers = HumanTeamItem.loadTeamFromItem(tabletItem);

            // Encontrar el índice del item en el array
            int itemIndex = -1;
            for (int i = 0; i < teamMembers.length; i++) {
                if (teamMembers[i] == correspondingItem) {
                    itemIndex = i;
                    break;
                }
            }

            if (itemIndex != -1) {
                // Actualizar el item con el estado final de la entidad
                EntitySnapshot finalSnapshot = EntitySnapshot.fromEntity(dyingEntity);
                ItemStack updatedItem = HumanInfoItem.createVirtualItem(finalSnapshot);
                teamMembers[itemIndex] = updatedItem;

                // Luego eliminarlo (marcarlo como vacío)
                teamMembers[itemIndex] = ItemStack.EMPTY;

                updateEquippedTablet(session.getPlayer(), teamMembers);

                MagicRealms.LOGGER.debug("Updated and removed item for dead entity: {} at index: {}",
                        dyingEntity.getEntityName(), itemIndex);
            } else {
                MagicRealms.LOGGER.warn("Could not find item index for dying entity: {}", dyingEntity.getEntityName());
            }

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error updating item before entity death: {}", e.getMessage(), e);
        }
    }

    private static void updateEquippedTablet(ServerPlayer player, ItemStack[] newTeamMembers) {
        try {
            if (!CurioUtils.isWearingTablet(player)) {
                MagicRealms.LOGGER.warn("Player {} is not wearing a tablet", player.getName().getString());
                return;
            }

            ItemStack equippedTablet = CurioUtils.getTablet(player);
            if (equippedTablet.isEmpty()) {
                MagicRealms.LOGGER.warn("Could not get equipped tablet for player: {}", player.getName().getString());
                return;
            }

            // Actualizar directamente la tablet equipada
            HumanTeamItem.saveTeamToItem(equippedTablet, newTeamMembers);

            MagicRealms.LOGGER.debug("Updated equipped tablet directly for player: {}", player.getName().getString());

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error updating equipped tablet for player {}: {}",
                    player.getName().getString(), e.getMessage(), e);
        }
    }

    // Métodos de utilidad para verificar estado
    public static boolean hasActiveSummons(ServerPlayer player) {
        return ACTIVE_SUMMONS.containsKey(player.getUUID());
    }

    public static boolean isOnCooldown(ServerPlayer player) {
        Long cooldownEnd = PLAYER_COOLDOWNS.get(player.getUUID());
        if (cooldownEnd == null) return false;

        if (System.currentTimeMillis() >= cooldownEnd) {
            PLAYER_COOLDOWNS.remove(player.getUUID());
            return false;
        }

        return true;
    }

    public static long getRemainingCooldown(ServerPlayer player) {
        Long cooldownEnd = PLAYER_COOLDOWNS.get(player.getUUID());
        if (cooldownEnd == null) return 0;

        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        SummonSession session = ACTIVE_SUMMONS.get(player.getUUID());
        if (session != null) {
            cleanupSummons(player, session, true);
        }
    }

    // Clase interna para manejar sesiones de invocación
    private static class SummonSession {
        private final ServerPlayer player;
        private final ItemStack tabletItem;
        private final List<ItemStack> memberItems;
        private final List<RandomHumanEntity> summonedEntities;
        private final Map<RandomHumanEntity, ItemStack> entityItemMap;
        private final long startTime;

        public SummonSession(ServerPlayer player, ItemStack tabletItem, List<ItemStack> memberItems) {
            this.player = player;
            this.tabletItem = tabletItem;
            this.memberItems = new ArrayList<>(memberItems);
            this.summonedEntities = new ArrayList<>();
            this.entityItemMap = new HashMap<>();
            this.startTime = System.currentTimeMillis();
        }

        public void addSummonedEntity(RandomHumanEntity entity, ItemStack correspondingItem) {
            summonedEntities.add(entity);
            entityItemMap.put(entity, correspondingItem);
        }

        // Getters
        public ServerPlayer getPlayer() { return player; }
        public ItemStack getTabletItem() { return tabletItem; }
        public List<ItemStack> getMemberItems() { return memberItems; }
        public List<RandomHumanEntity> getSummonedEntities() { return summonedEntities; }
        public Map<RandomHumanEntity, ItemStack> getEntityItemMap() { return entityItemMap; }
        public long getStartTime() { return startTime; }
    }
}
