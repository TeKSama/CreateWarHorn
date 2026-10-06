package com.tek_sama.createwarhorn.registry;

import com.tek_sama.createwarhorn.CreateWarHorn;
import com.tek_sama.createwarhorn.block.WarHornBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntityTypes {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateWarHorn.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WarHornBlockEntity>> WAR_HORN =
        BLOCK_ENTITY_TYPES.register("war_horn", () -> BlockEntityType.Builder
            .of(WarHornBlockEntity::new, ModBlocks.WAR_HORN.get())
            .build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
