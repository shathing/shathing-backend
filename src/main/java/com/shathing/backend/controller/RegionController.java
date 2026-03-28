package com.shathing.backend.controller;

import com.shathing.backend.dto.response.RegionResponse;
import com.shathing.backend.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @GetMapping("/region")
    public ResponseEntity<RegionResponse> getRegion(@RequestParam Long regionId) {
        return ResponseEntity.ok(regionService.getRegion(regionId));
    }

    @GetMapping("/regions")
    public ResponseEntity<List<RegionResponse>> getRegions(
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(regionService.getRegions(countryCode, search));
    }
}
