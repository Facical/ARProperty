package com.arproperty.service;

/** 편의시설 점수화 엔진 (거리 기반 점수, 가중치 프리셋, 등급 계산) */

import com.arproperty.dto.LivabilityDto;
import com.arproperty.entity.LivingInfra;
import com.arproperty.repository.LivingInfraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivabilityService {

    private final LivingInfraRepository livingInfraRepository;

    public record NearbyResult(
            List<LivabilityDto.InfraNearby> items,
            long totalCount,
            int radiusMeters,
            int page,
            int pageSize
    ) {}

    @Transactional(readOnly = true)
    public NearbyResult findNearbyInfra(
            double lat,
            double lon,
            int radiusMeters,
            String category,
            int page,
            int pageSize
    ) {
        int clampedRadius = Math.min(Math.max(radiusMeters, 1), 3000);
        int clampedPage = Math.max(page, 1);
        int clampedPageSize = Math.min(Math.max(pageSize, 1), 100);
        long offsetLong = (long) (clampedPage - 1) * clampedPageSize;
        int offset = offsetLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) offsetLong;
        String normalizedCategory = normalizeCategory(category);

        List<LivabilityDto.InfraNearby> items = livingInfraRepository
                .findNearby(lat, lon, clampedRadius, normalizedCategory, clampedPageSize, offset)
                .stream()
                .map(p -> LivabilityDto.InfraNearby.builder()
                        .infraId(p.getInfraId())
                        .category(p.getCategory())
                        .subCategory(p.getSubCategory())
                        .name(p.getName())
                        .lat(p.getLat())
                        .lon(p.getLon())
                        .address(p.getAddress())
                        .distanceMeters(p.getDistanceMeters())
                        .build())
                .toList();
        long totalCount = livingInfraRepository.countNearby(lat, lon, clampedRadius, normalizedCategory);

        return new NearbyResult(items, totalCount, clampedRadius, clampedPage, clampedPageSize);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        String lowered = category.trim().toLowerCase();
        try {
            return LivingInfra.Category.valueOf(lowered).name();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown category: " + category);
        }
    }
}
