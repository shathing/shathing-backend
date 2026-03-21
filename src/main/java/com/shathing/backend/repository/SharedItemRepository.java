package com.shathing.backend.repository;

import com.shathing.backend.entity.SharedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SharedItemRepository extends JpaRepository<SharedItem, Long> {

    @Query("select s from SharedItem s")
    Page<SharedItem> findAllForPage(Pageable pageable);

    @Query("""
            select s
            from SharedItem s
            join s.category c
            where c.id = :categoryId
            """)
    Page<SharedItem> findPageByCategoryId(Long categoryId, Pageable pageable);

    @Query("""
            select s
            from SharedItem s
            join s.legalDong l
            where l.code like concat(:legalDongCodePrefix, '%')
            """)
    Page<SharedItem> findPageByLegalDongCodePrefix(String legalDongCodePrefix, Pageable pageable);

    @Query("""
            select s
            from SharedItem s
            join s.category c
            join s.legalDong l
            where c.id = :categoryId
              and l.code like concat(:legalDongCodePrefix, '%')
            """)
    Page<SharedItem> findPageByCategoryIdAndLegalDongCodePrefix(
            Long categoryId,
            String legalDongCodePrefix,
            Pageable pageable
    );

    @Query("""
            select distinct s
            from SharedItem s
            left join fetch s.photoUrls
            join fetch s.category
            join fetch s.legalDong
            join fetch s.member
            where s.id = :id
            """)
    Optional<SharedItem> findByIdWithDetails(Long id);

    @Query("""
            select distinct s
            from SharedItem s
            left join fetch s.photoUrls
            join fetch s.category
            join fetch s.legalDong
            join fetch s.member
            where s.id in :ids
            """)
    List<SharedItem> findAllWithDetailsByIdIn(List<Long> ids);
}
