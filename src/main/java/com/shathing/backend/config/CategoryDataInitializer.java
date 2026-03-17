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

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.category", name = "initialize-on-startup", havingValue = "true")
public class CategoryDataInitializer implements ApplicationRunner {

    private static final List<String> CATEGORY_NAMES = List.of(
            "디지털기기",
            "생활가전",
            "가구/인테리어",
            "공구",
            "생활/주방",
            "유아동",
            "여성의류",
            "여성잡화",
            "남성의류",
            "남성잡화",
            "뷰티/미용",
            "스포츠/레저",
            "취미/게임/음반",
            "도서",
            "여행/이동장비",
            "반려동물용품",
            "식물",
            "기타 공유물품",
            "빌려주세요"
    );

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            log.info("Skip category initialization because table already contains data.");
            return;
        }

        List<Category> categories = IntStream.range(0, CATEGORY_NAMES.size())
                .mapToObj(index -> new Category(CATEGORY_NAMES.get(index), index + 1))
                .toList();

        categoryRepository.saveAll(categories);
        log.info("Initialized {} categories.", categories.size());
    }
}
