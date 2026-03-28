package com.shathing.backend.config;

import com.shathing.backend.entity.Region;
import com.shathing.backend.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.region", name = "initialize-on-startup", havingValue = "true")
public class RegionCsvLoader implements ApplicationRunner {

    private static final String KOREA_COUNTRY_CODE = "KR";
    private static final String UNITED_STATES_COUNTRY_CODE = "US";

    private final RegionRepository regionRepository;

    @Value("${app.region.korea.csv-path:}")
    private String koreaCsvPath;

    @Value("${app.region.us.states-path:}")
    private String usStatesPath;

    @Value("${app.region.us.counties-path:}")
    private String usCountiesPath;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        loadKoreaRegionsIfNecessary();
        loadUnitedStatesRegionsIfNecessary();
    }

    private void loadKoreaRegionsIfNecessary() throws Exception {
        if (regionRepository.existsByCountryCode(KOREA_COUNTRY_CODE)) {
            log.info("Skip Korea region load because KR regions already exist.");
            return;
        }

        if (koreaCsvPath == null || koreaCsvPath.isBlank()) {
            log.info("Skip Korea region load because app.region.korea.csv-path is empty.");
            return;
        }

        ClassPathResource resource = new ClassPathResource(koreaCsvPath);
        if (!resource.exists()) {
            log.warn("Skip Korea region load because resource does not exist: {}", koreaCsvPath);
            return;
        }

        Map<String, Region> depth1Regions = new LinkedHashMap<>();
        Map<String, Region> depth2Regions = new LinkedHashMap<>();
        Map<String, Region> leafRegions = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean header = true;

            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }

                String[] columns = line.split(",", -1);
                if (columns.length < 8) {
                    throw new IllegalStateException("Unexpected CSV format: " + line);
                }

                String sidoName = columns[1].trim();
                String sigunguName = columns[2].trim();
                String eupMyeonDongName = columns[3].trim();
                String riName = columns[4].trim();
                String deletedAt = columns[7].trim();

                if (eupMyeonDongName.isEmpty() || !riName.isEmpty() || !deletedAt.isEmpty()) {
                    continue;
                }

                Region depth1Region = depth1Regions.computeIfAbsent(
                        sidoName,
                        key -> regionRepository.save(new Region(KOREA_COUNTRY_CODE, null, 1, key))
                );

                if (sigunguName.isEmpty()) {
                    String leafKey = sidoName + "|" + eupMyeonDongName;
                    leafRegions.computeIfAbsent(
                            leafKey,
                            key -> regionRepository.save(new Region(KOREA_COUNTRY_CODE, depth1Region, 2, eupMyeonDongName))
                    );
                    continue;
                }

                String depth2Key = sidoName + "|" + sigunguName;
                Region depth2Region = depth2Regions.computeIfAbsent(
                        depth2Key,
                        key -> regionRepository.save(new Region(KOREA_COUNTRY_CODE, depth1Region, 2, sigunguName))
                );

                String leafKey = depth2Key + "|" + eupMyeonDongName;
                leafRegions.computeIfAbsent(
                        leafKey,
                        key -> regionRepository.save(new Region(KOREA_COUNTRY_CODE, depth2Region, 3, eupMyeonDongName))
                );
            }
        }

        log.info("Loaded Korea regions from {}.", koreaCsvPath);
    }

    private void loadUnitedStatesRegionsIfNecessary() throws Exception {
        if (regionRepository.existsByCountryCode(UNITED_STATES_COUNTRY_CODE)) {
            log.info("Skip United States region load because US regions already exist.");
            return;
        }

        if (usStatesPath == null || usStatesPath.isBlank() || usCountiesPath == null || usCountiesPath.isBlank()) {
            log.info("Skip United States region load because state/county paths are empty.");
            return;
        }

        ClassPathResource stateResource = new ClassPathResource(usStatesPath);
        ClassPathResource countyResource = new ClassPathResource(usCountiesPath);
        if (!stateResource.exists() || !countyResource.exists()) {
            log.warn(
                    "Skip United States region load because resources do not exist. states={}, counties={}",
                    usStatesPath,
                    usCountiesPath
            );
            return;
        }

        Map<String, Region> stateRegionsByGeoid = loadUnitedStatesStates(stateResource);
        loadUnitedStatesCounties(countyResource, stateRegionsByGeoid);

        log.info("Loaded United States regions from {} and {}.", usStatesPath, usCountiesPath);
    }

    private Map<String, Region> loadUnitedStatesStates(ClassPathResource resource) throws Exception {
        Map<String, Region> stateRegionsByGeoid = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean header = true;

            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }

                String[] columns = line.split("\\|", -1);
                if (columns.length < 4) {
                    throw new IllegalStateException("Unexpected US state file format: " + line);
                }

                String geoid = columns[1].trim();
                String name = columns[3].trim();
                if (geoid.isEmpty() || name.isEmpty()) {
                    continue;
                }

                stateRegionsByGeoid.put(
                        geoid,
                        regionRepository.save(new Region(UNITED_STATES_COUNTRY_CODE, null, 1, name))
                );
            }
        }

        return stateRegionsByGeoid;
    }

    private void loadUnitedStatesCounties(ClassPathResource resource, Map<String, Region> stateRegionsByGeoid) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean header = true;

            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }

                String[] columns = line.split("\\|", -1);
                if (columns.length < 5) {
                    throw new IllegalStateException("Unexpected US county file format: " + line);
                }

                String countyGeoid = columns[1].trim();
                String name = columns[4].trim();
                if (countyGeoid.length() < 2 || name.isEmpty()) {
                    continue;
                }

                String stateGeoid = countyGeoid.substring(0, 2);
                Region stateRegion = stateRegionsByGeoid.get(stateGeoid);
                if (stateRegion == null) {
                    throw new IllegalStateException("Missing parent state for county GEOID: " + countyGeoid);
                }

                regionRepository.save(new Region(UNITED_STATES_COUNTRY_CODE, stateRegion, 2, name));
            }
        }
    }
}
