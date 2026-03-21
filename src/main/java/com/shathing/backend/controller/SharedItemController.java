package com.shathing.backend.controller;

import com.shathing.backend.dto.request.CreateSharedItemRequest;
import com.shathing.backend.dto.response.CreateSharedItemResponse;
import com.shathing.backend.dto.response.PageResponse;
import com.shathing.backend.dto.response.SharedItemResponse;
import com.shathing.backend.service.SharedItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/share/posts")
    public ResponseEntity<PageResponse<SharedItemResponse>> getSharedItems(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String legalDongCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(sharedItemService.getSharedItems(categoryId, legalDongCode, page, size));
    }

    @GetMapping("/share/posts/{id}")
    public ResponseEntity<SharedItemResponse> getSharedItem(@PathVariable Long id) {
        return ResponseEntity.ok(sharedItemService.getSharedItem(id));
    }
}
