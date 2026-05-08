package com.arproperty.external.datagokr;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class BaseDataGoKrClient {

    private final RestClient restClient;
    private final String apiKey;

    public BaseDataGoKrClient(
            @Value("${app.api.data-go-kr-base-url:https://apis.data.go.kr}") String baseUrl,
            @Value("${app.api.data-go-kr-key:}") String apiKey,
            RestClient.Builder builder
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public String get(String path, Map<String, String> queryParams) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("DATA_GO_KR_API_KEY is required.");
        }
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(path).queryParam("serviceKey", apiKey);
                    queryParams.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                })
                .retrieve()
                .body(String.class);
    }
}
