package com.shathing.backend.service;

import com.shathing.backend.dto.response.RegionResponse;
import com.shathing.backend.entity.Region;
import com.shathing.backend.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;

    @Transactional(readOnly = true)
    public RegionResponse getRegion(Long regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역입니다."));
        return toResponse(region);
    }

    @Transactional(readOnly = true)
    public List<RegionResponse> getRegions(String countryCode, String search) {
        String normalizedCountryCode = normalizeCountryCode(countryCode);
        String normalizedSearch = normalizeSearch(search);

        List<Region> regions;
        if (normalizedSearch != null) {
            regions = findRegionsBySearch(normalizedCountryCode, normalizedSearch);
        } else if (normalizedCountryCode != null) {
            regions = regionRepository.findAllByCountryCodeAndParentIsNullOrderByNameAsc(normalizedCountryCode);
        } else {
            regions = regionRepository.findAllByParentIsNullOrderByCountryCodeAscNameAsc();
        }

        return regions.stream()
                .map(this::toResponse)
                .toList();
    }

    private List<Region> findRegionsBySearch(String countryCode, String search) {
        if (countryCode != null) {
            return regionRepository.findTop50ByCountryCodeAndNameContainingIgnoreCaseOrderByDepthAscNameAsc(
                    countryCode,
                    search
            );
        }
        return regionRepository.findTop50ByNameContainingIgnoreCaseOrderByCountryCodeAscDepthAscNameAsc(search);
    }

    private RegionResponse toResponse(Region region) {
        return new RegionResponse(
                region.getId(),
                region.getCountryCode(),
                region.getDepth(),
                region.getName(),
                buildFullName(region)
        );
    }

    private String buildFullName(Region region) {
        Region parent = region.getParent();
        if (parent == null) {
            return region.getName();
        }
        return buildFullName(parent) + " / " + region.getName();
    }

    private String normalizeCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return null;
        }
        return countryCode.trim().toUpperCase();
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search.trim();
    }
}
