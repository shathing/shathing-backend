package com.shathing.backend.service;

import com.shathing.backend.dto.request.CreateSharedItemRequest;
import com.shathing.backend.dto.response.CreateSharedItemResponse;
import com.shathing.backend.dto.response.PageResponse;
import com.shathing.backend.dto.response.SharedItemResponse;
import com.shathing.backend.entity.Category;
import com.shathing.backend.entity.LegalDong;
import com.shathing.backend.entity.Member;
import com.shathing.backend.entity.SharedItem;
import com.shathing.backend.repository.CategoryRepository;
import com.shathing.backend.repository.LegalDongRepository;
import com.shathing.backend.repository.MemberRepository;
import com.shathing.backend.repository.SharedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SharedItemService {

    private final SharedItemRepository sharedItemRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final LegalDongRepository legalDongRepository;

    @Transactional
    public CreateSharedItemResponse createSharedItem(Long memberId, CreateSharedItemRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
        LegalDong legalDong = legalDongRepository.findById(request.getLegalDongCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 법정동입니다."));

        SharedItem sharedItem = sharedItemRepository.save(new SharedItem(
                request.getTitle(),
                request.getContent(),
                request.getPhotoUrls(),
                category,
                legalDong,
                member
        ));

        return new CreateSharedItemResponse(sharedItem.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<SharedItemResponse> getSharedItems(Long categoryId, String legalDongCode, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        String normalizedLegalDongCode = normalizeLegalDongCode(legalDongCode);

        PageRequest pageable = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(Sort.Direction.DESC, "createdDate", "id")
        );

        Page<SharedItem> sharedItemPage;
        if (categoryId != null && normalizedLegalDongCode != null) {
            sharedItemPage = sharedItemRepository.findPageByCategoryIdAndLegalDongCodePrefix(
                    categoryId,
                    normalizedLegalDongCode,
                    pageable
            );
        } else if (categoryId != null) {
            sharedItemPage = sharedItemRepository.findPageByCategoryId(categoryId, pageable);
        } else if (normalizedLegalDongCode != null) {
            sharedItemPage = sharedItemRepository.findPageByLegalDongCodePrefix(normalizedLegalDongCode, pageable);
        } else {
            sharedItemPage = sharedItemRepository.findAllForPage(pageable);
        }

        List<Long> ids = sharedItemPage.getContent().stream()
                .map(SharedItem::getId)
                .toList();

        List<SharedItemResponse> content = ids.isEmpty()
                ? List.of()
                : mapInOriginalOrder(sharedItemRepository.findAllWithDetailsByIdIn(ids), ids);

        return new PageResponse<>(
                content,
                sharedItemPage.getNumber(),
                sharedItemPage.getSize(),
                sharedItemPage.getTotalElements(),
                sharedItemPage.getTotalPages(),
                sharedItemPage.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public SharedItemResponse getSharedItem(Long id) {
        SharedItem sharedItem = sharedItemRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공유 물품입니다."));
        return toResponse(sharedItem);
    }

    private SharedItemResponse toResponse(SharedItem sharedItem) {
        return new SharedItemResponse(
                sharedItem.getId(),
                sharedItem.getTitle(),
                sharedItem.getContent(),
                sharedItem.getPhotoUrls(),
                new SharedItemResponse.CategoryInfo(
                        sharedItem.getCategory().getId(),
                        sharedItem.getCategory().getName()
                ),
                new SharedItemResponse.LegalDongInfo(
                        sharedItem.getLegalDong().getCode(),
                        sharedItem.getLegalDong().getSidoName(),
                        sharedItem.getLegalDong().getSigunguName(),
                        sharedItem.getLegalDong().getEupMyeonDongName()
                ),
                new SharedItemResponse.MemberInfo(
                        sharedItem.getMember().getId(),
                        sharedItem.getMember().getUsername()
                ),
                sharedItem.getCreatedDate()
        );
    }

    private String normalizeLegalDongCode(String legalDongCode) {
        if (legalDongCode == null || legalDongCode.isBlank()) {
            return null;
        }
        return legalDongCode.trim();
    }

    private List<SharedItemResponse> mapInOriginalOrder(List<SharedItem> sharedItems, List<Long> ids) {
        Map<Long, SharedItem> sharedItemMap = new LinkedHashMap<>();
        for (SharedItem sharedItem : sharedItems) {
            sharedItemMap.put(sharedItem.getId(), sharedItem);
        }

        return ids.stream()
                .map(sharedItemMap::get)
                .map(this::toResponse)
                .toList();
    }
}
