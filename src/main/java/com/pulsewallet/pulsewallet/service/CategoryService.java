package com.pulsewallet.pulsewallet.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.CategoryRequest;
import com.pulsewallet.pulsewallet.dto.CategoryResponse;
import com.pulsewallet.pulsewallet.entity.Category;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.DuplicateResourceException;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.CategoryRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;

/**
 * CRUD over {@link Category}, scoped so a user only ever edits or deletes
 * categories they created. System defaults (seeded with {@code user IS NULL})
 * are visible to everyone but owned by no one - {@link #requireOwned} treats
 * "belongs to someone else" and "is a system default" the same way: 404, not
 * 403, per the convention documented on {@link ResourceNotFoundException}.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listForUser(Long userId) {
        return categoryRepository.findByUserIdOrUserIsNull(userId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getVisibleToUser(Long id, Long userId) {
        Category category = categoryRepository.findById(id)
                .filter(c -> c.getUser() == null || c.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse create(Long userId, CategoryRequest request) {
        User user = userRepository.getReferenceById(userId);
        Category category = new Category(request.name().trim(), request.type(), user);
        try {
            category = categoryRepository.save(category);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("You already have a category named '" + request.name() + "'");
        }
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse update(Long id, Long userId, CategoryRequest request) {
        Category category = requireOwned(id, userId);
        category.setName(request.name().trim());
        category.setType(request.type());
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        categoryRepository.delete(requireOwned(id, userId));
    }

    private Category requireOwned(Long id, Long userId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        if (category.getUser() == null || !category.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Category", id);
        }
        return category;
    }
}
