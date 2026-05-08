package com.arproperty.repository;

/** AptComplex JPA Repository - 단지 조회 (kaptCode 단건 조회 포함) */

import com.arproperty.entity.AptComplex;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AptComplexRepository extends JpaRepository<AptComplex, Integer> {

    Optional<AptComplex> findByKaptCode(String kaptCode);
}
