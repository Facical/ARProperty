package com.arproperty.controller;

import com.arproperty.dto.BuildingDto.BuildingDetailResponse;
import com.arproperty.dto.BuildingDto.BuildingNearbyResponse;
import com.arproperty.dto.BuildingDto.ComplexInfo;
import com.arproperty.dto.BuildingDto.LivabilitySummary;
import com.arproperty.dto.TradeDto.TradeItemResponse;
import com.arproperty.service.BuildingService;
import com.arproperty.service.LivabilityService;
import com.arproperty.service.LivabilityService.NearbyResult;
import com.arproperty.service.TradeService;
import com.arproperty.service.TradeService.TradeResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {BuildingController.class, LivabilityController.class, TradeController.class})
class ControllerErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BuildingService buildingService;

    @MockBean
    private LivabilityService livabilityService;

    @MockBean
    private TradeService tradeService;

    @Test
    void buildingDetailReturnsStandardResponse() throws Exception {
        BuildingDetailResponse detail = new BuildingDetailResponse(
                100,
                17,
                "Okgye Demo Complex",
                "101",
                36.13918,
                128.42137,
                20,
                1,
                20,
                60.5,
                "RC",
                12500.5,
                "2015-06-20",
                new ComplexInfo("A12345678", 850, 8, 1200, 16, "district", "Demo Constructor"),
                new LivabilitySummary(82.0, "A")
        );
        when(buildingService.findDetail(100)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/buildings/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.data.building_id").value(100))
                .andExpect(jsonPath("$.data.complex_name").value("Okgye Demo Complex"))
                .andExpect(jsonPath("$.data.ground_floors").value(20))
                .andExpect(jsonPath("$.data.complex_info.households").value(850))
                .andExpect(jsonPath("$.data.livability.total_score").value(82.0));
    }

    @Test
    void buildingDetailReturnsNotFound() throws Exception {
        when(buildingService.findDetail(404))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "BUILDING_NOT_FOUND", "Building not found: 404"));

        mockMvc.perform(get("/api/v1/buildings/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error.code").value("BUILDING_NOT_FOUND"));
    }

    @Test
    void buildingTradesReturnsStandardResponseAndMetadata() throws Exception {
        TradeItemResponse item = new TradeItemResponse(
                1234,
                "101",
                15,
                84.0,
                58000,
                null,
                null,
                "2026-03-15",
                "매매",
                "중개거래"
        );
        when(tradeService.findBuildingTrades(100, "매매", 2026, 1, 20))
                .thenReturn(new TradeResult(List.of(item), 45, 1, 20));

        mockMvc.perform(get("/api/v1/buildings/100/trades")
                        .queryParam("type", "매매")
                        .queryParam("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.data[0].trade_id").value(1234))
                .andExpect(jsonPath("$.data[0].dong_name").value("101"))
                .andExpect(jsonPath("$.data[0].deal_amount").value(58000))
                .andExpect(jsonPath("$.data[0].trade_type").value("매매"))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.page_size").value(20))
                .andExpect(jsonPath("$.meta.total_count").value(45));
    }

    @Test
    void buildingTradesReturnsNotFound() throws Exception {
        when(tradeService.findBuildingTrades(eq(404), isNull(), isNull(), eq(1), eq(20)))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "BUILDING_NOT_FOUND", "Building not found: 404"));

        mockMvc.perform(get("/api/v1/buildings/404/trades"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error.code").value("BUILDING_NOT_FOUND"));
    }

    @Test
    void buildingTradesReturnsInvalidParameterForUnknownType() throws Exception {
        when(tradeService.findBuildingTrades(eq(100), eq("분양권"), isNull(), eq(1), eq(20)))
                .thenThrow(new IllegalArgumentException("Unknown trade type: 분양권"));

        mockMvc.perform(get("/api/v1/buildings/100/trades")
                        .queryParam("type", "분양권"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
    }

    @Test
    void buildingsNearbyReturnsOkgyeResultWithStandardMetadata() throws Exception {
        BuildingNearbyResponse item = new BuildingNearbyResponse(
                100,
                17,
                "Okgye Demo Complex",
                "101",
                36.13918,
                128.42137,
                20,
                "A",
                82.0,
                32.5,
                null
        );
        when(buildingService.findNearby(anyDouble(), anyDouble(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new BuildingService.NearbyResult(List.of(item), 1));

        mockMvc.perform(get("/api/v1/buildings/nearby")
                        .queryParam("lat", "36.13918")
                        .queryParam("lon", "128.42137")
                        .queryParam("radius", "1000")
                        .queryParam("page", "1")
                        .queryParam("page_size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.data[0].building_id").value(100))
                .andExpect(jsonPath("$.data[0].complex_name").value("Okgye Demo Complex"))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.page_size").value(20))
                .andExpect(jsonPath("$.meta.total_count").value(1))
                .andExpect(jsonPath("$.meta.radius").value(1000));
    }

    @Test
    void buildingsNearbyReturnsInvalidParameterWhenLatIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/buildings/nearby")
                        .queryParam("lon", "128.421"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
    }

    @Test
    void buildingsNearbyReturnsInvalidParameterWhenLatIsNotNumeric() throws Exception {
        mockMvc.perform(get("/api/v1/buildings/nearby")
                        .queryParam("lat", "abc")
                        .queryParam("lon", "128.421"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
    }

    @Test
    void buildingsNearbyReturnsInvalidCoordinatesOutsideGumiRange() throws Exception {
        mockMvc.perform(get("/api/v1/buildings/nearby")
                        .queryParam("lat", "37.5665")
                        .queryParam("lon", "126.9780"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error.code").value("INVALID_COORDINATES"));
    }

    @Test
    void infraNearbyReturnsInvalidCoordinatesOutsideGumiRange() throws Exception {
        mockMvc.perform(get("/api/v1/livability/infra/nearby")
                        .queryParam("lat", "37.5665")
                        .queryParam("lon", "126.9780"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error.code").value("INVALID_COORDINATES"));
    }

    @Test
    void infraNearbyReturnsInvalidParameterForUnknownCategory() throws Exception {
        when(livabilityService.findNearbyInfra(anyDouble(), anyDouble(), anyInt(), eq("invalid"), anyInt(), anyInt()))
                .thenThrow(new IllegalArgumentException("Unknown category: invalid"));

        mockMvc.perform(get("/api/v1/livability/infra/nearby")
                        .queryParam("lat", "36.139")
                        .queryParam("lon", "128.421")
                        .queryParam("category", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
    }

    @Test
    void infraNearbyReturnsPagingMetadataFromServiceResult() throws Exception {
        when(livabilityService.findNearbyInfra(anyDouble(), anyDouble(), anyInt(), isNull(), anyInt(), anyInt()))
                .thenReturn(new NearbyResult(List.of(), 123, 3000, 1, 100));

        mockMvc.perform(get("/api/v1/livability/infra/nearby")
                        .queryParam("lat", "36.139")
                        .queryParam("lon", "128.421")
                        .queryParam("radius", "99999")
                        .queryParam("page", "-2")
                        .queryParam("page_size", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.meta.count").value(0))
                .andExpect(jsonPath("$.meta.total_count").value(123))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.page_size").value(100))
                .andExpect(jsonPath("$.meta.radius_m").value(3000));
    }

    @Test
    void unexpectedErrorsHideRawExceptionMessage() throws Exception {
        when(buildingService.findNearby(anyDouble(), anyDouble(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("database password leaked"));

        mockMvc.perform(get("/api/v1/buildings/nearby")
                        .queryParam("lat", "36.139")
                        .queryParam("lon", "128.421"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Internal server error"));
    }
}
