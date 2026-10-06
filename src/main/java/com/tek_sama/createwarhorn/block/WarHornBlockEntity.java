package com.tek_sama.createwarhorn.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.particle.AirParticleData;
import com.simibubi.create.foundation.utility.CreateLang;
import com.tek_sama.createwarhorn.advancement.ModCriteriaTriggers;
import com.tek_sama.createwarhorn.effect.PanicEffectHandler;
import com.tek_sama.createwarhorn.registry.ModBlockEntityTypes;
import com.tek_sama.createwarhorn.registry.ModSounds;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Tracks the War Horn's compressed-air charge and fires the blast once armed.
 *
 * Charge cycle:
 * - Every tick this block is driven by a nonzero rotation speed, it fills its internal air
 *   reserve (0-{@link #MAX_AIR}) at a rate proportional to |RPM|. Pitch of the eventual blast is
 *   NOT affected by RPM - only fill speed is.
 * - Once the reserve hits {@link #MAX_AIR} the horn is "armed" and stays armed (rotation can stop
 *   or continue, it just won't overfill).
 * - A rising-edge redstone signal while armed instantly empties the reserve and plays the long
 *   range blast sound, then vents air puffs out its mouth for a while as it "cools down".
 *
 * Range (see {@link RangeTier}): set via the brass dial on top of the block - hold right-click on
 * it and scroll, exactly Create's own {@link ScrollOptionBehaviour} widget (the same one a Belt
 * uses for its distribution method). Higher tiers play louder (further = more audible blocks) and
 * take proportionally longer to recharge; the floor is intentional, not just flavour - it's
 * clamped so charging can never finish faster than {@link #BASE_MIN_CHARGE_SECONDS}s even on the
 * shortest tier, comfortably longer than the blast sample itself, so two blasts can never overlap.
 *
 * Contraption note: like most Create kinetic blocks that don't implement a MovementBehaviour,
 * this block entity simply stops ticking while assembled onto a moving contraption (Create never
 * crashes on that - the block is safe to include in a contraption, it just won't charge/fire
 * mid-flight). Once placed back into the world it resumes ticking at its real world position, so
 * every particle/sound computed here always uses genuine world coordinates rather than
 * contraption-local ones.
 */
public class WarHornBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {

    public static final float MAX_AIR = 100f;

    // Reference point the whole charge-rate curve is tuned against: at this speed, on the
    // shortest range tier, the tank fills in exactly BASE_MIN_CHARGE_SECONDS.
    private static final float REFERENCE_RPM = 256f;
    // Minimum recharge time on the shortest tier (VERY_SHORT) - also comfortably above the blast
    // sample's ~12.5s length, so two blasts can never overlap on any tier.
    private static final float BASE_MIN_CHARGE_SECONDS = 15f;
    private static final int BASE_MIN_CHARGE_TICKS = Math.round(BASE_MIN_CHARGE_SECONDS * 20);
    // Derived so that REFERENCE_RPM * this == one full tank in exactly BASE_MIN_CHARGE_TICKS.
    private static final float BASE_FILL_RATE_PER_RPM = MAX_AIR / (REFERENCE_RPM * BASE_MIN_CHARGE_TICKS);
    // Volume at the LONG tier (volumeMultiplier 1.0) = 384 blocks under vanilla's "volume * 16
    // blocks" hearing rule - the new base the other three tiers scale down from (96/192/288/384).
    private static final float BASE_VOLUME = 24f;

    private float airLevel;
    private boolean armed;
    private boolean wasPowered;
    private int syncCooldown;
    // How many more ticks to vent air puffs for, counting down from BASE_MIN_CHARGE_TICKS right
    // as the blast fires - i.e. only while the blast is still "ringing out", not while charging.
    private int ventingTicks;

    private ScrollOptionBehaviour<RangeTier> rangeTier;

    public WarHornBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.WAR_HORN.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(rangeTier = new ScrollOptionBehaviour<>(RangeTier.class,
            Component.translatable("block.createwarhorn.war_horn.range_label"), this, new RangeDialSlot()));
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null)
            return;

        if (ventingTicks > 0) {
            ventingTicks--;
            if (level.isClientSide)
                spawnVentingParticles();
        }

        float speed = getSpeed();
        if (speed == 0 || airLevel >= MAX_AIR || level.isClientSide)
            return;

        boolean wasArmed = armed;
        airLevel = Math.min(MAX_AIR, airLevel + currentFillPerTick(speed));
        if (airLevel >= MAX_AIR)
            armed = true;

        if (armed && !wasArmed)
            // A mechanical "ready" ding once the tank tops out, so you don't have to stare at the
            // goggle overlay to know when it's safe to pulse the redstone.
            AllSoundEvents.CONFIRM.playOnServer(level, worldPosition, 1f, 1f);

        setChanged();
        if (syncCooldown-- <= 0) {
            syncCooldown = 4;
            sendData();
        }
    }

    /** Fill-per-tick at the given |speed|, under the current range tier's rate and hard floor. */
    private float currentFillPerTick(float speed) {
        int tierMul = rangeTier.get().chargeMultiplier();
        float fillRatePerRpm = BASE_FILL_RATE_PER_RPM / tierMul;
        float maxFillPerTick = MAX_AIR / (BASE_MIN_CHARGE_TICKS * tierMul);
        return Mth.clamp(Math.abs(speed) * fillRatePerRpm, 0f, maxFillPerTick);
    }

    /** How far the current blast (or the currently selected tier, when idle) can be heard. */
    private int audibleBlocks() {
        return Math.round(BASE_VOLUME * rangeTier.get().volumeMultiplier() * 16f);
    }

    // Halved from an earlier pass (was 4) - still enough to read clearly once spread across a face.
    private static final int VENT_PARTICLES_PER_TICK = 2;
    // How far a puff's spawn point can wander from the face center along each of the two axes
    // that make up the front face (a full block face spans -0.5..0.5, kept slightly inside that
    // so puffs don't spawn past the block's edges).
    private static final double FACE_SPREAD = 0.42;
    // ~8px (0.5 block) of inward drift over a puff's short lifetime.
    private static final double INWARD_DRIFT = 0.025;

    private void spawnVentingParticles() {
        // Spread across the whole front face, not a single point, drifting inward - as if the
        // horn were still breathing in the last of the blast.
        Direction facing = getBlockState().getValue(WarHornBlock.HORIZONTAL_FACING);
        Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 faceCenter = Vec3.atCenterOf(worldPosition).add(normal.scale(0.52));
        Vec3 inward = normal.scale(-INWARD_DRIFT);

        RandomSource r = level.random;
        for (int i = 0; i < VENT_PARTICLES_PER_TICK; i++) {
            double u = (r.nextDouble() * 2 - 1) * FACE_SPREAD;
            double v = (r.nextDouble() * 2 - 1) * FACE_SPREAD;
            Vec3 spot = faceCenter.add(acrossFace(facing.getAxis(), u, v));

            // Same particle Create's own Backtank uses while filling from a shaft - unlike the
            // Steam Engine's jet, this one actually travels with the velocity it's given.
            level.addParticle(new AirParticleData(1, 0.05f), spot.x, spot.y, spot.z, inward.x, inward.y, inward.z);
        }
    }

    /** u/v offsets across the two axes that span a block face perpendicular to a horizontal facing. */
    private static Vec3 acrossFace(Direction.Axis facingAxis, double u, double v) {
        return facingAxis == Direction.Axis.X ? new Vec3(0, v, u) : new Vec3(u, v, 0);
    }

    /** Called by {@link WarHornBlock#neighborChanged} whenever the redstone signal here changes. */
    public void updateRedstone(boolean powered) {
        boolean risingEdge = powered && !wasPowered;
        wasPowered = powered;
        if (risingEdge && armed)
            fire();
    }

    private void fire() {
        if (level == null || level.isClientSide)
            return;

        airLevel = 0f;
        armed = false;
        ventingTicks = BASE_MIN_CHARGE_TICKS;
        setChanged();
        sendData();

        // Pitch is fixed on purpose: RPM only ever affects charge speed, never blast pitch.
        // Volume scales with the chosen range tier; SoundSource.BLOCKS ties it to the player's
        // "Blocks" volume slider like any other block sound.
        float volume = BASE_VOLUME * rangeTier.get().volumeMultiplier();
        level.playSound(null, worldPosition, ModSounds.WAR_HORN_BLAST.get(), SoundSource.BLOCKS, volume, 1f);

        boolean wardenHit = spookNearbyMobs();
        if (level instanceof ServerLevel serverLevel)
            notifyAdvancements(serverLevel, wardenHit);
    }

    // How long a spooked mob scrambles for; the Warden - being especially sound-sensitive - gets
    // twice this on top of taking direct damage from the blast.
    private static final int PANIC_DURATION_TICKS = 280;
    private static final float WARDEN_DAMAGE = 20f;

    /**
     * Anything within earshot of the blast scrambles for a while - bosses are unfazed by noise.
     * @return whether a Warden was among the mobs hit, for the "Goûte à ta Propre Médecine" advancement.
     */
    private boolean spookNearbyMobs() {
        if (!(level instanceof ServerLevel serverLevel))
            return false;

        boolean wardenHit = false;
        AABB earshot = new AABB(worldPosition).inflate(audibleBlocks());
        for (Mob mob : serverLevel.getEntitiesOfClass(Mob.class, earshot)) {
            if (mob instanceof WitherBoss || mob instanceof EnderDragon)
                continue;

            if (mob instanceof Warden warden) {
                warden.hurt(serverLevel.damageSources().generic(), WARDEN_DAMAGE);
                PanicEffectHandler.panic(warden, PANIC_DURATION_TICKS * 2);
                wardenHit = true;
            } else {
                PanicEffectHandler.panic(mob, PANIC_DURATION_TICKS);
            }
        }
        return wardenHit;
    }

    // Redstone signals don't carry player identity as they propagate through wires/repeaters/etc,
    // so there's no true way to trace "who pulled the lever". This approximates it: whoever is
    // closest to the horn when it fires, within a radius tight enough that they were plausibly
    // the one operating it, is credited as the trigger for "Onde de Choc".
    private static final double TRIGGER_CREDIT_RADIUS = 16;

    /** Awards "Onde de Choc" (fired on Long range) and "Goûte à ta Propre Médecine" (Warden hit). */
    private void notifyAdvancements(ServerLevel serverLevel, boolean wardenHit) {
        Vec3 center = Vec3.atCenterOf(worldPosition);

        if (wardenHit) {
            double earshot = audibleBlocks();
            for (ServerPlayer player : serverLevel.players())
                if (player.distanceToSqr(center) <= earshot * earshot)
                    ModCriteriaTriggers.WARDEN_HIT.get().trigger(player);
        }

        if (rangeTier.get() == RangeTier.LONG) {
            ServerPlayer trigger = findTriggeringPlayer(serverLevel, center);
            if (trigger != null)
                ModCriteriaTriggers.BLAST_FULL_POWER.get().trigger(trigger);
        }
    }

    @Nullable
    private static ServerPlayer findTriggeringPlayer(ServerLevel serverLevel, Vec3 center) {
        ServerPlayer closest = null;
        double closestDistSq = TRIGGER_CREDIT_RADIUS * TRIGGER_CREDIT_RADIUS;
        for (ServerPlayer player : serverLevel.players()) {
            double distSq = player.distanceToSqr(center);
            if (distSq <= closestDistSq) {
                closestDistSq = distSq;
                closest = player;
            }
        }
        return closest;
    }

    public float getAirLevel() {
        return airLevel;
    }

    public boolean isArmed() {
        return armed;
    }

    public RangeTier getRangeTier() {
        return rangeTier.get();
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putFloat("AirLevel", airLevel);
        compound.putBoolean("Armed", armed);
        compound.putBoolean("WasPowered", wasPowered);
        compound.putInt("VentingTicks", ventingTicks);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        airLevel = compound.getFloat("AirLevel");
        armed = compound.getBoolean("Armed");
        wasPowered = compound.getBoolean("WasPowered");
        ventingTicks = compound.getInt("VentingTicks");
    }

    private static final int AIR_BAR_SEGMENTS = 10;

    /**
     * {@code CreateLang.translate(key)} silently prefixes {@code key} with "create." (it's meant
     * for Create's own translation keys) - using it directly on our own keys just rendered the
     * raw, untranslated key string. This resolves our own keys the normal way first, then hands
     * the finished text to CreateLang purely for its goggle formatting/indentation helpers.
     */
    private static LangBuilder ownText(String key, Object... args) {
        return CreateLang.text(Component.translatable(key, args).getString());
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // Keeps Create's own "Stress Impact: X su, at current speed" section (from
        // KineticBlockEntity) and appends our own air charge bar + range/ETA underneath it.
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        ownText("block.createwarhorn.war_horn.tooltip.air_charge")
            .style(ChatFormatting.GRAY)
            .forGoggles(tooltip);

        int percent = Math.round((airLevel / MAX_AIR) * 100f);
        int filledSegments = Math.round((airLevel / MAX_AIR) * AIR_BAR_SEGMENTS);

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < AIR_BAR_SEGMENTS; i++)
            bar.append(i < filledSegments ? '|' : ' ');
        bar.append(']');

        // Create's usual goggle convention: grey while filling, green once the value is "good to
        // go" - here, once the horn is fully armed and ready to fire.
        ChatFormatting barColor = armed ? ChatFormatting.GREEN : ChatFormatting.GRAY;

        // Raw literal text (not translation keys), so CreateLang.text() directly - no need to
        // route these through ownText().
        CreateLang.text(bar.toString())
            .style(barColor)
            .space()
            .add(CreateLang.text(percent + "%").style(ChatFormatting.WHITE))
            .forGoggles(tooltip, 1);

        ownText("block.createwarhorn.war_horn.tooltip.range")
            .style(ChatFormatting.GRAY)
            .space()
            .add(ownText(rangeTier.get().getTranslationKey()).style(ChatFormatting.WHITE))
            .space()
            .add(ownText("block.createwarhorn.war_horn.tooltip.audible_range", audibleBlocks())
                .style(ChatFormatting.GOLD))
            .forGoggles(tooltip, 1);

        tooltip.add(timeRemainingLine());

        return true;
    }

    /** RPM + chosen range = time left until the horn is armed, exactly what powers the fill logic above. */
    private Component timeRemainingLine() {
        if (armed)
            return Component.translatable("block.createwarhorn.war_horn.tooltip.ready")
                .withStyle(ChatFormatting.GREEN);

        float speed = getSpeed();
        if (speed == 0)
            return Component.translatable("block.createwarhorn.war_horn.tooltip.idle")
                .withStyle(ChatFormatting.DARK_GRAY);

        float perTick = currentFillPerTick(speed);
        float remainingSeconds = perTick > 0 ? (MAX_AIR - airLevel) / perTick / 20f : Float.POSITIVE_INFINITY;

        return Component.translatable("block.createwarhorn.war_horn.tooltip.time_remaining",
            String.format("%.1f", remainingSeconds)).withStyle(ChatFormatting.AQUA);
    }
}
