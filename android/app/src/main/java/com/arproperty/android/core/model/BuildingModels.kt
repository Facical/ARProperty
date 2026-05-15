package com.arproperty.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BuildingSummary(
    @SerialName("building_id") val buildingId: Int,
    @SerialName("complex_id") val complexId: Int,
    @SerialName("complex_name") val complexName: String,
    @SerialName("dong_name") val dongName: String,
    val lat: Double,
    val lon: Double,
    @SerialName("ground_floors") val groundFloors: Int? = null,
    @SerialName("livability_grade") val livabilityGrade: String? = null,
    @SerialName("livability_score") val livabilityScore: Double? = null,
    @SerialName("distance_m") val distanceMeters: Double? = null,
    @SerialName("latest_trade") val latestTrade: LatestTrade? = null,
)

@Serializable
data class LatestTrade(
    @SerialName("deal_amount") val dealAmount: Int? = null,
    @SerialName("exclusive_area") val exclusiveArea: Double? = null,
    val floor: Int? = null,
    @SerialName("deal_date") val dealDate: String? = null,
    @SerialName("trade_type") val tradeType: String? = null,
)

@Serializable
data class BuildingDetail(
    @SerialName("building_id") val buildingId: Int,
    @SerialName("complex_id") val complexId: Int,
    @SerialName("complex_name") val complexName: String,
    @SerialName("dong_name") val dongName: String,
    val lat: Double,
    val lon: Double,
    @SerialName("ground_floors") val groundFloors: Int,
    @SerialName("underground_floors") val undergroundFloors: Int,
    @SerialName("highest_floor") val highestFloor: Int,
    @SerialName("building_height") val buildingHeight: Double? = null,
)
