package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.CreditCardIntelligenceResponse;
import com.pulsewallet.pulsewallet.dto.CreditCardIntelligenceResponse.CardHealth;
import com.pulsewallet.pulsewallet.dto.CreditCardIntelligenceResponse.CreditCardSummary;
import com.pulsewallet.pulsewallet.dto.CreditCardIntelligenceResponse.RepaymentCapacity;
import com.pulsewallet.pulsewallet.dto.FinancialPlanResponse;
import com.pulsewallet.pulsewallet.dto.PurchaseCheckRequest;
import com.pulsewallet.pulsewallet.dto.PurchaseCheckResponse;
import com.pulsewallet.pulsewallet.dto.PurchaseCheckResponse.CardAssessment;
import com.pulsewallet.pulsewallet.entity.CreditCard;
import com.pulsewallet.pulsewallet.repository.CreditCardRepository;

/**
 * Credit-card intelligence calculations: utilization, card health, repayment
 * capacity, multi-card summary, and purchase readiness assessment.
 *
 * <p>All thresholds are <b>PulseWallet's internal planning thresholds</b>
 * and are <em>not</em> official credit-bureau or banking classifications.
 *
 * <p>All money calculations use {@link BigDecimal} with
 * {@link RoundingMode#HALF_UP} — floating-point is never used for money.
 */
@Service
public class CreditCardIntelligenceService {

