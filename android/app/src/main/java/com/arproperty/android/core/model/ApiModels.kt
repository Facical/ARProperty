package com.arproperty.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val status: String,
    val data: T,
    val meta: ApiMeta? = null,
)

@Serializable
data class ApiMeta(
    val page: Int? = null,
    @SerialName("page_size") val pageSize: Int? = null,
    @SerialName("total_count") val totalCount: Int? = null,
)

@Serializable
data class ApiErrorResponse(
    val status: String,
    val error: ApiError,
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
)
