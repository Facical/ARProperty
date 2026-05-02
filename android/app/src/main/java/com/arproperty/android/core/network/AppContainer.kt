package com.arproperty.android.core.network

import android.content.Context
import com.arproperty.android.BuildConfig
import com.arproperty.android.core.model.BuildingDetail
import com.arproperty.android.core.model.BuildingSummary
import com.arproperty.android.core.model.ComplexDetail
import com.arproperty.android.core.model.ComplexSummary
import com.arproperty.android.core.model.LivabilityComparisonItem
import com.arproperty.android.core.model.LivabilityDetail
import com.arproperty.android.core.model.TradeItem
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

interface AppContainer {
    val healthRepository: HealthRepository
    val buildingRepository: BuildingRepository
    val complexRepository: ComplexRepository
    val livabilityRepository: LivabilityRepository
}

@OptIn(ExperimentalSerializationApi::class)
class DefaultAppContainer(
    context: Context,
) : AppContainer {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            },
        )
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(httpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val healthApiService = retrofit.create(HealthApiService::class.java)
    private val buildingsApiService = retrofit.create(BuildingsApiService::class.java)
    private val complexesApiService = retrofit.create(ComplexesApiService::class.java)
    private val livabilityApiService = retrofit.create(LivabilityApiService::class.java)

    override val healthRepository: HealthRepository = DefaultHealthRepository(healthApiService)
    override val buildingRepository: BuildingRepository = DefaultBuildingRepository(buildingsApiService)
    override val complexRepository: ComplexRepository = DefaultComplexRepository(complexesApiService)
    override val livabilityRepository: LivabilityRepository = DefaultLivabilityRepository(livabilityApiService)
}

interface HealthRepository {
    suspend fun getHealthStatus(): Result<Map<String, String>>
}

interface BuildingRepository {
    suspend fun getNearbyBuildings(lat: Double, lon: Double, radius: Int = 500): Result<List<BuildingSummary>>
    suspend fun getBuildingDetail(buildingId: Int): Result<BuildingDetail>
    suspend fun getBuildingTrades(buildingId: Int): Result<List<TradeItem>>
}

interface ComplexRepository {
    suspend fun getComplexes(dongCode: String? = null, name: String? = null): Result<List<ComplexSummary>>
    suspend fun getComplexDetail(complexId: Int): Result<ComplexDetail>
}

interface LivabilityRepository {
    suspend fun getLivability(buildingId: Int, preset: String? = null): Result<LivabilityDetail>
    suspend fun compareLivability(buildingIds: List<Int>, preset: String? = null): Result<List<LivabilityComparisonItem>>
}

private class DefaultHealthRepository(
    private val service: HealthApiService,
) : HealthRepository {
    override suspend fun getHealthStatus(): Result<Map<String, String>> =
        runCatching { service.getHealth().data }
}

private class DefaultBuildingRepository(
    private val service: BuildingsApiService,
) : BuildingRepository {
    override suspend fun getNearbyBuildings(
        lat: Double,
        lon: Double,
        radius: Int,
    ): Result<List<BuildingSummary>> = runCatching {
        service.getNearbyBuildings(lat = lat, lon = lon, radius = radius).data
    }

    override suspend fun getBuildingDetail(buildingId: Int): Result<BuildingDetail> =
        runCatching { service.getBuildingDetail(buildingId).data }

    override suspend fun getBuildingTrades(buildingId: Int): Result<List<TradeItem>> =
        runCatching { service.getBuildingTrades(buildingId).data }
}

private class DefaultComplexRepository(
    private val service: ComplexesApiService,
) : ComplexRepository {
    override suspend fun getComplexes(
        dongCode: String?,
        name: String?,
    ): Result<List<ComplexSummary>> = runCatching {
        service.getComplexes(dongCode = dongCode, name = name).data
    }

    override suspend fun getComplexDetail(complexId: Int): Result<ComplexDetail> =
        runCatching { service.getComplexDetail(complexId).data }
}

private class DefaultLivabilityRepository(
    private val service: LivabilityApiService,
) : LivabilityRepository {
    override suspend fun getLivability(
        buildingId: Int,
        preset: String?,
    ): Result<LivabilityDetail> = runCatching {
        service.getLivability(buildingId = buildingId, preset = preset).data
    }

    override suspend fun compareLivability(
        buildingIds: List<Int>,
        preset: String?,
    ): Result<List<LivabilityComparisonItem>> = runCatching {
        service.compareLivability(
            buildingIds = buildingIds.joinToString(","),
            preset = preset,
        ).data
    }
}
