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
    @SerialName("structure_type") val structureType: String? = null,
    @SerialName("total_area") val totalArea: Double? = null,
    @SerialName("use_approval_date") val useApprovalDate: String? = null,
    @SerialName("complex_info") val complexInfo: BuildingComplexInfo? = null,
    val livability: BuildingLivabilitySummary? = null,
)

@Serializable
data class BuildingComplexInfo(
    @SerialName("kapt_code") val kaptCode: String? = null,
    val households: Int? = null,
    @SerialName("building_count") val buildingCount: Int? = null,
    @SerialName("parking_count") val parkingCount: Int? = null,
    @SerialName("elevator_count") val elevatorCount: Int? = null,
    @SerialName("heating_type") val heatingType: String? = null,
    val constructor: String? = null,
)

@Serializable
data class BuildingLivabilitySummary(
    @SerialName("total_score") val totalScore: Double? = null,
    val grade: String? = null,
)
