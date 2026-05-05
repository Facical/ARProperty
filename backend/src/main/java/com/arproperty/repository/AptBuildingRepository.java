package com.arproperty.repository;

/** AptBuilding JPA Repository (PostGIS 공간 쿼리 포함) - 단지별 동 목록 조회 */

import com.arproperty.entity.AptBuilding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AptBuildingRepository extends JpaRepository<AptBuilding, Integer> {

    List<AptBuilding> findByComplex_ComplexId(Integer complexId);
}
