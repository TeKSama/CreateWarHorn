package com.tek_sama.createwarhorn.effect;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import com.tek_sama.createwarhorn.CreateWarHorn;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Makes nearby mobs scramble in random directions for a while after the War Horn fires.
 *
 * This is a lightweight approximation rather than a proper AI behaviour: vanilla has no reusable
 * "panic" status effect, and giving every affected mob a real pathfinding Goal (like vanilla's own
 * {@code PanicGoal}, which only reacts to being hurt/on fire by default) would mean injecting and
 * later removing a custom Goal per mob type, with more edge cases to get right. Instead, every
 * panicking mob just gets its horizontal velocity overwritten with a fresh random direction every
 * few ticks - cheap, works identically on any {@link Mob} regardless of its AI setup, and reads
 * as "spooked and scrambling" without needing to fight the mob's existing goal selector.
 */
@EventBusSubscriber(modid = CreateWarHorn.MODID)
public class PanicEffectHandler {

    private static final double PANIC_SPEED = 0.35;
    // How often (in ticks) a panicking mob picks a new random direction - keeps the movement
    // looking like scrambling bursts instead of a single frozen shove, without needing to
    // override the velocity on literally every tick.
    private static final int REDIRECT_INTERVAL = 4;

    private static final Map<Mob, Integer> PANICKING = new WeakHashMap<>();

    /** Starts (or refreshes, whichever is longer) panic on this mob for the given duration. */
    public static void panic(Mob mob, int durationTicks) {
        PANICKING.merge(mob, durationTicks, Math::max);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PANICKING.isEmpty())
            return;

        Iterator<Map.Entry<Mob, Integer>> it = PANICKING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Mob, Integer> entry = it.next();
            Mob mob = entry.getKey();

            if (mob == null || !mob.isAlive() || mob.isRemoved()) {
                it.remove();
                continue;
            }

            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft <= 0) {
                it.remove();
                continue;
            }
            entry.setValue(ticksLeft);

            if (ticksLeft % REDIRECT_INTERVAL != 0)
                continue;

            RandomSource random = mob.getRandom();
            double dx = (random.nextDouble() * 2 - 1) * PANIC_SPEED;
            double dz = (random.nextDouble() * 2 - 1) * PANIC_SPEED;
            mob.setDeltaMovement(dx, mob.getDeltaMovement().y, dz);
            // Forces the new velocity to actually sync to clients, same trick vanilla uses for
            // knockback - just mutating deltaMovement server-side isn't enough on its own.
            mob.hurtMarked = true;
        }
    }
}
