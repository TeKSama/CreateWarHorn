package com.tek_sama.createwarhorn.registry;

import com.simibubi.create.AllCreativeModeTabs;
import com.tek_sama.createwarhorn.CreateWarHorn;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Injects the War Horn into Create's own "Base" creative tab instead of creating a new tab for
 * this addon, per the mod's design goal of staying invisible as a separate mod in the creative menu.
 */
@EventBusSubscriber(modid = CreateWarHorn.MODID)
public class ModCreativeTab {

    /** No-op hook purely to force this class to classload from the main mod constructor. */
    public static void init() {}

    @SubscribeEvent
    public static void addToCreateTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey())
            event.accept(ModItems.WAR_HORN);
    }
}
