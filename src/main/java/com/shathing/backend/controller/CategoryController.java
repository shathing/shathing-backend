package com.shathing.backend.controller;

import com.shathing.backend.dto.response.CategoryResponse;
import com.shathing.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/category")
    public ResponseEntity<CategoryResponse> getCategory(
            @RequestParam Long categoryId,
            @RequestParam(defaultValue = "KR") String countryCode
    ) {
        return ResponseEntity.ok(categoryService.getCategory(categoryId, countryCode));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @RequestParam(defaultValue = "KR") String countryCode
    ) {
        return ResponseEntity.ok(categoryService.getCategories(countryCode));
    }
}
