package com.pulsewallet.pulsewallet.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulsewallet.pulsewallet.entity.FraudAlert;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    boolean existsByTransactionId(Long transactionId);

    long countByUserId(Long userId);

    long countByUserIdAndResolvedFalse(Long userId);

    List<FraudAlert> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
}
