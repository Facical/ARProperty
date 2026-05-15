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

    @Transactional(readOnly = true)
    public List<LivabilityDto.InfraNearby> findNearbyInfra(
            double lat,
            double lon,
            int radiusMeters,
            String category
    ) {
        String normalizedCategory = normalizeCategory(category);

        return livingInfraRepository
                .findNearby(lat, lon, radiusMeters, normalizedCategory)
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
