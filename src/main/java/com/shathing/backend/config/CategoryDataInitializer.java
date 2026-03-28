package com.shathing.backend.config;

import com.shathing.backend.entity.Category;
import com.shathing.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.category", name = "initialize-on-startup", havingValue = "true")
public class CategoryDataInitializer implements ApplicationRunner {

    private static final List<CategorySeed> CATEGORY_SEEDS = List.of(
            new CategorySeed("디지털기기", "Electronics", 1),
            new CategorySeed("생활가전", "Appliances", 2),
            new CategorySeed("가구/인테리어", "Furniture", 3),
            new CategorySeed("공구", "Tools", 4),
            new CategorySeed("생활/주방", "Home", 5),
            new CategorySeed("유아동", "Kids", 6),
            new CategorySeed("여성의류", "Women's Clothing", 7),
            new CategorySeed("여성잡화", "Women's Acc.", 8),
            new CategorySeed("남성의류", "Men's Wear", 9),
            new CategorySeed("남성잡화", "Men's Acc.", 10),
            new CategorySeed("뷰티/미용", "Beauty", 11),
            new CategorySeed("스포츠/레저", "Sports", 12),
            new CategorySeed("취미/게임/음반", "Hobbies", 13),
            new CategorySeed("도서", "Books", 14),
            new CategorySeed("여행/이동장비", "Travel", 15),
            new CategorySeed("반려동물용품", "Pet Supplies", 16),
            new CategorySeed("식물", "Plants", 17),
            new CategorySeed("기타 공유물품", "Other", 18),
            new CategorySeed("빌려주세요", "Looking to Borrow", 19)
    );

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<Integer, Category> categoryByDisplayOrder = new LinkedHashMap<>();
        for (Category category : categoryRepository.findAllByOrderByDisplayOrderAsc()) {
            categoryByDisplayOrder.put(category.getDisplayOrder(), category);
        }

        int createdCount = 0;
        int updatedCount = 0;
        for (CategorySeed seed : CATEGORY_SEEDS) {
            Category existingCategory = categoryByDisplayOrder.get(seed.displayOrder());
            if (existingCategory == null) {
                categoryRepository.save(new Category(seed.nameKr(), seed.nameUs(), seed.displayOrder()));
                createdCount++;
                continue;
            }

            existingCategory.updateNames(seed.nameKr(), seed.nameUs());
            updatedCount++;
        }

        log.info("Category initialization completed. created={}, updated={}", createdCount, updatedCount);
    }

    private record CategorySeed(String nameKr, String nameUs, int displayOrder) {
    }
}
