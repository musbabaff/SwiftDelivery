package com.nettyforge.swiftdelivery.model;

/**
 * Defines how categories or items are selected for delivery events.
 */
public enum SelectionMode {
    /**
     * Randomly select from available options.
     */
    RANDOM,
    
    /**
     * Use a fixed, specified value.
     */
    FIXED
}
