package net.alshanex.magic_realms.block;

import com.mojang.serialization.MapCodec;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.SeatEntity;
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
        BlockPos above = context.getClickedPos().above();
        if (!context.getLevel().getBlockState(above).canBeReplaced(context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player.isPassenger() || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (SeatEntity.isOccupied(level, pos)) {
            return InteractionResult.PASS;
        }

        Direction facing = state.getValue(FACING).getOpposite();
        double seatX = pos.getX() + 0.5;
        double seatY = pos.getY() + 0.65;
        double seatZ = pos.getZ() + 0.5;

        SeatEntity seat = MREntityRegistry.SEAT.get().create(level);
        if (seat != null) {
            seat.setChairPos(pos);
            seat.setPos(seatX, seatY, seatZ);
            seat.setYRot(facing.toYRot());
            level.addFreshEntity(seat);
            player.startRiding(seat);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            SeatEntity.removeSeatsAt(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}