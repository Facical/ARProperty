package com.arproperty.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TradeItem(
    @SerialName("trade_id") val tradeId: Int,
    @SerialName("dong_name") val dongName: String,
    val floor: Int? = null,
    @SerialName("exclusive_area") val exclusiveArea: Double? = null,
    @SerialName("deal_amount") val dealAmount: Int? = null,
    val deposit: Int? = null,
    @SerialName("monthly_rent") val monthlyRent: Int? = null,
    @SerialName("deal_date") val dealDate: String,
    @SerialName("trade_type") val tradeType: String,
)
