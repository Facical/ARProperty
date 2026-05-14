package com.arproperty.repository;

/** LivingInfra JPA Repository (PostGIS 반경 검색) - 생활 인프라 POI 조회 */

import com.arproperty.entity.LivingInfra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LivingInfraRepository extends JpaRepository<LivingInfra, Integer> {

    interface InfraNearbyProjection {
        Integer getInfraId();
        String getCategory();
        String getSubCategory();
        String getName();
        Double getLat();
        Double getLon();
        String getAddress();
        Double getDistanceMeters();
    }

    @Query(value = """
            SELECT
                i.infra_id           AS infraId,
                i.category           AS category,
                i.sub_category       AS subCategory,
                i.name               AS name,
                ST_Y(i.point_geom)   AS lat,
                ST_X(i.point_geom)   AS lon,
                i.address            AS address,
                ST_Distance(
                    i.point_geom::geography,
                    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
                )                    AS distanceMeters
            FROM living_infra_gumi i
            WHERE ST_DWithin(
                    i.point_geom::geography,
                    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                    :radius
                  )
              AND (CAST(:category AS VARCHAR) IS NULL OR i.category = CAST(:category AS VARCHAR))
            ORDER BY distanceMeters ASC
            """, nativeQuery = true)
    List<InfraNearbyProjection> findNearby(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radius") int radiusMeters,
            @Param("category") String category
    );
}
