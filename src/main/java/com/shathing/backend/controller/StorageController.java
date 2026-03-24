package com.shathing.backend.controller;

import com.shathing.backend.dto.request.CreatePresignedUploadUrlRequest;
import com.shathing.backend.dto.response.PresignedUploadUrlResponse;
import com.shathing.backend.service.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/uploads/presigned-url")
    public ResponseEntity<PresignedUploadUrlResponse> createPresignedUploadUrl(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CreatePresignedUploadUrlRequest request
    ) {
        return ResponseEntity.ok(storageService.createPresignedUploadUrl(memberId, request));
    }
}
