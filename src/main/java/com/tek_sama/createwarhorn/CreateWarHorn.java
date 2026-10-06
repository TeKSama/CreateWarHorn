package com.tek_sama.createwarhorn;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tek_sama.createwarhorn.advancement.ModCriteriaTriggers;
import com.tek_sama.createwarhorn.registry.ModBlockEntityTypes;
import com.tek_sama.createwarhorn.registry.ModBlocks;
import com.tek_sama.createwarhorn.registry.ModCreativeTab;
import com.tek_sama.createwarhorn.registry.ModItems;
import com.tek_sama.createwarhorn.registry.ModSounds;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Create: War Horn - a Create addon.
 * Adds a kinetic block that charges on compressed air while driven by a Create shaft, then
 * unleashes a long range sonic blast on a redstone pulse once fully charged.
 */
@Mod(CreateWarHorn.MODID)
public class CreateWarHorn {

    public static final String MODID = "createwarhorn";

    /** Stress Impact of the War Horn, in Stress Units per RPM. Comparable to a Turntable. */
    public static final double STRESS_IMPACT = 4.0;

    public CreateWarHorn(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntityTypes.register(modEventBus);
        ModSounds.register(modEventBus);
        ModCriteriaTriggers.register(modEventBus);
        // ModCreativeTab registers itself via @EventBusSubscriber; touching the class here just
        // guarantees it gets classloaded even if nothing else references it.
        ModCreativeTab.init();

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Registers the War Horn with Create's kinetic stress network so it draws Stress Units
        // from the network like any other consumer (Turntable, Mechanical Mixer, etc).
        BlockStressValues.IMPACTS.register(ModBlocks.WAR_HORN.get(), () -> STRESS_IMPACT);

        // Both use Create's own tooltip machinery, picked up automatically by Create's existing
        // ItemTooltipEvent handler from here on - no rendering code needed on our end:
        // - ItemDescription reads block.createwarhorn.war_horn.tooltip.summary (and any
        //   .conditionN/.behaviourN pairs) from our lang files and renders it with the same
        //   "Hold [Shift] for Description" layout and STANDARD_CREATE color palette as e.g. the
        //   Wrench (words wrapped in "_underscores_" in the lang value become the highlight color).
        // - KineticStats adds the "Impact sur le stress cinétique" bar every Create kinetic
        //   machine shows (e.g. the Mechanical Drill).
        TooltipModifier.REGISTRY.register(ModItems.WAR_HORN.get(),
            new ItemDescription.Modifier(ModItems.WAR_HORN.get(), Palette.STANDARD_CREATE)
                .andThen(new KineticStats(ModBlocks.WAR_HORN.get())));
    }
}
