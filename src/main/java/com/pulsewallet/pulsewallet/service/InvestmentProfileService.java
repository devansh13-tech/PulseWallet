package com.pulsewallet.pulsewallet.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.InvestmentProfileRequest;
import com.pulsewallet.pulsewallet.dto.InvestmentProfileResponse;
import com.pulsewallet.pulsewallet.entity.InvestmentProfile;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.repository.InvestmentProfileRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;

@Service
public class InvestmentProfileService {

    private final InvestmentProfileRepository profileRepository;
    private final UserRepository userRepository;

    public InvestmentProfileService(InvestmentProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public InvestmentProfileResponse get(Long userId) {
        validateUserId(userId);
        return profileRepository.findByUserId(userId).map(InvestmentProfileResponse::from).orElse(null);
    }

    @Transactional
    public InvestmentProfileResponse save(Long userId, InvestmentProfileRequest request) {
        validateUserId(userId);
        validate(request);
        InvestmentProfile profile = profileRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.getReferenceById(userId);
            return new InvestmentProfile(user, request.riskPreference(), request.timeHorizonYears());
        });
        profile.setRiskPreference(request.riskPreference());
        profile.setTimeHorizonYears(request.timeHorizonYears());
        return InvestmentProfileResponse.from(profileRepository.save(profile));
    }

    private void validateUserId(Long userId) {
        if (userId == null)
            throw new IllegalArgumentException("userId must not be null");
    }

    private void validate(InvestmentProfileRequest request) {
        if (request == null || request.riskPreference() == null || request.timeHorizonYears() == null
                || request.timeHorizonYears() <= 0) {
            throw new IllegalArgumentException("riskPreference and a positive timeHorizonYears are required");
        }
    }
}
