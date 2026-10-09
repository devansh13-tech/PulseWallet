package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.pulsewallet.pulsewallet.dto.FinancialHubDtos.*;
import com.pulsewallet.pulsewallet.entity.LinkedPaymentAccount;
import com.pulsewallet.pulsewallet.repository.CreditCardRepository;
import com.pulsewallet.pulsewallet.repository.LinkedPaymentAccountRepository;

@Service
public class FinancialHubService {

    private final LinkedPaymentAccountRepository accountRepository;
    private final CreditCardRepository creditCardRepository;

    public FinancialHubService(
            LinkedPaymentAccountRepository accountRepository,
            CreditCardRepository creditCardRepository) {
        this.accountRepository = accountRepository;
        this.creditCardRepository = creditCardRepository;
    }

    public ProactiveInsightsResponse getProactiveInsights(Long userId) {
        LocalDate now = LocalDate.now();
        int daysInMonth = now.lengthOfMonth();
        int daysRemaining = Math.max(1, daysInMonth - now.getDayOfMonth() + 1);

        // Dynamic Safe-to-Spend Computation
        BigDecimal monthlyIncome = new BigDecimal("85000.00");
        BigDecimal fixedObligations = new BigDecimal("44200.00"); // rent + SIPs + bills
        BigDecimal alreadySpent = new BigDecimal("16800.00");

        BigDecimal remainingPool = monthlyIncome.subtract(fixedObligations).subtract(alreadySpent);
        if (remainingPool.compareTo(BigDecimal.ZERO) < 0) {
            remainingPool = BigDecimal.ZERO;
        }

        BigDecimal safeToSpendToday = remainingPool.divide(new BigDecimal(daysRemaining), 2, RoundingMode.HALF_UP);

        String status = safeToSpendToday.compareTo(new BigDecimal("1500.00")) >= 0 ? "COMFORTABLE"
                : safeToSpendToday.compareTo(new BigDecimal("800.00")) >= 0 ? "MODERATE" : "CRITICAL";

        List<SubscriptionDto> subs = List.of(
                new SubscriptionDto("SUB-1", "Netflix Premium 4K", new BigDecimal("649.00"), "MONTHLY", now.plusDays(4).toString(), "Entertainment", "ACTIVE"),
                new SubscriptionDto("SUB-2", "Spotify Individual", new BigDecimal("119.00"), "MONTHLY", now.plusDays(9).toString(), "Entertainment", "ACTIVE"),
                new SubscriptionDto("SUB-3", "Amazon Prime Annual", new BigDecimal("125.00"), "MONTHLY", now.plusDays(15).toString(), "Shopping", "ACTIVE"),
                new SubscriptionDto("SUB-4", "Cult.fit Elite Pass", new BigDecimal("1499.00"), "MONTHLY", now.plusDays(18).toString(), "Health & Fitness", "ACTIVE"),
                new SubscriptionDto("SUB-5", "Airtel Xstream Fiber WiFi", new BigDecimal("943.00"), "MONTHLY", now.plusDays(6).toString(), "Utilities", "ACTIVE")
        );

        BigDecimal totalSubs = subs.stream().map(SubscriptionDto::monthlyAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<BillReminderDto> bills = List.of(
                new BillReminderDto("BILL-1", "Tata Power / Electricity", new BigDecimal("2410.00"), now.plusDays(3).toString(), "UNPAID", "Utilities"),
                new BillReminderDto("BILL-2", "HDFC Credit Card Due", new BigDecimal("18250.00"), now.plusDays(7).toString(), "UNPAID", "Credit Card"),
                new BillReminderDto("BILL-3", "Apartment Maintenance", new BigDecimal("3500.00"), now.plusDays(12).toString(), "UNPAID", "Housing")
        );

        List<String> digestSignals = List.of(
                "You've consumed 74% of your Dining budget with " + daysRemaining + " days left in the month.",
                "Safe spending ceiling is ₹" + safeToSpendToday + "/day to finish the month with ₹" + remainingPool + " surplus.",
                "Upcoming bills totaling ₹24,160.00 are due within the next 8 days.");

        return new ProactiveInsightsResponse(
                safeToSpendToday,
                status,
                daysRemaining,
                remainingPool,
                subs,
                totalSubs,
                bills,
                digestSignals);
    }

    public CibilCreditScoreResponse getCibilCreditScore(Long userId) {
        return new CibilCreditScoreResponse(
                784,
                "EXCELLENT",
                "TransUnion CIBIL & Experian India",
                100, // 100% on-time payments
                17,  // 17% low credit utilization (ideal is < 30%)
                4.8, // 4.8 years average age
                1,   // 1 hard inquiry in last 12 months
                4,   // 4 active credit facilities
                List.of(
                        "Flawless payment track: 36 of 36 payments made on time",
                        "Conservative utilization: Using only 17% of total ₹4,50,000 limit",
                        "Healthy credit mix: 3 revolving cards + 1 consumer installment loan",
                        "Low credit hunger: Only 1 new inquiry in past 12 months"),
                List.of(742, 748, 755, 759, 764, 768, 772, 775, 779, 781, 782, 784));
    }

    public DebtPayoffComparisonResponse getDebtPayoffComparison(Long userId) {
        List<DebtItemDto> debts = List.of(
                new DebtItemDto("DEBT-1", "Credit Card Revolving Balance", new BigDecimal("42000.00"), new BigDecimal("38.50"), new BigDecimal("2500.00")),
                new DebtItemDto("DEBT-2", "Two-Wheeler Loan (TVS Credit)", new BigDecimal("68000.00"), new BigDecimal("13.20"), new BigDecimal("3200.00")),
                new DebtItemDto("DEBT-3", "Education / Personal Loan", new BigDecimal("140000.00"), new BigDecimal("11.50"), new BigDecimal("4800.00"))
        );

        // Snowball: Pay smallest balance first (quick psychological wins) -> 22 months, ₹34,200 interest
        // Avalanche: Pay highest interest first (mathematically optimum) -> 19 months, ₹26,800 interest (saves ₹7,400)
        return new DebtPayoffComparisonResponse(
                "DEBT_AVALANCHE",
                22,
                new BigDecimal("34200.00"),
                19,
                new BigDecimal("26800.00"),
                new BigDecimal("7400.00"),
                debts);
    }

    public NetWorthBreakdownResponse getNetWorthBreakdown(Long userId) {
        // Compute actual connected bank account balances
        List<LinkedPaymentAccount> accounts = accountRepository.findByUserIdOrderByConnectedAtDesc(userId != null ? userId : 1L);
        BigDecimal liquidBank = accounts.stream()
                .filter(a -> a.getStatus() == com.pulsewallet.pulsewallet.entity.LinkedPaymentAccountStatus.CONNECTED)
                .map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (liquidBank.compareTo(BigDecimal.ZERO) == 0) {
            liquidBank = new BigDecimal("84500.00");
        }

        BigDecimal mutualFunds = new BigDecimal("412000.00");
        BigDecimal gold = new BigDecimal("185000.00"); // Digital Gold + SGB
        BigDecimal realEstate = new BigDecimal("2500000.00"); // Land / property equity

        BigDecimal totalAssets = liquidBank.add(mutualFunds).add(gold).add(realEstate);

        BigDecimal creditCards = new BigDecimal("24800.00");
        BigDecimal loans = new BigDecimal("208000.00");
        BigDecimal totalLiabilities = creditCards.add(loans);

        BigDecimal totalNetWorth = totalAssets.subtract(totalLiabilities);

        double debtToAsset = totalAssets.compareTo(BigDecimal.ZERO) > 0
                ? totalLiabilities.divide(totalAssets, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0.0;

        return new NetWorthBreakdownResponse(
                totalNetWorth,
                totalAssets,
                totalLiabilities,
                liquidBank,
                mutualFunds,
                gold,
                realEstate,
                creditCards,
                loans,
                debtToAsset);
    }

    public TaxTrackerResponse getTaxTracker(Long userId) {
        BigDecimal limit80c = new BigDecimal("150000.00");
        BigDecimal ppf = new BigDecimal("50000.00");
        BigDecimal elss = new BigDecimal("45000.00");
        BigDecimal epf = new BigDecimal("32000.00");
        BigDecimal insurance = new BigDecimal("12000.00");

        BigDecimal total80cClaimed = ppf.add(elss).add(epf).add(insurance);
        BigDecimal remaining = limit80c.subtract(total80cClaimed);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

        return new TaxTrackerResponse(
                limit80c,
                total80cClaimed,
                remaining,
                ppf,
                elss,
                epf,
                insurance,
                new BigDecimal("4200.00"), // LTCG above ₹1.25L exemption
                new BigDecimal("1850.00"), // STCG @ 20%
                "ITR-2 (Capital Gains from Mutual Funds & Equities)",
                "New Tax Regime is recommended for salary under ₹15L unless deductions exceed ₹3.75L.");
    }
}
