package com.pulsewallet.pulsewallet.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulsewallet.pulsewallet.entity.InvestmentProfile;

public interface InvestmentProfileRepository extends JpaRepository<InvestmentProfile, Long> {

    Optional<InvestmentProfile> findByUserId(Long userId);
}
