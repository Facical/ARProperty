package com.arproperty.service;

import com.arproperty.entity.AptBuilding;
import com.arproperty.external.vworld.BuildingDataClient;
import com.arproperty.repository.AptBuildingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VWorldBuildingSyncService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private final BuildingDataClient buildingDataClient;
    private final AptBuildingRepository aptBuildingRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.sync.samgu-complex-name:삼구트리니엔}")
    private String samguComplexName;

    @Transactional
    public int syncSamguBuildingGeometry() {
        List<BuildingDataClient.BuildingFeature> features = buildingDataClient.fetchSamguTrinienBuildingFeatures();
        List<AptBuilding> buildings = aptBuildingRepository.findByComplex_ComplexNameContainingIgnoreCase(samguComplexName);

        if (buildings.isEmpty()) {
            log.warn("No apt_building_master rows found for complex name containing '{}'.", samguComplexName);
            return 0;
        }

        Map<String, BuildingDataClient.BuildingFeature> featureByDong = new HashMap<>();
        Map<String, BuildingDataClient.BuildingFeature> featureByDongNumber = new HashMap<>();
        for (BuildingDataClient.BuildingFeature feature : features) {
            String normalized = normalizeDongName(feature.dongName());
            featureByDong.put(normalized, feature);
            String dongNumber = extractDongNumber(normalized);
            if (!dongNumber.isEmpty()) {
                featureByDongNumber.put(dongNumber, feature);
            }
        }

        int updated = 0;
        for (AptBuilding building : buildings) {
            BuildingDataClient.BuildingFeature feature = featureByDong.get(normalizeDongName(building.getDongName()));
            if (feature == null) {
                String dongNumber = extractDongNumber(building.getDongName());
                if (!dongNumber.isEmpty()) {
                    feature = featureByDongNumber.get(dongNumber);
                }
            }
            if (feature == null) {
                continue;
            }

            Polygon polygon = parseFirstPolygon(feature.geometryJson());
            if (polygon == null) {
                continue;
            }

            Point centroid = polygon.getCentroid();
            building.setBuildingManagementNumber(feature.buildingManagementNumber());
            building.setPolygonGeom(polygon);
            building.setCentroid(centroid);
            updated++;
        }

        if (updated > 0) {
            aptBuildingRepository.saveAll(buildings);
        }

        log.info("VWorld sync done. target='{}', fetched={}, updated={}", samguComplexName, features.size(), updated);
        return updated;
    }

    private Polygon parseFirstPolygon(String geometryJson) {
        try {
            JsonNode geometryNode = objectMapper.readTree(geometryJson);
            JsonNode outerRingNode = geometryNode.path("coordinates").path(0).path(0);
            if (!outerRingNode.isArray() || outerRingNode.size() < 4) {
                return null;
            }

            Coordinate[] coordinates = new Coordinate[outerRingNode.size() + 1];
            for (int i = 0; i < outerRingNode.size(); i++) {
                JsonNode point = outerRingNode.path(i);
                coordinates[i] = new Coordinate(point.path(0).asDouble(), point.path(1).asDouble());
            }

            Coordinate first = coordinates[0];
            Coordinate last = coordinates[outerRingNode.size() - 1];
            if (!first.equals2D(last)) {
                coordinates[outerRingNode.size()] = new Coordinate(first.x, first.y);
            } else {
                coordinates = java.util.Arrays.copyOf(coordinates, outerRingNode.size());
            }

            LinearRing shell = GEOMETRY_FACTORY.createLinearRing(coordinates);
            return GEOMETRY_FACTORY.createPolygon(shell);
        } catch (Exception e) {
            log.warn("Failed to parse VWorld geometry JSON.", e);
            return null;
        }
    }

    private String normalizeDongName(String dongName) {
        if (dongName == null) {
            return "";
        }
        String trimmed = dongName.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.endsWith("동") ? trimmed : trimmed + "동";
    }

    private String extractDongNumber(String dongName) {
        if (dongName == null) {
            return "";
        }
        StringBuilder numbers = new StringBuilder();
        for (char ch : dongName.toCharArray()) {
            if (Character.isDigit(ch)) {
                numbers.append(ch);
            }
        }
        return numbers.toString();
    }
}
