package com.arproperty.controller;

import com.arproperty.dto.ApiResponse;
import com.arproperty.dto.TradeDto.TradeItemResponse;
import com.arproperty.service.TradeService;
import com.arproperty.service.TradeService.TradeResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 거래 이력 API - /api/v1/buildings/{id}/trades */
@RestController
@RequestMapping("/api/v1/buildings")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @GetMapping("/{buildingId}/trades")
    public ApiResponse<List<TradeItemResponse>> getBuildingTrades(
            @PathVariable("buildingId") int buildingId,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize
    ) {
        TradeResult result = tradeService.findBuildingTrades(buildingId, type, year, page, pageSize);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", result.page());
        meta.put("page_size", result.pageSize());
        meta.put("total_count", result.totalCount());

        return ApiResponse.success(result.items(), meta);
    }
}
