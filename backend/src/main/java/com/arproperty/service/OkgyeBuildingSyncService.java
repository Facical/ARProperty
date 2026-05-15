package com.arproperty.service;

import com.arproperty.entity.AptBuilding;
import com.arproperty.entity.AptComplex;
import com.arproperty.external.vworld.BuildingDataClient;
import com.arproperty.repository.AptBuildingRepository;
import com.arproperty.repository.AptComplexRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OkgyeBuildingSyncService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private static final int MIN_APT_FLOORS = 10;
    private static final double MATCH_THRESHOLD_METERS = 500.0;
    private static final int PAGE_SIZE = 1000;
    private static final int MAX_PAGES = 5;

    @Value("${app.sync.okgye-bjd-code:4719012800}")
    private String okgyeBjdCode;

    private final BuildingDataClient buildingDataClient;
    private final AptComplexRepository aptComplexRepository;
    private final AptBuildingRepository aptBuildingRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public int syncOkgyeBuildings() {
        List<AptComplex> complexes = aptComplexRepository.findAll().stream()
                .filter(c -> okgyeBjdCode.equals(c.getLegalDongCode()))
                .filter(c -> c.getCentroid() != null)
                .toList();
        if (complexes.isEmpty()) {
            log.warn("sync-okgye-buildings: 옥계동 단지가 없습니다. 먼저 --sync-okgye-complex를 실행하세요.");
            return 0;
        }
        log.info("sync-okgye-buildings: target complexes={}", complexes.size());

        List<BuildingDataClient.BuildingFeature> all = fetchAllPages();
        log.info("sync-okgye-buildings: fetched VWorld features={}", all.size());

        int skippedFar = 0;
        int skippedNonApt = 0;
        int skippedNoPolygon = 0;
        int skippedDupDong = 0;
        Map<String, AptBuilding> picked = new LinkedHashMap<>();
        for (BuildingDataClient.BuildingFeature feature : all) {
            if (!isAptCandidate(feature)) {
                skippedNonApt++;
                continue;
            }
            Polygon polygon = parseFirstPolygon(feature.geometryJson());
            if (polygon == null) {
                skippedNoPolygon++;
                continue;
            }
            Point centroid = polygon.getCentroid();
            AptComplex nearest = findNearestComplex(complexes, centroid);
            if (nearest == null) {
                skippedFar++;
                continue;
            }
            String dongName = normalizeDongName(feature.dongName(), feature.buildingManagementNumber());
            String key = nearest.getComplexId() + "|" + dongName;
            if (picked.containsKey(key)) {
                skippedDupDong++;
                continue;
            }

            AptBuilding building = aptBuildingRepository.findByBuildingManagementNumber(feature.buildingManagementNumber())
                    .orElseGet(AptBuilding::new);
            building.setComplex(nearest);
            building.setDongName(dongName);
            building.setBuildingManagementNumber(feature.buildingManagementNumber());
            building.setPolygonGeom(polygon);
            building.setCentroid(centroid);
            if (feature.groundFloors() > 0) {
                building.setGroundFloors(feature.groundFloors());
            }
            picked.put(key, building);
        }

        aptBuildingRepository.saveAll(picked.values());
        int updated = picked.size();
        log.info("sync-okgye-buildings done. fetched={}, updated={}, skipped(non-apt)={}, skipped(no-polygon)={}, skipped(far>{}m)={}, skipped(dup-dong)={}",
                all.size(), updated, skippedNonApt, skippedNoPolygon, (int) MATCH_THRESHOLD_METERS, skippedFar, skippedDupDong);
        return updated;
    }

    private List<BuildingDataClient.BuildingFeature> fetchAllPages() {
        List<BuildingDataClient.BuildingFeature> all = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            List<BuildingDataClient.BuildingFeature> chunk =
                    buildingDataClient.fetchByBdMgtSnPrefix(okgyeBjdCode, page, PAGE_SIZE);
            if (chunk.isEmpty()) break;
            int newCount = 0;
            for (BuildingDataClient.BuildingFeature f : chunk) {
                if (seen.add(f.buildingManagementNumber())) {
                    all.add(f);
                    newCount++;
                }
            }
            log.info("sync-okgye-buildings: page={} fetched={} (new={}, total={})",
                    page, chunk.size(), newCount, all.size());
            if (newCount == 0) break;
            if (chunk.size() < PAGE_SIZE) break;
        }
        return all;
    }

    private boolean isAptCandidate(BuildingDataClient.BuildingFeature f) {
        if (f.groundFloors() < MIN_APT_FLOORS) return false;
        return StringUtils.hasText(f.dongName());
    }

    private AptComplex findNearestComplex(List<AptComplex> complexes, Point centroid) {
        AptComplex nearest = null;
        double bestMeters = Double.MAX_VALUE;
        double cosLat = Math.cos(Math.toRadians(centroid.getY()));
        for (AptComplex c : complexes) {
            Point cc = c.getCentroid();
            double dLat = (centroid.getY() - cc.getY()) * 111_000.0;
            double dLon = (centroid.getX() - cc.getX()) * 111_000.0 * cosLat;
            double meters = Math.sqrt(dLat * dLat + dLon * dLon);
            if (meters < bestMeters) {
                bestMeters = meters;
                nearest = c;
            }
        }
        return bestMeters <= MATCH_THRESHOLD_METERS ? nearest : null;
    }

    private String normalizeDongName(String dongName, String bdMgtSn) {
        if (!StringUtils.hasText(dongName)) {
            return "동-" + bdMgtSn.substring(Math.max(0, bdMgtSn.length() - 6));
        }
        String trimmed = dongName.trim();
        return trimmed.endsWith("동") ? trimmed : trimmed + "동";
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
}
