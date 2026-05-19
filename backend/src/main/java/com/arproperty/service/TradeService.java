package com.arproperty.service;

import com.arproperty.controller.ApiException;
import com.arproperty.dto.TradeDto.TradeItemResponse;
import com.arproperty.repository.AptBuildingRepository;
import com.arproperty.repository.AptTradeHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 거래 이력 서비스 (매매/전월세 조회) */
@Service
public class TradeService {

    private static final Set<String> SUPPORTED_TYPES = Set.of("매매", "전세", "월세");

    private final AptTradeHistoryRepository tradeHistoryRepository;
    private final AptBuildingRepository buildingRepository;

    public TradeService(
            AptTradeHistoryRepository tradeHistoryRepository,
            AptBuildingRepository buildingRepository
    ) {
        this.tradeHistoryRepository = tradeHistoryRepository;
        this.buildingRepository = buildingRepository;
    }

    public record TradeResult(
            List<TradeItemResponse> items,
            long totalCount,
            int page,
            int pageSize
    ) {}

    @Transactional(readOnly = true)
    public TradeResult findBuildingTrades(
            int buildingId,
            String type,
            Integer year,
            int page,
            int pageSize
    ) {
        if (!buildingRepository.existsById(buildingId)) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "BUILDING_NOT_FOUND",
                    "Building not found: " + buildingId
            );
        }

        String normalizedType = normalizeType(type);
        Integer normalizedYear = normalizeYear(year);
        int clampedPage = Math.max(page, 1);
        int clampedPageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (clampedPage - 1) * clampedPageSize;

        List<Object[]> rows = tradeHistoryRepository.findBuildingTrades(
                buildingId,
                normalizedType,
                normalizedYear,
                clampedPageSize,
                offset
        );
        long total = tradeHistoryRepository.countBuildingTrades(buildingId, normalizedType, normalizedYear);

        List<TradeItemResponse> items = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            items.add(mapRow(row));
        }
        return new TradeResult(items, total, clampedPage, clampedPageSize);
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) return null;
        String normalized = type.trim();
        if (!SUPPORTED_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Unknown trade type: " + type);
        }
        return normalized;
    }

    private Integer normalizeYear(Integer year) {
        if (year == null) return null;
        if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException("year must be between 1900 and 2100");
        }
        return year;
    }

    private TradeItemResponse mapRow(Object[] r) {
        return new TradeItemResponse(
                toInteger(r[0]),
                (String) r[1],
                toInteger(r[2]),
                toDouble(r[3]),
                toInteger(r[4]),
                toInteger(r[5]),
                toInteger(r[6]),
                mapDate(r[7]),
                r[8] == null ? null : r[8].toString(),
                (String) r[9]
        );
    }

    private String mapDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate localDate) return localDate.toString();
        if (value instanceof Date sqlDate) return sqlDate.toLocalDate().toString();
        return value.toString();
    }

    private Integer toInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal decimal) return decimal.doubleValue();
        return ((Number) value).doubleValue();
    }
}
