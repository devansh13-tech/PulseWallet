package com.pulsewallet.pulsewallet.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulsewallet.pulsewallet.entity.LinkedPaymentAccount;

public interface LinkedPaymentAccountRepository extends JpaRepository<LinkedPaymentAccount, Long> {

    List<LinkedPaymentAccount> findByUserIdOrderByConnectedAtDesc(Long userId);

    Optional<LinkedPaymentAccount> findByIdAndUserId(Long id, Long userId);
}
