package com.tek_sama.createwarhorn.block;

import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;

import net.minecraft.core.Direction;

/** Positions the War Horn's range dial on the block's top face - copied from how Create positions
 *  the Brass Tunnel's own distribution-mode dial ({@code BrassTunnelModeSlot}). */
public class RangeDialSlot extends CenteredSideValueBoxTransform {

    public RangeDialSlot() {
        super((state, d) -> d == Direction.UP);
    }

    @Override
    public int getOverrideColor() {
        return 0xB08D57;
    }
}
