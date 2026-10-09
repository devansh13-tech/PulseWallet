package com.pulsewallet.pulsewallet.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.LinkedPaymentAccountRequest;
import com.pulsewallet.pulsewallet.dto.LinkedPaymentAccountResponse;
import com.pulsewallet.pulsewallet.entity.LinkedPaymentAccount;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.LinkedPaymentAccountRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;

@Service
public class LinkedPaymentAccountService {

    private final LinkedPaymentAccountRepository accountRepository;
    private final UserRepository userRepository;

    public LinkedPaymentAccountService(LinkedPaymentAccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<LinkedPaymentAccountResponse> list(Long userId) {
        return accountRepository.findByUserIdOrderByConnectedAtDesc(userId).stream()
                .map(LinkedPaymentAccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public LinkedPaymentAccountResponse get(Long id, Long userId) {
        return LinkedPaymentAccountResponse.from(requireOwned(id, userId));
    }

    @Transactional
    public LinkedPaymentAccountResponse create(Long userId, LinkedPaymentAccountRequest request) {
        validate(request);
        User user = userRepository.getReferenceById(userId);
        LinkedPaymentAccount account = new LinkedPaymentAccount(
                user,
                request.providerName().trim(),
                request.accountName().trim(),
                request.accountType(),
                request.maskedIdentifier().trim(),
                request.balance() != null ? request.balance() : java.math.BigDecimal.ZERO,
                request.accountSubtype() != null ? request.accountSubtype().trim() : null,
                request.ifscCode() != null ? request.ifscCode().trim().toUpperCase() : null);
        return LinkedPaymentAccountResponse.from(accountRepository.save(account));
    }

    @Transactional
    public LinkedPaymentAccountResponse updateBalance(Long id, Long userId, java.math.BigDecimal balance) {
        LinkedPaymentAccount account = requireOwned(id, userId);
        account.setBalance(balance != null ? balance : java.math.BigDecimal.ZERO);
        account.touchSync();
        return LinkedPaymentAccountResponse.from(account);
    }

    @Transactional
    public LinkedPaymentAccountResponse disconnect(Long id, Long userId) {
        LinkedPaymentAccount account = requireOwned(id, userId);
        account.disconnect();
        return LinkedPaymentAccountResponse.from(account);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        accountRepository.delete(requireOwned(id, userId));
    }

    private LinkedPaymentAccount requireOwned(Long id, Long userId) {
        return accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Linked payment account", id));
    }

    private void validate(LinkedPaymentAccountRequest request) {
        if (request == null)
            throw new IllegalArgumentException("linked account details are required");
        if (request.maskedIdentifier().matches(".*\\d{12,}.*"))
            throw new IllegalArgumentException("maskedIdentifier must not contain a full account or card number");
    }
}
