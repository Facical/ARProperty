package com.arproperty.entity;

/** apt_trade_history - 거래 이력 (@Entity) */

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "apt_trade_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AptTradeHistory {

    public enum TradeType {
        매매, 전세, 월세
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_id")
    private Integer tradeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complex_id", nullable = false)
    private AptComplex complex;

    @Column(name = "dong_name", length = 50)
    private String dongName;

    @Column(name = "floor")
    private Integer floor;

    @Column(name = "exclusive_area", precision = 8, scale = 2)
    private BigDecimal exclusiveArea;

    @Column(name = "deal_amount")
    private Integer dealAmount;

    @Column(name = "deposit")
    private Integer deposit;

    @Column(name = "monthly_rent")
    private Integer monthlyRent;

    @Column(name = "deal_date", nullable = false)
    private LocalDate dealDate;

    @Column(name = "deal_year", nullable = false)
    private Integer dealYear;

    @Column(name = "deal_month", nullable = false)
    private Integer dealMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", length = 10, nullable = false)
    private TradeType tradeType;

    @Column(name = "building_year")
    private Integer buildingYear;

    @Column(name = "jibun", length = 20)
    private String jibun;

    @Column(name = "apt_name", length = 100)
    private String aptName;

    @Column(name = "dealing_type", length = 20)
    private String dealingType;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
