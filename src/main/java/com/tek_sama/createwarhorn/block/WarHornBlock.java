package com.tek_sama.createwarhorn.block;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.tek_sama.createwarhorn.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The War Horn block. Driven horizontally by a shaft from behind (like a Millstone/Saw), it
 * charges an internal compressed-air reserve while rotating and fires a sonic blast forward
 * out of the block's facing side when it receives a redstone pulse while fully charged.
 *
 * Its range dial (the brass nub on top) needs no interaction code here at all: it's a
 * {@link com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour}
 * added in {@link WarHornBlockEntity#addBehaviours}, and Create's own input handler generically
 * detects the hold-right-click-and-scroll gesture on any block exposing one.
 */
public class WarHornBlock extends HorizontalKineticBlock implements IBE<WarHornBlockEntity> {

    public WarHornBlock(Properties properties) {
        super(properties);
    }

    /**
     * Normal placement: the mouth faces the player (like a Furnace). Sneak-placement: the mouth
     * instead faces the same way the player is looking (as if aiming it past themselves).
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction lookDirection = context.getHorizontalDirection();
        Player player = context.getPlayer();
        boolean sneaking = player != null && player.isShiftKeyDown();
        Direction mouthFacing = sneaking ? lookDirection : lookDirection.getOpposite();
        return defaultBlockState().setValue(HORIZONTAL_FACING, mouthFacing);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos fromPos,
            boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, fromPos, isMoving);
        if (level.isClientSide)
            return;
        boolean powered = level.hasNeighborSignal(pos);
        withBlockEntityDo(level, pos, be -> be.updateRedstone(powered));
    }

    @Override
    public Class<WarHornBlockEntity> getBlockEntityClass() {
        return WarHornBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends WarHornBlockEntity> getBlockEntityType() {
        return ModBlockEntityTypes.WAR_HORN.get();
    }
}
