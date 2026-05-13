package com.arproperty.external.kakao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/** Kakao Local API 클라이언트 (키워드 검색 → 좌표) */
@Component
public class LocalApiClient {

    private static final String KEYWORD_PATH = "/v2/local/search/keyword.json";

    private final String apiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public LocalApiClient(
            @Value("${app.api.kakao-rest-key:}") String apiKey,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.restClient = restClientBuilder.baseUrl("https://dapi.kakao.com").build();
        this.objectMapper = objectMapper;
    }

    public Optional<KakaoCoord> searchKeyword(String query) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("KAKAO_REST_API_KEY is required.");
        }
        if (!StringUtils.hasText(query)) {
            return Optional.empty();
        }

        String body = restClient.get()
                .uri(uriBuilder -> uriBuilder.path(KEYWORD_PATH)
                        .queryParam("query", query)
                        .queryParam("size", 1)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                .retrieve()
                .body(String.class);

        return parseFirstCoord(body);
    }

    public Optional<KakaoCoord> parseFirstCoord(String body) {
        try {
            JsonNode docs = objectMapper.readTree(body).path("documents");
            if (!docs.isArray() || docs.isEmpty()) {
                return Optional.empty();
            }
            JsonNode first = docs.get(0);
            double lon = Double.parseDouble(first.path("x").asText("0"));
            double lat = Double.parseDouble(first.path("y").asText("0"));
            if (lon == 0.0 && lat == 0.0) {
                return Optional.empty();
            }
            return Optional.of(new KakaoCoord(lon, lat, first.path("place_name").asText(""), first.path("address_name").asText("")));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Kakao Local response.", e);
        }
    }

    public record KakaoCoord(double lon, double lat, String placeName, String addressName) {
    }
}
