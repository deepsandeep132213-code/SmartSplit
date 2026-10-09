package com.smartsplit.ai;

/**
 * A nearby place returned by PlacesService.
 * All fields come directly from the Google Places API response — nothing invented.
 */
public record PlaceResult(
        String name,
        String address,
        String rating,        // e.g. "4.3" or "N/A"
        String userRatingsTotal, // e.g. "1 204"
        String openNow,       // "Open now", "Closed", or "Unknown"
        double lat,
        double lng
) {}
