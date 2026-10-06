package com.tek_sama.createwarhorn.registry;

import com.tek_sama.createwarhorn.CreateWarHorn;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateWarHorn.MODID);

    // A plain BlockItem is enough - the tooltip (description + kinetic stats) is entirely
    // supplied by Create's own TooltipModifier system, registered in
    // CreateWarHorn#commonSetup, so no custom Item subclass is needed here.
    public static final DeferredItem<BlockItem> WAR_HORN = ITEMS.register("war_horn",
        () -> new BlockItem(ModBlocks.WAR_HORN.get(), new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
