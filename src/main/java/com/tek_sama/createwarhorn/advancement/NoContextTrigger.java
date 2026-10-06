package com.tek_sama.createwarhorn.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * A criterion trigger with no extra JSON conditions beyond the standard player predicate - just
 * "did this specific in-code event happen to this player". Used for both War Horn advancement
 * criteria that have no vanilla trigger equivalent (firing at full power, hurting a Warden with
 * the blast); the two are distinguished by which registry id ({@link ModCriteriaTriggers}) is
 * referenced in each advancement's JSON, not by anything on this class.
 */
public class NoContextTrigger extends SimpleCriterionTrigger<NoContextTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        trigger(player, instance -> true);
    }

    public record Instance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player))
            .apply(i, Instance::new));
    }
}
