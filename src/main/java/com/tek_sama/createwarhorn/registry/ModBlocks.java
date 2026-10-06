package com.tek_sama.createwarhorn.registry;

import com.tek_sama.createwarhorn.CreateWarHorn;
import com.tek_sama.createwarhorn.block.WarHornBlock;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateWarHorn.MODID);

    public static final DeferredBlock<WarHornBlock> WAR_HORN = BLOCKS.register("war_horn",
        () -> new WarHornBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            // Same hardness/resistance as a vanilla Iron Block; needs a pickaxe to harvest via
            // the minecraft:mineable/pickaxe tag (data/minecraft/tags/block/mineable/pickaxe.json).
            .strength(5.0f, 6.0f)
            .sound(SoundType.NETHERITE_BLOCK)));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
