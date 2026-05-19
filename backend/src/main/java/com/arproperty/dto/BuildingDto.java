package com.arproperty.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 건물 관련 DTO (NearbyBuilding 응답) */
public final class BuildingDto {

    private BuildingDto() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BuildingNearbyResponse(
            @JsonProperty("building_id") int buildingId,
            @JsonProperty("complex_id") int complexId,
            @JsonProperty("complex_name") String complexName,
            @JsonProperty("dong_name") String dongName,
            double lat,
            double lon,
            @JsonProperty("ground_floors") Integer groundFloors,
            @JsonProperty("livability_grade") String livabilityGrade,
            @JsonProperty("livability_score") Double livabilityScore,
            @JsonProperty("distance_m") Double distanceMeters,
            @JsonProperty("latest_trade") LatestTrade latestTrade
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BuildingDetailResponse(
            @JsonProperty("building_id") int buildingId,
            @JsonProperty("complex_id") int complexId,
            @JsonProperty("complex_name") String complexName,
            @JsonProperty("dong_name") String dongName,
            double lat,
            double lon,
            @JsonProperty("ground_floors") int groundFloors,
            @JsonProperty("underground_floors") int undergroundFloors,
            @JsonProperty("highest_floor") int highestFloor,
            @JsonProperty("building_height") Double buildingHeight,
            @JsonProperty("structure_type") String structureType,
            @JsonProperty("total_area") Double totalArea,
            @JsonProperty("use_approval_date") String useApprovalDate,
            @JsonProperty("complex_info") ComplexInfo complexInfo,
            LivabilitySummary livability
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ComplexInfo(
            @JsonProperty("kapt_code") String kaptCode,
            Integer households,
            @JsonProperty("building_count") Integer buildingCount,
            @JsonProperty("parking_count") Integer parkingCount,
            @JsonProperty("elevator_count") Integer elevatorCount,
            @JsonProperty("heating_type") String heatingType,
            String constructor
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LivabilitySummary(
            @JsonProperty("total_score") Double totalScore,
            String grade
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LatestTrade(
            @JsonProperty("deal_amount") Integer dealAmount,
            @JsonProperty("exclusive_area") Double exclusiveArea,
            Integer floor,
            @JsonProperty("deal_date") String dealDate,
            @JsonProperty("trade_type") String tradeType
    ) {}
}
