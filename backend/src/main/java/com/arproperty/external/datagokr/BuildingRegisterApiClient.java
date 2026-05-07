package com.arproperty.external.datagokr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BuildingRegisterApiClient {

    private static final String TITLE_INFO_PATH = "/1613000/BldRgstService_v2/getBrTitleInfo";

    private final BaseDataGoKrClient baseDataGoKrClient;
    private final XmlMapper xmlMapper = new XmlMapper();

    public BuildingRegisterItem fetchTitleInfo(String sigunguCd, String bjdongCd, String bun, String ji) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("sigunguCd", sigunguCd);
        params.put("bjdongCd", bjdongCd);
        params.put("platGbCd", "0");
        params.put("bun", bun);
        params.put("ji", ji);
        params.put("numOfRows", "10");
        params.put("pageNo", "1");

        String xml = baseDataGoKrClient.get(TITLE_INFO_PATH, params);
        return parseFirstItem(xml);
    }

    public BuildingRegisterItem parseFirstItem(String xml) {
        try {
            JsonNode root = xmlMapper.readTree(xml.getBytes());
            JsonNode itemNode = root.path("body").path("items").path("item");
            if (itemNode.isMissingNode()) {
                return null;
            }
            if (itemNode.isArray()) {
                if (itemNode.isEmpty()) {
                    return null;
                }
                itemNode = itemNode.get(0);
            }

            return new BuildingRegisterItem(
                    itemNode.path("grndFlrCnt").asInt(0),
                    itemNode.path("ugrndFlrCnt").asInt(0),
                    itemNode.path("heit").decimalValue(),
                    itemNode.path("strctCdNm").asText(""),
                    itemNode.path("totArea").decimalValue(),
                    parseDate(itemNode.path("useAprDay").asText(""))
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse building register XML response.", e);
        }
    }

    private LocalDate parseDate(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) {
            return null;
        }
        return LocalDate.of(
                Integer.parseInt(yyyymmdd.substring(0, 4)),
                Integer.parseInt(yyyymmdd.substring(4, 6)),
                Integer.parseInt(yyyymmdd.substring(6, 8))
        );
    }

    public record BuildingRegisterItem(
            Integer groundFloors,
            Integer undergroundFloors,
            BigDecimal buildingHeight,
            String structureType,
            BigDecimal totalArea,
            LocalDate useApprovalDate
    ) {
    }
}
