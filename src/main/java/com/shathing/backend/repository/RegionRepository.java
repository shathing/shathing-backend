package com.shathing.backend.repository;

import com.shathing.backend.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Long> {

    boolean existsByCountryCode(String countryCode);

    List<Region> findAllByParentIsNullOrderByCountryCodeAscNameAsc();

    List<Region> findAllByCountryCodeAndParentIsNullOrderByNameAsc(String countryCode);

    List<Region> findAllByParent_IdIn(Collection<Long> parentIds);

    List<Region> findTop50ByNameContainingIgnoreCaseOrderByCountryCodeAscDepthAscNameAsc(String search);

    List<Region> findTop50ByCountryCodeAndNameContainingIgnoreCaseOrderByDepthAscNameAsc(
            String countryCode,
            String search
    );
}
