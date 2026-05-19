package com.arproperty.repository;

/** AptBuilding JPA Repository (PostGIS 공간 쿼리 포함) - 단지별 동 목록 / 반경 검색 */

import com.arproperty.entity.AptBuilding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AptBuildingRepository extends JpaRepository<AptBuilding, Integer> {

    List<AptBuilding> findByComplex_ComplexId(Integer complexId);

    List<AptBuilding> findByComplex_ComplexNameContainingIgnoreCase(String complexName);

    Optional<AptBuilding> findByBuildingManagementNumber(String buildingManagementNumber);

    @Query(value = """
        SELECT
            b.building_id, b.complex_id, c.complex_name, b.dong_name,
            ST_Y(b.centroid) AS lat, ST_X(b.centroid) AS lon,
            b.ground_floors, b.underground_floors, b.highest_floor,
            b.building_height, b.structure_type, b.total_area, b.use_approval_date,
            c.kapt_code, c.households, c.building_count, c.parking_count,
            c.elevator_count, c.heating_type, c.constructor,
            s.total_score AS livability_score, s.grade AS livability_grade
        FROM apt_building_master b
        JOIN apt_complex_master c ON c.complex_id = b.complex_id
        LEFT JOIN building_livability_score s
            ON s.building_id = b.building_id AND s.weight_preset = 'default'
        WHERE b.building_id = :buildingId
        """, nativeQuery = true)
    List<Object[]> findDetail(@Param("buildingId") int buildingId);

    @Query(value = """
        SELECT
            b.building_id, b.complex_id, c.complex_name, b.dong_name,
            ST_Y(b.centroid) AS lat, ST_X(b.centroid) AS lon,
            b.ground_floors, b.underground_floors, b.highest_floor, b.building_height,
            s.grade AS livability_grade, s.total_score AS livability_score,
            ST_DistanceSphere(b.centroid, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)) AS distance_m,
            t.deal_amount AS latest_deal_amount, t.exclusive_area AS latest_area,
            t.floor AS latest_floor, t.deal_date AS latest_deal_date, t.trade_type AS latest_trade_type
        FROM apt_building_master b
        JOIN apt_complex_master c ON c.complex_id = b.complex_id
        LEFT JOIN building_livability_score s
            ON s.building_id = b.building_id AND s.weight_preset = 'default'
        LEFT JOIN LATERAL (
            SELECT th.deal_amount, th.exclusive_area, th.floor, th.deal_date, th.trade_type
            FROM apt_trade_history th
            WHERE th.complex_id = b.complex_id AND th.dong_name = b.dong_name
            ORDER BY th.deal_date DESC
            LIMIT 1
        ) t ON TRUE
        WHERE b.centroid IS NOT NULL
          AND ST_DWithin(
                b.centroid::geography,
                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                :radius
              )
        ORDER BY distance_m ASC
        LIMIT :pageSize OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findNearby(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radius") int radius,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM apt_building_master b
        WHERE b.centroid IS NOT NULL
          AND ST_DWithin(
                b.centroid::geography,
                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                :radius
              )
        """, nativeQuery = true)
    long countNearby(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radius") int radius
    );
}
