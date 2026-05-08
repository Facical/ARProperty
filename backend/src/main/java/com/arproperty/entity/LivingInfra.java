package com.arproperty.entity;

/** living_infra_gumi - 구미시 생활 인프라 (@Entity, PostGIS Point) */

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "living_infra_gumi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivingInfra {

    public enum Category {
        medical, education, convenience, transport, safety, leisure
    }

    public enum DataSource {
        kakao, gumi_opendata, data_go_kr, manual
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "infra_id")
    private Integer infraId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private Category category;

    @Column(name = "sub_category", length = 50, nullable = false)
    private String subCategory;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "point_geom", columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point pointGeom;

    @Column(name = "address", length = 300)
    private String address;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail_json", columnDefinition = "jsonb")
    private JsonNode detailJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", length = 50, nullable = false)
    private DataSource dataSource;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
