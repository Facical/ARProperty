package com.arproperty.service;

import com.arproperty.controller.ApiException;
import com.arproperty.dto.BuildingDto.BuildingDetailResponse;
import com.arproperty.dto.BuildingDto.BuildingNearbyResponse;
import com.arproperty.dto.BuildingDto.ComplexInfo;
import com.arproperty.dto.BuildingDto.LivabilitySummary;
import com.arproperty.dto.BuildingDto.LatestTrade;
import com.arproperty.repository.AptBuildingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 건물 조회 서비스 (주변 건물 검색) */
@Service
public class BuildingService {

    private final AptBuildingRepository buildingRepository;

    public BuildingService(AptBuildingRepository buildingRepository) {
        this.buildingRepository = buildingRepository;
    }

    public record NearbyResult(List<BuildingNearbyResponse> items, long totalCount) {}

    @Transactional(readOnly = true)
    public BuildingDetailResponse findDetail(int buildingId) {
        return buildingRepository.findDetail(buildingId).stream()
                .findFirst()
                .map(this::mapDetailRow)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "BUILDING_NOT_FOUND",
                        "Building not found: " + buildingId
                ));
    }

    @Transactional(readOnly = true)
    public NearbyResult findNearby(double lat, double lon, int radius, int page, int pageSize) {
        int clampedRadius = Math.min(Math.max(radius, 1), 2000);
        int clampedPage = Math.max(page, 1);
        int clampedPageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (clampedPage - 1) * clampedPageSize;

        List<Object[]> rows = buildingRepository.findNearby(lat, lon, clampedRadius, clampedPageSize, offset);
        List<BuildingNearbyResponse> items = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            items.add(mapRow(r));
        }
        long total = buildingRepository.countNearby(lat, lon, clampedRadius);
        return new NearbyResult(items, total);
    }

    private BuildingDetailResponse mapDetailRow(Object[] r) {
        ComplexInfo complexInfo = new ComplexInfo(
                (String) r[13],
                toInteger(r[14]),
                toInteger(r[15]),
                toInteger(r[16]),
                toInteger(r[17]),
                (String) r[18],
                (String) r[19]
        );

        Double livabilityScore = toDouble(r[20]);
        String livabilityGrade = r[21] == null ? null : (String) r[21];
        LivabilitySummary livability = (livabilityScore == null && livabilityGrade == null)
                ? null
                : new LivabilitySummary(livabilityScore, livabilityGrade);

        return new BuildingDetailResponse(
                toInteger(r[0]),
                toInteger(r[1]),
                (String) r[2],
                (String) r[3],
                toDoubleOrZero(r[4]),
                toDoubleOrZero(r[5]),
                toIntegerOrZero(r[6]),
                toIntegerOrZero(r[7]),
                toIntegerOrZero(r[8]),
                toDouble(r[9]),
                (String) r[10],
                toDouble(r[11]),
                mapLocalDate(r[12]),
                complexInfo,
                livability
        );
    }

    private BuildingNearbyResponse mapRow(Object[] r) {
        // 컬럼 순서는 AptBuildingRepository.findNearby native query SELECT 절 기준
        int buildingId = ((Number) r[0]).intValue();
        int complexId = ((Number) r[1]).intValue();
        String complexName = (String) r[2];
        String dongName = (String) r[3];
        double lat = ((Number) r[4]).doubleValue();
        double lon = ((Number) r[5]).doubleValue();
        Integer groundFloors = r[6] == null ? null : ((Number) r[6]).intValue();
        // r[7] underground_floors, r[8] highest_floor, r[9] building_height — 현재 DTO 미사용
        String livabilityGrade = r[10] == null ? null : (String) r[10];
        Double livabilityScore = r[11] == null ? null : ((Number) r[11]).doubleValue();
        Double distanceMeters = r[12] == null ? null : ((Number) r[12]).doubleValue();

        Integer dealAmount = r[13] == null ? null : ((Number) r[13]).intValue();
        Double exclusiveArea = r[14] == null ? null : ((BigDecimal) r[14]).doubleValue();
        Integer floor = r[15] == null ? null : ((Number) r[15]).intValue();
        String dealDate = mapDealDate(r[16]);
        String tradeType = r[17] == null ? null : r[17].toString();

        LatestTrade latest = (dealAmount == null && exclusiveArea == null && floor == null
                && dealDate == null && tradeType == null)
                ? null
                : new LatestTrade(dealAmount, exclusiveArea, floor, dealDate, tradeType);

        return new BuildingNearbyResponse(
                buildingId, complexId, complexName, dongName,
                lat, lon, groundFloors,
                livabilityGrade, livabilityScore, distanceMeters,
                latest
        );
    }

    private String mapDealDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate ld) return ld.toString();
        if (value instanceof Date sqlDate) return sqlDate.toLocalDate().toString();
        return value.toString();
    }

    private String mapLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate localDate) return localDate.toString();
        if (value instanceof Date sqlDate) return sqlDate.toLocalDate().toString();
        return value.toString();
    }

    private Integer toInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private int toIntegerOrZero(Object value) {
        Integer number = toInteger(value);
        return number == null ? 0 : number;
    }

    private Double toDouble(Object value) {
        return value == null ? null : ((Number) value).doubleValue();
    }

    private double toDoubleOrZero(Object value) {
        Double number = toDouble(value);
        return number == null ? 0.0 : number;
    }
}
