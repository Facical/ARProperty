package com.arproperty.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ComplexSummary(
    @SerialName("complex_id") val complexId: Int,
    @SerialName("kapt_code") val kaptCode: String? = null,
    @SerialName("complex_name") val complexName: String,
    @SerialName("road_address") val roadAddress: String? = null,
    val households: Int? = null,
    @SerialName("building_count") val buildingCount: Int? = null,
)

@Serializable
data class ComplexDetail(
    @SerialName("complex_id") val complexId: Int,
    @SerialName("complex_name") val complexName: String,
    @SerialName("road_address") val roadAddress: String? = null,
    @SerialName("parcel_address") val parcelAddress: String? = null,
    val households: Int? = null,
    @SerialName("building_count") val buildingCount: Int? = null,
)
