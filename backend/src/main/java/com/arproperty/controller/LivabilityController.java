package com.arproperty.controller;

/** 편의시설 점수 API - /api/v1/livability */

import com.arproperty.dto.ApiResponse;
import com.arproperty.dto.LivabilityDto;
import com.arproperty.service.LivabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/livability")
@RequiredArgsConstructor
public class LivabilityController {

    private final LivabilityService livabilityService;

    @GetMapping("/infra/nearby")
    public ApiResponse<List<LivabilityDto.InfraNearby>> getInfraNearby(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon,
            @RequestParam(name = "radius", defaultValue = "1000") int radius,
            @RequestParam(name = "category", required = false) String category
    ) {
        List<LivabilityDto.InfraNearby> data =
                livabilityService.findNearbyInfra(lat, lon, radius, category);

        Map<String, Object> meta = Map.of(
                "count", data.size(),
                "radius_m", radius,
                "center", Map.of("lat", lat, "lon", lon)
        );

        return ApiResponse.success(data, meta);
    }
}
