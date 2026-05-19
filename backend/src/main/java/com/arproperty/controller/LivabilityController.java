package com.arproperty.controller;

/** 편의시설 점수 API - /api/v1/livability */

import com.arproperty.dto.ApiResponse;
import com.arproperty.dto.LivabilityDto;
import com.arproperty.service.LivabilityService;
import com.arproperty.service.LivabilityService.NearbyResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
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
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "100") int pageSize
    ) {
        RequestValidation.requireGumiCoordinates(lat, lon);

        NearbyResult result = livabilityService.findNearbyInfra(lat, lon, radius, category, page, pageSize);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("count", result.items().size());
        meta.put("total_count", result.totalCount());
        meta.put("page", result.page());
        meta.put("page_size", result.pageSize());
        meta.put("radius_m", result.radiusMeters());
        meta.put("center", Map.of("lat", lat, "lon", lon));

        return ApiResponse.success(result.items(), meta);
    }
}
