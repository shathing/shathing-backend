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
import org.springframework.jdbc.core.JdbcTemplate;
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
    private static final String INSERT_REGION_SQL = """
            insert into regions (country_code, parent_id, depth, name)
            values (?, ?, ?, ?)
            """;

    private final RegionRepository regionRepository;
    private final JdbcTemplate jdbcTemplate;

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

        batchInsertRegions(
                stateNamesByGeoid.values().stream()
                        .map(name -> new RegionInsertRow(UNITED_STATES_COUNTRY_CODE, null, 1, name))
                        .toList()
        );

        Map<String, Region> statesByName = regionRepository
                .findAllByCountryCodeAndDepthAndParentIsNullOrderByNameAsc(UNITED_STATES_COUNTRY_CODE, 1)
                .stream()
                .collect(LinkedHashMap::new, (map, region) -> map.put(region.getName(), region), Map::putAll);

        Map<String, Region> stateRegionsByGeoid = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : stateNamesByGeoid.entrySet()) {
            stateRegionsByGeoid.put(entry.getKey(), statesByName.get(entry.getValue()));
        }
        return stateRegionsByGeoid;
    }

    private void loadUnitedStatesCounties(ClassPathResource resource, Map<String, Region> stateRegionsByGeoid) throws Exception {
        List<RegionInsertRow> counties = new ArrayList<>();

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

                counties.add(new RegionInsertRow(UNITED_STATES_COUNTRY_CODE, stateRegion.getId(), 2, name));
            }
        }

        batchInsertRegions(counties);
    }

    private Map<String, Region> saveKoreaDepth1Regions(Set<String> depth1Names) {
        batchInsertRegions(
                depth1Names.stream()
                        .map(name -> new RegionInsertRow(KOREA_COUNTRY_CODE, null, 1, name))
                        .toList()
        );

        return regionRepository.findAllByCountryCodeAndDepthAndParentIsNullOrderByNameAsc(KOREA_COUNTRY_CODE, 1)
                .stream()
                .collect(LinkedHashMap::new, (map, region) -> map.put(region.getName(), region), Map::putAll);
    }

    private Map<String, Region> saveKoreaDepth2Regions(Set<String> depth2Keys, Map<String, Region> depth1Regions) {
        List<String> orderedDepth2Keys = new ArrayList<>(depth2Keys);
        batchInsertRegions(
                orderedDepth2Keys.stream()
                        .map(key -> {
                            String[] parts = key.split("\\|", 2);
                            Region parent = depth1Regions.get(parts[0]);
                            return new RegionInsertRow(KOREA_COUNTRY_CODE, parent.getId(), 2, parts[1]);
                        })
                        .toList()
        );

        Map<Long, String> depth1NamesById = depth1Regions.values().stream()
                .collect(LinkedHashMap::new, (map, region) -> map.put(region.getId(), region.getName()), Map::putAll);
        List<Region> savedDepth2Regions = regionRepository.findAllByCountryCodeAndDepthAndParent_IdInOrderByNameAsc(
                KOREA_COUNTRY_CODE,
                2,
                depth1NamesById.keySet()
        );
        Map<String, Region> depth2RegionMap = new LinkedHashMap<>();
        for (Region region : savedDepth2Regions) {
            String parentName = depth1NamesById.get(region.getParent().getId());
            String key = parentName + "|" + region.getName();
            if (depth2Keys.contains(key)) {
                depth2RegionMap.put(key, region);
            }
        }
        return depth2RegionMap;
    }

    private void saveKoreaLeafRegions(
            Set<String> leafKeys,
            Map<String, Region> depth1Regions,
            Map<String, Region> depth2Regions
    ) {
        List<RegionInsertRow> leafRegions = leafKeys.stream()
                .map(key -> toKoreaLeafRegion(key, depth1Regions, depth2Regions))
                .toList();

        batchInsertRegions(leafRegions);
    }

    private RegionInsertRow toKoreaLeafRegion(
            String key,
            Map<String, Region> depth1Regions,
            Map<String, Region> depth2Regions
    ) {
        String[] parts = key.split("\\|");
        if (parts.length == 2) {
            Region parent = depth1Regions.get(parts[0]);
            return new RegionInsertRow(KOREA_COUNTRY_CODE, parent.getId(), 2, parts[1]);
        }
        if (parts.length == 3) {
            Region parent = depth2Regions.get(parts[0] + "|" + parts[1]);
            return new RegionInsertRow(KOREA_COUNTRY_CODE, parent.getId(), 3, parts[2]);
        }
        throw new IllegalStateException("Unexpected Korea leaf key: " + key);
    }

    private void batchInsertRegions(List<RegionInsertRow> rows) {
        jdbcTemplate.batchUpdate(
                INSERT_REGION_SQL,
                rows,
                1000,
                (ps, row) -> {
                    ps.setString(1, row.countryCode());
                    if (row.parentId() == null) {
                        ps.setNull(2, java.sql.Types.BIGINT);
                    } else {
                        ps.setLong(2, row.parentId());
                    }
                    ps.setInt(3, row.depth());
                    ps.setString(4, row.name());
                }
        );
    }

    private record RegionInsertRow(String countryCode, Long parentId, int depth, String name) {
    }
}
