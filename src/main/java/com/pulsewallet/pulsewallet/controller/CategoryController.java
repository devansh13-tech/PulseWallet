package com.pulsewallet.pulsewallet.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.dto.CategoryRequest;
import com.pulsewallet.pulsewallet.dto.CategoryResponse;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.CategoryService;

import jakarta.validation.Valid;

/**
 * All endpoints require a valid JWT (see {@code SecurityConfig}). The
 * authenticated user's id comes only from {@code @AuthenticationPrincipal} -
 * never from a path or body parameter - so a caller cannot ask to act as
 * another user.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** System defaults plus this user's own categories. */
    @GetMapping
    public ApiResponse<List<CategoryResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(categoryService.listForUser(principal.getId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> get(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(categoryService.getVisibleToUser(id, principal.getId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> create(
            @Valid @RequestBody CategoryRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Category created", categoryService.create(principal.getId(), request));
    }

    /** System default categories cannot be edited - only categories this user created. */
    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Category updated", categoryService.update(id, principal.getId(), request));
    }

    /** System default categories cannot be deleted - only categories this user created. */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        categoryService.delete(id, principal.getId());
        return ApiResponse.message("Category deleted");
    }
}
