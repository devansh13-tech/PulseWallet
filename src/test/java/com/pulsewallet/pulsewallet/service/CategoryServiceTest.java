package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.pulsewallet.pulsewallet.dto.CategoryRequest;
import com.pulsewallet.pulsewallet.dto.CategoryResponse;
import com.pulsewallet.pulsewallet.entity.Category;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.DuplicateResourceException;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.CategoryRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;
import com.pulsewallet.pulsewallet.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    private CategoryService categoryService;
    private User owner;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository, userRepository);
        owner = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), OWNER_ID);
    }

    @Test
    void listForUser_includesSystemDefaultsAndTheUsersOwnCategories() {
        Category systemDefault = TestEntities.withId(new Category("Groceries", TransactionType.EXPENSE, null), 1L);
        Category own = TestEntities.withId(new Category("Side Hustle", TransactionType.INCOME, owner), 2L);
        when(categoryRepository.findByUserIdOrUserIsNull(OWNER_ID)).thenReturn(List.of(systemDefault, own));

        List<CategoryResponse> result = categoryService.listForUser(OWNER_ID);

        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(c -> assertThat(c.system()).isTrue());
        assertThat(result).anySatisfy(c -> assertThat(c.system()).isFalse());
    }

    @Test
    void create_wrapsADatabaseUniqueConstraintViolationAsADuplicateResourceException() {
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(categoryRepository.save(any(Category.class))).thenThrow(new DataIntegrityViolationException("dup"));
        CategoryRequest request = new CategoryRequest("Groceries", TransactionType.EXPENSE);

        assertThatThrownBy(() -> categoryService.create(OWNER_ID, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void update_rejectsASystemDefaultCategoryAsIfItDidNotExist() {
        Category systemDefault = TestEntities.withId(new Category("Groceries", TransactionType.EXPENSE, null), 1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(systemDefault));
        CategoryRequest request = new CategoryRequest("Renamed", TransactionType.EXPENSE);

        assertThatThrownBy(() -> categoryService.update(1L, OWNER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_rejectsACategoryOwnedByAnotherUser() {
        User otherOwner = TestEntities.withId(new User("Bob", "bob@example.com", "hash"), OTHER_USER_ID);
        Category othersCategory = TestEntities.withId(
                new Category("Bob's Category", TransactionType.EXPENSE, otherOwner), 3L);
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(othersCategory));
        CategoryRequest request = new CategoryRequest("Renamed", TransactionType.EXPENSE);

        assertThatThrownBy(() -> categoryService.update(3L, OWNER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_succeedsForACategoryTheUserOwns() {
        Category own = TestEntities.withId(new Category("Old Name", TransactionType.EXPENSE, owner), 4L);
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(own));
        CategoryRequest request = new CategoryRequest("New Name", TransactionType.EXPENSE);

        CategoryResponse response = categoryService.update(4L, OWNER_ID, request);

        assertThat(response.name()).isEqualTo("New Name");
    }
}
