package com.arproperty.external.vworld;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Vworld 2D Data API client for Samgu Trinien building polygons. */
@Component
public class BuildingDataClient {

    private static final String DATA_PATH = "/req/data";
    private static final String BUILDING_LAYER = "LT_C_SPBD";
    private static final String SAMGU_TRINIEN_BD_MGT_SN_PREFIX = "4719012800109070000";

    private final String apiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public BuildingDataClient(
            @Value("${app.api.vworld-key:}") String apiKey,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.restClient = restClientBuilder.baseUrl("https://api.vworld.kr").build();
        this.objectMapper = objectMapper;
    }

    public String fetchSamguTrinienBuildings() {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("VWORLD_API_KEY is required.");
        }

        return restClient.get()
                .uri(this::buildSamguTrinienUri)
                .retrieve()
                .body(String.class);
    }

    public List<BuildingFeature> fetchSamguTrinienBuildingFeatures() {
        return parseBuildingFeatures(fetchSamguTrinienBuildings());
    }

    public List<BuildingFeature> parseBuildingFeatures(String responseBody) {
        try {
            JsonNode response = objectMapper.readTree(responseBody).path("response");
            String status = response.path("status").asText();
            if (!"OK".equals(status)) {
                JsonNode error = response.path("error");
                throw new IllegalStateException("VWorld API error: " + error.path("code").asText() + " " + error.path("text").asText());
            }

            JsonNode features = response.path("result").path("featureCollection").path("features");
            List<BuildingFeature> result = new ArrayList<>();
            for (JsonNode feature : features) {
                JsonNode properties = feature.path("properties");
                JsonNode geometry = feature.path("geometry");
                result.add(new BuildingFeature(
                        feature.path("id").asText(),
                        properties.path("buld_nm").asText(),
                        properties.path("buld_nm_dc").asText(),
                        properties.path("rd_nm").asText(),
                        properties.path("gro_flo_co").asInt(),
                        properties.path("bd_mgt_sn").asText(),
                        geometry.path("type").asText(),
                        geometry.toString()
                ));
            }
            return result;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse VWorld building response.", e);
        }
    }

    private URI buildSamguTrinienUri(org.springframework.web.util.UriBuilder uriBuilder) {
        return uriBuilder.path(DATA_PATH)
                .queryParam("service", "data")
                .queryParam("request", "GetFeature")
                .queryParam("data", BUILDING_LAYER)
                .queryParam("key", apiKey)
                .queryParam("domain", "localhost")
                .queryParam("format", "json")
                .queryParam("crs", "EPSG:4326")
                .queryParam("size", "100")
                .queryParam("page", "1")
                .queryParam("geometry", "true")
                .queryParam("attribute", "true")
                .queryParam("attrFilter", "bd_mgt_sn:like:" + SAMGU_TRINIEN_BD_MGT_SN_PREFIX)
                .build();
    }

    public record BuildingFeature(
            String id,
            String buildingName,
            String dongName,
            String roadName,
            int groundFloors,
            String buildingManagementNumber,
            String geometryType,
            String geometryJson
    ) {
    }
}
