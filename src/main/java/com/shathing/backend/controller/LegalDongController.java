package com.shathing.backend.controller;

import com.shathing.backend.dto.response.LegalDongItemResponse;
import com.shathing.backend.service.LegalDongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LegalDongController {

    private final LegalDongService legalDongService;

    @GetMapping("/legal-dongs")
    public ResponseEntity<List<LegalDongItemResponse>> getLegalDongs(
            @RequestParam(required = false) String code
    ) {
        return ResponseEntity.ok(legalDongService.getLegalDongs(code));
    }
}
