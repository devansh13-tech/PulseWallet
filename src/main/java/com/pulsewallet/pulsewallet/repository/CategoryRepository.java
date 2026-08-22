package com.pulsewallet.pulsewallet.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulsewallet.pulsewallet.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** System defaults (user IS NULL) plus whatever this user created for themselves. */
    List<Category> findByUserIdOrUserIsNull(Long userId);
}
