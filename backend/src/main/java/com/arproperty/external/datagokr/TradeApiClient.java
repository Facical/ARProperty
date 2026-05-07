package com.arproperty.external.datagokr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TradeApiClient {

    private static final String TRADE_PATH = "/1613000/RTMSDataSvcAptTradeDev/getRTMSDataSvcAptTradeDev";

    private final BaseDataGoKrClient baseDataGoKrClient;
    private final XmlMapper xmlMapper = new XmlMapper();

    public List<TradeItem> fetchTradeItems(String lawdCd, String dealYmd) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("LAWD_CD", lawdCd);
        params.put("DEAL_YMD", dealYmd);
        params.put("pageNo", "1");
        params.put("numOfRows", "1000");

        String xml = baseDataGoKrClient.get(TRADE_PATH, params);
        return parseTradeItems(xml);
    }

    public List<TradeItem> parseTradeItems(String xml) {
        try {
            JsonNode root = xmlMapper.readTree(xml.getBytes());
            JsonNode itemsNode = root.path("body").path("items").path("item");
            List<TradeItem> results = new ArrayList<>();
            if (itemsNode.isMissingNode()) {
                return results;
            }
            if (itemsNode.isArray()) {
                for (JsonNode node : itemsNode) {
                    results.add(toTradeItem(node));
                }
            } else {
                results.add(toTradeItem(itemsNode));
            }
            return results;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse trade XML response.", e);
        }
    }

    private TradeItem toTradeItem(JsonNode node) {
        int year = node.path("dealYear").asInt();
        int month = node.path("dealMonth").asInt();
        int day = node.path("dealDay").asInt();
        LocalDate dealDate = LocalDate.of(year, month, day);

        return new TradeItem(
                node.path("aptNm").asText(""),
                node.path("dong").asText(""),
                node.path("jibun").asText(""),
                node.path("exclusiveUseArea").decimalValue(),
                parseMoney(node.path("dealAmount").asText("0")),
                node.path("floor").asInt(0),
                year,
                month,
                dealDate
        );
    }

    private int parseMoney(String amount) {
        return Integer.parseInt(amount.replace(",", "").trim());
    }

    public record TradeItem(
            String aptName,
            String dongName,
            String jibun,
            BigDecimal exclusiveArea,
            Integer dealAmount,
            Integer floor,
            Integer dealYear,
            Integer dealMonth,
            LocalDate dealDate
    ) {
    }
}
