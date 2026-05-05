package com.arproperty.entity;

/** apt_building_master - 아파트 동 마스터 (@Entity, PostGIS Polygon) */

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "apt_building_master",
        uniqueConstraints = @UniqueConstraint(columnNames = {"complex_id", "dong_name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AptBuilding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "building_id")
    private Integer buildingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complex_id", nullable = false)
    private AptComplex complex;

    @Column(name = "dong_name", length = 50, nullable = false)
    private String dongName;

    @Column(name = "polygon_geom", columnDefinition = "geometry(Polygon,4326)")
    private Polygon polygonGeom;

    @Column(name = "centroid", columnDefinition = "geometry(Point,4326)")
    private Point centroid;

    @Column(name = "ground_floors")
    private Integer groundFloors;

    @Column(name = "underground_floors")
    private Integer undergroundFloors;

    @Column(name = "highest_floor")
    private Integer highestFloor;

    @Column(name = "building_height", precision = 8, scale = 2)
    private BigDecimal buildingHeight;

    @Column(name = "structure_type", length = 50)
    private String structureType;

    @Column(name = "total_area", precision = 12, scale = 2)
    private BigDecimal totalArea;

    @Column(name = "use_approval_date")
    private LocalDate useApprovalDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
