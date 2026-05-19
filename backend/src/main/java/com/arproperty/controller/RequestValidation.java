package com.arproperty.controller;

import org.springframework.http.HttpStatus;

final class RequestValidation {

    private static final double GUMI_MIN_LAT = 36.05;
    private static final double GUMI_MAX_LAT = 36.25;
    private static final double GUMI_MIN_LON = 128.20;
    private static final double GUMI_MAX_LON = 128.50;

    private RequestValidation() {
    }

    static void requireGumiCoordinates(double lat, double lon) {
        if (!Double.isFinite(lat) || !Double.isFinite(lon)
                || lat < GUMI_MIN_LAT || lat > GUMI_MAX_LAT
                || lon < GUMI_MIN_LON || lon > GUMI_MAX_LON) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_COORDINATES",
                    "lat/lon must be within Gumi coordinate range"
            );
        }
    }
}
