package com.arproperty.entity;

/** api_call_log - API 호출 제한 추적 (@Entity) */

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "api_call_log",
        uniqueConstraints = @UniqueConstraint(columnNames = {"api_name", "call_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer logId;

    @Column(name = "api_name", length = 50, nullable = false)
    private String apiName;

    @Column(name = "call_date", nullable = false)
    private LocalDate callDate;

    @Column(name = "call_count")
    private Integer callCount;

    @Column(name = "daily_limit", nullable = false)
    private Integer dailyLimit;
}
