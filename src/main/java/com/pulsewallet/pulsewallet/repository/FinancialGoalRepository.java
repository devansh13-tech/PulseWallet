package com.pulsewallet.pulsewallet.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulsewallet.pulsewallet.entity.FinancialGoal;

public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, Long> {

    List<FinancialGoal> findByUserIdOrderByTargetDateAsc(Long userId);

    Optional<FinancialGoal> findByIdAndUserId(Long id, Long userId);
}