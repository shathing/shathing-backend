package com.shathing.backend.service;

import com.shathing.backend.dto.request.CreateSharedItemRequest;
import com.shathing.backend.dto.response.CreateSharedItemResponse;
import com.shathing.backend.entity.Category;
import com.shathing.backend.entity.LegalDong;
import com.shathing.backend.entity.Member;
import com.shathing.backend.entity.SharedItem;
import com.shathing.backend.repository.CategoryRepository;
import com.shathing.backend.repository.LegalDongRepository;
import com.shathing.backend.repository.MemberRepository;
import com.shathing.backend.repository.SharedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
