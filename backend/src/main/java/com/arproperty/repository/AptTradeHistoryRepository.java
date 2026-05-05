package com.arproperty.repository;

/** AptTradeHistory JPA Repository - 단지 기준 거래 이력 최신순 조회 */

import com.arproperty.entity.AptTradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AptTradeHistoryRepository extends JpaRepository<AptTradeHistory, Integer> {

    List<AptTradeHistory> findByComplex_ComplexIdOrderByDealDateDesc(Integer complexId);
}
