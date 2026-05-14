package com.arproperty.dto;

/** 편의시설 점수 DTO */

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class LivabilityDto {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InfraNearby {
        @JsonProperty("infra_id")
        private Integer infraId;

        private String category;

        @JsonProperty("sub_category")
        private String subCategory;

        private String name;

        private Double lat;

        private Double lon;

        private String address;

        @JsonProperty("distance_m")
        private Double distanceMeters;
    }
}
