package com.arproperty.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LivabilityDetail(
    @SerialName("building_id") val buildingId: Int,
    @SerialName("total_score") val totalScore: Double,
    val grade: String,
    @SerialName("weight_preset") val weightPreset: String,
)

@Serializable
data class LivabilityComparisonItem(
    @SerialName("building_id") val buildingId: Int,
    @SerialName("complex_name") val complexName: String,
    @SerialName("dong_name") val dongName: String,
    @SerialName("total_score") val totalScore: Double,
    val grade: String,
)

@Serializable
data class InfraNearby(
    @SerialName("infra_id") val infraId: Int? = null,
    val category: String,
    @SerialName("sub_category") val subCategory: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val address: String? = null,
    @SerialName("distance_m") val distanceMeters: Double? = null,
)
