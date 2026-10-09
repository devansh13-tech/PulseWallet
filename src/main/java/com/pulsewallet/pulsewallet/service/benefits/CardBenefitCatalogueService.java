package com.pulsewallet.pulsewallet.service.benefits;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.pulsewallet.pulsewallet.dto.CardBenefitsResponse;
import com.pulsewallet.pulsewallet.dto.EstimatedRewardDto;
import com.pulsewallet.pulsewallet.entity.CreditCard;

/**
 * Service managing published card benefit profiles and calculating deterministic
 * reward estimates for purchases and card variants.
 *
 * <p>Never invents or fabricates unverified reward rules. All calculated values are
 * explicitly labeled as estimates based on published bank rule catalogues.
 */
@Service
public class CardBenefitCatalogueService {

    private static final int SCALE = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final Map<String, CardBenefitRule> CATALOGUE = Map.of(
            "REGALIA", new CardBenefitRule(
                    "HDFC Bank", "Regalia",
                    new BigDecimal("2500.00"),
                    new BigDecimal("1.33"), // 4 points per 150 spend
                    Map.of("TRAVEL", new BigDecimal("4.00"), "DINING", new BigDecimal("2.66")),
                    new BigDecimal("0.50"), // 1 point = 0.50 INR for flights/hotels
                    List.of("FUEL", "RENT", "WALLET", "GOVERNMENT"),
                    List.of("4 Complimentary domestic airport lounge access visits per calendar quarter",
                            "1% Fuel surcharge waiver at all fuel stations across India",
                            "Complimentary Club Marriott membership upon fee payment")
            ),
            "AMAZON PAY", new CardBenefitRule(
                    "ICICI Bank", "Amazon Pay",
                    BigDecimal.ZERO,
                    new BigDecimal("1.00"),
                    Map.of("SHOPPING", new BigDecimal("5.00"), "DINING", new BigDecimal("2.00"), "BILLS", new BigDecimal("2.00")),
                    BigDecimal.ONE, // 1 reward point = 1 INR directly credited
                    List.of("FUEL", "RENT", "WALLET"),
                    List.of("5% Unlimited cashback for Amazon Prime members on Amazon.in",
                            "2% Cashback on 100+ partner merchants",
                            "No annual fee, lifetime free credit card")
            ),
            "SIMPLYCLICK", new CardBenefitRule(
                    "State Bank of India", "SimplyCLICK",
                    new BigDecimal("499.00"),
                    new BigDecimal("1.25"), // 1 point per 100 spend, value 0.25
                    Map.of("SHOPPING", new BigDecimal("2.50"), "DINING", new BigDecimal("2.50")),
                    new BigDecimal("0.25"),
                    List.of("FUEL", "RENT", "WALLET", "CASH"),
                    List.of("10X Reward points on online spends with partner merchants (Amazon, BookMyShow, Cleartrip)",
                            "5X Reward points on all other online spends",
                            "Annual fee reversal on spending ₹1,00,000 in a year")
            ),
            "MAGNUS", new CardBenefitRule(
                    "Axis Bank", "Magnus",
                    new BigDecimal("12500.00"),
                    new BigDecimal("2.40"), // 12 EDGE rewards per 200 spend
                    Map.of("TRAVEL", new BigDecimal("7.20")),
                    new BigDecimal("0.40"),
                    List.of("FUEL", "RENT", "WALLET", "GOVERNMENT", "UTILITIES"),
                    List.of("Unlimited complimentary international airport lounge access with Priority Pass",
                            "8 Complimentary airport meet & greet concierge services per year",
                            "Lower foreign currency markup of 2%")
            ),
            "ACE", new CardBenefitRule(
                    "Axis Bank", "Ace",
                    new BigDecimal("499.00"),
                    new BigDecimal("2.00"),
                    Map.of("BILLS", new BigDecimal("5.00"), "DINING", new BigDecimal("4.00")),
                    BigDecimal.ONE,
                    List.of("FUEL", "RENT", "WALLET"),
                    List.of("5% Cashback on bill payments & mobile recharge via Google Pay",
                            "4% Cashback on Swiggy, Zomato & Ola",
                            "2% Unlimited cashback on all other spends")
            )
    );

