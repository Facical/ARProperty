package com.arproperty.service;

import com.arproperty.entity.AptBuilding;
import com.arproperty.entity.AptComplex;
import com.arproperty.entity.AptTradeHistory;
import com.arproperty.external.datagokr.BuildingRegisterApiClient;
import com.arproperty.external.datagokr.TradeApiClient;
import com.arproperty.repository.AptBuildingRepository;
import com.arproperty.repository.AptTradeHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataGoKrSyncService {

    private final TradeApiClient tradeApiClient;
    private final BuildingRegisterApiClient buildingRegisterApiClient;
    private final AptBuildingRepository aptBuildingRepository;
    private final AptTradeHistoryRepository aptTradeHistoryRepository;

    @Value("${app.sync.samgu-complex-name:삼구트리니엔}")
    private String samguComplexName;

    @Value("${app.sync.samgu-building-csv:../data/삼구트리니엔 API/gumi_building_data 건축물 대장 표제부 (옥계 삼구트리니엔).csv}")
    private String samguBuildingCsvPath;

    @Value("${app.sync.samgu-trade-csv:../data/삼구트리니엔 API/gumi_apt_trade_2020_2026 아파트 매매 실거래가 (옥계 삼구 트리니엔).csv}")
    private String samguTradeCsvPath;

    @Value("${app.sync.samgu-csv-charset:MS949}")
    private String samguCsvCharset;

    @Transactional
    public int syncTrades(String lawdCd, String dealYmd) {
        List<TradeApiClient.TradeItem> items = tradeApiClient.fetchTradeItems(lawdCd, dealYmd);
        List<AptBuilding> buildings = aptBuildingRepository.findByComplex_ComplexNameContainingIgnoreCase(samguComplexName);
        Map<String, AptBuilding> buildingByDong = new HashMap<>();
        for (AptBuilding building : buildings) {
            buildingByDong.put(normalizeDongName(building.getDongName()), building);
        }

        int inserted = 0;
        for (TradeApiClient.TradeItem item : items) {
            AptBuilding building = buildingByDong.get(normalizeDongName(item.dongName()));
            if (building == null) {
                continue;
            }
            Integer complexId = building.getComplex().getComplexId();
            if (aptTradeHistoryRepository.existsByComplex_ComplexIdAndDongNameAndFloorAndExclusiveAreaAndDealDateAndDealAmount(
                    complexId, normalizeDongName(item.dongName()), item.floor(), item.exclusiveArea(), item.dealDate(), item.dealAmount())) {
                continue;
            }

            aptTradeHistoryRepository.save(AptTradeHistory.builder()
                    .complex(building.getComplex())
                    .dongName(normalizeDongName(item.dongName()))
                    .floor(item.floor())
                    .exclusiveArea(item.exclusiveArea())
                    .dealAmount(item.dealAmount())
                    .dealDate(item.dealDate())
                    .dealYear(item.dealYear())
                    .dealMonth(item.dealMonth())
                    .tradeType(AptTradeHistory.TradeType.매매)
                    .jibun(item.jibun())
                    .aptName(item.aptName())
                    .build());
            inserted++;
        }
        log.info("Trade sync done. lawdCd={}, dealYmd={}, fetched={}, inserted={}", lawdCd, dealYmd, items.size(), inserted);
        return inserted;
    }

    @Transactional
    public int enrichBuildingFromRegister() {
        List<AptBuilding> buildings = aptBuildingRepository.findByComplex_ComplexNameContainingIgnoreCase(samguComplexName);
        int updated = 0;
        for (AptBuilding building : buildings) {
            String dong = normalizeDongName(building.getDongName());
            List<AptTradeHistory> histories = aptTradeHistoryRepository.findByComplex_ComplexIdOrderByDealDateDesc(building.getComplex().getComplexId());
            AptTradeHistory match = histories.stream()
                    .filter(h -> dong.equals(normalizeDongName(h.getDongName())))
                    .filter(h -> h.getJibun() != null && h.getJibun().contains("-"))
                    .findFirst().orElse(null);
            if (match == null) {
                continue;
            }
            String[] bunJi = splitJibun(match.getJibun());
            if (bunJi == null) {
                continue;
            }
            BuildingRegisterApiClient.BuildingRegisterItem item = buildingRegisterApiClient.fetchTitleInfo(
                    building.getComplex().getSigunguCd().trim(), building.getComplex().getBjdongCd().trim(), bunJi[0], bunJi[1]);
            if (item == null) {
                continue;
            }
            building.setGroundFloors(item.groundFloors());
            building.setUndergroundFloors(item.undergroundFloors());
            building.setBuildingHeight(item.buildingHeight());
            building.setStructureType(item.structureType());
            building.setTotalArea(item.totalArea());
            building.setUseApprovalDate(item.useApprovalDate());
            updated++;
        }
        if (updated > 0) {
            aptBuildingRepository.saveAll(buildings);
        }
        log.info("Building register enrich done. updated={}", updated);
        return updated;
    }

    @Transactional
    public LocalSyncResult syncSamguFromLocalFiles() {
        List<AptBuilding> buildings = aptBuildingRepository.findByComplex_ComplexNameContainingIgnoreCase(samguComplexName);
        if (buildings.isEmpty()) {
            return new LocalSyncResult(0, 0);
        }
        AptComplex complex = buildings.get(0).getComplex();

        int buildingUpdated = updateBuildingsFromCsv(buildings);
        int tradeInserted = insertTradesFromCsv(complex);
        return new LocalSyncResult(buildingUpdated, tradeInserted);
    }

    private int updateBuildingsFromCsv(List<AptBuilding> buildings) {
        Map<String, AptBuilding> buildingByDong = new HashMap<>();
        for (AptBuilding b : buildings) {
            buildingByDong.put(normalizeDongName(b.getDongName()), b);
        }
        Map<String, LedgerRow> bestLedgerByDong = readLedgerRows();
        int updated = 0;
        for (Map.Entry<String, LedgerRow> e : bestLedgerByDong.entrySet()) {
            AptBuilding building = buildingByDong.get(e.getKey());
            if (building == null) {
                continue;
            }
            LedgerRow row = e.getValue();
            building.setGroundFloors(row.groundFloors);
            building.setUndergroundFloors(row.undergroundFloors);
            building.setTotalArea(row.totalArea);
            building.setUseApprovalDate(row.useApprovalDate);
            building.setStructureType(row.structureType);
            updated++;
        }
        if (updated > 0) {
            aptBuildingRepository.saveAll(buildings);
        }
        return updated;
    }

    private int insertTradesFromCsv(AptComplex complex) {
        int inserted = 0;
        for (TradeCsvRow row : readSamguTradeRows()) {
            if (aptTradeHistoryRepository.existsByComplex_ComplexIdAndDongNameAndFloorAndExclusiveAreaAndDealDateAndDealAmount(
                    complex.getComplexId(), row.dongName, row.floor, row.exclusiveArea, row.dealDate, row.dealAmount)) {
                continue;
            }
            aptTradeHistoryRepository.save(AptTradeHistory.builder()
                    .complex(complex)
                    .dongName(row.dongName)
                    .floor(row.floor)
                    .exclusiveArea(row.exclusiveArea)
                    .dealAmount(row.dealAmount)
                    .dealDate(row.dealDate)
                    .dealYear(row.dealDate.getYear())
                    .dealMonth(row.dealDate.getMonthValue())
                    .tradeType(AptTradeHistory.TradeType.매매)
                    .jibun(row.jibun)
                    .aptName(row.aptName)
                    .build());
            inserted++;
        }
        return inserted;
    }

    private Map<String, LedgerRow> readLedgerRows() {
        Map<String, LedgerRow> byDong = new LinkedHashMap<>();
        Path path = Path.of(samguBuildingCsvPath);
        try {
            List<String> lines = decodeLines(path);
            if (lines.isEmpty()) {
                return byDong;
            }
            String header = lines.get(0);
            if (header == null) {
                return byDong;
            }
            Map<String, Integer> idx = headerIndex(splitCsvLine(header));
            for (int li = 1; li < lines.size(); li++) {
                String line = lines.get(li);
                List<String> cols = splitCsvLine(line);
                String bldNm = val(cols, idx, "bldNm");
                if (!bldNm.contains("삼구트리니엔")) {
                    continue;
                }
                String dongNm = normalizeDongName(val(cols, idx, "dongNm"));
                if (dongNm.isBlank() || dongNm.startsWith("P-")) {
                    continue;
                }
                BigDecimal totArea = decimalOrNull(val(cols, idx, "totArea"));
                Integer ground = intOrNull(val(cols, idx, "grndFlrCnt"));
                Integer under = intOrNull(val(cols, idx, "ugrndFlrCnt"));
                LocalDate useApprovalDate = parseYmd8(val(cols, idx, "useAprDay"));
                String structure = val(cols, idx, "strctCdNm");

                LedgerRow curr = new LedgerRow(ground, under, totArea, useApprovalDate, structure);
                LedgerRow prev = byDong.get(dongNm);
                if (prev == null || (curr.totalArea != null && prev.totalArea != null && curr.totalArea.compareTo(prev.totalArea) > 0)) {
                    byDong.put(dongNm, curr);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read building CSV: " + path, e);
        }
        return byDong;
    }

    private List<TradeCsvRow> readSamguTradeRows() {
        List<TradeCsvRow> rows = new ArrayList<>();
        Path path = Path.of(samguTradeCsvPath);
        try {
            List<String> lines = decodeLines(path);
            if (lines.isEmpty()) {
                return rows;
            }
            String header = lines.get(0);
            if (header == null) {
                return rows;
            }
            Map<String, Integer> idx = headerIndex(splitCsvLine(header));
            for (int li = 1; li < lines.size(); li++) {
                String line = lines.get(li);
                List<String> cols = splitCsvLine(line);
                String aptNm = val(cols, idx, "aptNm");
                if (!aptNm.contains("삼구트리니엔")) {
                    continue;
                }
                Integer year = intOrNull(val(cols, idx, "dealYear"));
                Integer month = intOrNull(val(cols, idx, "dealMonth"));
                Integer day = intOrNull(val(cols, idx, "dealDay"));
                if (year == null || month == null || day == null) {
                    continue;
                }
                LocalDate dealDate = LocalDate.of(year, month, day);
                Integer amount = intOrNull(val(cols, idx, "dealAmount").replace(",", ""));
                if (amount == null) {
                    continue;
                }
                BigDecimal area = decimalOrNull(val(cols, idx, "excluUseAr"));
                Integer floor = intOrNull(val(cols, idx, "floor"));
                String dong = normalizeDongName(val(cols, idx, "aptDong"));
                if (dong.isBlank()) {
                    dong = null;
                }
                rows.add(new TradeCsvRow(aptNm, dong, val(cols, idx, "jibun"), dealDate, amount, area, floor));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read trade CSV: " + path, e);
        }
        return rows;
    }

    private Map<String, Integer> headerIndex(List<String> headerCols) {
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < headerCols.size(); i++) {
            idx.put(headerCols.get(i).trim(), i);
        }
        return idx;
    }

    private String val(List<String> cols, Map<String, Integer> idx, String key) {
        Integer i = idx.get(key);
        if (i == null || i < 0 || i >= cols.size()) {
            return "";
        }
        return cols.get(i).trim();
    }

    private List<String> splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString());
        return result;
    }

    private List<String> decodeLines(Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        List<Charset> candidates = List.of(Charset.forName("UTF-8"), Charset.forName(samguCsvCharset), Charset.forName("MS949"));
        for (Charset charset : candidates) {
            try {
                CharBuffer decoded = charset.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes));
                return List.of(decoded.toString().split("\\R"));
            } catch (CharacterCodingException ignored) {
            }
        }
        return Files.readAllLines(path);
    }

    private Integer intOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal decimalOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseYmd8(String s) {
        if (s == null || s.length() != 8) {
            return null;
        }
        return LocalDate.of(
                Integer.parseInt(s.substring(0, 4)),
                Integer.parseInt(s.substring(4, 6)),
                Integer.parseInt(s.substring(6, 8))
        );
    }

    private String[] splitJibun(String jibun) {
        if (jibun == null || !jibun.contains("-")) {
            return null;
        }
        String[] arr = jibun.trim().split("-");
        if (arr.length != 2) {
            return null;
        }
        return new String[]{leftPad4(arr[0]), leftPad4(arr[1])};
    }

    private String leftPad4(String input) {
        String digits = input.trim();
        if (digits.length() >= 4) {
            return digits;
        }
        return "0".repeat(4 - digits.length()) + digits;
    }

    private String normalizeDongName(String dongName) {
        if (dongName == null) {
            return "";
        }
        String trimmed = dongName.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.endsWith("동") ? trimmed : trimmed + "동";
    }

    private record LedgerRow(
            Integer groundFloors,
            Integer undergroundFloors,
            BigDecimal totalArea,
            LocalDate useApprovalDate,
            String structureType
    ) {
    }

    private record TradeCsvRow(
            String aptName,
            String dongName,
            String jibun,
            LocalDate dealDate,
            Integer dealAmount,
            BigDecimal exclusiveArea,
            Integer floor
    ) {
    }

    public record LocalSyncResult(int buildingUpdated, int tradeInserted) {
    }
}
