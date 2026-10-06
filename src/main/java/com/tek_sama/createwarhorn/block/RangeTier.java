package com.tek_sama.createwarhorn.block;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

/**
 * How far the War Horn's blast should carry, set via the hold-right-click-and-scroll dial on top
 * of the block (a {@link com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour},
 * the exact same widget Create itself uses for e.g. a Belt's distribution method).
 *
 * Tuned for multiplayer servers rather than covering the whole map: {@link #volumeMultiplier}
 * scales the blast's volume (further = louder - vanilla's "audible up to volume * 16 blocks" rule
 * turns that directly into hearing distance), {@link #chargeMultiplier} scales how long it takes
 * to recharge (further = slower).
 */
public enum RangeTier implements INamedIconOptions {

    VERY_SHORT("very_short", AllIcons.I_PRIORITY_VERY_LOW, 1, 0.25f),
    SHORT("short", AllIcons.I_PRIORITY_LOW, 2, 0.5f),
    MEDIUM("medium", AllIcons.I_PRIORITY_HIGH, 3, 0.75f),
    LONG("long", AllIcons.I_PRIORITY_VERY_HIGH, 4, 1.0f);

    private final String translationKey;
    private final AllIcons icon;
    private final int chargeMultiplier;
    private final float volumeMultiplier;

    RangeTier(String translationKey, AllIcons icon, int chargeMultiplier, float volumeMultiplier) {
        this.translationKey = translationKey;
        this.icon = icon;
        this.chargeMultiplier = chargeMultiplier;
        this.volumeMultiplier = volumeMultiplier;
    }

    /** 1 for VERY_SHORT, 2 for SHORT, 3 for MEDIUM, 4 for LONG - scales minimum charge time. */
    public int chargeMultiplier() {
        return chargeMultiplier;
    }

    /** 0.25/0.5/0.75/1.0 - scales blast volume, and with it, audible distance. */
    public float volumeMultiplier() {
        return volumeMultiplier;
    }

    @Override
    public String getTranslationKey() {
        return "block.createwarhorn.war_horn.range." + translationKey;
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }
}
