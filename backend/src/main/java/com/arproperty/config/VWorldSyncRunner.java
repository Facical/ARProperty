package com.arproperty.config;

import com.arproperty.service.DataGoKrSyncService;
import com.arproperty.service.VWorldBuildingSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VWorldSyncRunner implements ApplicationRunner {

    private final VWorldBuildingSyncService vWorldBuildingSyncService;
    private final DataGoKrSyncService dataGoKrSyncService;

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption("sync-vworld-samgu")) {
            int updated = vWorldBuildingSyncService.syncSamguBuildingGeometry();
            log.info("sync-vworld-samgu completed. updated={}", updated);
        }

        if (args.containsOption("sync-trade")) {
            String lawdCd = firstOption(args, "lawdCd", "47190");
            String dealYmd = firstOption(args, "dealYmd", java.time.YearMonth.now().toString().replace("-", ""));
            int inserted = dataGoKrSyncService.syncTrades(lawdCd, dealYmd);
            log.info("sync-trade completed. inserted={}", inserted);
        }

        if (args.containsOption("sync-building-register")) {
            int updated = dataGoKrSyncService.enrichBuildingFromRegister();
            log.info("sync-building-register completed. updated={}", updated);
        }

        if (args.containsOption("sync-local-samgu")) {
            var result = dataGoKrSyncService.syncSamguFromLocalFiles();
            log.info("sync-local-samgu completed. buildingUpdated={}, tradeInserted={}",
                    result.buildingUpdated(), result.tradeInserted());
        }
    }

    private String firstOption(ApplicationArguments args, String key, String fallback) {
        if (!args.containsOption(key)) {
            return fallback;
        }
        return args.getOptionValues(key).stream().findFirst().orElse(fallback);
    }
}
