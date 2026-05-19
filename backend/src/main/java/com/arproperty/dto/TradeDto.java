package com.arproperty.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 거래 이력 DTO */
public class TradeDto {

    public record TradeItemResponse(
            @JsonProperty("trade_id") Integer tradeId,
            @JsonProperty("dong_name") String dongName,
            Integer floor,
            @JsonProperty("exclusive_area") Double exclusiveArea,
            @JsonProperty("deal_amount") Integer dealAmount,
            Integer deposit,
            @JsonProperty("monthly_rent") Integer monthlyRent,
            @JsonProperty("deal_date") String dealDate,
            @JsonProperty("trade_type") String tradeType,
            @JsonProperty("dealing_type") String dealingType
    ) {}
}
