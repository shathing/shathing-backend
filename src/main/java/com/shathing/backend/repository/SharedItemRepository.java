package com.shathing.backend.repository;

import com.shathing.backend.entity.SharedItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedItemRepository extends JpaRepository<SharedItem, Long> {
}
