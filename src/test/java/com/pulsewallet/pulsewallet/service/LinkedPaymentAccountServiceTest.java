package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.LinkedPaymentAccountRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class LinkedPaymentAccountServiceTest {

    @Mock
    private LinkedPaymentAccountRepository accountRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private LinkedPaymentAccountService accountService;

    @Test
    void anotherUsersAccountIsNotVisibleToTheRequestingUser() {
        when(accountRepository.findByIdAndUserId(42L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.get(42L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateBalanceUpdatesBalanceAndSyncTime() {
        com.pulsewallet.pulsewallet.entity.User user = new com.pulsewallet.pulsewallet.entity.User("Ada", "ada@example.com", "hash");
        com.pulsewallet.pulsewallet.entity.LinkedPaymentAccount account = new com.pulsewallet.pulsewallet.entity.LinkedPaymentAccount(
                user, "HDFC", "Salary", com.pulsewallet.pulsewallet.entity.LinkedPaymentAccountType.BANK, "****1234");
        when(accountRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(account));

        var updated = accountService.updateBalance(1L, 7L, java.math.BigDecimal.valueOf(25000));
        org.assertj.core.api.Assertions.assertThat(updated.balance()).isEqualByComparingTo("25000");
    }
}
