package com.arproperty.service;

import com.arproperty.repository.LivingInfraRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LivabilityServiceTest {

    @Mock
    private LivingInfraRepository livingInfraRepository;

    @InjectMocks
    private LivabilityService livabilityService;

    @Test
    void findNearbyInfraClampsRadiusAndPaging() {
        when(livingInfraRepository.findNearby(36.139, 128.421, 3000, null, 100, 0))
                .thenReturn(List.of(projection()));
        when(livingInfraRepository.countNearby(36.139, 128.421, 3000, null))
                .thenReturn(150L);

        LivabilityService.NearbyResult result =
                livabilityService.findNearbyInfra(36.139, 128.421, 99999, null, -2, 999);

        assertThat(result.radiusMeters()).isEqualTo(3000);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(100);
        assertThat(result.totalCount()).isEqualTo(150);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getInfraId()).isEqualTo(1);
    }

    @Test
    void findNearbyInfraNormalizesCategoryAndOffsetsPage() {
        when(livingInfraRepository.findNearby(36.139, 128.421, 500, "medical", 20, 40))
                .thenReturn(List.of());
        when(livingInfraRepository.countNearby(36.139, 128.421, 500, "medical"))
                .thenReturn(0L);

        livabilityService.findNearbyInfra(36.139, 128.421, 500, " MEDICAL ", 3, 20);

        verify(livingInfraRepository).findNearby(36.139, 128.421, 500, "medical", 20, 40);
        verify(livingInfraRepository).countNearby(36.139, 128.421, 500, "medical");
    }

    @Test
    void findNearbyInfraCapsHugeOffset() {
        when(livingInfraRepository.findNearby(36.139, 128.421, 500, null, 100, Integer.MAX_VALUE))
                .thenReturn(List.of());
        when(livingInfraRepository.countNearby(36.139, 128.421, 500, null))
                .thenReturn(0L);

        LivabilityService.NearbyResult result =
                livabilityService.findNearbyInfra(36.139, 128.421, 500, null, Integer.MAX_VALUE, 100);

        assertThat(result.page()).isEqualTo(Integer.MAX_VALUE);
        verify(livingInfraRepository).findNearby(36.139, 128.421, 500, null, 100, Integer.MAX_VALUE);
    }

    @Test
    void findNearbyInfraRejectsUnknownCategory() {
        assertThatThrownBy(() ->
                livabilityService.findNearbyInfra(36.139, 128.421, 500, "invalid", 1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown category: invalid");

        verifyNoInteractions(livingInfraRepository);
    }

    private LivingInfraRepository.InfraNearbyProjection projection() {
        return new LivingInfraRepository.InfraNearbyProjection() {
            @Override
            public Integer getInfraId() {
                return 1;
            }

            @Override
            public String getCategory() {
                return "medical";
            }

            @Override
            public String getSubCategory() {
                return "pharmacy";
            }

            @Override
            public String getName() {
                return "Test Pharmacy";
            }

            @Override
            public Double getLat() {
                return 36.139;
            }

            @Override
            public Double getLon() {
                return 128.421;
            }

            @Override
            public String getAddress() {
                return "Okgye-dong, Gumi";
            }

            @Override
            public Double getDistanceMeters() {
                return 10.0;
            }
        };
    }
}
