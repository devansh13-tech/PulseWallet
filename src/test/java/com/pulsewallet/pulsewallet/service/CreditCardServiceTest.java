package com.pulsewallet.pulsewallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewallet.pulsewallet.dto.CreditCardRequest;
import com.pulsewallet.pulsewallet.dto.CreditCardResponse;
import com.pulsewallet.pulsewallet.entity.CreditCard;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.CreditCardRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;
import com.pulsewallet.pulsewallet.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceTest {

    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private CreditCardService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = TestEntities.withId(new User("Alice", "alice@example.com", "hash"), 1L);
    }

    private CreditCardRequest validRequest() {
        return new CreditCardRequest("HDFC", "Regalia", "1234",
                new BigDecimal("100000"), new BigDecimal("25000"),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15),
                "5% cashback", "Lounge access");
    }

    private CreditCard cardEntity() {
        CreditCard card = new CreditCard(user, "HDFC", "Regalia", "1234",
                new BigDecimal("100000"), new BigDecimal("25000"),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15),
                "5% cashback", "Lounge access");
        return TestEntities.withId(card, 10L);
    }

    @Test
    void createReturnsResponse() {
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        CreditCard saved = cardEntity();
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(saved);

        CreditCardResponse response = service.create(1L, validRequest());

        assertNotNull(response);
        assertEquals("HDFC", response.issuer());
        assertEquals("Regalia", response.cardName());
        assertEquals("1234", response.lastFourDigits());
    }

    @Test
    void getAllReturnsUserCards() {
        when(creditCardRepository.findAllByUserId(1L)).thenReturn(List.of(cardEntity()));

        List<CreditCardResponse> result = service.getAll(1L);

        assertEquals(1, result.size());
        assertEquals("Regalia", result.get(0).cardName());
    }

    @Test
    void getByIdReturnsCard() {
        when(creditCardRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(cardEntity()));

        CreditCardResponse result = service.getById(10L, 1L);

        assertEquals(10L, result.id());
    }

    @Test
    void getByIdThrowsWhenNotOwned() {
        when(creditCardRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(99L, 1L));
    }

    @Test
    void updateModifiesAndReturns() {
        CreditCard existing = cardEntity();
        when(creditCardRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(existing));

        CreditCardRequest update = new CreditCardRequest("ICICI", "Amazon Pay", "5678",
                new BigDecimal("200000"), new BigDecimal("10000"),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 20),
                null, null);

        CreditCardResponse result = service.update(10L, 1L, update);

        assertEquals("ICICI", result.issuer());
        assertEquals("Amazon Pay", result.cardName());
        assertEquals("5678", result.lastFourDigits());
    }

    @Test
    void deleteRemovesCard() {
        CreditCard existing = cardEntity();
        when(creditCardRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(existing));

        service.delete(10L, 1L);

        verify(creditCardRepository).delete(existing);
    }

    @Test
    void deleteThrowsWhenNotOwned() {
        when(creditCardRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L, 1L));
    }

    @Test
    void validationRejectsInvalidDigits() {
        CreditCardRequest bad = new CreditCardRequest("HDFC", "Card", "12X4",
                new BigDecimal("100000"), BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(15), null, null);

        assertThrows(IllegalArgumentException.class, () -> service.create(1L, bad));
    }

    @Test
    void validationRejectsOutstandingExceedingLimit() {
        CreditCardRequest bad = new CreditCardRequest("HDFC", "Card", "1234",
                new BigDecimal("100000"), new BigDecimal("200000"),
                LocalDate.now(), LocalDate.now().plusDays(15), null, null);

        assertThrows(IllegalArgumentException.class, () -> service.create(1L, bad));
    }
}
