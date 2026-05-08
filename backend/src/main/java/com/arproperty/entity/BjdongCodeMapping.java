package com.arproperty.entity;

/** bjdong_code_mapping - 법정동코드 매핑 (@Entity) */

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "bjdong_code_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BjdongCodeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code_id")
    private Integer codeId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "legal_dong_code", length = 10, nullable = false, unique = true)
    private String legalDongCode;

    @Column(name = "sido_name", length = 20, nullable = false)
    private String sidoName;

    @Column(name = "sigungu_name", length = 20, nullable = false)
    private String sigunguName;

    @Column(name = "dong_name", length = 30, nullable = false)
    private String dongName;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