    private static final int SCALE = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);

    // PulseWallet internal planning thresholds (not official credit-bureau rules)
    private static final BigDecimal THRESHOLD_LOW = new BigDecimal("30");
    private static final BigDecimal THRESHOLD_MODERATE = new BigDecimal("50");
    private static final BigDecimal THRESHOLD_HIGH = new BigDecimal("75");

    private static final int UPCOMING_DUE_DAYS = 7;

    private final CreditCardRepository creditCardRepository;
    private final FinancialPlanService financialPlanService;
    private final Clock clock;

    public CreditCardIntelligenceService(
            CreditCardRepository creditCardRepository,
            FinancialPlanService financialPlanService,
            Clock clock) {
        this.creditCardRepository = creditCardRepository;
        this.financialPlanService = financialPlanService;
        this.clock = clock;
    }

    // -----------------------------------------------------------------------
    // Full intelligence report
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public CreditCardIntelligenceResponse getIntelligence(Long userId) {
        List<CreditCard> cards = creditCardRepository.findAllByUserId(userId);
        LocalDate today = LocalDate.now(clock);

        List<CardHealth> healthList = cards.stream()
                .map(card -> buildCardHealth(card, today))
                .toList();

        CreditCardSummary summary = buildSummary(cards, today);
        RepaymentCapacity repayment = buildRepaymentCapacity(userId, summary.totalOutstanding());

        List<String> warnings = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        for (CardHealth ch : healthList) {
            warnings.addAll(ch.warnings());
            recommendations.addAll(ch.recommendations());
        }

        if (repayment.status().equals("STRETCHED") || repayment.status().equals("HIGH_RISK")) {
            warnings.add("Your overall repayment capacity is " + repayment.status().toLowerCase().replace('_', ' ')
                    + ". Consider reducing card outstanding before additional spending.");
        }

        if (summary.overallUtilization().compareTo(THRESHOLD_HIGH) > 0) {
            recommendations.add(
                    "Overall credit utilization is above 75%. Reducing outstanding balances can improve your financial flexibility.");
        }

        return new CreditCardIntelligenceResponse(healthList, summary, repayment, warnings, recommendations);
    }

    // -----------------------------------------------------------------------
    // Purchase readiness assessment
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PurchaseCheckResponse checkPurchase(Long userId, PurchaseCheckRequest request) {
        List<CreditCard> cards = creditCardRepository.findAllByUserId(userId);

        FinancialPlanResponse plan = financialPlanService.calculate(userId);
        BigDecimal monthlyLeft = scale(plan.monthlyLeft());
        BigDecimal availableAfterPlan = computeAvailableAfterPlan(plan);

        List<CardAssessment> assessments = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        String overallStatus = "SAFE";

        for (CreditCard card : cards) {
            CardAssessment assessment = assessCard(card, request.amount());
            assessments.add(assessment);

            if ("NOT_RECOMMENDED".equals(assessment.safetyStatus())) {
                overallStatus = "NOT_RECOMMENDED";
            } else if ("CAUTION".equals(assessment.safetyStatus()) && !"NOT_RECOMMENDED".equals(overallStatus)) {
                overallStatus = "CAUTION";
            }
        }

        // Financial capacity check
        if (request.amount().compareTo(availableAfterPlan) > 0) {
            warnings.add("This purchase of " + request.amount().setScale(SCALE, RoundingMode.HALF_UP)
                    + " would consume more than your available monthly surplus after financial plan allocations ("
                    + availableAfterPlan + ").");
            if (!"NOT_RECOMMENDED".equals(overallStatus)) {
                overallStatus = "CAUTION";
            }
        }

        if (request.amount().compareTo(monthlyLeft) > 0) {
            warnings.add("This purchase exceeds your current monthly left (" + monthlyLeft
                    + "). It may impact your ability to meet existing financial commitments.");
            overallStatus = "NOT_RECOMMENDED";
        }

        if (cards.isEmpty()) {
            warnings.add("No credit cards are recorded. Add your cards to get per-card assessments.");
        }

        if ("SAFE".equals(overallStatus)) {
            recommendations.add("This purchase appears financially manageable given your current financial capacity.");
        } else if ("CAUTION".equals(overallStatus)) {
            recommendations.add(
                    "Proceed with caution. Consider whether this purchase aligns with your financial priorities.");
        } else {
            recommendations.add(
                    "This purchase is not recommended based on your current financial situation. Consider deferring or reducing the amount.");
        }

        return new PurchaseCheckResponse(
                request.amount(),
                request.category(),
                request.purpose(),
                overallStatus,
                monthlyLeft,
                availableAfterPlan,
                assessments,
                warnings,
                recommendations);
    }

    // -----------------------------------------------------------------------
    // Card-level calculations
    // -----------------------------------------------------------------------

    CardHealth buildCardHealth(CreditCard card, LocalDate today) {
        BigDecimal available = availableCredit(card);
        BigDecimal utilization = utilizationPercentage(card);
        String utilizationLevel = utilizationLevel(utilization);
        String health = healthStatus(utilizationLevel);

        List<String> warnings = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if ("HIGH".equals(utilizationLevel) || "VERY_HIGH".equals(utilizationLevel)) {
            warnings.add("Your utilization on " + card.getCardName() + " (••••" + card.getLastFourDigits()
                    + ") is " + utilizationLevel.toLowerCase().replace('_', ' ')
                    + " relative to its limit.");
        }

        if (card.getDueDate() != null && !card.getDueDate().isBefore(today)
                && card.getDueDate().isBefore(today.plusDays(UPCOMING_DUE_DAYS + 1))
                && card.getCurrentOutstanding().signum() > 0) {
            warnings.add("Payment due date for " + card.getCardName() + " (••••" + card.getLastFourDigits()
                    + ") is within " + UPCOMING_DUE_DAYS + " days.");
        }

        if ("WARNING".equals(health) || "CRITICAL".equals(health)) {
            recommendations.add("Consider reducing the outstanding balance on " + card.getCardName()
                    + " to improve its utilization ratio.");
        }

        if (card.getCurrentOutstanding().signum() == 0) {
            recommendations.add(card.getCardName() + " has zero outstanding — healthy usage.");
        }

        return new CardHealth(
                card.getId(),
                card.getCardName(),
                card.getIssuer(),
                card.getLastFourDigits(),
                card.getCreditLimit(),
                card.getCurrentOutstanding(),
                available,
                utilization,
                utilizationLevel,
                health,
                card.getStatementDate(),
                card.getDueDate(),
                card.getRewardsDescription(),
                card.getBenefitsDescription(),
                warnings,
                recommendations);
    }

    // -----------------------------------------------------------------------
    // Multi-card summary
    // -----------------------------------------------------------------------

    CreditCardSummary buildSummary(List<CreditCard> cards, LocalDate today) {
        if (cards.isEmpty()) {
            return new CreditCardSummary(ZERO, ZERO, ZERO, ZERO, 0, 0, 0);
        }

        BigDecimal totalLimit = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        int needingAttention = 0;
        int upcomingDue = 0;

        for (CreditCard card : cards) {
            totalLimit = totalLimit.add(card.getCreditLimit());
            totalOutstanding = totalOutstanding.add(card.getCurrentOutstanding());

            BigDecimal util = utilizationPercentage(card);
            String level = utilizationLevel(util);
            if ("HIGH".equals(level) || "VERY_HIGH".equals(level)) {
                needingAttention++;
            }

            if (card.getDueDate() != null && !card.getDueDate().isBefore(today)
                    && card.getDueDate().isBefore(today.plusDays(UPCOMING_DUE_DAYS + 1))
                    && card.getCurrentOutstanding().signum() > 0) {
                upcomingDue++;
            }
        }

        BigDecimal totalAvailable = scale(totalLimit.subtract(totalOutstanding));
        BigDecimal overallUtil = totalLimit.signum() == 0 ? ZERO
                : scale(totalOutstanding.multiply(HUNDRED).divide(totalLimit, SCALE, RoundingMode.HALF_UP));

        return new CreditCardSummary(
                scale(totalLimit), scale(totalOutstanding), totalAvailable, overallUtil,
                cards.size(), needingAttention, upcomingDue);
    }

    // -----------------------------------------------------------------------
    // Repayment capacity
    // -----------------------------------------------------------------------

    RepaymentCapacity buildRepaymentCapacity(Long userId, BigDecimal totalOutstanding) {
        FinancialPlanResponse plan = financialPlanService.calculate(userId);
        BigDecimal monthlyLeft = scale(plan.monthlyLeft());
        BigDecimal availableAfterPlan = computeAvailableAfterPlan(plan);

        List<String> explanation = new ArrayList<>();
        String status;

        explanation.add("Monthly income: " + plan.monthlyIncome()
                + ", Monthly expenses: " + plan.monthlyExpenses()
                + ", Monthly left: " + monthlyLeft);
        explanation.add("Goal savings required: " + plan.goalSavingsRequired()
                + ", Emergency allocation: " + plan.recommendedEmergencyAllocation()
                + ", Investment allocation: " + plan.recommendedInvestmentAmount());
        explanation.add("Available after financial plan: " + availableAfterPlan);
        explanation.add("Total card outstanding: " + totalOutstanding);

        if (availableAfterPlan.signum() <= 0) {
            status = "HIGH_RISK";
            explanation.add(
                    "No surplus remains after financial plan allocations. Card repayment may compete with other financial priorities.");
        } else if (totalOutstanding.compareTo(availableAfterPlan) <= 0) {
            status = "COMFORTABLE";
            explanation.add(
                    "Total card outstanding is within available surplus. Repayment appears manageable.");
        } else if (totalOutstanding.compareTo(availableAfterPlan.multiply(BigDecimal.valueOf(2))) <= 0) {
            status = "MANAGEABLE";
            explanation.add(
                    "Total card outstanding exceeds one month's surplus but can be cleared within two months.");
        } else if (totalOutstanding.compareTo(availableAfterPlan.multiply(BigDecimal.valueOf(4))) <= 0) {
            status = "STRETCHED";
            explanation.add(
                    "Total card outstanding would take several months to clear from available surplus.");
        } else {
            status = "HIGH_RISK";
            explanation.add(
                    "Total card outstanding significantly exceeds available monthly surplus. Consider a repayment plan.");
        }

        return new RepaymentCapacity(status, monthlyLeft, availableAfterPlan, totalOutstanding, explanation);
    }

    // -----------------------------------------------------------------------
    // Per-card purchase assessment
    // -----------------------------------------------------------------------

    private CardAssessment assessCard(CreditCard card, BigDecimal purchaseAmount) {
        BigDecimal available = availableCredit(card);
        BigDecimal projectedOutstanding = scale(card.getCurrentOutstanding().add(purchaseAmount));
        BigDecimal projectedUtil = card.getCreditLimit().signum() == 0 ? ZERO
                : scale(projectedOutstanding.multiply(HUNDRED).divide(card.getCreditLimit(), SCALE,
                        RoundingMode.HALF_UP));

        List<String> reasons = new ArrayList<>();
        String safetyStatus;

        if (purchaseAmount.compareTo(available) > 0) {
            safetyStatus = "NOT_RECOMMENDED";
            reasons.add("This purchase of " + purchaseAmount.setScale(SCALE, RoundingMode.HALF_UP)
                    + " exceeds the available credit (" + available + ") on " + card.getCardName() + ".");
        } else if (projectedUtil.compareTo(THRESHOLD_HIGH) > 0) {
            safetyStatus = "CAUTION";
            reasons.add("Adding this purchase would push " + card.getCardName() + " utilization to "
                    + projectedUtil + "%, which is in the high range.");
        } else if (projectedUtil.compareTo(THRESHOLD_MODERATE) > 0) {
            safetyStatus = "CAUTION";
            reasons.add("Projected utilization on " + card.getCardName() + " would be " + projectedUtil
                    + "%, entering moderate territory.");
        } else {
            safetyStatus = "SAFE";
            reasons.add("This purchase appears manageable on " + card.getCardName()
                    + " with projected utilization at " + projectedUtil + "%.");
        }

        return new CardAssessment(
                card.getId(),
                card.getCardName(),
                card.getIssuer(),
                card.getLastFourDigits(),
                card.getCreditLimit(),
                card.getCurrentOutstanding(),
                available,
                projectedOutstanding,
                projectedUtil,
                safetyStatus,
                reasons);
    }

    // -----------------------------------------------------------------------
    // Utility methods
    // -----------------------------------------------------------------------

    BigDecimal availableCredit(CreditCard card) {
        return scale(card.getCreditLimit().subtract(card.getCurrentOutstanding()).max(BigDecimal.ZERO));
    }

    BigDecimal utilizationPercentage(CreditCard card) {
        if (card.getCreditLimit().signum() == 0) {
            return ZERO;
        }
        return scale(card.getCurrentOutstanding().multiply(HUNDRED)
                .divide(card.getCreditLimit(), SCALE, RoundingMode.HALF_UP));
    }

    String utilizationLevel(BigDecimal utilization) {
        if (utilization.compareTo(THRESHOLD_LOW) <= 0) return "LOW";
        if (utilization.compareTo(THRESHOLD_MODERATE) <= 0) return "MODERATE";
        if (utilization.compareTo(THRESHOLD_HIGH) <= 0) return "HIGH";
        return "VERY_HIGH";
    }

    String healthStatus(String utilizationLevel) {
        return switch (utilizationLevel) {
            case "LOW" -> "HEALTHY";
            case "MODERATE" -> "WATCH";
            case "HIGH" -> "WARNING";
            case "VERY_HIGH" -> "CRITICAL";
            default -> "UNKNOWN";
        };
    }

    private BigDecimal computeAvailableAfterPlan(FinancialPlanResponse plan) {
        BigDecimal monthlyLeft = value(plan.monthlyLeft()).max(BigDecimal.ZERO);
        BigDecimal goalSavings = value(plan.goalSavingsRequired());
        BigDecimal emergency = value(plan.recommendedEmergencyAllocation());
        BigDecimal investment = value(plan.recommendedInvestmentAmount());
        return scale(monthlyLeft.subtract(goalSavings).subtract(emergency).subtract(investment).max(BigDecimal.ZERO));
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal scale(BigDecimal amount) {
        return value(amount).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
