package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewallet.pulsewallet.dto.InvestmentProfileRequest;
import com.pulsewallet.pulsewallet.entity.InvestmentProfile;
import com.pulsewallet.pulsewallet.entity.InvestmentRiskPreference;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.repository.InvestmentProfileRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class InvestmentProfileServiceTest {

    @Mock
    private InvestmentProfileRepository profileRepository;
    @Mock
    private UserRepository userRepository;

    private InvestmentProfileService service;

    @BeforeEach
    void setUp() {
        service = new InvestmentProfileService(profileRepository, userRepository);
    }

    @Test
    void getUsesAuthenticatedUserScopeAndReturnsEmptyWhenNotConfigured() {
        when(profileRepository.findByUserId(9L)).thenReturn(Optional.empty());

        service.get(9L);

        verify(profileRepository).findByUserId(9L);
    }

    @Test
    void saveRejectsMissingRiskOrNonPositiveHorizon() {
        assertThatThrownBy(() -> service.save(1L, new InvestmentProfileRequest(null, 5)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.save(1L, new InvestmentProfileRequest(InvestmentRiskPreference.MODERATE, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void savesOneProfileForTheAuthenticatedUser() {
        User user = new User("Ada", "ada@example.com", "hash");
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        InvestmentProfile saved = new InvestmentProfile(user, InvestmentRiskPreference.CONSERVATIVE, 3);
        when(profileRepository.save(org.mockito.ArgumentMatchers.any(InvestmentProfile.class))).thenReturn(saved);

        service.save(1L, new InvestmentProfileRequest(InvestmentRiskPreference.CONSERVATIVE, 3));

        verify(profileRepository).findByUserId(1L);
        verify(userRepository).getReferenceById(1L);
    }
}
