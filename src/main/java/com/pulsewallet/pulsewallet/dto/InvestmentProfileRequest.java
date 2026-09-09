package com.pulsewallet.pulsewallet.dto;

import com.pulsewallet.pulsewallet.entity.InvestmentRiskPreference;

public record InvestmentProfileRequest(
        InvestmentRiskPreference riskPreference,
        Integer timeHorizonYears) {
}
