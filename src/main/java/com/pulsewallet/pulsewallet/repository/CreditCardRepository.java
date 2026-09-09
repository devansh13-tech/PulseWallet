package com.pulsewallet.pulsewallet.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulsewallet.pulsewallet.entity.CreditCard;

/**
 * All queries enforce user ownership — a user can never access
 * another user's credit-card records through this repository.
 */
public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    List<CreditCard> findAllByUserId(Long userId);

    Optional<CreditCard> findByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}
