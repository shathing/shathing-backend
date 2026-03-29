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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        Set<String> depth1Names = new LinkedHashSet<>();
        Set<String> depth2Keys = new LinkedHashSet<>();
        Set<String> leafKeys = new LinkedHashSet<>();

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

                depth1Names.add(sidoName);

                if (sigunguName.isEmpty()) {
                    leafKeys.add(sidoName + "|" + eupMyeonDongName);
                    continue;
                }

                String depth2Key = sidoName + "|" + sigunguName;
                depth2Keys.add(depth2Key);
                leafKeys.add(depth2Key + "|" + eupMyeonDongName);
            }
        }

        Map<String, Region> depth1Regions = saveKoreaDepth1Regions(depth1Names);
        Map<String, Region> depth2Regions = saveKoreaDepth2Regions(depth2Keys, depth1Regions);
        saveKoreaLeafRegions(leafKeys, depth1Regions, depth2Regions);

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
        Map<String, String> stateNamesByGeoid = new LinkedHashMap<>();

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

                stateNamesByGeoid.put(geoid, name);
            }
        }

        List<Region> states = stateNamesByGeoid.values().stream()
                .map(name -> new Region(UNITED_STATES_COUNTRY_CODE, null, 1, name))
                .toList();
        List<Region> savedStates = regionRepository.saveAll(states);

        Map<String, Region> stateRegionsByGeoid = new LinkedHashMap<>();
        int index = 0;
        for (String geoid : stateNamesByGeoid.keySet()) {
            stateRegionsByGeoid.put(geoid, savedStates.get(index++));
        }
        return stateRegionsByGeoid;
    }

    private void loadUnitedStatesCounties(ClassPathResource resource, Map<String, Region> stateRegionsByGeoid) throws Exception {
        List<Region> counties = new ArrayList<>();

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

                counties.add(new Region(UNITED_STATES_COUNTRY_CODE, stateRegion, 2, name));
            }
        }

        regionRepository.saveAll(counties);
    }

    private Map<String, Region> saveKoreaDepth1Regions(Set<String> depth1Names) {
        List<Region> savedDepth1Regions = regionRepository.saveAll(
                depth1Names.stream()
                        .map(name -> new Region(KOREA_COUNTRY_CODE, null, 1, name))
                        .toList()
        );

        Map<String, Region> depth1Regions = new LinkedHashMap<>();
        int index = 0;
        for (String depth1Name : depth1Names) {
            depth1Regions.put(depth1Name, savedDepth1Regions.get(index++));
        }
        return depth1Regions;
    }

    private Map<String, Region> saveKoreaDepth2Regions(Set<String> depth2Keys, Map<String, Region> depth1Regions) {
        List<String> orderedDepth2Keys = new ArrayList<>(depth2Keys);
        List<Region> depth2Regions = orderedDepth2Keys.stream()
                .map(key -> {
                    String[] parts = key.split("\\|", 2);
                    Region parent = depth1Regions.get(parts[0]);
                    return new Region(KOREA_COUNTRY_CODE, parent, 2, parts[1]);
                })
                .toList();

        List<Region> savedDepth2Regions = regionRepository.saveAll(depth2Regions);
        Map<String, Region> depth2RegionMap = new LinkedHashMap<>();
        for (int i = 0; i < orderedDepth2Keys.size(); i++) {
            depth2RegionMap.put(orderedDepth2Keys.get(i), savedDepth2Regions.get(i));
        }
        return depth2RegionMap;
    }

    private void saveKoreaLeafRegions(
            Set<String> leafKeys,
            Map<String, Region> depth1Regions,
            Map<String, Region> depth2Regions
    ) {
        List<Region> leafRegions = leafKeys.stream()
                .map(key -> toKoreaLeafRegion(key, depth1Regions, depth2Regions))
                .toList();

        regionRepository.saveAll(leafRegions);
    }

    private Region toKoreaLeafRegion(
            String key,
            Map<String, Region> depth1Regions,
            Map<String, Region> depth2Regions
    ) {
        String[] parts = key.split("\\|");
        if (parts.length == 2) {
            Region parent = depth1Regions.get(parts[0]);
            return new Region(KOREA_COUNTRY_CODE, parent, 2, parts[1]);
        }
        if (parts.length == 3) {
            Region parent = depth2Regions.get(parts[0] + "|" + parts[1]);
            return new Region(KOREA_COUNTRY_CODE, parent, 3, parts[2]);
        }
        throw new IllegalStateException("Unexpected Korea leaf key: " + key);
    }
}
