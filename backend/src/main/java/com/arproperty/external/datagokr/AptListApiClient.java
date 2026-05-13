package com.arproperty.external.datagokr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 공동주택 단지 목록 API 클라이언트 (K-apt AptListService3, JSON) */
@Component
@RequiredArgsConstructor
public class AptListApiClient {

    private static final String LEGALDONG_PATH = "/1613000/AptListService3/getLegaldongAptList3";

    private final BaseDataGoKrClient baseDataGoKrClient;
    private final ObjectMapper objectMapper;

    public List<AptListItem> fetchByLegalDong(String bjdCode) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("bjdCode", bjdCode);
        params.put("numOfRows", "100");
        params.put("pageNo", "1");
        params.put("type", "json");

        String body = baseDataGoKrClient.get(LEGALDONG_PATH, params);
        return parseItems(body);
    }

    public List<AptListItem> parseItems(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode items = root.path("response").path("body").path("items");
            List<AptListItem> result = new ArrayList<>();
            if (items.isMissingNode() || items.isNull()) {
                return result;
            }
            JsonNode iterable = items.isArray() ? items : items.path("item");
            if (iterable.isArray()) {
                for (JsonNode node : iterable) {
                    result.add(toItem(node));
                }
            } else if (!iterable.isMissingNode() && !iterable.isNull()) {
                result.add(toItem(iterable));
            }
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse apt_list response.", e);
        }
    }

    private AptListItem toItem(JsonNode node) {
        return new AptListItem(
                node.path("kaptCode").asText(""),
                node.path("kaptName").asText(""),
                node.path("doroJuso").asText(""),
                buildParcelAddress(node),
                node.path("bjdCode").asText("")
        );
    }

    private String buildParcelAddress(JsonNode node) {
        String as1 = node.path("as1").asText("");
        String as2 = node.path("as2").asText("");
        String as3 = node.path("as3").asText("");
        String as4 = node.path("as4").asText("");
        StringBuilder sb = new StringBuilder();
        for (String part : new String[]{as1, as2, as3, as4}) {
            if (part != null && !part.isBlank()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(part);
            }
        }
        return sb.toString();
    }

    public record AptListItem(
            String kaptCode,
            String kaptName,
            String roadAddress,
            String parcelAddress,
            String bjdCode
    ) {
    }
}
