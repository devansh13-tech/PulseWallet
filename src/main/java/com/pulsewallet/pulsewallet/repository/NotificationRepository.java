package com.pulsewallet.pulsewallet.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pulsewallet.pulsewallet.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadFalse(Long userId);

    @Query("select count(n) > 0 from Notification n where n.fraudAlert.id = :fraudAlertId")
    boolean existsByFraudAlertId(@Param("fraudAlertId") Long fraudAlertId);

    @Query("select count(n) > 0 from Notification n where n.transaction.id = :transactionId")
    boolean existsByTransactionId(@Param("transactionId") Long transactionId);
}
