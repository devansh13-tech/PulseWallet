package com.pulsewallet.pulsewallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewallet.pulsewallet.dto.CreditCardIntelligenceResponse;
import com.pulsewallet.pulsewallet.dto.FinancialPlanResponse;
import com.pulsewallet.pulsewallet.dto.PurchaseCheckRequest;
import com.pulsewallet.pulsewallet.dto.PurchaseCheckResponse;
import com.pulsewallet.pulsewallet.entity.CreditCard;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.repository.CreditCardRepository;
import com.pulsewallet.pulsewallet.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class CreditCardIntelligenceServiceTest {

    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private FinancialPlanService financialPlanService;

    private CreditCardIntelligenceService service;
    private User user;
    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-09-10T00:00:00Z"), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        service = new CreditCardIntelligenceService(creditCardRepository, financialPlanService, fixedClock);
        user = TestEntities.withId(new User("Alice", "alice@example.com", "hash"), 1L);
    }

    private CreditCard card(Long id, BigDecimal limit, BigDecimal outstanding) {
        return card(id, limit, outstanding, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15));
    }

    private CreditCard card(Long id, BigDecimal limit, BigDecimal outstanding,
            LocalDate statementDate, LocalDate dueDate) {
        CreditCard c = new CreditCard(user, "TestBank", "TestCard-" + id, String.valueOf(1000 + id),
                limit, outstanding, statementDate, dueDate, null, null);
        return TestEntities.withId(c, id);
    }

    private FinancialPlanResponse planResponse(BigDecimal income, BigDecimal expenses,
            BigDecimal goalSavings, BigDecimal emergency, BigDecimal investment) {
        BigDecimal left = income.subtract(expenses).setScale(2, RoundingMode.HALF_UP);
        return new FinancialPlanResponse(income, expenses, left, goalSavings,
                expenses.multiply(BigDecimal.valueOf(3)), emergency, investment,
                BigDecimal.ZERO, null, null, List.of(), List.of(), List.of());
    }

    // -----------------------------------------------------------------------
    // Utilization calculation
    // -----------------------------------------------------------------------

    @Test
    void utilizationPercentageCalculatesCorrectly() {
        CreditCard c = card(1L, new BigDecimal("100000"), new BigDecimal("25000"));
        BigDecimal util = service.utilizationPercentage(c);
        assertEquals(new BigDecimal("25.00"), util);
    }

    @Test
    void utilizationPercentageZeroLimit() {
        CreditCard c = card(1L, BigDecimal.ZERO, BigDecimal.ZERO);
        BigDecimal util = service.utilizationPercentage(c);
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), util);
    }

    @Test
    void availableCreditCalculation() {
        CreditCard c = card(1L, new BigDecimal("100000"), new BigDecimal("30000"));
        BigDecimal available = service.availableCredit(c);
        assertEquals(new BigDecimal("70000.00"), available);
    }

    @Test
    void availableCreditNeverNegative() {
        CreditCard c = card(1L, new BigDecimal("100000"), new BigDecimal("150000"));
        BigDecimal available = service.availableCredit(c);
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), available);
    }

    // -----------------------------------------------------------------------
    // Utilization level
    // -----------------------------------------------------------------------

    @Test
    void utilizationLevelLow() {
        assertEquals("LOW", service.utilizationLevel(new BigDecimal("20")));
        assertEquals("LOW", service.utilizationLevel(new BigDecimal("30")));
    }

    @Test
    void utilizationLevelModerate() {
        assertEquals("MODERATE", service.utilizationLevel(new BigDecimal("31")));
        assertEquals("MODERATE", service.utilizationLevel(new BigDecimal("50")));
    }

    @Test
    void utilizationLevelHigh() {
        assertEquals("HIGH", service.utilizationLevel(new BigDecimal("51")));
        assertEquals("HIGH", service.utilizationLevel(new BigDecimal("75")));
    }

    @Test
    void utilizationLevelVeryHigh() {
        assertEquals("VERY_HIGH", service.utilizationLevel(new BigDecimal("76")));
        assertEquals("VERY_HIGH", service.utilizationLevel(new BigDecimal("100")));
    }

    // -----------------------------------------------------------------------
    // Health status
    // -----------------------------------------------------------------------

    @Test
    void healthStatusMapsCorrectly() {
        assertEquals("HEALTHY", service.healthStatus("LOW"));
        assertEquals("WATCH", service.healthStatus("MODERATE"));
        assertEquals("WARNING", service.healthStatus("HIGH"));
        assertEquals("CRITICAL", service.healthStatus("VERY_HIGH"));
    }

    // -----------------------------------------------------------------------
    // Intelligence - no cards
    // -----------------------------------------------------------------------

    @Test
    void intelligenceWithNoCardsReturnsZeroSafeSummary() {
        when(creditCardRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
        when(financialPlanService.calculate(1L)).thenReturn(
                planResponse(new BigDecimal("50000"), new BigDecimal("30000"),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        CreditCardIntelligenceResponse result = service.getIntelligence(1L);

        assertNotNull(result);
        assertTrue(result.cards().isEmpty());
        assertEquals(0, result.summary().numberOfCards());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), result.summary().totalCreditLimit());
    }

    // -----------------------------------------------------------------------
    // Intelligence - multiple cards
    // -----------------------------------------------------------------------

    @Test
    void intelligenceWithMultipleCardsAggregatesCorrectly() {
        CreditCard c1 = card(1L, new BigDecimal("100000"), new BigDecimal("20000"));
        CreditCard c2 = card(2L, new BigDecimal("200000"), new BigDecimal("80000"));
        when(creditCardRepository.findAllByUserId(1L)).thenReturn(List.of(c1, c2));
        when(financialPlanService.calculate(1L)).thenReturn(
                planResponse(new BigDecimal("80000"), new BigDecimal("40000"),
                        new BigDecimal("5000"), new BigDecimal("3000"), new BigDecimal("5000")));

        CreditCardIntelligenceResponse result = service.getIntelligence(1L);

        assertEquals(2, result.summary().numberOfCards());
        assertEquals(new BigDecimal("300000.00"), result.summary().totalCreditLimit());
        assertEquals(new BigDecimal("100000.00"), result.summary().totalOutstanding());
        assertEquals(new BigDecimal("200000.00"), result.summary().totalAvailableCredit());
        // 100000 / 300000 * 100 = 33.33
        assertEquals(new BigDecimal("33.33"), result.summary().overallUtilization());
    }

    // -----------------------------------------------------------------------
    // Repayment capacity
    // -----------------------------------------------------------------------

    @Test
    void repaymentCapacityComfortable() {
        when(financialPlanService.calculate(1L)).thenReturn(
                planResponse(new BigDecimal("100000"), new BigDecimal("40000"),
                        new BigDecimal("5000"), new BigDecimal("3000"), new BigDecimal("5000")));

        var result = service.buildRepaymentCapacity(1L, new BigDecimal("10000"));

        assertEquals("COMFORTABLE", result.status());
    }

    @Test
    void repaymentCapacityHighRiskWhenNoSurplus() {
        when(financialPlanService.calculate(1L)).thenReturn(
                planResponse(new BigDecimal("40000"), new BigDecimal("45000"),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        var result = service.buildRepaymentCapacity(1L, new BigDecimal("50000"));

        assertEquals("HIGH_RISK", result.status());
    }

    @Test
    void repaymentCapacityStretched() {
        when(financialPlanService.calculate(1L)).thenReturn(
                planResponse(new BigDecimal("50000"), new BigDecimal("30000"),
                        new BigDecimal("5000"), new BigDecimal("2000"), new BigDecimal("3000")));

        // Available = 20000 - 5000 - 2000 - 3000 = 10000
        // Outstanding = 35000 > 2*10000=20000 but <= 4*10000=40000 => STRETCHED
        var result = service.buildRepaymentCapacity(1L, new BigDecimal("35000"));

        assertEquals("STRETCHED", result.status());
    }

    // -----------------------------------------------------------------------
    // Card health
    // -----------------------------------------------------------------------

    @Test
    void cardHealthHighUtilizationGeneratesWarning() {
        CreditCard c = card(1L, new BigDecimal("100000"), new BigDecimal("80000"));
        LocalDate today = LocalDate.now(fixedClock);

        var health = service.buildCardHealth(c, today);

        assertEquals("VERY_HIGH", health.utilizationLevel());
        assertEquals("CRITICAL", health.healthStatus());
        assertFalse(health.warnings().isEmpty());
    }

    @Test
    void cardHealthLowUtilizationIsHealthy() {
        CreditCard c = card(1L, new BigDecimal("100000"), new BigDecimal("10000"));
        LocalDate today = LocalDate.now(fixedClock);

        var health = service.buildCardHealth(c, today);

        assertEquals("LOW", health.utilizationLevel());
        assertEquals("HEALTHY", health.healthStatus());
    }

    @Test
    void cardHealthUpcomingDueDateGeneratesWarning() {
        LocalDate today = LocalDate.now(fixedClock);
        CreditCard c = card(1L, new BigDecimal("100000"), new BigDecimal("5000"),
                today.minusDays(20), today.plusDays(3));

        var health = service.buildCardHealth(c, today);

        assertTrue(health.warnings().stream().anyMatch(w -> w.contains("due date")));
    }

    // -----------------------------------------------------------------------
    // Purchase check
    // -----------------------------------------------------------------------

    @Test
    void purchaseCheckSafeWhenAffordable() {
        CreditCard c = card(1L, new BigDecimal("100000"), new BigDecimal("10000"));
        when(creditCardRepository.findAllByUserId(1L)).thenReturn(List.of(c));
        when(financialPlanService.calculate(1L)).thenReturn(
                planResponse(new BigDecimal("80000"), new BigDecimal("30000"),
                        new BigDecimal("5000"), new BigDecimal("2000"), new BigDecimal("3000")));

        PurchaseCheckResponse result = service.checkPurchase(1L,
                new PurchaseCheckRequest(new BigDecimal("5000"), "ELECTRONICS", "Phone"));

        assertEquals("SAFE", result.financialSafetyStatus());
        assertEquals(1, result.cardAssessments().size());
        assertEquals("SAFE", result.cardAssessments().get(0).safetyStatus());
    }

    @Test
    void purchaseCheckNotRecommendedWhenExceedsMonthlyLeft() {
        CreditCard c = card(1L, new BigDecimal("100000"), new BigDecimal("10000"));
        when(creditCardRepository.findAllByUserId(1L)).thenReturn(List.of(c));
        when(financialPlanService.calculate(1L)).thenReturn(
                planResponse(new BigDecimal("40000"), new BigDecimal("38000"),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        PurchaseCheckResponse result = service.checkPurchase(1L,
                new PurchaseCheckRequest(new BigDecimal("5000"), "TRAVEL", "Flight"));

        assertEquals("NOT_RECOMMENDED", result.financialSafetyStatus());
    }

    @Test
    void purchaseCheckCautionWhenHighUtilizationAfterPurchase() {
        CreditCard c = card(1L, new BigDecimal("100000"), new BigDecimal("60000"));
        when(creditCardRepository.findAllByUserId(1L)).thenReturn(List.of(c));
        when(financialPlanService.calculate(1L)).thenReturn(
                planResponse(new BigDecimal("100000"), new BigDecimal("30000"),
                        new BigDecimal("5000"), new BigDecimal("2000"), new BigDecimal("3000")));

        // 60000 + 20000 = 80000 / 100000 = 80% -> CAUTION
        PurchaseCheckResponse result = service.checkPurchase(1L,
                new PurchaseCheckRequest(new BigDecimal("20000"), "SHOPPING", null));

        assertEquals("CAUTION", result.financialSafetyStatus());
    }

    @Test
    void purchaseCheckWithMultipleCards() {
        CreditCard c1 = card(1L, new BigDecimal("100000"), new BigDecimal("10000"));
        CreditCard c2 = card(2L, new BigDecimal("50000"), new BigDecimal("40000"));
        when(creditCardRepository.findAllByUserId(1L)).thenReturn(List.of(c1, c2));
        when(financialPlanService.calculate(1L)).thenReturn(
                planResponse(new BigDecimal("80000"), new BigDecimal("30000"),
                        new BigDecimal("5000"), new BigDecimal("2000"), new BigDecimal("3000")));

        PurchaseCheckResponse result = service.checkPurchase(1L,
                new PurchaseCheckRequest(new BigDecimal("15000"), "TRAVEL", null));

        assertEquals(2, result.cardAssessments().size());
    }

    @Test
    void purchaseCheckWithNoCardsReturnsWarning() {
        when(creditCardRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());
        when(financialPlanService.calculate(1L)).thenReturn(
                planResponse(new BigDecimal("50000"), new BigDecimal("30000"),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        PurchaseCheckResponse result = service.checkPurchase(1L,
                new PurchaseCheckRequest(new BigDecimal("5000"), "FOOD", null));

        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("No credit cards")));
    }

    // -----------------------------------------------------------------------
    // Summary
    // -----------------------------------------------------------------------

    @Test
    void summaryCountsCardsNeedingAttention() {
        // c1: 60% = HIGH, c2: 10% = LOW
        CreditCard c1 = card(1L, new BigDecimal("100000"), new BigDecimal("60000"));
        CreditCard c2 = card(2L, new BigDecimal("100000"), new BigDecimal("10000"));
        LocalDate today = LocalDate.now(fixedClock);

        var summary = service.buildSummary(List.of(c1, c2), today);

        assertEquals(2, summary.numberOfCards());
        assertEquals(1, summary.cardsNeedingAttention());
    }
}
