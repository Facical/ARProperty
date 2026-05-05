package com.arproperty.entity;

/** apt_complex_master - 아파트 단지 마스터 (@Entity) */

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "apt_complex_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AptComplex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complex_id")
    private Integer complexId;

    @Column(name = "kapt_code", length = 20, unique = true)
    private String kaptCode;

    @Column(name = "reb_complex_id", length = 20)
    private String rebComplexId;

    @Column(name = "complex_name", length = 100, nullable = false)
    private String complexName;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "legal_dong_code", length = 10, nullable = false)
    private String legalDongCode;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "sigungu_cd", length = 5, nullable = false)
    private String sigunguCd;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "bjdong_cd", length = 5, nullable = false)
    private String bjdongCd;

    @Column(name = "road_address", length = 200)
    private String roadAddress;

    @Column(name = "parcel_address", length = 200)
    private String parcelAddress;

    @Column(name = "households")
    private Integer households;

    @Column(name = "building_count")
    private Integer buildingCount;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "constructor", length = 100)
    private String constructor;

    @Column(name = "heating_type", length = 50)
    private String heatingType;

    @Column(name = "management_type", length = 50)
    private String managementType;

    @Column(name = "parking_count")
    private Integer parkingCount;

    @Column(name = "elevator_count")
    private Integer elevatorCount;

    @Column(name = "centroid", columnDefinition = "geometry(Point,4326)")
    private Point centroid;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
