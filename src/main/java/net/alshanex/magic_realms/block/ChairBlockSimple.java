package net.alshanex.magic_realms.block;

import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChairBlockSimple extends HorizontalDirectionalBlock {
    public static final MapCodec<ChairBlockSimple> CODEC = simpleCodec(ChairBlockSimple::new);

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

    public ChairBlockSimple(Properties properties) {
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
}