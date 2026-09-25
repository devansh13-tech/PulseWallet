package com.pulsewallet.pulsewallet.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.CreditCardRequest;
import com.pulsewallet.pulsewallet.dto.CreditCardResponse;
import com.pulsewallet.pulsewallet.entity.CreditCard;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.CreditCardRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;

/**
 * CRUD operations for credit-card metadata.
 *
 * <p>Every method takes the authenticated {@code userId} from the controller's
 * {@code @AuthenticationPrincipal} — a user ID is <em>never</em> accepted
 * from the request body.
 */
@Service
public class CreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final UserRepository userRepository;

    public CreditCardService(CreditCardRepository creditCardRepository, UserRepository userRepository) {
        this.creditCardRepository = creditCardRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CreditCardResponse create(Long userId, CreditCardRequest request) {
        validate(request);
        var user = userRepository.getReferenceById(userId);
        var card = new CreditCard(
                user,
                request.issuer().trim(),
                request.cardName().trim(),
                request.lastFourDigits(),
                request.creditLimit(),
                request.currentOutstanding(),
                request.statementDate(),
                request.dueDate(),
                cleanText(request.rewardsDescription()),
                cleanText(request.benefitsDescription()));
        return CreditCardResponse.from(creditCardRepository.save(card));
    }

    @Transactional(readOnly = true)
    public List<CreditCardResponse> getAll(Long userId) {
        return creditCardRepository.findAllByUserId(userId).stream()
                .map(CreditCardResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CreditCardResponse getById(Long id, Long userId) {
        return CreditCardResponse.from(requireOwned(id, userId));
    }

    @Transactional
    public CreditCardResponse update(Long id, Long userId, CreditCardRequest request) {
        validate(request);
        CreditCard card = requireOwned(id, userId);
        card.setIssuer(request.issuer().trim());
        card.setCardName(request.cardName().trim());
        card.setLastFourDigits(request.lastFourDigits());
        card.setCreditLimit(request.creditLimit());
        card.setCurrentOutstanding(request.currentOutstanding());
        card.setStatementDate(request.statementDate());
        card.setDueDate(request.dueDate());
        card.setRewardsDescription(cleanText(request.rewardsDescription()));
        card.setBenefitsDescription(cleanText(request.benefitsDescription()));
        return CreditCardResponse.from(card);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        creditCardRepository.delete(requireOwned(id, userId));
    }

    private CreditCard requireOwned(Long id, Long userId) {
        return creditCardRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit card", id));
    }

    private void validate(CreditCardRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }
        if (request.lastFourDigits() != null && !request.lastFourDigits().matches("\\d{4}")) {
            throw new IllegalArgumentException("lastFourDigits must be exactly 4 numeric digits");
        }
        if (request.currentOutstanding() != null && request.creditLimit() != null
                && request.currentOutstanding().compareTo(request.creditLimit()) > 0) {
            throw new IllegalArgumentException("currentOutstanding must not exceed creditLimit");
        }
    }

    private String cleanText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
