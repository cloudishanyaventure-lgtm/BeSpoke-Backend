package com.BeSpoke.entity;

/**
 * What a proposal is for. Chosen once per quote — every line item sits under it.
 *
 * <p>The first five are rated per square foot and priced off the PRD's rooms; the last
 * two are priced by hand (items or categories first, then a rate a director, design
 * manager or project manager signs off). The rates themselves are not here: they live in
 * the QUOTE_RATE picklist so the studio can move them without a deploy.
 */
public enum QuoteCategory {
    DESIGN_DRAWINGS_STRUCTURE_INTERIOR,
    DESIGN_DRAWINGS_STRUCTURE,
    DESIGN_DRAWINGS_INTERIOR_MEP,
    DESIGN_DRAWINGS_INTERIOR_NO_MEP,
    DESIGN_WITH_PROJECT_MANAGEMENT,
    TURNKEY,
    MODULAR_FURNITURE
}
