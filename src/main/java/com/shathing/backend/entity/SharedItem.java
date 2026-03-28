package com.shathing.backend.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shared_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SharedItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @ElementCollection
    @CollectionTable(name = "shared_item_photo", joinColumns = @JoinColumn(name = "shared_item_id"))
    @Column(name = "photo_url", nullable = false)
    @OrderColumn(name = "display_order")
    private List<String> photoUrls = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public SharedItem(
            String title,
            String content,
            List<String> photoUrls,
            Category category,
            Region region,
            Member member
    ) {
        this.title = title;
        this.content = content;
        this.photoUrls.addAll(photoUrls);
        this.category = category;
        this.region = region;
        this.member = member;
    }
}
