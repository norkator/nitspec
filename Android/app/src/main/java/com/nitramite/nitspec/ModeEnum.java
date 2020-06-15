package com.nitramite.nitspec;

/* Scope system working behaviour modes */
public enum ModeEnum {

    BUTTON_MANUAL_FIRE_ONLY,                    // Only fire from button, no target selecting
    BUTTON_SELECT_TARGET_AUTO_FIRE,             // Select target from button, automatically fire at target point
    BUTTON_SELECT_TARGET_BUTTON_MANUAL_FIRE,    // Select target from button, use same button to fire with second click

} // End of enum