package com.arproperty.service;

import com.arproperty.controller.ApiException;
import com.arproperty.dto.TradeDto.TradeItemResponse;
import com.arproperty.repository.AptBuildingRepository;
import com.arproperty.repository.AptTradeHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private AptTradeHistoryRepository tradeHistoryRepository;

    @Mock
    private AptBuildingRepository buildingRepository;

    @InjectMocks
    private TradeService tradeService;

    @Test
    void findBuildingTradesMapsRowsAndClampsPaging() {
        when(buildingRepository.existsById(100)).thenReturn(true);
        when(tradeHistoryRepository.findBuildingTrades(100, "매매", 2026, 100, 0))
                .thenReturn(List.<Object[]>of(tradeRow()));
        when(tradeHistoryRepository.countBuildingTrades(100, "매매", 2026)).thenReturn(1L);

        TradeService.TradeResult result = tradeService.findBuildingTrades(100, " 매매 ", 2026, -2, 999);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(100);
        assertThat(result.totalCount()).isEqualTo(1L);
        assertThat(result.items()).hasSize(1);

        TradeItemResponse item = result.items().get(0);
        assertThat(item.tradeId()).isEqualTo(1234);
        assertThat(item.dongName()).isEqualTo("101");
        assertThat(item.floor()).isEqualTo(15);
        assertThat(item.exclusiveArea()).isEqualTo(84.0);
        assertThat(item.dealAmount()).isEqualTo(58000);
        assertThat(item.dealDate()).isEqualTo("2026-03-15");
        assertThat(item.tradeType()).isEqualTo("매매");
        assertThat(item.dealingType()).isEqualTo("중개거래");
    }

    @Test
    void findBuildingTradesThrowsBuildingNotFound() {
        when(buildingRepository.existsById(404)).thenReturn(false);

        assertThatThrownBy(() -> tradeService.findBuildingTrades(404, null, null, 1, 20))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(e.getCode()).isEqualTo("BUILDING_NOT_FOUND");
                    assertThat(e.getMessage()).isEqualTo("Building not found: 404");
                });
    }

    @Test
    void findBuildingTradesRejectsUnknownType() {
        when(buildingRepository.existsById(100)).thenReturn(true);

        assertThatThrownBy(() -> tradeService.findBuildingTrades(100, "분양권", null, 1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown trade type: 분양권");
    }

    private Object[] tradeRow() {
        return new Object[] {
                1234,
                "101",
                15,
                new BigDecimal("84.0"),
                58000,
                null,
                null,
                LocalDate.of(2026, 3, 15),
                "매매",
                "중개거래"
        };
    }
}
