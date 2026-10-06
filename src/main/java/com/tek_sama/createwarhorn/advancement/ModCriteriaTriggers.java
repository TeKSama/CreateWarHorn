package com.tek_sama.createwarhorn.advancement;

import com.tek_sama.createwarhorn.CreateWarHorn;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCriteriaTriggers {

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES =
        DeferredRegister.create(Registries.TRIGGER_TYPE, CreateWarHorn.MODID);

    /** Fired when the War Horn blasts (which, by design, only ever happens at 100% charge). */
    public static final DeferredHolder<CriterionTrigger<?>, NoContextTrigger> BLAST_FULL_POWER =
        TRIGGER_TYPES.register("blast_full_power", NoContextTrigger::new);

    /** Fired when a Warden takes damage from a War Horn blast. */
    public static final DeferredHolder<CriterionTrigger<?>, NoContextTrigger> WARDEN_HIT =
        TRIGGER_TYPES.register("warden_hit", NoContextTrigger::new);

    public static void register(IEventBus modEventBus) {
        TRIGGER_TYPES.register(modEventBus);
    }
}
