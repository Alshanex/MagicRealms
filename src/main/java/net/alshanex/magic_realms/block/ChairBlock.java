package net.alshanex.magic_realms.block;

import com.mojang.serialization.MapCodec;
import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.events.ExclusiveMercenaryTracker;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

public class ChairBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<ChairBlock> CODEC = simpleCodec(ChairBlock::new);

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // -- NORTH facing (backrest at south, +Z side) --
    private static final VoxelShape LEG_SE_NORTH = Block.box(11, 0, 11, 13, 8, 13);       // back-right leg
    private static final VoxelShape LEG_NE_NORTH = Block.box(11, 0, 3, 13, 8, 5);          // front-right leg
    private static final VoxelShape LEG_SW_NORTH = Block.box(3, 0, 11, 5, 8, 13);          // back-left leg
    private static final VoxelShape LEG_NW_NORTH = Block.box(3, 0, 3, 5, 8, 5);            // front-left leg
    private static final VoxelShape SEAT_NORTH = Block.box(3, 8, 3, 13, 10, 13);           // seat
    private static final VoxelShape CUSHION_NORTH = Block.box(4, 9.5, 4, 12, 10.5, 12);    // cushion
    private static final VoxelShape BACK_POST_L_NORTH = Block.box(3, 10, 11, 5, 22, 13);   // left backrest post
    private static final VoxelShape BACK_POST_R_NORTH = Block.box(11, 10, 11, 13, 22, 13); // right backrest post
    private static final VoxelShape BACK_PANEL_NORTH = Block.box(5, 12, 11, 11, 20, 13);   // backrest panel
    private static final VoxelShape BACK_FRAME_NORTH = Block.box(4.75, 11.75, 10.75, 11.25, 20.25, 12.25); // backrest frame

    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            LEG_SE_NORTH, LEG_NE_NORTH, LEG_SW_NORTH, LEG_NW_NORTH,
            SEAT_NORTH, CUSHION_NORTH, BACK_POST_L_NORTH, BACK_POST_R_NORTH,
            BACK_PANEL_NORTH, BACK_FRAME_NORTH
    );

    // -- SOUTH facing (backrest at north, -Z side) -- rotate 180 around center (8,8): swap x→16-x, z→16-z
    private static final VoxelShape LEG_SE_SOUTH = Block.box(3, 0, 3, 5, 8, 5);
    private static final VoxelShape LEG_NE_SOUTH = Block.box(3, 0, 11, 5, 8, 13);
    private static final VoxelShape LEG_SW_SOUTH = Block.box(11, 0, 3, 13, 8, 5);
    private static final VoxelShape LEG_NW_SOUTH = Block.box(11, 0, 11, 13, 8, 13);
    private static final VoxelShape SEAT_SOUTH = Block.box(3, 8, 3, 13, 10, 13);
    private static final VoxelShape CUSHION_SOUTH = Block.box(4, 9.5, 4, 12, 10.5, 12);
    private static final VoxelShape BACK_POST_L_SOUTH = Block.box(11, 10, 3, 13, 22, 5);
    private static final VoxelShape BACK_POST_R_SOUTH = Block.box(3, 10, 3, 5, 22, 5);
    private static final VoxelShape BACK_PANEL_SOUTH = Block.box(5, 12, 3, 11, 20, 5);
    private static final VoxelShape BACK_FRAME_SOUTH = Block.box(4.75, 11.75, 3.75, 11.25, 20.25, 5.25);

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            LEG_SE_SOUTH, LEG_NE_SOUTH, LEG_SW_SOUTH, LEG_NW_SOUTH,
            SEAT_SOUTH, CUSHION_SOUTH, BACK_POST_L_SOUTH, BACK_POST_R_SOUTH,
            BACK_PANEL_SOUTH, BACK_FRAME_SOUTH
    );

    // -- EAST facing (backrest at west, -X side) -- rotate 90 CW: (x,z) → (16-z, x)
    private static final VoxelShape LEG_SE_EAST = Block.box(3, 0, 11, 5, 8, 13);
    private static final VoxelShape LEG_NE_EAST = Block.box(11, 0, 11, 13, 8, 13);
    private static final VoxelShape LEG_SW_EAST = Block.box(3, 0, 3, 5, 8, 5);
    private static final VoxelShape LEG_NW_EAST = Block.box(11, 0, 3, 13, 8, 5);
    private static final VoxelShape SEAT_EAST = Block.box(3, 8, 3, 13, 10, 13);
    private static final VoxelShape CUSHION_EAST = Block.box(4, 9.5, 4, 12, 10.5, 12);
    private static final VoxelShape BACK_POST_L_EAST = Block.box(3, 10, 3, 5, 22, 5);
    private static final VoxelShape BACK_POST_R_EAST = Block.box(3, 10, 11, 5, 22, 13);
    private static final VoxelShape BACK_PANEL_EAST = Block.box(3, 12, 5, 5, 20, 11);
    private static final VoxelShape BACK_FRAME_EAST = Block.box(3.75, 11.75, 4.75, 5.25, 20.25, 11.25);

    private static final VoxelShape SHAPE_WEST = Shapes.or(
            LEG_SE_EAST, LEG_NE_EAST, LEG_SW_EAST, LEG_NW_EAST,
            SEAT_EAST, CUSHION_EAST, BACK_POST_L_EAST, BACK_POST_R_EAST,
            BACK_PANEL_EAST, BACK_FRAME_EAST
    );

    // -- WEST facing (backrest at east, +X side) -- rotate 90 CCW: (x,z) → (z, 16-x)
    private static final VoxelShape LEG_SE_WEST = Block.box(11, 0, 3, 13, 8, 5);
    private static final VoxelShape LEG_NE_WEST = Block.box(3, 0, 3, 5, 8, 5);
    private static final VoxelShape LEG_SW_WEST = Block.box(11, 0, 11, 13, 8, 13);
    private static final VoxelShape LEG_NW_WEST = Block.box(3, 0, 11, 5, 8, 13);
    private static final VoxelShape SEAT_WEST = Block.box(3, 8, 3, 13, 10, 13);
    private static final VoxelShape CUSHION_WEST = Block.box(4, 9.5, 4, 12, 10.5, 12);
    private static final VoxelShape BACK_POST_L_WEST = Block.box(11, 10, 11, 13, 22, 13);
    private static final VoxelShape BACK_POST_R_WEST = Block.box(11, 10, 3, 13, 22, 5);
    private static final VoxelShape BACK_PANEL_WEST = Block.box(11, 12, 5, 13, 20, 11);
    private static final VoxelShape BACK_FRAME_WEST = Block.box(10.75, 11.75, 4.75, 12.25, 20.25, 11.25);

    private static final VoxelShape SHAPE_EAST = Shapes.or(
            LEG_SE_WEST, LEG_NE_WEST, LEG_SW_WEST, LEG_NW_WEST,
            SEAT_WEST, CUSHION_WEST, BACK_POST_L_WEST, BACK_POST_R_WEST,
            BACK_PANEL_WEST, BACK_FRAME_WEST
    );

    // Cooldown tracking per chair position
    private static final Map<BlockPos, Long> SPAWN_COOLDOWNS = new HashMap<>();
    private static final long SPAWN_COOLDOWN_TICKS = 600; // 30 seconds
    private static final int SPAWN_CHECK_RADIUS = 20;

    // Entity spawn chances
    private static final double EXCLUSIVE_MERCENARY_CHANCE = 0.01; // 1%

    public ChairBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (facing) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos above = context.getClickedPos().above();
        if (!context.getLevel().getBlockState(above).canBeReplaced(context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            // Check if there's already an entity sitting in this chair
            AbstractMercenaryEntity sittingEntity = getSittingEntity(serverLevel, pos);
            if (sittingEntity != null) {
                sittingEntity.unsitFromChair();
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private boolean isOverworld(ServerLevel level) {
        return level.dimension() == Level.OVERWORLD;
    }

    private void trySpawnEntity(ServerLevel level, BlockPos pos, BlockState state) {
        if (canSpawnEntity(level, pos)) {
            spawnRandomEntity(level, pos, state);
        }
    }

    private boolean canSpawnEntity(ServerLevel level, BlockPos pos) {
        // Check cooldown
        long currentTime = level.getGameTime();
        Long lastSpawnTime = SPAWN_COOLDOWNS.get(pos);
        if (lastSpawnTime != null && (currentTime - lastSpawnTime) < SPAWN_COOLDOWN_TICKS) {
            return false;
        }

        // Check if chair is already occupied
        AbstractMercenaryEntity sitting = getSittingEntity(level, pos);
        if (sitting != null) {
            return false;
        }

        // Check entity count in radius
        AABB searchArea = new AABB(pos).inflate(SPAWN_CHECK_RADIUS);
        List<AbstractMercenaryEntity> nearbyEntities = level.getEntitiesOfClass(AbstractMercenaryEntity.class, searchArea);

        if (nearbyEntities.size() >= Config.maxMercenariesInRadius) {
            return false;
        }
        return level.getDifficulty() == Difficulty.PEACEFUL ? false : level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
    }

    private void spawnRandomEntity(ServerLevel level, BlockPos pos, BlockState state) {
        if (!isOverworld(level)) {
            return;
        }

        try {
            AbstractMercenaryEntity entityToSpawn = null;

            // Determine what type of entity to spawn
            double random = level.getRandom().nextDouble();

            if (random < EXCLUSIVE_MERCENARY_CHANCE) {
                // Try to spawn an exclusive mercenary using tags
                entityToSpawn = tryCreateExclusiveMercenaryUsingTags(level);
                /*
                MagicRealms.LOGGER.debug("Chair at {} rolled for exclusive mercenary ({}%), result: {}",
                        pos, (int)(EXCLUSIVE_MERCENARY_CHANCE * 100),
                        entityToSpawn != null ? entityToSpawn.getClass().getSimpleName() : "none available");

                 */
            }

            // If no exclusive mercenary could be spawned, default to RandomHumanEntity
            if (entityToSpawn == null) {
                entityToSpawn = new RandomHumanEntity(MREntityRegistry.HUMAN.get(), level);
                //MagicRealms.LOGGER.debug("Chair at {} spawning RandomHumanEntity (fallback or 90% chance)", pos);
            }

            // Position and spawn the entity
            spawnAndSitEntity(level, pos, state, entityToSpawn);

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error determining entity type to spawn at chair " + pos, e);
        }
    }

    @Nullable
    private AbstractMercenaryEntity tryCreateExclusiveMercenaryUsingTags(ServerLevel level) {
        // Get all entity types tagged as exclusive mercenaries
        List<EntityType<?>> exclusiveMercenaryTypes = BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(entityType -> entityType.is(ModTags.EXCLUSIVE_MERCENARIES))
                .toList();

        if (exclusiveMercenaryTypes.isEmpty()) {
            MagicRealms.LOGGER.debug("No exclusive mercenaries found in tag");
            return null;
        }

        // Get available types that aren't already in the world
        List<EntityType<?>> availableTypes = ExclusiveMercenaryTracker.getAvailableExclusiveMercenaries(level, exclusiveMercenaryTypes);

        if (availableTypes.isEmpty()) {
            MagicRealms.LOGGER.debug("All exclusive mercenary types already exist in world");
            return null;
        }

        // Randomly select one of the available types
        EntityType<?> selectedType = availableTypes.get(level.getRandom().nextInt(availableTypes.size()));

        // Create the entity using the EntityType.create method
        AbstractMercenaryEntity entity = createEntityFromType(selectedType, level);
        if (entity != null) {
            /*
            MagicRealms.LOGGER.debug("Selected exclusive mercenary: {} (available in world)",
                    selectedType.getDescriptionId());

             */
            return entity;
        }

        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private AbstractMercenaryEntity createEntityFromType(EntityType<?> entityType, Level level) {
        try {
            // Try to create an entity of the given type
            var entity = entityType.create(level);
            if (entity instanceof AbstractMercenaryEntity mercenary && mercenary.isExclusiveMercenary()) {
                return mercenary;
            } else {
                MagicRealms.LOGGER.warn("Entity type {} is tagged as exclusive mercenary but doesn't implement isExclusiveMercenary() correctly",
                        entityType.getDescriptionId());
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to create entity of type: {}", entityType.getDescriptionId(), e);
        }
        return null;
    }

    private void spawnAndSitEntity(ServerLevel level, BlockPos pos, BlockState state, AbstractMercenaryEntity entity) {
        try {
            // Position the entity on the chair
            Vec3 sittingPos = getSittingPosition(pos, state);
            entity.moveTo(sittingPos.x, sittingPos.y, sittingPos.z);

            entity.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.COMMAND, null);

            // Spawn the entity first (this triggers finalizeSpawn and the event tracking)
            boolean spawned = level.addFreshEntity(entity);

            // THEN make it sit (after it's properly initialized)
            if (spawned) {
                // Wait a tick for initialization to complete, then sit
                level.getServer().execute(() -> {
                    if (entity.isAlive() && !entity.isRemoved()) {
                        entity.sitInChair(pos);
                        entity.setYHeadRot(getSittingYaw(state));
                        entity.setYBodyRot(getSittingYaw(state));
                        entity.setYRot(getSittingYaw(state));
                    }
                });
/*
                MagicRealms.LOGGER.debug("Successfully spawned {} at chair {}",
                        entity.getClass().getSimpleName(), pos);

 */
            }

            // Update cooldown
            SPAWN_COOLDOWNS.put(pos.immutable(), level.getGameTime());

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error spawning entity {} at chair {}", entity.getClass().getSimpleName(), pos, e);
        }
    }

    public static Vec3 getSittingPosition(BlockPos chairPos, BlockState state) {
        // Get the center of the seat, with proper Y positioning
        Vec3 center = Vec3.atBottomCenterOf(chairPos).add(0, 0, 0);

        // Adjust position slightly towards the backrest for better sitting position
        Direction facing = state.getValue(FACING);
        Vec3 offset = switch (facing) {
            case NORTH -> new Vec3(0, 0, -0.1); // Slightly towards north (backrest)
            case SOUTH -> new Vec3(0, 0, 0.1);  // Slightly towards south (backrest)
            case EAST -> new Vec3(0.1, 0, 0);   // Slightly towards east (backrest)
            case WEST -> new Vec3(-0.1, 0, 0);  // Slightly towards west (backrest)
            default -> Vec3.ZERO;
        };

        return center.add(offset);
    }

    public static float getSittingYaw(BlockState state) {
        // Face the direction opposite to the chair facing (so they face away from backrest)
        Direction facing = state.getValue(FACING).getOpposite();
        return facing.toYRot();
    }

    @Nullable
    public static AbstractMercenaryEntity getSittingEntity(ServerLevel level, BlockPos chairPos) {
        AABB searchArea = new AABB(chairPos).inflate(1.0);
        List<AbstractMercenaryEntity> entities = level.getEntitiesOfClass(AbstractMercenaryEntity.class, searchArea);

        for (AbstractMercenaryEntity entity : entities) {
            if (entity.isSittingInChair() && chairPos.equals(entity.getChairPosition())) {
                return entity;
            }
        }
        return null;
    }

    // Block Entity methods
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChairBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (!level.isClientSide()) {
            return (level1, pos, state1, blockEntity) -> {
                if (blockEntity instanceof ChairBlockEntity chairBlockEntity) {
                    chairBlockEntity.tick();
                }
            };
        }
        return null;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            // Make any sitting entity unsit when chair is broken
            AbstractMercenaryEntity sittingEntity = getSittingEntity(serverLevel, pos);
            if (sittingEntity != null) {
                sittingEntity.unsitFromChair();
            }

            // Remove cooldown tracking
            SPAWN_COOLDOWNS.remove(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // Helper method for the block entity to access spawn logic
    public void tickSpawnLogic(ServerLevel level, BlockPos pos, BlockState state) {
        // 1% chance per tick (20 ticks per second, so roughly every 5 seconds on average)
        if (level.getRandom().nextFloat() < 0.01f) {
            trySpawnEntity(level, pos, state);
        }
    }
}