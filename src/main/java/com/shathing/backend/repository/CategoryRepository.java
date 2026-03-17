package com.shathing.backend.repository;

import com.shathing.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderByDisplayOrderAsc();
}
