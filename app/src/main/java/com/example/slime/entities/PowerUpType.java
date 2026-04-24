package com.example.slime.entities;

public enum PowerUpType {
    /** Rocket: massive upward boost, ignores max fall speed briefly. */
    JETPACK,
    /** Bubble: absorbs one fatal fall, teleporting the slime back up. */
    SHIELD,
    /** 2x: doubles all score gains for a fixed duration. */
    MULTIPLIER
}
