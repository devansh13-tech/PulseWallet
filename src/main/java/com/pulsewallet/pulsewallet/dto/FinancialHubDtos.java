package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.util.List;

public class FinancialHubDtos {

    public record SubscriptionDto(
            String id,
            String name,
            BigDecimal monthlyAmount,
            String billingCycle, // MONTHLY, QUARTERLY, ANNUAL
            String nextDueDate,
            String category,
            String status) {}

    public record BillReminderDto(
            String id,
            String billerName,
            BigDecimal amount,
            String dueDate,
            String status, // UNPAID, PAID
            String category) {}

    public record ProactiveInsightsResponse(
            BigDecimal safeToSpendToday,
            String safeSpendStatus, // COMFORTABLE, MODERATE, CRITICAL
            int daysRemainingInMonth,
            BigDecimal projectedMonthEndSurplus,
            List<SubscriptionDto> detectedSubscriptions,
            BigDecimal totalMonthlySubscriptions,
            List<BillReminderDto> upcomingBills,
            List<String> weeklyDigestSignals) {}

    public record DebtItemDto(
            String id,
            String debtName,
            BigDecimal currentBalance,
            BigDecimal interestRatePercent,
            BigDecimal minimumMonthlyEmi) {}

    public record DebtPayoffComparisonResponse(
            String recommendedStrategy,
            int snowballMonthsToDebtFree,
            BigDecimal snowballTotalInterest,
            int avalancheMonthsToDebtFree,
            BigDecimal avalancheTotalInterest,
            BigDecimal interestSavedWithAvalanche,
            List<DebtItemDto> userDebts) {}

    public record CibilCreditScoreResponse(
            int score,
            String scoreBand, // EXCELLENT, GOOD, FAIR, POOR
            String provider, // CIBIL / Experian
            int paymentHistoryPercent,
            int creditUtilizationPercent,
            double averageCreditAgeYears,
            int hardInquiriesCount,
            int activeAccountsCount,
            List<String> scoreFactors,
            List<Integer> historyTrend) {}

    public record NetWorthBreakdownResponse(
            BigDecimal totalNetWorth,
            BigDecimal totalAssets,
            BigDecimal totalLiabilities,
            BigDecimal liquidBankBalances,
            BigDecimal mutualFundsAndEquities,
            BigDecimal goldHoldings,
            BigDecimal realEstateProperty,
            BigDecimal creditCardOutstanding,
            BigDecimal loansOutstanding,
            double debtToAssetRatio) {}

    public record TaxTrackerResponse(
            BigDecimal section80cMaxLimit,
            BigDecimal section80cClaimed,
            BigDecimal section80cRemaining,
            BigDecimal ppfContribution,
            BigDecimal elssContribution,
            BigDecimal epfContribution,
            BigDecimal lifeInsuranceContribution,
            BigDecimal estimatedLtcgTax,
            BigDecimal estimatedStcgTax,
            String itrFormSuggestion,
            String taxRegimeComparison) {}
}
