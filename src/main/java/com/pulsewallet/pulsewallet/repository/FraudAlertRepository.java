package com.pulsewallet.pulsewallet.repository;

import com.pulsewallet.pulsewallet.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    boolean existsByTransactionId(Long transactionId);
}
