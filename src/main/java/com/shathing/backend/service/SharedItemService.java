package com.shathing.backend.service;

import com.shathing.backend.dto.request.CreateSharedItemRequest;
import com.shathing.backend.dto.response.CreateSharedItemResponse;
import com.shathing.backend.dto.response.PageResponse;
import com.shathing.backend.dto.response.SharedItemResponse;
import com.shathing.backend.entity.Category;
import com.shathing.backend.entity.Member;
import com.shathing.backend.entity.Region;
import com.shathing.backend.entity.SharedItem;
import com.shathing.backend.repository.CategoryRepository;
import com.shathing.backend.repository.MemberRepository;
import com.shathing.backend.repository.RegionRepository;
import com.shathing.backend.repository.SharedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SharedItemService {

    private final SharedItemRepository sharedItemRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final RegionRepository regionRepository;

    @Transactional
    public CreateSharedItemResponse createSharedItem(Long memberId, CreateSharedItemRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
        Region region = regionRepository.findById(request.getRegionId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역입니다."));

        SharedItem sharedItem = sharedItemRepository.save(new SharedItem(
                request.getTitle(),
                request.getContent(),
                request.getPhotoUrls(),
                category,
                region,
                member
        ));

        return new CreateSharedItemResponse(sharedItem.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<SharedItemResponse> getSharedItems(Long categoryId, Long regionId, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        List<Long> regionIds = resolveRegionIds(regionId);

        PageRequest pageable = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(Sort.Direction.DESC, "createdDate", "id")
        );

        Page<SharedItem> sharedItemPage;
        if (categoryId != null && regionIds != null) {
            sharedItemPage = sharedItemRepository.findAllByCategory_IdAndRegion_IdIn(categoryId, regionIds, pageable);
        } else if (categoryId != null) {
            sharedItemPage = sharedItemRepository.findAllByCategory_Id(categoryId, pageable);
        } else if (regionIds != null) {
            sharedItemPage = sharedItemRepository.findAllByRegion_IdIn(regionIds, pageable);
        } else {
            sharedItemPage = sharedItemRepository.findAll(pageable);
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
                new SharedItemResponse.RegionInfo(
                        sharedItem.getRegion().getId(),
                        sharedItem.getRegion().getCountryCode(),
                        sharedItem.getRegion().getDepth(),
                        sharedItem.getRegion().getName(),
                        buildRegionFullName(sharedItem.getRegion())
                ),
                new SharedItemResponse.MemberInfo(
                        sharedItem.getMember().getId(),
                        sharedItem.getMember().getUsername()
                ),
                sharedItem.getCreatedDate()
        );
    }

    private List<Long> resolveRegionIds(Long regionId) {
        if (regionId == null) {
            return null;
        }

        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역입니다."));

        List<Long> regionIds = new ArrayList<>();
        List<Long> frontier = List.of(region.getId());

        while (!frontier.isEmpty()) {
            regionIds.addAll(frontier);
            frontier = regionRepository.findAllByParent_IdIn(frontier).stream()
                    .map(Region::getId)
                    .toList();
        }

        return regionIds;
    }

    private String buildRegionFullName(Region region) {
        Region parent = region.getParent();
        if (parent == null) {
            return region.getName();
        }
        return buildRegionFullName(parent) + " / " + region.getName();
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
