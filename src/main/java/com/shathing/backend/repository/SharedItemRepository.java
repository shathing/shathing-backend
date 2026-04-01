package com.shathing.backend.repository;

import com.shathing.backend.entity.SharedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SharedItemRepository extends JpaRepository<SharedItem, Long>, JpaSpecificationExecutor<SharedItem> {

    Page<SharedItem> findAllByCategory_Id(Long categoryId, Pageable pageable);

    Page<SharedItem> findAllByRegion_IdIn(Collection<Long> regionIds, Pageable pageable);

    Page<SharedItem> findAllByCategory_IdAndRegion_IdIn(Long categoryId, Collection<Long> regionIds, Pageable pageable);

    @Query("""
            select distinct s
            from SharedItem s
            left join fetch s.photoUrls
            join fetch s.category
            join fetch s.region
            join fetch s.member
            where s.id = :id
            """)
    Optional<SharedItem> findByIdWithDetails(Long id);

    @Query("""
            select distinct s
            from SharedItem s
            left join fetch s.photoUrls
            join fetch s.category
            join fetch s.region
            join fetch s.member
            where s.id in :ids
            """)
    List<SharedItem> findAllWithDetailsByIdIn(List<Long> ids);
}
