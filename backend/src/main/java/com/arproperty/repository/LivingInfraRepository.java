package com.arproperty.repository;

/** LivingInfra JPA Repository (PostGIS 반경 검색) - 생활 인프라 POI 조회 */

import com.arproperty.entity.LivingInfra;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LivingInfraRepository extends JpaRepository<LivingInfra, Integer> {
}
