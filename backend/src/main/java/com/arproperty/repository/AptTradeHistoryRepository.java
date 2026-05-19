package com.arproperty.repository;

/** AptTradeHistory JPA Repository - 단지 기준 거래 이력 최신순 조회 */

import com.arproperty.entity.AptTradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AptTradeHistoryRepository extends JpaRepository<AptTradeHistory, Integer> {

    List<AptTradeHistory> findByComplex_ComplexIdOrderByDealDateDesc(Integer complexId);

    @Query(value = """
        SELECT
            th.trade_id, th.dong_name, th.floor, th.exclusive_area,
            th.deal_amount, th.deposit, th.monthly_rent,
            th.deal_date, th.trade_type, th.dealing_type
        FROM apt_trade_history th
        JOIN apt_building_master b
            ON b.complex_id = th.complex_id AND b.dong_name = th.dong_name
        WHERE b.building_id = :buildingId
          AND (:type IS NULL OR th.trade_type = :type)
          AND (:year IS NULL OR th.deal_year = :year)
        ORDER BY th.deal_date DESC, th.trade_id DESC
        LIMIT :pageSize OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findBuildingTrades(
            @Param("buildingId") int buildingId,
            @Param("type") String type,
            @Param("year") Integer year,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM apt_trade_history th
        JOIN apt_building_master b
            ON b.complex_id = th.complex_id AND b.dong_name = th.dong_name
        WHERE b.building_id = :buildingId
          AND (:type IS NULL OR th.trade_type = :type)
          AND (:year IS NULL OR th.deal_year = :year)
        """, nativeQuery = true)
    long countBuildingTrades(
            @Param("buildingId") int buildingId,
            @Param("type") String type,
            @Param("year") Integer year
    );

    boolean existsByComplex_ComplexIdAndDongNameAndFloorAndExclusiveAreaAndDealDateAndDealAmount(
            Integer complexId,
            String dongName,
            Integer floor,
            java.math.BigDecimal exclusiveArea,
            java.time.LocalDate dealDate,
            Integer dealAmount
    );
}
