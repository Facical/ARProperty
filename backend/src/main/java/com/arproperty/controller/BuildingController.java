package com.arproperty.controller;

import com.arproperty.dto.ApiResponse;
import com.arproperty.dto.BuildingDto.BuildingDetailResponse;
import com.arproperty.dto.BuildingDto.BuildingNearbyResponse;
import com.arproperty.service.BuildingService;
import com.arproperty.service.BuildingService.NearbyResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 건물 API - /api/v1/buildings */
@RestController
@RequestMapping("/api/v1/buildings")
public class BuildingController {

    private final BuildingService buildingService;

    public BuildingController(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @GetMapping("/nearby")
    public ApiResponse<List<BuildingNearbyResponse>> getNearby(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon,
            @RequestParam(name = "radius", defaultValue = "500") int radius,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize
    ) {
        RequestValidation.requireGumiCoordinates(lat, lon);

        NearbyResult result = buildingService.findNearby(lat, lon, radius, page, pageSize);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", page);
        meta.put("page_size", pageSize);
        meta.put("total_count", result.totalCount());
        meta.put("center_lat", lat);
        meta.put("center_lon", lon);
        meta.put("radius", Math.min(Math.max(radius, 1), 2000));

        return ApiResponse.success(result.items(), meta);
    }

    @GetMapping("/{buildingId}")
    public ApiResponse<BuildingDetailResponse> getDetail(
            @PathVariable("buildingId") int buildingId
    ) {
        return ApiResponse.success(buildingService.findDetail(buildingId));
    }
}
