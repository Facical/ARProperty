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
    @SerialName("ground_floors") val groundFloors: Int,
    @SerialName("livability_grade") val livabilityGrade: String? = null,
    @SerialName("livability_score") val livabilityScore: Double? = null,
    @SerialName("distance_m") val distanceMeters: Double? = null,
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
