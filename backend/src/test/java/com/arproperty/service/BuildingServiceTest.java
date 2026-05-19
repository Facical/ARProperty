package com.arproperty.service;

import com.arproperty.controller.ApiException;
import com.arproperty.dto.BuildingDto.BuildingDetailResponse;
import com.arproperty.repository.AptBuildingRepository;
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
class BuildingServiceTest {

    @Mock
    private AptBuildingRepository buildingRepository;

    @InjectMocks
    private BuildingService buildingService;

    @Test
    void findDetailMapsBuildingComplexAndLivabilityFields() {
        when(buildingRepository.findDetail(100)).thenReturn(List.<Object[]>of(detailRow()));

        BuildingDetailResponse detail = buildingService.findDetail(100);

        assertThat(detail.buildingId()).isEqualTo(100);
        assertThat(detail.complexId()).isEqualTo(17);
        assertThat(detail.complexName()).isEqualTo("Okgye Demo Complex");
        assertThat(detail.dongName()).isEqualTo("101");
        assertThat(detail.lat()).isEqualTo(36.13918);
        assertThat(detail.lon()).isEqualTo(128.42137);
        assertThat(detail.groundFloors()).isEqualTo(20);
        assertThat(detail.undergroundFloors()).isEqualTo(1);
        assertThat(detail.highestFloor()).isEqualTo(20);
        assertThat(detail.buildingHeight()).isEqualTo(60.5);
        assertThat(detail.useApprovalDate()).isEqualTo("2015-06-20");
        assertThat(detail.complexInfo().households()).isEqualTo(850);
        assertThat(detail.livability().totalScore()).isEqualTo(82.0);
        assertThat(detail.livability().grade()).isEqualTo("A");
    }

    @Test
    void findDetailThrowsBuildingNotFound() {
        when(buildingRepository.findDetail(404)).thenReturn(List.of());

        assertThatThrownBy(() -> buildingService.findDetail(404))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(e.getCode()).isEqualTo("BUILDING_NOT_FOUND");
                    assertThat(e.getMessage()).isEqualTo("Building not found: 404");
                });
    }

    private Object[] detailRow() {
        return new Object[] {
                100,
                17,
                "Okgye Demo Complex",
                "101",
                36.13918,
                128.42137,
                20,
                1,
                20,
                new BigDecimal("60.5"),
                "RC",
                new BigDecimal("12500.5"),
                LocalDate.of(2015, 6, 20),
                "A12345678",
                850,
                8,
                1200,
                16,
                "district",
                "Demo Constructor",
                new BigDecimal("82.0"),
                "A"
        };
    }
}
