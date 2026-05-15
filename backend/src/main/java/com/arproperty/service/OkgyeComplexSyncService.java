package com.arproperty.service;

import com.arproperty.entity.AptComplex;
import com.arproperty.external.datagokr.AptListApiClient;
import com.arproperty.external.kakao.LocalApiClient;
import com.arproperty.repository.AptComplexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OkgyeComplexSyncService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private static final String DEFAULT_BJD_FULL = "4719012800";
    private static final String DEFAULT_SIGUNGU = "47190";
    private static final String DEFAULT_BJDONG = "12800";

    private final AptListApiClient aptListApiClient;
    private final LocalApiClient localApiClient;
    private final AptComplexRepository aptComplexRepository;

    @Value("${app.sync.okgye-bjd-code:" + DEFAULT_BJD_FULL + "}")
    private String okgyeBjdCode;

    @Transactional
    public int syncOkgyeComplexCoordinates() {
        List<AptListApiClient.AptListItem> items = aptListApiClient.fetchByLegalDong(okgyeBjdCode);
        log.info("sync-okgye fetched apt_list count={}", items.size());

        int updated = 0;
        for (AptListApiClient.AptListItem item : items) {
            if (!StringUtils.hasText(item.kaptCode()) || !StringUtils.hasText(item.kaptName())) {
                continue;
            }

            Optional<LocalApiClient.KakaoCoord> coord = lookupCoord(item);
            if (coord.isEmpty()) {
                log.warn("sync-okgye 좌표 미발견: kaptCode={}, kaptName={}", item.kaptCode(), item.kaptName());
                continue;
            }

            Point centroid = GEOMETRY_FACTORY.createPoint(new Coordinate(coord.get().lon(), coord.get().lat()));

            AptComplex complex = aptComplexRepository.findByKaptCode(item.kaptCode())
                    .orElseGet(AptComplex::new);

            complex.setKaptCode(item.kaptCode());
            complex.setComplexName(item.kaptName());
            complex.setLegalDongCode(okgyeBjdCode);
            complex.setSigunguCd(DEFAULT_SIGUNGU);
            complex.setBjdongCd(DEFAULT_BJDONG);
            if (StringUtils.hasText(item.roadAddress())) {
                complex.setRoadAddress(item.roadAddress());
            }
            if (StringUtils.hasText(item.parcelAddress())) {
                complex.setParcelAddress(item.parcelAddress());
            }
            complex.setCentroid(centroid);

            aptComplexRepository.save(complex);
            updated++;
        }

        log.info("sync-okgye done. fetched={}, updated={}", items.size(), updated);
        return updated;
    }

    private Optional<LocalApiClient.KakaoCoord> lookupCoord(AptListApiClient.AptListItem item) {
        String primaryQuery = buildPrimaryQuery(item);
        Optional<LocalApiClient.KakaoCoord> coord = localApiClient.searchKeyword(primaryQuery);
        if (coord.isPresent()) {
            return coord;
        }
        return localApiClient.searchKeyword(item.kaptName() + " 옥계동");
    }

    private String buildPrimaryQuery(AptListApiClient.AptListItem item) {
        if (StringUtils.hasText(item.roadAddress())) {
            return item.kaptName() + " " + item.roadAddress();
        }
        if (StringUtils.hasText(item.parcelAddress())) {
            return item.kaptName() + " " + item.parcelAddress();
        }
        return item.kaptName();
    }
}
