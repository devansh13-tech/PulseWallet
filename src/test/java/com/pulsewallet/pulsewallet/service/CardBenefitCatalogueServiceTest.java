package com.pulsewallet.pulsewallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pulsewallet.pulsewallet.dto.CardBenefitsResponse;
import com.pulsewallet.pulsewallet.dto.EstimatedRewardDto;
import com.pulsewallet.pulsewallet.entity.CreditCard;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.service.benefits.CardBenefitCatalogueService;
import com.pulsewallet.pulsewallet.support.TestEntities;

class CardBenefitCatalogueServiceTest {

    private CardBenefitCatalogueService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new CardBenefitCatalogueService();
        user = TestEntities.withId(new User("Alice", "alice@example.com", "hash"), 1L);
    }

    private CreditCard card(String name, String issuer) {
        CreditCard c = new CreditCard(user, issuer, name, "1234",
                new BigDecimal("100000"), new BigDecimal("25000"),
                LocalDate.now(), LocalDate.now().plusDays(15), "5% rewards", "Airport lounge");
        return TestEntities.withId(c, 10L);
    }

    @Test
    void getBenefitsForRecognizedCard() {
        CreditCard regalia = card("HDFC Regalia Gold", "HDFC Bank");
        CardBenefitsResponse response = service.getBenefitsForCard(regalia);

        assertNotNull(response);
        assertEquals("Regalia", response.cardName());
        assertEquals("HDFC Bank", response.issuer());
        assertEquals(new BigDecimal("2500.00"), response.publishedAnnualFee());
        assertTrue(response.isVerified());
        assertTrue(response.excludedCategories().contains("FUEL"));
    }

    @Test
    void estimateRewardForEligibleCategoryMultiplier() {
        CreditCard amazonPay = card("ICICI Amazon Pay", "ICICI Bank");

        // 5% cashback on Shopping
        EstimatedRewardDto estimate = service.estimateReward(amazonPay, "SHOPPING", new BigDecimal("10000.00"));

        assertNotNull(estimate);
        assertTrue(estimate.isEstimate());
        assertEquals(new BigDecimal("5.00"), estimate.rewardRateApplied());
        assertEquals(new BigDecimal("500.00"), estimate.estimatedCashbackValue());
        assertEquals(new BigDecimal("500.00"), estimate.pointsEarned());
    }

    @Test
    void estimateRewardForExcludedCategoryReturnsZero() {
        CreditCard regalia = card("HDFC Regalia", "HDFC Bank");

        // Fuel is excluded
        EstimatedRewardDto estimate = service.estimateReward(regalia, "FUEL", new BigDecimal("5000.00"));

        assertNotNull(estimate);
        assertEquals(new BigDecimal("0.00"), estimate.estimatedCashbackValue());
        assertEquals(new BigDecimal("0.00"), estimate.pointsEarned());
        assertTrue(estimate.explanation().contains("excluded"));
    }

    @Test
    void estimateRewardForUncataloguedCardUsesBaseline() {
        CreditCard genericCard = card("Generic Gold Card", "Local Bank");

        EstimatedRewardDto estimate = service.estimateReward(genericCard, "GENERAL", new BigDecimal("2000.00"));

        assertNotNull(estimate);
        assertEquals(new BigDecimal("1.00"), estimate.rewardRateApplied());
        assertEquals(new BigDecimal("20.00"), estimate.estimatedCashbackValue());
        assertEquals("ESTIMATED_BASELINE", estimate.source());
    }

    @Test
    void estimateRewardRejectsNegativeAmount() {
        CreditCard regalia = card("HDFC Regalia", "HDFC Bank");
        EstimatedRewardDto estimate = service.estimateReward(regalia, "DINING", new BigDecimal("-100.00"));

        assertEquals(new BigDecimal("0.00"), estimate.pointsEarned());
        assertEquals(new BigDecimal("0.00"), estimate.estimatedCashbackValue());
    }
}
