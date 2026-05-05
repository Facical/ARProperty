package com.arproperty.entity;

/** building_livability_score - 편의시설 점수 캐시 (@Entity) */

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "building_livability_score",
        uniqueConstraints = @UniqueConstraint(columnNames = {"building_id", "weight_preset"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildingLivabilityScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Integer scoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private AptBuilding building;

    @Column(name = "total_score", precision = 5, scale = 1, nullable = false)
    private BigDecimal totalScore;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "grade", length = 1, nullable = false)
    private String grade;

    @Column(name = "medical_score", precision = 5, scale = 1)
    private BigDecimal medicalScore;

    @Column(name = "education_score", precision = 5, scale = 1)
    private BigDecimal educationScore;

    @Column(name = "convenience_score", precision = 5, scale = 1)
    private BigDecimal convenienceScore;

    @Column(name = "transport_score", precision = 5, scale = 1)
    private BigDecimal transportScore;

    @Column(name = "safety_score", precision = 5, scale = 1)
    private BigDecimal safetyScore;

    @Column(name = "leisure_score", precision = 5, scale = 1)
    private BigDecimal leisureScore;

    @Column(name = "weight_preset", length = 20)
    private String weightPreset;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nearest_json", columnDefinition = "jsonb")
    private JsonNode nearestJson;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
