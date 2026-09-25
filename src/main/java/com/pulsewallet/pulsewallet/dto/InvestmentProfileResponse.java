package com.pulsewallet.pulsewallet.dto;

import com.pulsewallet.pulsewallet.entity.InvestmentProfile;
import com.pulsewallet.pulsewallet.entity.InvestmentRiskPreference;

public record InvestmentProfileResponse(
        Long id,
        InvestmentRiskPreference riskPreference,
        Integer timeHorizonYears) {

    public static InvestmentProfileResponse from(InvestmentProfile profile) {
        return new InvestmentProfileResponse(profile.getId(), profile.getRiskPreference(),
                profile.getTimeHorizonYears());
    }
}
