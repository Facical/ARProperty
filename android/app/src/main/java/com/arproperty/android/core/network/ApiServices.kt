package com.arproperty.android.core.network

import com.arproperty.android.core.model.ApiResponse
import com.arproperty.android.core.model.BuildingDetail
import com.arproperty.android.core.model.BuildingSummary
import com.arproperty.android.core.model.ComplexDetail
import com.arproperty.android.core.model.ComplexSummary
import com.arproperty.android.core.model.LivabilityComparisonItem
import com.arproperty.android.core.model.LivabilityDetail
import com.arproperty.android.core.model.TradeItem
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface HealthApiService {
    @GET("health")
    suspend fun getHealth(): ApiResponse<Map<String, String>>
}

interface BuildingsApiService {
    @GET("api/v1/buildings/nearby")
    suspend fun getNearbyBuildings(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radius") radius: Int? = null,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
    ): ApiResponse<List<BuildingSummary>>

    @GET("api/v1/buildings/{buildingId}")
    suspend fun getBuildingDetail(
        @Path("buildingId") buildingId: Int,
    ): ApiResponse<BuildingDetail>

    @GET("api/v1/buildings/{buildingId}/trades")
    suspend fun getBuildingTrades(
        @Path("buildingId") buildingId: Int,
        @Query("type") type: String? = null,
        @Query("year") year: Int? = null,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
    ): ApiResponse<List<TradeItem>>
}

interface ComplexesApiService {
    @GET("api/v1/complexes")
    suspend fun getComplexes(
        @Query("dong_code") dongCode: String? = null,
        @Query("name") name: String? = null,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
    ): ApiResponse<List<ComplexSummary>>

    @GET("api/v1/complexes/{complexId}")
    suspend fun getComplexDetail(
        @Path("complexId") complexId: Int,
    ): ApiResponse<ComplexDetail>
}

interface LivabilityApiService {
    @GET("api/v1/livability/{buildingId}")
    suspend fun getLivability(
        @Path("buildingId") buildingId: Int,
        @Query("preset") preset: String? = null,
    ): ApiResponse<LivabilityDetail>

    @GET("api/v1/livability/compare")
    suspend fun compareLivability(
        @Query("building_ids") buildingIds: String,
        @Query("preset") preset: String? = null,
    ): ApiResponse<List<LivabilityComparisonItem>>
}
