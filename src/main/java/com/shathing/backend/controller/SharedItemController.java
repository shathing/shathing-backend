package com.shathing.backend.controller;

import com.shathing.backend.dto.request.CreateSharedItemRequest;
import com.shathing.backend.dto.response.CreateSharedItemResponse;
import com.shathing.backend.service.SharedItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SharedItemController {

    private final SharedItemService sharedItemService;

    @PostMapping("/share/post")
    public ResponseEntity<CreateSharedItemResponse> createSharedItem(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CreateSharedItemRequest request
    ) {
        return ResponseEntity.ok(sharedItemService.createSharedItem(memberId, request));
    }
}
