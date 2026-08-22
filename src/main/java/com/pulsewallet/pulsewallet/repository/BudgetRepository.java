package com.pulsewallet.pulsewallet.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulsewallet.pulsewallet.entity.Budget;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserId(Long userId);

    Optional<Budget> findByIdAndUserId(Long id, Long userId);
}
