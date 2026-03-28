package com.shathing.backend.service;

import com.shathing.backend.dto.response.CategoryResponse;
import com.shathing.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long categoryId, String countryCode) {
        String normalizedCountryCode = normalizeCountryCode(countryCode);
        return categoryRepository.findById(categoryId)
                .map(category -> new CategoryResponse(category.getId(), category.getDisplayName(normalizedCountryCode)))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(String countryCode) {
        String normalizedCountryCode = normalizeCountryCode(countryCode);
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(category -> new CategoryResponse(category.getId(), category.getDisplayName(normalizedCountryCode)))
                .toList();
    }

    private String normalizeCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return "KR";
        }
        String normalizedCountryCode = countryCode.trim().toUpperCase();
        if (!normalizedCountryCode.equals("KR") && !normalizedCountryCode.equals("US")) {
            throw new IllegalArgumentException("지원하지 않는 국가 코드입니다.");
        }
        return normalizedCountryCode;
    }
}