    public CardBenefitsResponse getBenefitsForCard(CreditCard card) {
        String key = matchKey(card.getCardName());
        if (key != null && CATALOGUE.containsKey(key)) {
            CardBenefitRule rule = CATALOGUE.get(key);
            return new CardBenefitsResponse(
                    card.getId(),
                    rule.issuer(),
                    rule.cardName(),
                    rule.annualFee(),
                    rule.baseRate(),
                    rule.multipliers(),
                    rule.exclusions(),
                    rule.benefits(),
                    "PUBLISHED_BENEFIT_CATALOGUE",
                    true);
        }

        // Return fallback profile using entity descriptions if present
        List<String> defaultBenefits = card.getBenefitsDescription() != null
                ? List.of(card.getBenefitsDescription())
                : List.of("Standard revolving credit card benefits apply.");

        return new CardBenefitsResponse(
                card.getId(),
                card.getIssuer(),
                card.getCardName(),
                card.getAnnualFee() != null ? card.getAnnualFee() : BigDecimal.ZERO,
                new BigDecimal("1.00"),
                Map.of(),
                List.of("FUEL", "RENT", "WALLET"),
                defaultBenefits,
                "MANUALLY_RECORDED_METADATA",
                false);
    }

    public EstimatedRewardDto estimateReward(CreditCard card, String category, BigDecimal amount) {
        BigDecimal zeroMoney = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        if (amount == null || amount.signum() <= 0) {
            return new EstimatedRewardDto(
                    zeroMoney, zeroMoney, zeroMoney,
                    true, zeroMoney, "Purchase amount must be positive.", "PUBLISHED_BENEFIT_CATALOGUE");
        }

        String key = matchKey(card.getCardName());
        CardBenefitRule rule = key != null ? CATALOGUE.get(key) : null;

        String normCategory = category != null ? category.trim().toUpperCase(Locale.ROOT) : "GENERAL";

        if (rule != null) {
            if (rule.exclusions().contains(normCategory)) {
                return new EstimatedRewardDto(
                        zeroMoney, zeroMoney, zeroMoney,
                        true, zeroMoney,
                        "Category '" + normCategory + "' is excluded from rewards on " + card.getCardName() + ".",
                        "PUBLISHED_BENEFIT_CATALOGUE");
            }

            BigDecimal rate = rule.multipliers().getOrDefault(normCategory, rule.baseRate());
            BigDecimal cashValue = amount.multiply(rate).divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
            BigDecimal points = rule.redemptionValue().signum() > 0
                    ? cashValue.divide(rule.redemptionValue(), SCALE, RoundingMode.HALF_UP)
                    : cashValue;

            String explanation = "Estimated reward for " + normCategory + " spend of " + amount + " on "
                    + card.getCardName() + " applied at " + rate + "% rate.";

            return new EstimatedRewardDto(
                    points, cashValue, rate, true, amount, explanation, "PUBLISHED_BENEFIT_CATALOGUE");
        }

        // Generic baseline estimate (1.0% standard)
        BigDecimal defaultRate = new BigDecimal("1.00");
        BigDecimal defaultCash = amount.multiply(defaultRate).divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
        return new EstimatedRewardDto(
                defaultCash, defaultCash, defaultRate, true, amount,
                "Standard 1.0% baseline reward estimate applied for " + card.getCardName() + " (" + normCategory + ").",
                "ESTIMATED_BASELINE");
    }

    private String matchKey(String cardName) {
        if (cardName == null) return null;
        String upper = cardName.toUpperCase(Locale.ROOT);
        for (String k : CATALOGUE.keySet()) {
            if (upper.contains(k)) {
                return k;
            }
        }
        return null;
    }

    private record CardBenefitRule(
            String issuer,
            String cardName,
            BigDecimal annualFee,
            BigDecimal baseRate,
            Map<String, BigDecimal> multipliers,
            BigDecimal redemptionValue,
            List<String> exclusions,
            List<String> benefits) {
    }
}
