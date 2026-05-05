package com.arproperty.repository;

/** BuildingLivabilityScore JPA Repository - 건물·가중치별 점수 캐시 조회 */

import com.arproperty.entity.BuildingLivabilityScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BuildingLivabilityScoreRepository extends JpaRepository<BuildingLivabilityScore, Integer> {

    Optional<BuildingLivabilityScore> findByBuilding_BuildingIdAndWeightPreset(Integer buildingId, String weightPreset);
}
