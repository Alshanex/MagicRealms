package net.alshanex.magic_realms.block;

import com.mojang.serialization.MapCodec;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import java.util.List;

public class ChairBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<ChairBlock> CODEC = simpleCodec(ChairBlock::new);

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // Chair dimensions - oriented for NORTH facing (backrest at north side)
    private static final VoxelShape SEAT = Block.box(2, 8, 2, 14, 10, 14);
    private static final VoxelShape BACKREST_NORTH = Block.box(2, 10, 2, 14, 18, 4);
    private static final VoxelShape FRONT_LEFT_LEG_NORTH = Block.box(2, 0, 12, 4, 8, 14);
    private static final VoxelShape FRONT_RIGHT_LEG_NORTH = Block.box(12, 0, 12, 14, 8, 14);
    private static final VoxelShape BACK_LEFT_LEG_NORTH = Block.box(2, 0, 2, 4, 8, 4);
    private static final VoxelShape BACK_RIGHT_LEG_NORTH = Block.box(12, 0, 2, 14, 8, 4);
    private static final VoxelShape BACKREST_SUPPORT_LEFT_NORTH = Block.box(2, 8, 2, 4, 10, 4);
    private static final VoxelShape BACKREST_SUPPORT_RIGHT_NORTH = Block.box(12, 8, 2, 14, 10, 4);

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            SEAT, BACKREST_NORTH, FRONT_LEFT_LEG_NORTH, FRONT_RIGHT_LEG_NORTH,
            BACK_LEFT_LEG_NORTH, BACK_RIGHT_LEG_NORTH, BACKREST_SUPPORT_LEFT_NORTH, BACKREST_SUPPORT_RIGHT_NORTH
    );

    // Rotated shapes for other directions
    private static final VoxelShape BACKREST_SOUTH = Block.box(2, 10, 12, 14, 18, 14);
    private static final VoxelShape FRONT_LEFT_LEG_SOUTH = Block.box(12, 0, 2, 14, 8, 4);
    private static final VoxelShape FRONT_RIGHT_LEG_SOUTH = Block.box(2, 0, 2, 4, 8, 4);
    private static final VoxelShape BACK_LEFT_LEG_SOUTH = Block.box(12, 0, 12, 14, 8, 14);
    private static final VoxelShape BACK_RIGHT_LEG_SOUTH = Block.box(2, 0, 12, 4, 8, 14);
    private static final VoxelShape BACKREST_SUPPORT_LEFT_SOUTH = Block.box(12, 8, 12, 14, 10, 14);
    private static final VoxelShape BACKREST_SUPPORT_RIGHT_SOUTH = Block.box(2, 8, 12, 4, 10, 14);

    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            SEAT, BACKREST_SOUTH, FRONT_LEFT_LEG_SOUTH, FRONT_RIGHT_LEG_SOUTH,
            BACK_LEFT_LEG_SOUTH, BACK_RIGHT_LEG_SOUTH, BACKREST_SUPPORT_LEFT_SOUTH, BACKREST_SUPPORT_RIGHT_SOUTH
    );

    private static final VoxelShape BACKREST_EAST = Block.box(12, 10, 2, 14, 18, 14);
    private static final VoxelShape FRONT_LEFT_LEG_EAST = Block.box(2, 0, 2, 4, 8, 4);
    private static final VoxelShape FRONT_RIGHT_LEG_EAST = Block.box(2, 0, 12, 4, 8, 14);
    private static final VoxelShape BACK_LEFT_LEG_EAST = Block.box(12, 0, 2, 14, 8, 4);
    private static final VoxelShape BACK_RIGHT_LEG_EAST = Block.box(12, 0, 12, 14, 8, 14);
    private static final VoxelShape BACKREST_SUPPORT_LEFT_EAST = Block.box(12, 8, 2, 14, 10, 4);
    private static final VoxelShape BACKREST_SUPPORT_RIGHT_EAST = Block.box(12, 8, 12, 14, 10, 14);

    private static final VoxelShape SHAPE_EAST = Shapes.or(
            SEAT, BACKREST_EAST, FRONT_LEFT_LEG_EAST, FRONT_RIGHT_LEG_EAST,
            BACK_LEFT_LEG_EAST, BACK_RIGHT_LEG_EAST, BACKREST_SUPPORT_LEFT_EAST, BACKREST_SUPPORT_RIGHT_EAST
    );

    private static final VoxelShape BACKREST_WEST = Block.box(2, 10, 2, 4, 18, 14);
    private static final VoxelShape FRONT_LEFT_LEG_WEST = Block.box(12, 0, 12, 14, 8, 14);
    private static final VoxelShape FRONT_RIGHT_LEG_WEST = Block.box(12, 0, 2, 14, 8, 4);
    private static final VoxelShape BACK_LEFT_LEG_WEST = Block.box(2, 0, 12, 4, 8, 14);
    private static final VoxelShape BACK_RIGHT_LEG_WEST = Block.box(2, 0, 2, 4, 8, 4);
    private static final VoxelShape BACKREST_SUPPORT_LEFT_WEST = Block.box(2, 8, 12, 4, 10, 14);
    private static final VoxelShape BACKREST_SUPPORT_RIGHT_WEST = Block.box(2, 8, 2, 4, 10, 4);

    private static final VoxelShape SHAPE_WEST = Shapes.or(
            SEAT, BACKREST_WEST, FRONT_LEFT_LEG_WEST, FRONT_RIGHT_LEG_WEST,
            BACK_LEFT_LEG_WEST, BACK_RIGHT_LEG_WEST, BACKREST_SUPPORT_LEFT_WEST, BACKREST_SUPPORT_RIGHT_WEST
    );

    // Cooldown tracking per chair position
    private static final java.util.Map<BlockPos, Long> SPAWN_COOLDOWNS = new java.util.HashMap<>();
    private static final long SPAWN_COOLDOWN_TICKS = 600; // 30 seconds
    private static final int MAX_ENTITIES_IN_RADIUS = 10;
    private static final int SPAWN_CHECK_RADIUS = 20;

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
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            // Check if there's already an entity sitting in this chair
            RandomHumanEntity sittingEntity = getSittingEntity(serverLevel, pos);
            if (sittingEntity != null) {
                // Make the entity unsit
                sittingEntity.unsitFromChair();
                return InteractionResult.SUCCESS;
            }

            // Try to spawn an entity if conditions are met
            if (canSpawnEntity(serverLevel, pos)) {
                spawnAndSitEntity(serverLevel, pos, state);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private void trySpawnEntity(ServerLevel level, BlockPos pos, BlockState state) {
        if (canSpawnEntity(level, pos)) {
            spawnAndSitEntity(level, pos, state);
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
        RandomHumanEntity sitting = getSittingEntity(level, pos);
        if (sitting != null) {
            return false;
        }

        // Check entity count in radius
        AABB searchArea = new AABB(pos).inflate(SPAWN_CHECK_RADIUS);
        List<RandomHumanEntity> nearbyEntities = level.getEntitiesOfClass(RandomHumanEntity.class, searchArea);

        if (nearbyEntities.size() >= MAX_ENTITIES_IN_RADIUS) {
            return false;
        }
        return true;
    }

    private void spawnAndSitEntity(ServerLevel level, BlockPos pos, BlockState state) {
        try {
            // Create the entity
            RandomHumanEntity entity = new RandomHumanEntity(MREntityRegistry.HUMAN.get(), level);

            // Position the entity on the chair
            Vec3 sittingPos = getSittingPosition(pos, state);
            entity.moveTo(sittingPos.x, sittingPos.y, sittingPos.z);

            entity.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.COMMAND, null);

            // Spawn the entity first (this triggers finalizeSpawn)
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
            }

            // Update cooldown
            SPAWN_COOLDOWNS.put(pos.immutable(), level.getGameTime());
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error spawning entity at chair " + pos, e);
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
    public static RandomHumanEntity getSittingEntity(ServerLevel level, BlockPos chairPos) {
        AABB searchArea = new AABB(chairPos).inflate(1.0);
        List<RandomHumanEntity> entities = level.getEntitiesOfClass(RandomHumanEntity.class, searchArea);

        for (RandomHumanEntity entity : entities) {
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
            RandomHumanEntity sittingEntity = getSittingEntity(serverLevel, pos);
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