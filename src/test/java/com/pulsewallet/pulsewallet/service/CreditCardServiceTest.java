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
import org.mockito.ArgumentCaptor;
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
        assertEquals(new BigDecimal("75000.00"), response.availableCredit());
        assertEquals(new BigDecimal("25.00"), response.creditUtilization());
    }

    @Test
    void createWithCardNetworkAndAnnualFeePersistsCorrectly() {
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        CreditCardRequest request = new CreditCardRequest("SBI", "SimplyCLICK", "VISA", "4321",
                new BigDecimal("50000"), new BigDecimal("10000"),
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 25),
                new BigDecimal("499.00"), "10x rewards on partners", "Fuel surcharge waiver");

        CreditCard saved = new CreditCard(user, "SBI", "SimplyCLICK", "VISA", "4321",
                new BigDecimal("50000"), new BigDecimal("10000"),
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 25),
                new BigDecimal("499.00"), "10x rewards on partners", "Fuel surcharge waiver");
        TestEntities.withId(saved, 20L);
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(saved);

        CreditCardResponse response = service.create(1L, request);

        assertNotNull(response);
        assertEquals("SBI", response.issuer());
        assertEquals("SimplyCLICK", response.cardName());
        assertEquals("VISA", response.cardNetwork());
        assertEquals("4321", response.lastFourDigits());
        assertEquals(new BigDecimal("499.00"), response.annualFee());
        assertEquals(new BigDecimal("40000.00"), response.availableCredit());
        assertEquals(new BigDecimal("20.00"), response.creditUtilization());
    }

    @Test
    void getAllReturnsUserCards() {
        when(creditCardRepository.findAllByUserId(1L)).thenReturn(List.of(cardEntity()));

        List<CreditCardResponse> result = service.getAll(1L);

        assertEquals(1, result.size());
        assertEquals("Regalia", result.get(0).cardName());
        assertEquals(new BigDecimal("75000.00"), result.get(0).availableCredit());
        assertEquals(new BigDecimal("25.00"), result.get(0).creditUtilization());
    }

    @Test
    void getByIdReturnsCard() {
        when(creditCardRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(cardEntity()));

        CreditCardResponse result = service.getById(10L, 1L);

        assertEquals(10L, result.id());
        assertEquals(new BigDecimal("75000.00"), result.availableCredit());
        assertEquals(new BigDecimal("25.00"), result.creditUtilization());
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
        assertEquals(new BigDecimal("190000.00"), result.availableCredit());
        assertEquals(new BigDecimal("5.00"), result.creditUtilization());
    }

    @Test
    void updateWithNetworkAndAnnualFeeModifiesAllFields() {
        CreditCard existing = cardEntity();
        when(creditCardRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(existing));

        CreditCardRequest update = new CreditCardRequest("Axis", "Magnus", "MASTERCARD", "9999",
                new BigDecimal("500000"), new BigDecimal("150000"),
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 22),
                new BigDecimal("12500.00"), "EDGE Rewards", "Unlimited airport lounges");

        CreditCardResponse result = service.update(10L, 1L, update);

        assertEquals("Axis", result.issuer());
        assertEquals("Magnus", result.cardName());
        assertEquals("MASTERCARD", result.cardNetwork());
        assertEquals("9999", result.lastFourDigits());
        assertEquals(new BigDecimal("12500.00"), result.annualFee());
        assertEquals(new BigDecimal("350000.00"), result.availableCredit());
        assertEquals(new BigDecimal("30.00"), result.creditUtilization());
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
    void validationRejectsNullRequest() {
        assertThrows(IllegalArgumentException.class, () -> service.create(1L, null));
    }

    @Test
    void validationRejectsBlankIssuer() {
        CreditCardRequest bad = new CreditCardRequest("   ", "Card", "1234",
                new BigDecimal("100000"), BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(15), null, null);

        assertThrows(IllegalArgumentException.class, () -> service.create(1L, bad));
    }

    @Test
    void validationRejectsBlankCardName() {
        CreditCardRequest bad = new CreditCardRequest("HDFC", " ", "1234",
                new BigDecimal("100000"), BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(15), null, null);

        assertThrows(IllegalArgumentException.class, () -> service.create(1L, bad));
    }

    @Test
    void validationRejectsInvalidDigits() {
        CreditCardRequest bad = new CreditCardRequest("HDFC", "Card", "12X4",
                new BigDecimal("100000"), BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(15), null, null);

        assertThrows(IllegalArgumentException.class, () -> service.create(1L, bad));
    }

    @Test
    void validationRejectsNullOrNegativeCreditLimit() {
        CreditCardRequest bad = new CreditCardRequest("HDFC", "Card", "1234",
                new BigDecimal("-500"), BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(15), null, null);

        assertThrows(IllegalArgumentException.class, () -> service.create(1L, bad));
    }

    @Test
    void validationRejectsNegativeOutstanding() {
        CreditCardRequest bad = new CreditCardRequest("HDFC", "Card", "1234",
                new BigDecimal("100000"), new BigDecimal("-100"),
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

    @Test
    void validationRejectsNullDates() {
        CreditCardRequest bad = new CreditCardRequest("HDFC", "Card", "1234",
                new BigDecimal("100000"), BigDecimal.ZERO,
                null, LocalDate.now().plusDays(15), null, null);

        assertThrows(IllegalArgumentException.class, () -> service.create(1L, bad));
    }

    @Test
    void validationRejectsNegativeAnnualFee() {
        CreditCardRequest bad = new CreditCardRequest("HDFC", "Card", "VISA", "1234",
                new BigDecimal("100000"), BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now().plusDays(15),
                new BigDecimal("-50"), null, null);

        assertThrows(IllegalArgumentException.class, () -> service.create(1L, bad));
    }

    @Test
    void calculateAvailableCreditAndUtilizationWorkDirectly() {
        BigDecimal limit = new BigDecimal("100000");
        BigDecimal outstanding = new BigDecimal("25000");

        BigDecimal available = service.calculateAvailableCredit(limit, outstanding);
        BigDecimal utilization = service.calculateCreditUtilization(limit, outstanding);

        assertEquals(new BigDecimal("75000.00"), available);
        assertEquals(new BigDecimal("25.00"), utilization);

        // Zero limit edge case
        assertEquals(new BigDecimal("0.00"), service.calculateCreditUtilization(BigDecimal.ZERO, outstanding));

        // Zero outstanding edge case
        assertEquals(new BigDecimal("100000.00"), service.calculateAvailableCredit(limit, BigDecimal.ZERO));
        assertEquals(new BigDecimal("0.00"), service.calculateCreditUtilization(limit, BigDecimal.ZERO));
    }
}
