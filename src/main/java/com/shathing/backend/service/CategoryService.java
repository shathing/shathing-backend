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
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName()))
                .toList();
    }
}
