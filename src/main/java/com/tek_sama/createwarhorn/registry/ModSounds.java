package com.tek_sama.createwarhorn.registry;

import com.tek_sama.createwarhorn.CreateWarHorn;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(Registries.SOUND_EVENT, CreateWarHorn.MODID);

    // Variable range (rather than a fixed 16-block range) so passing a large `volume` to
    // Level#playSound genuinely extends how far the blast can be heard, per sounds.json.
    public static final DeferredHolder<SoundEvent, SoundEvent> WAR_HORN_BLAST = SOUND_EVENTS.register(
        "war_horn_blast",
        () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(CreateWarHorn.MODID, "war_horn_blast")));

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
